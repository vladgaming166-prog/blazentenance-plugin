package net.blazemc.blazentenance.common.message;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.blazemc.blazentenance.common.config.TextFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageFormatter {

    private static final Pattern HEX = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final TextFormat format;

    public MessageFormatter(TextFormat format) {
        this.format = format == null ? TextFormat.AUTO : format;
    }

    public Component deserialize(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return switch (select(input)) {
            case MINIMESSAGE -> MINI_MESSAGE.deserialize(toMiniMessage(input));
            case LEGACY, AUTO -> LEGACY.deserialize(input);
        };
    }

    public Component lines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Component.empty();
        }
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(deserialize(line == null ? "" : line));
        }
        return Component.join(JoinConfiguration.newlines(), components);
    }

    public Component prefixed(String prefix, String message) {
        return deserialize(prefix == null ? "" : prefix).append(deserialize(message == null ? "" : message));
    }

    private TextFormat select(String input) {
        if (format == TextFormat.LEGACY) {
            return TextFormat.LEGACY;
        }
        if (format == TextFormat.MINIMESSAGE) {
            return TextFormat.MINIMESSAGE;
        }
        if (looksLikeMiniMessage(input)) {
            return TextFormat.MINIMESSAGE;
        }
        return TextFormat.LEGACY;
    }

    static boolean looksLikeMiniMessage(String input) {
        return input.contains("<") && input.contains(">") && !input.contains("&");
    }

    static String toMiniMessage(String input) {
        String withHex = HEX.matcher(input).replaceAll(result -> "<#" + result.group(1) + ">");
        StringBuilder output = new StringBuilder();
        for (int index = 0; index < withHex.length(); index++) {
            char character = withHex.charAt(index);
            if (character == '&' && index + 1 < withHex.length()) {
                char code = Character.toLowerCase(withHex.charAt(index + 1));
                String tag = legacyTag(code);
                if (tag != null) {
                    output.append(tag);
                    index++;
                    continue;
                }
            }
            output.append(character);
        }
        return output.toString();
    }

    private static String legacyTag(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    public static String strip(String input) {
        if (input == null) {
            return "";
        }
        String withoutHex = HEX.matcher(input).replaceAll("");
        String withoutLegacy = withoutHex.replaceAll("(?i)&[0-9a-fk-orx]", "");
        return MiniMessage.miniMessage().stripTags(withoutLegacy);
    }
}
