package net.blazemc.blazentenance.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rejects reused handshake nonces for a short window.
 */
public final class ReplayCache {

    private final ConcurrentHashMap<String, Instant> seen = new ConcurrentHashMap<>();
    private final Duration ttl;

    public ReplayCache(Duration ttl) {
        this.ttl = ttl;
    }

    public boolean remember(UUID playerId, byte[] nonce) {
        prune();
        String key = playerId + ":" + HexFormat.of().formatHex(nonce);
        Instant now = Instant.now();
        Instant previous = seen.putIfAbsent(key, now.plus(ttl));
        return previous == null;
    }

    public void clear() {
        seen.clear();
    }

    private void prune() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, Instant>> iterator = seen.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Instant> entry = iterator.next();
            if (entry.getValue().isBefore(now)) {
                iterator.remove();
            }
        }
    }
}
