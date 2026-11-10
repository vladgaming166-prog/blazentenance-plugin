package net.blazemc.blazentenance.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

public final class ConfigFiles {

    private ConfigFiles() {
    }

    public static Map<String, Object> loadOrCreate(Path file, Class<?> resourceOwner, String resourceName)
            throws ConfigException {
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                try (InputStream input = resourceOwner.getResourceAsStream("/" + resourceName)) {
                    if (input == null) {
                        throw new ConfigException("Missing bundled resource: " + resourceName);
                    }
                    Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return YamlSupport.loadMap(file);
        } catch (IOException exception) {
            throw new ConfigException("Unable to load configuration file: " + file.getFileName(), exception);
        }
    }

    public static void copyResourceIfMissing(Path file, Class<?> resourceOwner, String resourceName)
            throws ConfigException {
        Objects.requireNonNull(resourceName, "resourceName");
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file)) {
                return;
            }
            try (InputStream input = resourceOwner.getResourceAsStream("/" + resourceName)) {
                if (input == null) {
                    throw new ConfigException("Missing bundled resource: " + resourceName);
                }
                Files.copy(input, file);
            }
        } catch (IOException exception) {
            throw new ConfigException("Unable to create " + file.getFileName(), exception);
        }
    }
}
