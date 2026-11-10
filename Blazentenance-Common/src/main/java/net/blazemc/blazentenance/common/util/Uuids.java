package net.blazemc.blazentenance.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class Uuids {

    private static final Pattern DASHED = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern UNDASHED = Pattern.compile("^[0-9a-fA-F]{32}$");

    private Uuids() {
    }

    public static Optional<UUID> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            if (DASHED.matcher(value).matches()) {
                return Optional.of(UUID.fromString(value));
            }
            if (UNDASHED.matcher(value).matches()) {
                return Optional.of(fromUndashed(value.toLowerCase(Locale.ROOT)));
            }
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public static UUID fromUndashed(String undashed) {
        if (undashed.length() != 32) {
            throw new IllegalArgumentException("UUID must be 32 hex characters");
        }
        long most = Long.parseUnsignedLong(undashed.substring(0, 16), 16);
        long least = Long.parseUnsignedLong(undashed.substring(16), 16);
        return new UUID(most, least);
    }

    public static String toUndashed(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    public static UUID offline(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
