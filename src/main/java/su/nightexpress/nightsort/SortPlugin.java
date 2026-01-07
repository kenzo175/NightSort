package su.nightexpress.nightsort;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.plugin.java.JavaPlugin;
import su.nightexpress.nightsort.command.CommandHandler;
import su.nightexpress.nightsort.listener.InventoryListener;
import su.nightexpress.nightsort.service.*;
import su.nightexpress.nightsort.service.messaging.DebounceService;
import su.nightexpress.nightsort.service.messaging.MessageService;
import su.nightexpress.nightsort.service.sort.SortService;
import su.nightexpress.nightsort.storage.FileStorage;
import su.nightexpress.nightsort.storage.IStorage;

public final class SortPlugin extends JavaPlugin {

    private IMessageService messages;
    private IStorage storage;
    private IDebounceService debounce;
    private SortService sortService;
    private CommandHandler commandHandler;
    private InventoryListener inventoryListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messages = new MessageService(this);
        this.storage = new FileStorage(this);
        this.debounce = new DebounceService();

        ConfigOptions opts = readConfigOptions();

        this.sortService = new SortService(this, debounce, storage, opts);

        this.commandHandler = new CommandHandler(this, storage, messages, sortService);

        getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS,
                event -> {
                    var main = Commands.literal("nightsort")
                            .executes(ctx -> commandHandler.handleCommand(ctx.getSource()))
                            .build();

                    var alias = Commands.literal("sort")
                            .executes(ctx -> commandHandler.handleCommand(ctx.getSource()))
                            .build();

                    event.registrar().register(main, "Toggle container sorting");
                    event.registrar().register(alias, "Alias for /nightsort");
                }
        );

        this.inventoryListener = new InventoryListener(sortService, storage);
        getServer().getPluginManager().registerEvents(inventoryListener, this);

        storage.load();
    }

    @Override
    public void onDisable() {
        storage.save();
    }

    private ConfigOptions readConfigOptions() {
        int idleTicks = getConfig().getInt("idle-ticks", 8);
        int maxConcurrentSorts = getConfig().getInt("max-concurrent-sorts", 4);
        int maxRetries = getConfig().getInt("max-retries", 3);
        int retryDelayTicks = getConfig().getInt("retry-delay-ticks", 2);

        var types = getConfig().getStringList("inventory-types");
        var allowed = new java.util.HashSet<org.bukkit.event.inventory.InventoryType>();
        if (types != null) {
            for (String s : types) {
                try {
                    var t = org.bukkit.event.inventory.InventoryType.valueOf(s.toUpperCase());
                    allowed.add(t);
                } catch (Exception ignored) {}
            }
        }
        if (allowed.isEmpty()) {
            allowed.add(org.bukkit.event.inventory.InventoryType.CHEST);
            allowed.add(org.bukkit.event.inventory.InventoryType.BARREL);
            allowed.add(org.bukkit.event.inventory.InventoryType.ENDER_CHEST);
            allowed.add(org.bukkit.event.inventory.InventoryType.SHULKER_BOX);
        }

        return new ConfigOptions(idleTicks, maxConcurrentSorts, maxRetries, retryDelayTicks, allowed);
    }

    public void reloadConfigAndApply() {
        try {
            reloadConfig();
            try { messages.reload(); } catch (Exception ex) { getLogger().warning("Failed to reload messages: " + ex.getMessage()); }
            ConfigOptions opts = readConfigOptions();
            sortService.updateConfig(opts);

            getLogger().info("NightSort config reloaded and applied.");
        } catch (Exception ex) {
            getLogger().warning("Failed to reload config: " + ex.getMessage());
        }
    }
}