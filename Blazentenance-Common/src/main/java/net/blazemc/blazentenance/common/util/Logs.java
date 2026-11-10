package net.blazemc.blazentenance.common.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Redacts secrets before they can be written to logs.
 */
public final class Logs {

    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)(secret|token|hmac|password|key)\\s*[=:]\\s*\\S+");

    private Logs() {
    }

    public static String redact(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        String redacted = SECRET_ASSIGNMENT.matcher(message).replaceAll("$1=***");
        if (looksLikeHexSecret(message)) {
            return "[redacted]";
        }
        return redacted;
    }

    private static boolean looksLikeHexSecret(String message) {
        String trimmed = message.trim();
        return trimmed.length() >= 32 && trimmed.length() <= 128 && trimmed.matches("[0-9a-fA-F]+");
    }

    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("secret") || lower.contains("token") || lower.contains("password");
    }
}
