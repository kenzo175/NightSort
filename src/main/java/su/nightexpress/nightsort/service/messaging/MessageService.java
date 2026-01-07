package su.nightexpress.nightsort.service.messaging;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import su.nightexpress.nightsort.service.IMessageService;
import su.nightexpress.nightsort.utils.colorizer.Colorizer;

import java.io.File;

public class MessageService implements IMessageService {

    private final Plugin plugin;
    private FileConfiguration cfg;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    public MessageService(Plugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            plugin.saveResource("messages.yml", false);
        }
        this.cfg = YamlConfiguration.loadConfiguration(f);
        String colorizer = cfg.getString("colorizer", "MINIMESSAGE");
        Colorizer.setColorizer(colorizer);
    }

    @Override
    public Component get(String key) {
        String raw = cfg.getString(key, "&cMissing message: " + key);
        String colored = Colorizer.colorize(raw);
        return legacy.deserialize(colored);
    }

    @Override
    public String getRaw(String key) {
        return cfg.getString(key, "&cMissing message: " + key);
    }

    public void reload() {
        loadMessages();
    }
}