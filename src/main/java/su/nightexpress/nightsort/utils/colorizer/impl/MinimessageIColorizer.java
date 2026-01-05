package su.nightexpress.nightsort.utils.colorizer.impl;

import su.nightexpress.nightsort.utils.colorizer.IColorizer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MinimessageIColorizer implements IColorizer {
    @Override
    public String colorize(String message) {
        if (message == null) return null;
        var comp = MiniMessage.miniMessage().deserialize(message);
        return LegacyComponentSerializer.legacySection().serialize(comp);
    }
}