package net.blazemc.blazentenance.velocity.whitelist;

import com.velocitypowered.api.proxy.Player;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.blazemc.blazentenance.common.storage.WhitelistedPlayer;
import net.blazemc.blazentenance.common.storage.YamlWhitelistStore;
import net.blazemc.blazentenance.velocity.BlazentenanceVelocity;
import net.blazemc.blazentenance.velocity.maintenance.MaintenanceManager;
import org.slf4j.Logger;

public final class WhitelistManager {

    private final Logger logger;
    private final MaintenanceManager maintenance;
    private final YamlWhitelistStore store;

    public WhitelistManager(
            BlazentenanceVelocity plugin,
            Logger logger,
            Path dataDirectory,
            MaintenanceManager maintenance
    ) {
        this.logger = logger;
        this.maintenance = maintenance;
        this.store = new YamlWhitelistStore(dataDirectory.resolve(this.maintenance.config().storageFile()));
    }

    public void load() throws IOException {
        store.load();
        logger.info("Loaded {} maintenance whitelist entries.", store.size());
    }

    public void save() throws IOException {
        store.save();
    }

    public YamlWhitelistStore.AddResult add(UUID uuid, String name) throws IOException {
        return store.add(new WhitelistedPlayer(uuid, name, Instant.now()));
    }

    public Optional<WhitelistedPlayer> remove(String identifier) throws IOException {
        return store.remove(identifier);
    }

    public boolean isWhitelisted(Player player) {
        return isWhitelisted(player.getUniqueId(), player.getUsername());
    }

    public boolean isWhitelisted(UUID uuid, String username) {
        if (uuid != null && store.contains(uuid)) {
            return true;
        }
        return username != null && store.containsName(username);
    }

    public List<WhitelistedPlayer> list() {
        return store.list();
    }

    public boolean isEmpty() {
        return store.isEmpty();
    }
}
