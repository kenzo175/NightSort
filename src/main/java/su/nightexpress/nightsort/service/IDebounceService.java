package su.nightexpress.nightsort.service;

public interface IDebounceService {
    boolean tryStart(int key);
    void finish(int key);
}