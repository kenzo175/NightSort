package su.nightexpress.nightsort.service.sort.impl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import su.nightexpress.nightsort.model.SnapshotEntry;
import su.nightexpress.nightsort.model.SnapshotJob;
import su.nightexpress.nightsort.service.IDebounceService;
import su.nightexpress.nightsort.storage.IStorage;

import java.util.Map;
import java.util.Objects;

public final class ApplyScheduler {

    private final Plugin plugin;
    private final IStorage storage;
    private final IDebounceService debounce;
    private final JobQueue jobQueue;

    private final int maxRetries;
    private final long retryDelayTicks;

    public ApplyScheduler(Plugin plugin, IStorage storage, IDebounceService debounce, JobQueue jobQueue, int maxRetries, long retryDelayTicks) {
        this.plugin = plugin;
        this.storage = storage;
        this.debounce = debounce;
        this.jobQueue = jobQueue;
        this.maxRetries = maxRetries;
        this.retryDelayTicks = retryDelayTicks;
    }

    public void attemptApply(SnapshotJob job, int attempt) {
        long delay = (attempt == 0) ? 1L : retryDelayTicks;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                Inventory inv = job.inv;
                var player = job.player;
                int key = job.invKey;

                if (inv == null || player == null) {
                    finishFailure(key);
                    return;
                }
                if (inv.getViewers().size() > 1) {
                    finishFailure(key);
                    return;
                }
                if (!storage.isActive(player.getUniqueId())) {
                    finishFailure(key);
                    return;
                }

                ItemStack cursor = player.getItemOnCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    if (attempt < maxRetries) {
                        attemptApply(job, attempt + 1);
                        return;
                    } else {
                        finishFailure(key);
                        return;
                    }
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
                    finishSuccess(key);
                    return;
                } else {
                    if (attempt < maxRetries) {
                        attemptApply(job, attempt + 1);
                        return;
                    } else {
                        finishFailure(key);
                        return;
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("apply attempt failed: " + t.getMessage());
                finishFailure(job.invKey);
            }
        }, delay);
    }

    private void finishSuccess(int invKey) {
        debounce.finish(invKey);
        jobQueue.onJobFinished(invKey);
    }

    private void finishFailure(int invKey) {
        debounce.finish(invKey);
        jobQueue.onJobFinished(invKey);
    }

    private Map<String, Integer> buildCountsFromInventory(Inventory inv) {
        Map<String, Integer> counts = new java.util.HashMap<>();
        for (ItemStack it : inv.getContents()) {
            if (it == null || it.getType() == Material.AIR) continue;
            ItemMeta meta = it.getItemMeta();
            String metaStr = (meta != null) ? meta.getAsString() : "";
            String key = it.getType().name() + "|" + metaStr;
            counts.merge(key, it.getAmount(), Integer::sum);
        }
        return counts;
    }
}