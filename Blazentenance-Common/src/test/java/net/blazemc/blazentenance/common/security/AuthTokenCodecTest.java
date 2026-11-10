package net.blazemc.blazentenance.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthTokenCodecTest {

    private final HmacSigner signer = new HmacSigner("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    private final AuthTokenCodec codec = new AuthTokenCodec(signer, Duration.ofSeconds(30));

    @Test
    void roundTripPreservesFields() throws Exception {
        UUID playerId = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        AuthToken original = AuthToken.issue(playerId, "Notch", true, true, AuthTokenCodec.randomNonce());
        AuthToken decoded = codec.decode(codec.encode(original));
        assertEquals(original.playerId(), decoded.playerId());
        assertEquals(original.username(), decoded.username());
        assertTrue(decoded.authorized());
        assertTrue(decoded.maintenanceEnabled());
    }

    @Test
    void tamperedPayloadIsRejected() {
        UUID playerId = UUID.randomUUID();
        byte[] encoded = codec.encode(AuthToken.issue(playerId, "Steve", true, false, AuthTokenCodec.randomNonce()));
        encoded[encoded.length - 1] ^= 0x5A;
        assertThrows(InvalidAuthTokenException.class, () -> codec.decode(encoded));
    }

    @Test
    void expiredTokenIsRejected() {
        AuthToken expired = new AuthToken(
                AuthToken.CURRENT_VERSION,
                Instant.now().minus(Duration.ofMinutes(5)),
                UUID.randomUUID(),
                "Alex",
                true,
                true,
                AuthTokenCodec.randomNonce()
        );
        byte[] encoded = codec.encode(expired);
        assertThrows(InvalidAuthTokenException.class, () -> codec.decode(encoded));
    }

    @Test
    void replayCacheRejectsDuplicateNonce() {
        ReplayCache cache = new ReplayCache(Duration.ofMinutes(1));
        UUID playerId = UUID.randomUUID();
        byte[] nonce = AuthTokenCodec.randomNonce();
        assertTrue(cache.remember(playerId, nonce));
        assertTrue(!cache.remember(playerId, nonce));
    }
}
