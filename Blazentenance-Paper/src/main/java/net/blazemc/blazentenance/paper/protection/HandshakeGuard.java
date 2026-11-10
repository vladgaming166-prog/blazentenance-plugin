package net.blazemc.blazentenance.paper.protection;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.blazemc.blazentenance.common.security.AuthToken;
import net.blazemc.blazentenance.common.security.AuthTokenCodec;
import net.blazemc.blazentenance.common.security.HmacSigner;
import net.blazemc.blazentenance.common.security.InvalidAuthTokenException;
import net.blazemc.blazentenance.common.security.ReplayCache;
import net.blazemc.blazentenance.common.security.Secrets;
import net.blazemc.blazentenance.paper.config.PaperConfig;

public final class HandshakeGuard {

    private final ConcurrentHashMap<UUID, Decision> decisions = new ConcurrentHashMap<>();
    private final ReplayCache replayCache = new ReplayCache(Duration.ofMinutes(2));
    private volatile AuthTokenCodec codec;
    private volatile boolean ready;

    public void reconfigure(PaperConfig config) {
        decisions.clear();
        replayCache.clear();
        if (!Secrets.isConfigured(config.sharedSecret())) {
            this.codec = null;
            this.ready = false;
            return;
        }
        this.codec = new AuthTokenCodec(new HmacSigner(Secrets.decodeHex(config.sharedSecret())));
        this.ready = true;
    }

    public boolean ready() {
        return ready;
    }

    public Decision accept(byte[] payload, UUID expectedId, String expectedName) {
        if (!ready || codec == null) {
            return Decision.invalid("handshake-not-configured");
        }
        try {
            AuthToken token = codec.decode(payload);
            if (!token.playerId().equals(expectedId)) {
                return Decision.invalid("uuid-mismatch");
            }
            if (expectedName != null && !expectedName.equalsIgnoreCase(token.username())) {
                return Decision.invalid("name-mismatch");
            }
            if (!replayCache.remember(token.playerId(), token.nonce())) {
                return Decision.invalid("replay");
            }
            Decision decision = new Decision(true, token.authorized(), token.maintenanceEnabled(), "ok");
            decisions.put(token.playerId(), decision);
            return decision;
        } catch (InvalidAuthTokenException | RuntimeException exception) {
            return Decision.invalid(exception.getMessage() == null ? "invalid-token" : exception.getMessage());
        }
    }

    public Decision peek(UUID playerId) {
        return decisions.get(playerId);
    }

    public void forget(UUID playerId) {
        if (playerId != null) {
            decisions.remove(playerId);
        }
    }

    public record Decision(boolean valid, boolean authorized, boolean maintenanceEnabled, String reason) {
        public static Decision invalid(String reason) {
            return new Decision(false, false, false, reason == null ? "invalid" : reason.toLowerCase(Locale.ROOT));
        }

        public boolean allowJoin() {
            return valid && authorized;
        }
    }
}
