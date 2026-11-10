package net.blazemc.blazentenance.paper.protection;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Inspects Paper's Velocity modern-forwarding configuration without logging secrets.
 */
public final class ForwardingInspector {

    public ForwardingStatus inspect() {
        Optional<ForwardingStatus> modern = inspectGlobalConfiguration();
        if (modern.isPresent()) {
            return modern.get();
        }
        return inspectLegacyPaperYml();
    }

    private Optional<ForwardingStatus> inspectGlobalConfiguration() {
        try {
            Class<?> global = Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
            Object instance = global.getMethod("get").invoke(null);
            Object proxies = field(instance, "proxies");
            Object velocity = field(proxies, "velocity");
            boolean enabled = booleanField(velocity, "enabled");
            String secret = stringField(velocity, "secret");
            boolean onlineMode = booleanField(velocity, "onlineMode");
            return Optional.of(new ForwardingStatus(true, enabled, hasText(secret), onlineMode, "paper-global.yml"));
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    private ForwardingStatus inspectLegacyPaperYml() {
        return new ForwardingStatus(false, false, false, false, "unknown");
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getField(name);
        return field.get(target);
    }

    private static boolean booleanField(Object target, String name) throws ReflectiveOperationException {
        Object value = field(target, name);
        return value instanceof Boolean bool && bool;
    }

    private static String stringField(Object target, String name) {
        try {
            Object value = field(target, name);
            return value == null ? "" : String.valueOf(value);
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record ForwardingStatus(
            boolean paperApiFound,
            boolean velocityEnabled,
            boolean secretConfigured,
            boolean onlineMode,
            String source
    ) {
        public boolean ready() {
            return velocityEnabled && secretConfigured;
        }
    }
}
