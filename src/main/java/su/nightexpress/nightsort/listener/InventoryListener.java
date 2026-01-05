package su.nightexpress.nightsort.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import su.nightexpress.nightsort.service.ISortService;
import su.nightexpress.nightsort.storage.IStorage;

public class InventoryListener implements Listener {

    private final ISortService sortService;
    private final IStorage storage;

    public InventoryListener(ISortService sortService, IStorage storage) {
        this.sortService = sortService;
        this.storage = storage;
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        sortService.scheduleSort(e.getInventory(), e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent e) {
        sortService.scheduleSort(e.getInventory(), e.getPlayer());
    }
}