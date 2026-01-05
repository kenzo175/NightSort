package su.nightexpress.nightsort.model;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

public final class SnapshotBuilder {

    private final Map<Material, Integer> materialIndex;
    private final Plugin plugin;

    public SnapshotBuilder(Plugin plugin, Map<Material, Integer> materialIndex) {
        this.plugin = plugin;
        this.materialIndex = materialIndex;
    }

    public SnapshotJob build(Inventory inv, Player player) {
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

        if (snapshots.size() < 2) return null;

        Map<String, Integer> snapshotCounts = buildCountsFromSnapshots(snapshots);
        int invKey = System.identityHashCode(inv);
        return new SnapshotJob(inv, player, snapshots, snapshotCounts, invKey);
    }

    private Map<String, Integer> buildCountsFromSnapshots(Collection<SnapshotEntry> snaps) {
        Map<String, Integer> counts = new HashMap<>(Math.max(16, snaps.size() * 2));
        for (SnapshotEntry e : snaps) {
            counts.merge(e.countKey, e.amount, Integer::sum);
        }
        return counts;
    }
}