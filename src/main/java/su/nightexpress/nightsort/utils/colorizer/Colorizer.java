package su.nightexpress.nightsort.utils.colorizer;

import su.nightexpress.nightsort.utils.colorizer.impl.LegacyIColorizer;
import su.nightexpress.nightsort.utils.colorizer.impl.MinimessageIColorizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Colorizer {
    private static IColorizer colorizer = new LegacyIColorizer();

    private Colorizer() {}

    public static void setColorizer(String type) {
        if (type == null) {
            colorizer = new LegacyIColorizer();
            return;
        }
        switch (type.toUpperCase(Locale.ROOT)) {
            case "MINIMESSAGE":
            case "MINI":
                colorizer = new MinimessageIColorizer();
                break;
            default:
                colorizer = new LegacyIColorizer();
                break;
        }
    }

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return message;
        return colorizer.colorize(message);
    }

    public static List<String> colorizeAll(List<String> list) {
        List<String> colored = new ArrayList<>();
        for (var str : list) colored.add(colorize(str));
        return colored;
    }
}