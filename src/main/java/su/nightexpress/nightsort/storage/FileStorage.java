package su.nightexpress.nightsort.storage;

import org.bukkit.plugin.Plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileStorage implements IStorage {

    private static final Path DATA_PATH = Path.of("plugins", "NightSort", "active.txt");

    private final Plugin plugin;
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();

    public FileStorage(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Set<UUID> getActivePlayers() {
        return Collections.unmodifiableSet(active);
    }

    @Override
    public void addActive(UUID u) {
        active.add(u);
    }

    @Override
    public void removeActive(UUID u) {
        active.remove(u);
    }

    @Override
    public boolean isActive(UUID u) {
        return active.contains(u);
    }

    @Override
    public void load() {
        try {
            if (!Files.exists(DATA_PATH)) return;
            for (String line : Files.readAllLines(DATA_PATH)) {
                if (!line.isBlank()) {
                    try {
                        active.add(UUID.fromString(line.trim()));
                    } catch (Exception ignored) {}
                }
            }
            plugin.getLogger().info("Loaded " + active.size() + " active players");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load active players: " + e.getMessage());
        }
    }

    @Override
    public void save() {
        try {
            Files.createDirectories(DATA_PATH.getParent());
            String out = String.join("\n", active.stream().map(UUID::toString).toList());
            Files.writeString(DATA_PATH, out);
            plugin.getLogger().info("Saved " + active.size() + " active players");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save active players: " + e.getMessage());
        }
    }
}