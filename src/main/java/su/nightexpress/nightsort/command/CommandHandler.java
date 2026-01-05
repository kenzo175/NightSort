package su.nightexpress.nightsort.command;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import su.nightexpress.nightsort.service.IMessageService;
import su.nightexpress.nightsort.storage.IStorage;
import su.nightexpress.nightsort.service.ISortService;

import java.util.UUID;

public class CommandHandler {

    private static final String PERMISSION_TOGGLE = "nightsort.toggle";

    private final IStorage storage;
    private final IMessageService messages;
    private final ISortService sortService;

    public CommandHandler(IStorage storage, IMessageService messages, ISortService sortService) {
        this.storage = storage;
        this.messages = messages;
        this.sortService = sortService;
    }

    public int handleCommand(CommandSourceStack source) {
        var sender = source.getExecutor();
        if (!(sender instanceof Player player)) {
            source.getSender().sendMessage(messages.get("only-player"));
            return Command.SINGLE_SUCCESS;
        }

        if (!player.hasPermission(PERMISSION_TOGGLE)) {
            player.sendMessage(messages.get("no-permission"));
            return Command.SINGLE_SUCCESS;
        }

        UUID uuid = player.getUniqueId();
        if (storage.isActive(uuid)) {
            storage.removeActive(uuid);
            player.sendMessage(messages.get("disabled"));
        } else {
            storage.addActive(uuid);

            // use interface getter for inventories text
            String invText = sortService.getInventoriesText();
            Component msg = messages.get("enabled").replaceText(r ->
                    r.matchLiteral("{inventories}").replacement(invText)
            );
            player.sendMessage(msg);
        }

        storage.save();
        return Command.SINGLE_SUCCESS;
    }
}