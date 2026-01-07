package su.nightexpress.nightsort.command;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import su.nightexpress.nightsort.SortPlugin;
import su.nightexpress.nightsort.service.IMessageService;
import su.nightexpress.nightsort.storage.IStorage;
import su.nightexpress.nightsort.service.ISortService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class CommandHandler implements TabExecutor {

    private final SortPlugin plugin;
    private final IStorage storage;
    private final IMessageService messages;
    private final ISortService sortService;

    public CommandHandler(SortPlugin plugin, IStorage storage, IMessageService messages, ISortService sortService) {
        this.plugin = plugin;
        this.storage = storage;
        this.messages = messages;
        this.sortService = sortService;
    }

    public int handleCommand(CommandSourceStack source) {
        Object exec = source.getExecutor();
        if (!(exec instanceof Player player)) {
            source.getSender().sendMessage(messages.get("only-player"));
            return Command.SINGLE_SUCCESS;
        }

        if (!player.hasPermission("nightsort.toggle")) {
            player.sendMessage(messages.get("no-permission"));
            return Command.SINGLE_SUCCESS;
        }

        UUID uuid = player.getUniqueId();
        if (storage.isActive(uuid)) {
            storage.removeActive(uuid);
            player.sendMessage(messages.get("disabled"));
        } else {
            storage.addActive(uuid);
            Component comp = messages.get("enabled").replaceText(r ->
                    r.matchLiteral("{inventories}").replacement(sortService.getInventoriesText())
            );
            player.sendMessage(comp);
        }

        storage.save();
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("only-player"));
            return true;
        }

        if (!player.hasPermission("nightsort.toggle")) {
            player.sendMessage(messages.get("no-permission"));
            return true;
        }

        if (args.length == 0) {
            UUID uuid = player.getUniqueId();
            if (storage.isActive(uuid)) {
                storage.removeActive(uuid);
                player.sendMessage(messages.get("disabled"));
            } else {
                storage.addActive(uuid);
                Component comp = messages.get("enabled").replaceText(r ->
                        r.matchLiteral("{inventories}").replacement(sortService.getInventoriesText())
                );
                player.sendMessage(comp);
            }
            storage.save();
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "on" -> {
                if (storage.isActive(player.getUniqueId())) player.sendMessage(messages.get("already-enabled"));
                else { storage.setActive(player.getUniqueId(), true); player.sendMessage(messages.get("enabled")); storage.save(); }
            }
            case "off" -> {
                if (!storage.isActive(player.getUniqueId())) player.sendMessage(messages.get("already-disabled"));
                else { storage.setActive(player.getUniqueId(), false); player.sendMessage(messages.get("disabled")); storage.save(); }
            }
            case "status" -> {
                boolean active = storage.isActive(player.getUniqueId());
                player.sendMessage(active ? messages.get("status-enabled") : messages.get("status-disabled"));
            }
            case "reload" -> {
                if (args.length == 1) {
                    plugin.reloadConfigAndApply();
                    player.sendMessage(messages.get("reloaded-all"));
                } else if ("messages".equalsIgnoreCase(args[1])) {
                    messages.reload();
                    player.sendMessage(messages.get("reloaded-messages"));
                } else if ("config".equalsIgnoreCase(args[1])) {
                    plugin.reloadConfigAndApply();
                    player.sendMessage(messages.get("reloaded-config"));
                } else {
                    player.sendMessage(messages.get("unknown-command"));
                }
            }
            default -> player.sendMessage(messages.get("unknown-command"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("on", "off", "status", "reload");
        if (args.length == 2 && "reload".equalsIgnoreCase(args[0])) return Arrays.asList("config", "messages");
        return Collections.emptyList();
    }
}