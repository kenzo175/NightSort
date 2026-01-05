package su.nightexpress.nightsort.model;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

public final class SnapshotJob {
    public final Inventory inv;
    public final Player player;
    public final List<SnapshotEntry> snapshots;
    public final Map<String, Integer> snapshotCounts;
    public final int invKey;

    public SnapshotJob(Inventory inv, Player player, List<SnapshotEntry> snapshots, Map<String, Integer> snapshotCounts, int invKey) {
        this.inv = inv;
        this.player = player;
        this.snapshots = snapshots;
        this.snapshotCounts = snapshotCounts;
        this.invKey = invKey;
    }
}