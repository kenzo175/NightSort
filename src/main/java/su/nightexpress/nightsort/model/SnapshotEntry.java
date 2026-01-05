package su.nightexpress.nightsort.model;

import org.bukkit.inventory.ItemStack;

public final class SnapshotEntry {
    public final ItemStack original;
    public final int materialRank;
    public final String namespace;
    public final String metaKey;
    public final int amount;
    public final String countKey;

    public SnapshotEntry(ItemStack original, int materialRank, String namespace, String metaKey, int amount) {
        this.original = original;
        this.materialRank = materialRank;
        this.namespace = namespace == null ? "" : namespace;
        this.metaKey = metaKey == null ? "" : metaKey;
        this.amount = amount;
        this.countKey = original.getType().name() + "|" + this.metaKey;
    }
}