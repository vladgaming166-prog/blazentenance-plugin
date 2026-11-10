package net.blazemc.blazentenance.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidsTest {

    @Test
    void parsesDashedAndUndashedUuids() {
        UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        assertEquals(uuid, Uuids.parse(uuid.toString()).orElseThrow());
        assertEquals(uuid, Uuids.parse(Uuids.toUndashed(uuid)).orElseThrow());
        assertTrue(Uuids.parse("not-a-uuid").isEmpty());
    }

    @Test
    void offlineUuidMatchesVanillaAlgorithm() {
        assertEquals(
                UUID.nameUUIDFromBytes("OfflinePlayer:Steve".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                Uuids.offline("Steve")
        );
    }
}
