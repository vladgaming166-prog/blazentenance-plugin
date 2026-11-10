package net.blazemc.blazentenance.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

public final class YamlSupport {

    private YamlSupport() {
    }

    public static Yaml pretty() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        options.setProcessComments(false);
        Representer representer = new Representer(options);
        representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(new SafeConstructor(new LoaderOptions()), representer, options);
    }

    public static Yaml loader() {
        return new Yaml(new SafeConstructor(new LoaderOptions()));
    }

    public static Map<String, Object> loadMap(Path file) throws IOException {
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = loader().load(reader);
            return asMap(loaded);
        }
    }

    public static Map<String, Object> loadMap(InputStream input) {
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return asMap(loader().load(reader));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read YAML resource", exception);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> copy.put(String.valueOf(key), nested));
            return copy;
        }
        return new LinkedHashMap<>();
    }

    public static Map<String, Object> map(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        return asMap(value);
    }

    public static boolean bool(Map<String, Object> map, String key, boolean fallback) {
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return fallback;
    }

    public static int integer(Map<String, Object> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public static String string(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        String raw = String.valueOf(value);
        return raw.isEmpty() ? fallback : raw;
    }

    public static List<String> stringList(Map<String, Object> map, String key, List<String> fallback) {
        Object value = map.get(key);
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                result.add(item == null ? "" : String.valueOf(item));
            }
            return List.copyOf(result);
        }
        if (value instanceof String string) {
            return List.of(string);
        }
        return List.copyOf(fallback);
    }

    public static <E extends Enum<E>> E enumerate(Map<String, Object> map, String key, Class<E> type, E fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, String.valueOf(value).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
