package net.blazemc.blazentenance.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Updates a single nested YAML scalar without rewriting the rest of the file,
 * so comments and formatting are preserved.
 */
public final class YamlSectionUpdater {

    private YamlSectionUpdater() {
    }

    public static boolean write(Path file, String section, String key, String value) throws IOException {
        if (!Files.exists(file)) {
            return false;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        Pattern sectionPattern = Pattern.compile("^(\\s*)" + Pattern.quote(section) + ":\\s*(#.*)?$");
        Pattern keyPattern = Pattern.compile("^(\\s*)" + Pattern.quote(key) + ":\\s*(.*)$");
        boolean inSection = false;
        int sectionIndent = -1;
        boolean updated = false;
        List<String> rewritten = new ArrayList<>(lines.size());
        for (String line : lines) {
            Matcher sectionMatcher = sectionPattern.matcher(line);
            if (sectionMatcher.matches()) {
                inSection = true;
                sectionIndent = sectionMatcher.group(1).length();
                rewritten.add(line);
                continue;
            }
            if (inSection) {
                int indent = leadingSpaces(line);
                boolean commentOrBlank = line.isBlank() || line.trim().startsWith("#");
                if (!commentOrBlank && indent <= sectionIndent) {
                    inSection = false;
                } else {
                    Matcher keyMatcher = keyPattern.matcher(line);
                    if (keyMatcher.matches() && indent > sectionIndent) {
                        String comment = extractInlineComment(keyMatcher.group(2));
                        rewritten.add(keyMatcher.group(1) + key + ": " + value + comment);
                        updated = true;
                        inSection = false;
                        continue;
                    }
                }
            }
            rewritten.add(line);
        }
        if (updated) {
            Files.write(file, rewritten, StandardCharsets.UTF_8);
        }
        return updated;
    }

    private static int leadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static String extractInlineComment(String remainder) {
        boolean inQuote = false;
        char quote = 0;
        for (int index = 0; index < remainder.length(); index++) {
            char character = remainder.charAt(index);
            if ((character == '"' || character == '\'') && (index == 0 || remainder.charAt(index - 1) != '\\')) {
                if (!inQuote) {
                    inQuote = true;
                    quote = character;
                } else if (character == quote) {
                    inQuote = false;
                }
            } else if (character == '#' && !inQuote) {
                return " " + remainder.substring(index);
            }
        }
        return "";
    }
}
