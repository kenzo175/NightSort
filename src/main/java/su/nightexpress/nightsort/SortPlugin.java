package su.nightexpress.nightsort;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.plugin.java.JavaPlugin;
import su.nightexpress.nightsort.command.CommandHandler;
import su.nightexpress.nightsort.service.messaging.DebounceService;
import su.nightexpress.nightsort.service.messaging.MessageService;
import su.nightexpress.nightsort.service.sort.SortService;
import su.nightexpress.nightsort.listener.InventoryListener;
import su.nightexpress.nightsort.service.*;
import su.nightexpress.nightsort.storage.FileStorage;
import su.nightexpress.nightsort.storage.IStorage;

public final class SortPlugin extends JavaPlugin {

    private IMessageService messages;
    private IStorage storage;
    private IDebounceService debounce;
    private ISortService sortService;
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        this.messages = new MessageService(this);
        this.storage = new FileStorage(this);
        this.debounce = new DebounceService();

        this.sortService = new SortService(this, debounce, storage);
        this.commandHandler = new CommandHandler(storage, messages, sortService);

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

        getServer().getPluginManager().registerEvents(new InventoryListener(sortService, storage), this);
        storage.load();
    }

    public void reloadConfigSafe() {
        reloadConfig();
    }

    @Override
    public void onDisable() {
        storage.save();
    }
}