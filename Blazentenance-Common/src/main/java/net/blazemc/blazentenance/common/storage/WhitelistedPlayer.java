package net.blazemc.blazentenance.common.storage;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WhitelistedPlayer(UUID uuid, String name, Instant addedAt) {

    public WhitelistedPlayer {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(addedAt, "addedAt");
    }

    public WhitelistedPlayer withName(String newName) {
        return new WhitelistedPlayer(uuid, newName, addedAt);
    }
}
