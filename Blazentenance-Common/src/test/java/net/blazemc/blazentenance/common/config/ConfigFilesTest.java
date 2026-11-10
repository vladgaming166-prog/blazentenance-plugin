package net.blazemc.blazentenance.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesDefaultConfigAndParsesMaintenanceState() throws Exception {
        Path config = tempDir.resolve("config.yml");
        Map<String, Object> loaded = ConfigFiles.loadOrCreate(config, ConfigFiles.class, "velocity-config.yml");
        PluginConfig parsed = PluginConfig.fromMap(loaded);
        assertTrue(Files.exists(config));
        assertFalse(parsed.maintenanceEnabled());
        assertEquals("players.yml", parsed.storageFile());
        assertTrue(parsed.kickMessage().stream().anyMatch(line -> line.contains("în lucru")));
        assertTrue(YamlSectionUpdater.write(config, "maintenance", "enabled", "true"));
        String content = Files.readString(config, StandardCharsets.UTF_8);
        assertTrue(content.contains("enabled: true"));
        assertTrue(content.contains("BLAZENTENANCE CONFIGURATION"));
        PluginConfig reloaded = PluginConfig.fromMap(YamlSupport.loadMap(config));
        assertTrue(reloaded.maintenanceEnabled());
    }
}
