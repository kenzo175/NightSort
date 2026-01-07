package su.nightexpress.nightsort.storage;

import java.util.Set;
import java.util.UUID;

public interface IStorage {
    Set<UUID> getActivePlayers();
    void addActive(UUID u);
    void removeActive(UUID u);
    boolean isActive(UUID uuid);
    void setActive(UUID uuid, boolean active);
    void load();
    void save();
}