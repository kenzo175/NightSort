package su.nightexpress.nightsort.service.sort.impl;

import org.bukkit.plugin.Plugin;
import su.nightexpress.nightsort.model.SnapshotJob;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public final class JobQueue {

    private final Plugin plugin;
    private final Semaphore concurrencyLimiter;
    private final ConcurrentLinkedQueue<SnapshotJob> pendingQueue = new ConcurrentLinkedQueue<>();
    private final AsyncSorter sorter;

    public JobQueue(Plugin plugin, int maxConcurrentSorts, AsyncSorter sorter) {
        this.plugin = plugin;
        this.concurrencyLimiter = new Semaphore(maxConcurrentSorts);
        this.sorter = sorter;
    }

    public void schedule(SnapshotJob job) {
        if (concurrencyLimiter.tryAcquire()) {
            sorter.start(job);
        } else {
            pendingQueue.offer(job);
        }
    }

    public void onJobFinished(int invKey) {
        concurrencyLimiter.release();

        SnapshotJob next = pendingQueue.poll();
        if (next != null) {
            if (concurrencyLimiter.tryAcquire()) {
                sorter.start(next);
            } else {
                pendingQueue.offer(next);
            }
        }
    }
}