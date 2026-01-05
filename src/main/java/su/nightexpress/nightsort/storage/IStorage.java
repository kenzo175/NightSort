package su.nightexpress.nightsort.storage;

import java.util.Set;
import java.util.UUID;

public interface IStorage {
    Set<UUID> getActivePlayers();
    void addActive(UUID u);
    void removeActive(UUID u);
    boolean isActive(UUID u);
    void load();
    void save();
}