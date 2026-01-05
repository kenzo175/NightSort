package su.nightexpress.nightsort.service.sort;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import su.nightexpress.nightsort.service.IDebounceService;
import su.nightexpress.nightsort.service.ISortService;
import su.nightexpress.nightsort.storage.IStorage;
import su.nightexpress.nightsort.utils.InventoryTitleResolver;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class SortService implements ISortService {

    private final Plugin plugin;
    private final IDebounceService debounce;
    private final IStorage storage;

    private static final long IDLE_TICKS = 8L;
    private static final int MAX_CONCURRENT_SORTS = 4;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_TICKS = 2L;

    private final Set<InventoryType> allowed = EnumSet.of(
            InventoryType.CHEST,
            InventoryType.BARREL,
            InventoryType.ENDER_CHEST,
            InventoryType.SHULKER_BOX
    );

    private final Map<Material, Integer> materialIndex = new EnumMap<>(Material.class);
    private final Comparator<SnapshotEntry> snapshotComparator;
    private final ConcurrentMap<Integer, Integer> scheduledTaskIds = new ConcurrentHashMap<>();
    private final Semaphore concurrencyLimiter = new Semaphore(MAX_CONCURRENT_SORTS);
    private final ConcurrentLinkedQueue<SnapshotJob> pendingQueue = new ConcurrentLinkedQueue<>();
    private final String inventoriesText;

    private static final Method INVENTORYVIEW_GETTITLE;
    private static final boolean INVENTORYVIEW_TITLE_IS_COMPONENT;
    static {
        Method m = null;
        boolean comp = false;
        try {
            m = InventoryView.class.getMethod("getTitle");
            comp = m.getReturnType().getName().contains("Component") || m.getReturnType().equals(net.kyori.adventure.text.Component.class);
        } catch (NoSuchMethodException ignored) {}
        INVENTORYVIEW_GETTITLE = m;
        INVENTORYVIEW_TITLE_IS_COMPONENT = comp;
    }

    public SortService(Plugin plugin, IDebounceService debounce, IStorage storage) {
        this.plugin = plugin;
        this.debounce = debounce;
        this.storage = storage;

        int idx = 0;
        for (Material m : Material.values()) materialIndex.put(m, idx++);

        this.snapshotComparator = Comparator
                .comparingInt((SnapshotEntry e) -> e.materialRank)
                .thenComparing(e -> e.namespace)
                .thenComparing(e -> e.metaKey)
                .thenComparingInt(e -> e.amount);

        this.inventoriesText = allowed.stream()
                .map(InventoryTitleResolver::resolve)
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    @Override
    public void scheduleSort(Inventory inv, HumanEntity viewer) {
        if (inv == null) return;
        if (!(viewer instanceof Player)) return;
        Player player = (Player) viewer;

        InventoryView view = player.getOpenInventory();
        if (view == null) return;
        Inventory top = view.getTopInventory();
        if (top == null || top != inv) return;

        if (!allowed.contains(inv.getType())) return;
        if (!storage.isActive(player.getUniqueId())) return;
        if (inv.getViewers().size() > 1) return;

        final int invKey = System.identityHashCode(inv);
        if (!debounce.tryStart(invKey)) return;

        cancelScheduledTaskIfPresent(invKey);

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            scheduledTaskIds.remove(invKey);

            InventoryView nowView = player.getOpenInventory();
            if (nowView == null) {
                debounce.finish(invKey);
                return;
            }
            Inventory nowTop = nowView.getTopInventory();
            if (nowTop != inv) {
                debounce.finish(invKey);
                return;
            }
            if (!allowed.contains(inv.getType())) {
                debounce.finish(invKey);
                return;
            }
            if (inv.getViewers().size() > 1) {
                debounce.finish(invKey);
                return;
            }
            if (!storage.isActive(player.getUniqueId())) {
                debounce.finish(invKey);
                return;
            }

            ItemStack[] contents = inv.getContents();
            List<SnapshotEntry> snapshots = new ArrayList<>(Math.max(8, contents.length));
            for (ItemStack it : contents) {
                if (it == null || it.getType() == Material.AIR) continue;
                ItemMeta meta = it.getItemMeta();
                String metaStr = (meta != null) ? meta.getAsString() : "";
                int rank = materialIndex.getOrDefault(it.getType(), 0);
                String namespace = it.getType().getKey().getNamespace();
                snapshots.add(new SnapshotEntry(it, rank, namespace, metaStr, it.getAmount()));
            }

            if (snapshots.size() < 2) {
                debounce.finish(invKey);
                return;
            }

            Map<String, Integer> snapshotCounts = buildCountsFromSnapshots(snapshots);
            long snapshotHash = computeHashFromSnapshots(snapshots);

            SnapshotJob job = new SnapshotJob(inv, player, snapshots, snapshotCounts, snapshotHash, invKey);

            if (concurrencyLimiter.tryAcquire()) {
                startAsyncSort(job);
            } else {
                pendingQueue.offer(job);
            }
        }, IDLE_TICKS).getTaskId();

        scheduledTaskIds.put(invKey, taskId);
    }

    private void cancelScheduledTaskIfPresent(int invKey) {
        Integer id = scheduledTaskIds.remove(invKey);
        if (id != null) {
            try { Bukkit.getScheduler().cancelTask(id); } catch (Exception ignored) {}
        }
    }

    private void startAsyncSort(SnapshotJob job) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                job.snapshots.sort(snapshotComparator);
            } catch (Throwable t) {
                plugin.getLogger().warning("Async sort failed: " + t.getMessage());
            }
            attemptApply(job, 0);
        });
    }

    private void attemptApply(SnapshotJob job, int attempt) {
        long delay = (attempt == 0) ? 1L : RETRY_DELAY_TICKS;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                Inventory inv = job.inv;
                Player player = job.player;
                int key = job.invKey;

                InventoryView nowView = player.getOpenInventory();
                if (nowView == null || nowView.getTopInventory() != inv) {
                    finishJobAndDispatchNext(key);
                    return;
                }

                if (!allowed.contains(inv.getType())) {
                    finishJobAndDispatchNext(key);
                    return;
                }
                if (inv.getViewers().size() > 1) {
                    finishJobAndDispatchNext(key);
                    return;
                }
                if (!storage.isActive(player.getUniqueId())) {
                    finishJobAndDispatchNext(key);
                    return;
                }

                ItemStack cursor = player.getItemOnCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    if (attempt < MAX_RETRIES) { attemptApply(job, attempt + 1); return; }
                    else { finishJobAndDispatchNext(key); return; }
                }

                long currentHash = computeHashFromInventory(inv);
                if (currentHash != job.snapshotHash) {
                    if (attempt < MAX_RETRIES) { attemptApply(job, attempt + 1); return; }
                    else { finishJobAndDispatchNext(key); return; }
                }

                Map<String, Integer> currentCounts = buildCountsFromInventory(inv);
                if (Objects.equals(job.snapshotCounts, currentCounts)) {
                    ItemStack[] newContents = new ItemStack[inv.getSize()];
                    int slot = 0;
                    for (SnapshotEntry e : job.snapshots) {
                        if (slot >= newContents.length) break;
                        newContents[slot++] = e.original;
                    }
                    inv.setContents(newContents);
                    finishJobAndDispatchNext(key);
                    return;
                } else {
                    if (attempt < MAX_RETRIES) { attemptApply(job, attempt + 1); return; }
                    else { finishJobAndDispatchNext(key); return; }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("apply attempt failed: " + t.getMessage());
                finishJobAndDispatchNext(job.invKey);
            }
        }, delay);
    }

    private void finishJobAndDispatchNext(int invKey) {
        try { debounce.finish(invKey); } finally {
            concurrencyLimiter.release();
            SnapshotJob next = pendingQueue.poll();
            if (next != null) {
                if (concurrencyLimiter.tryAcquire()) startAsyncSort(next);
                else pendingQueue.offer(next);
            }
        }
    }

    private long computeHashFromSnapshots(Collection<SnapshotEntry> snaps) {
        long h = 0xcbf29ce484222325L;
        final long prime = 0x100000001b3L;
        for (SnapshotEntry e : snaps) {
            long key = (((long) e.materialRank) << 32) ^ (e.metaKey.hashCode() & 0xffffffffL);
            h ^= key;
            h *= prime;
            h ^= e.amount;
            h *= prime;
        }
        return h;
    }

    private long computeHashFromInventory(Inventory inv) {
        long h = 0xcbf29ce484222325L;
        final long prime = 0x100000001b3L;
        for (ItemStack it : inv.getContents()) {
            if (it == null || it.getType() == Material.AIR) continue;
            ItemMeta meta = it.getItemMeta();
            String metaStr = (meta != null) ? meta.getAsString() : "";
            int materialRank = materialIndex.getOrDefault(it.getType(), 0);
            long key = (((long) materialRank) << 32) ^ (metaStr.hashCode() & 0xffffffffL);
            h ^= key;
            h *= prime;
            h ^= it.getAmount();
            h *= prime;
        }
        return h;
    }

    private Map<String, Integer> buildCountsFromSnapshots(Collection<SnapshotEntry> snaps) {
        Map<String, Integer> counts = new HashMap<>(Math.max(16, snaps.size() * 2));
        for (SnapshotEntry e : snaps) {
            counts.merge(e.countKey, e.amount, Integer::sum);
        }
        return counts;
    }

    private Map<String, Integer> buildCountsFromInventory(Inventory inv) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack it : inv.getContents()) {
            if (it == null || it.getType() == Material.AIR) continue;
            ItemMeta meta = it.getItemMeta();
            String metaStr = (meta != null) ? meta.getAsString() : "";
            String key = it.getType().name() + "|" + metaStr;
            counts.merge(key, it.getAmount(), Integer::sum);
        }
        return counts;
    }

    @Override
    public String getInventoriesText() {
        return inventoriesText;
    }

    private static final class SnapshotEntry {
        final ItemStack original;
        final int materialRank;
        final String namespace;
        final String metaKey;
        final int amount;
        final String countKey;

        SnapshotEntry(ItemStack original, int materialRank, String namespace, String metaKey, int amount) {
            this.original = original;
            this.materialRank = materialRank;
            this.namespace = namespace == null ? "" : namespace;
            this.metaKey = metaKey == null ? "" : metaKey;
            this.amount = amount;
            this.countKey = original.getType().name() + "|" + this.metaKey;
        }
    }

    private static final class SnapshotJob {
        final Inventory inv;
        final Player player;
        final List<SnapshotEntry> snapshots;
        final Map<String, Integer> snapshotCounts;
        final long snapshotHash;
        final int invKey;

        SnapshotJob(Inventory inv, Player player, List<SnapshotEntry> snapshots, Map<String, Integer> snapshotCounts, long snapshotHash, int invKey) {
            this.inv = inv;
            this.player = player;
            this.snapshots = snapshots;
            this.snapshotCounts = snapshotCounts;
            this.snapshotHash = snapshotHash;
            this.invKey = invKey;
        }
    }
}