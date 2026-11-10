package net.blazemc.blazentenance.velocity.maintenance;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.blazemc.blazentenance.common.Blazentenance;
import net.blazemc.blazentenance.common.config.ConfigException;
import net.blazemc.blazentenance.common.config.ConfigFiles;
import net.blazemc.blazentenance.common.config.PluginConfig;
import net.blazemc.blazentenance.common.config.YamlSectionUpdater;
import net.blazemc.blazentenance.common.config.YamlSupport;
import net.blazemc.blazentenance.common.message.MessageFormatter;
import net.blazemc.blazentenance.common.policy.AccessPolicy;
import net.blazemc.blazentenance.common.security.AuthTokenCodec;
import net.blazemc.blazentenance.common.security.HmacSigner;
import net.blazemc.blazentenance.common.security.Secrets;
import net.blazemc.blazentenance.velocity.BlazentenanceVelocity;
import net.blazemc.blazentenance.velocity.whitelist.WhitelistManager;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public final class MaintenanceManager {

    private final BlazentenanceVelocity plugin;
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final Path configFile;
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    private volatile PluginConfig config;
    private volatile MessageFormatter formatter;
    private volatile AuthTokenCodec authTokenCodec;

    public MaintenanceManager(
            BlazentenanceVelocity plugin,
            ProxyServer proxy,
            Logger logger,
            Path dataDirectory
    ) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.configFile = dataDirectory.resolve("config.yml");
    }

    public void load() throws ConfigException, IOException {
        Map<String, Object> raw = ConfigFiles.loadOrCreate(configFile, ConfigFiles.class, "velocity-config.yml");
        PluginConfig loaded = PluginConfig.fromMap(raw);
        if (!Secrets.isConfigured(loaded.sharedSecret())) {
            String generated = Secrets.generateHex(32);
            if (!YamlSectionUpdater.write(configFile, "security", "shared-secret", "\"" + generated + "\"")) {
                logger.warn("Could not write the generated shared secret back to config.yml. Add it manually under security.shared-secret.");
            } else {
                logger.info("Generated a new backend handshake secret. Copy security.shared-secret from Velocity into every Paper Blazentenance config.");
            }
            loaded = loaded.withSharedSecret(generated);
        }
        this.config = loaded;
        this.formatter = new MessageFormatter(loaded.textFormat());
        this.authTokenCodec = new AuthTokenCodec(new HmacSigner(Secrets.decodeHex(loaded.sharedSecret())));
        this.enabled.set(loaded.maintenanceEnabled());
    }

    public void reload() throws ConfigException, IOException {
        load();
    }

    public boolean enabled() {
        return enabled.get();
    }

    public PluginConfig config() {
        return config;
    }

    public MessageFormatter formatter() {
        return formatter;
    }

    public AuthTokenCodec authTokenCodec() {
        return authTokenCodec;
    }

    public Path configFile() {
        return configFile;
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public boolean enable(WhitelistManager whitelist) throws IOException {
        if (!enabled.compareAndSet(false, true)) {
            return false;
        }
        persistEnabled(true);
        config = config.withMaintenanceEnabled(true);
        if (config.kickExistingEnabled()) {
            kickUnauthorizedPlayers(whitelist);
        }
        logger.info("{}Maintenance mode has been ENABLED!", Blazentenance.CONSOLE_PREFIX);
        return true;
    }

    public boolean disable() throws IOException {
        if (!enabled.compareAndSet(true, false)) {
            return false;
        }
        persistEnabled(false);
        config = config.withMaintenanceEnabled(false);
        logger.info("{}Maintenance mode has been DISABLED!", Blazentenance.CONSOLE_PREFIX);
        return true;
    }

    public Component kickComponent() {
        return formatter.lines(config.kickMessage());
    }

    public boolean isBypass(Player player) {
        return player.hasPermission(config.bypassPermission());
    }

    public boolean isAdmin(com.velocitypowered.api.command.CommandSource source) {
        if (!(source instanceof Player)) {
            return true;
        }
        return source.hasPermission(config.adminPermission());
    }

    public boolean canJoin(Player player, boolean whitelisted) {
        return AccessPolicy.canJoin(enabled.get(), config.whitelistEnabled(), whitelisted, isBypass(player));
    }

    private void persistEnabled(boolean value) throws IOException {
        if (!YamlSectionUpdater.write(configFile, "maintenance", "enabled", Boolean.toString(value))) {
            logger.warn("Could not persist maintenance.enabled in config.yml. State is still applied in memory.");
        }
    }

    private void kickUnauthorizedPlayers(WhitelistManager whitelist) {
        Component reason = kickComponent();
        for (Player player : proxy.getAllPlayers()) {
            boolean whitelisted = whitelist.isWhitelisted(player);
            if (AccessPolicy.shouldKickExisting(
                    true,
                    true,
                    config.kickExistingExemptWhitelisted(),
                    whitelisted,
                    isBypass(player)
            )) {
                player.disconnect(reason);
            }
        }
    }
}
