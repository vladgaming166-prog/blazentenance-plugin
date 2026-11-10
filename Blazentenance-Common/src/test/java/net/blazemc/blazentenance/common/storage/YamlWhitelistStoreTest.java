package net.blazemc.blazentenance.common.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlWhitelistStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsPlayersAcrossReload() throws Exception {
        Path file = tempDir.resolve("players.yml");
        YamlWhitelistStore store = new YamlWhitelistStore(file);
        store.load();
        UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        assertEquals(
                YamlWhitelistStore.AddResult.ADDED,
                store.add(new WhitelistedPlayer(uuid, "Notch", Instant.parse("2026-01-01T00:00:00Z")))
        );
        assertEquals(YamlWhitelistStore.AddResult.ALREADY_PRESENT, store.add(new WhitelistedPlayer(uuid, "Notch", Instant.now())));

        YamlWhitelistStore reloaded = new YamlWhitelistStore(file);
        reloaded.load();
        assertTrue(reloaded.contains(uuid));
        assertTrue(reloaded.containsName("notch"));
        assertEquals("Notch", reloaded.find(uuid).orElseThrow().name());
        assertTrue(reloaded.remove("Notch").isPresent());
        assertTrue(reloaded.isEmpty());
    }

    @Test
    void createsFileWhenMissing() throws Exception {
        Path file = tempDir.resolve("players.yml");
        YamlWhitelistStore store = new YamlWhitelistStore(file);
        store.load();
        assertTrue(file.toFile().isFile());
        assertFalse(store.containsName("steve"));
    }
}
