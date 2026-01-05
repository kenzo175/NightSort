package su.nightexpress.nightsort.service;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;

public interface ISortService {
    void scheduleSort(Inventory inventory, HumanEntity viewer);
    String getInventoriesText();
}