package su.nightexpress.nightsort.service.option;

import org.bukkit.event.inventory.InventoryType;

import java.util.Set;

public final class ConfigOptions {
    public final int idleTicks;
    public final int maxConcurrentSorts;
    public final int maxRetries;
    public final int retryDelayTicks;
    public final Set<InventoryType> allowedTypes;

    public ConfigOptions(int idleTicks, int maxConcurrentSorts, int maxRetries, int retryDelayTicks, Set<InventoryType> allowedTypes) {
        this.idleTicks = Math.max(0, idleTicks);
        this.maxConcurrentSorts = Math.max(1, maxConcurrentSorts);
        this.maxRetries = Math.max(0, maxRetries);
        this.retryDelayTicks = Math.max(0, retryDelayTicks);
        this.allowedTypes = allowedTypes;
    }
}