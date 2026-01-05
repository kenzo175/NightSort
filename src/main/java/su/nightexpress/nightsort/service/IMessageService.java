package su.nightexpress.nightsort.service;

import net.kyori.adventure.text.Component;

public interface IMessageService {
    Component get(String key);
    String getRaw(String key);
}