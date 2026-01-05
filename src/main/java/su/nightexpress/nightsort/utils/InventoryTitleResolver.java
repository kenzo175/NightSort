package su.nightexpress.nightsort.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.inventory.InventoryType;

import java.lang.reflect.Method;

public final class InventoryTitleResolver {

    private static final Method GET_DEFAULT_TITLE;
    private static final boolean RETURNS_COMPONENT;

    static {
        Method method = null;
        boolean component = false;

        try {
            method = InventoryType.class.getMethod("getDefaultTitle");
            component = method.getReturnType().equals(Component.class);
        } catch (NoSuchMethodException ignored) {
        }

        GET_DEFAULT_TITLE = method;
        RETURNS_COMPONENT = component;
    }

    private InventoryTitleResolver() {
    }

    public static String resolve(InventoryType type) {
        try {
            if (GET_DEFAULT_TITLE == null) {
                return type.name();
            }

            Object value = GET_DEFAULT_TITLE.invoke(type);

            if (value == null) {
                return type.name();
            }

            if (RETURNS_COMPONENT) {
                return PlainTextComponentSerializer.plainText()
                        .serialize((Component) value);
            }

            return value.toString();

        } catch (Throwable ignored) {
            return type.name();
        }
    }
}