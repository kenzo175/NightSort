package su.nightexpress.nightsort.service.messaging;

import su.nightexpress.nightsort.service.IDebounceService;

import java.util.concurrent.ConcurrentHashMap;

public class DebounceService implements IDebounceService {

    private static final long DEBOUNCE_MS = 1000L;

    private final ConcurrentHashMap<Integer, Long> last = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Boolean> processing = new ConcurrentHashMap<>();

    @Override
    public boolean tryStart(int key) {
        long now = System.currentTimeMillis();
        Long lastTime = last.get(key);
        if (lastTime != null && (now - lastTime) < DEBOUNCE_MS) return false;
        Boolean already = processing.putIfAbsent(key, Boolean.TRUE);
        if (already != null && already) return false;
        last.put(key, now);
        return true;
    }

    @Override
    public void finish(int key) {
        processing.remove(key);
        last.put(key, System.currentTimeMillis());
    }
}