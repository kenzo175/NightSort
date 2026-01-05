package su.nightexpress.nightsort.service.sort.impl;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import su.nightexpress.nightsort.model.SnapshotEntry;
import su.nightexpress.nightsort.model.SnapshotJob;

import java.util.Comparator;

public final class AsyncSorter {

    private final Plugin plugin;
    private final Comparator<SnapshotEntry> comparator;
    private final ApplyScheduler applier;
    private final JobQueue jobQueue;

    public AsyncSorter(Plugin plugin, Comparator<SnapshotEntry> comparator, ApplyScheduler applier, JobQueue jobQueue) {
        this.plugin = plugin;
        this.comparator = comparator;
        this.applier = applier;
        this.jobQueue = jobQueue;
    }

    public void start(SnapshotJob job) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                job.snapshots.sort(comparator);
            } catch (Throwable t) {
                plugin.getLogger().warning("Async sort failed: " + t.getMessage());
            }
            applier.attemptApply(job, 0);
        });
    }
}