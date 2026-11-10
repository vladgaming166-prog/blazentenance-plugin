package net.blazemc.blazentenance.common.security;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

public final class Secrets {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private Secrets() {
    }

    public static String generateHex(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return HEX.formatHex(buffer);
    }

    public static byte[] decodeHex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Shared secret is missing");
        }
        String trimmed = hex.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Shared secret is blank");
        }
        if ((trimmed.length() & 1) == 1) {
            throw new IllegalArgumentException("Shared secret is not valid hex");
        }
        try {
            return HEX.parseHex(trimmed);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Shared secret is not valid hex", exception);
        }
    }

    public static boolean isConfigured(String hex) {
        return hex != null && !hex.trim().isEmpty();
    }
}
