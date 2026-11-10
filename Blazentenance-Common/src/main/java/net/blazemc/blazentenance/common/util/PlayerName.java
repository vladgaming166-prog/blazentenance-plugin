package net.blazemc.blazentenance.common.util;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class PlayerName {

    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9_]{1,16}$");

    private PlayerName() {
    }

    public static boolean isValid(String name) {
        return name != null && VALID.matcher(name).matches();
    }

    public static Optional<String> normalize(String name) {
        if (!isValid(name)) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
