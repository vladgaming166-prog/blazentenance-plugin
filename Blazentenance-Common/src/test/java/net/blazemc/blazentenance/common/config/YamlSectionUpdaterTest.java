package net.blazemc.blazentenance.common.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlSectionUpdaterTest {

    @TempDir
    Path tempDir;

    @Test
    void updatesOnlyMaintenanceEnabled() throws Exception {
        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, """
                maintenance:
                  enabled: false
                  whitelist:
                    enabled: true
                """, StandardCharsets.UTF_8);
        assertTrue(YamlSectionUpdater.write(file, "maintenance", "enabled", "true"));
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("maintenance:\n  enabled: true"));
        assertTrue(content.contains("whitelist:\n    enabled: true"));
        assertFalse(content.contains("maintenance:\n  enabled: false"));
    }
}
