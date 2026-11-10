package net.blazemc.blazentenance.common.security;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthToken(
        int version,
        Instant issuedAt,
        UUID playerId,
        String username,
        boolean authorized,
        boolean maintenanceEnabled,
        byte[] nonce
) {
    public static final int CURRENT_VERSION = 1;

    public AuthToken {
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(username, "username");
        nonce = nonce == null ? new byte[16] : nonce.clone();
    }

    public static AuthToken issue(
            UUID playerId,
            String username,
            boolean authorized,
            boolean maintenanceEnabled,
            byte[] nonce
    ) {
        return new AuthToken(
                CURRENT_VERSION,
                Instant.now(),
                playerId,
                username,
                authorized,
                maintenanceEnabled,
                nonce
        );
    }
}
