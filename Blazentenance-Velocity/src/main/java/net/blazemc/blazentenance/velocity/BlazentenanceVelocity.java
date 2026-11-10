package net.blazemc.blazentenance.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.nio.file.Path;
import net.blazemc.blazentenance.common.Blazentenance;
import net.blazemc.blazentenance.common.protocol.Channels;
import net.blazemc.blazentenance.velocity.command.BlazeMaintenanceCommand;
import net.blazemc.blazentenance.velocity.listener.ProxyConnectionListener;
import net.blazemc.blazentenance.velocity.listener.ProxyPingListener;
import net.blazemc.blazentenance.velocity.listener.ProxyPluginMessageListener;
import net.blazemc.blazentenance.velocity.maintenance.MaintenanceManager;
import net.blazemc.blazentenance.velocity.messaging.BackendBridge;
import net.blazemc.blazentenance.velocity.resolve.UuidResolver;
import net.blazemc.blazentenance.velocity.whitelist.WhitelistManager;
import org.slf4j.Logger;

@Plugin(
        id = Blazentenance.ID,
        name = Blazentenance.NAME,
        version = Blazentenance.VERSION,
        description = "Network-wide maintenance plugin for Velocity and Paper.",
        authors = {"BlazeMC Network"}
)
public final class BlazentenanceVelocity {

    public static final MinecraftChannelIdentifier AUTH_CHANNEL =
            MinecraftChannelIdentifier.from(Channels.AUTH);

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private MaintenanceManager maintenanceManager;
    private WhitelistManager whitelistManager;
    private UuidResolver uuidResolver;
    private BackendBridge backendBridge;

    @Inject
    public BlazentenanceVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.maintenanceManager = new MaintenanceManager(this, proxy, logger, dataDirectory);
        try {
            this.maintenanceManager.load();
        } catch (Exception exception) {
            logger.error("Failed to load Blazentenance. The proxy will refuse unsafe defaults.", exception);
            throw new IllegalStateException("Blazentenance failed to start", exception);
        }

        this.whitelistManager = new WhitelistManager(this, logger, dataDirectory, maintenanceManager);
        try {
            this.whitelistManager.load();
        } catch (Exception exception) {
            logger.error("Failed to load the maintenance whitelist.", exception);
            throw new IllegalStateException("Blazentenance whitelist failed to start", exception);
        }

        this.uuidResolver = new UuidResolver(proxy, maintenanceManager);
        this.backendBridge = new BackendBridge(this, proxy, maintenanceManager, whitelistManager, logger);

        proxy.getChannelRegistrar().register(AUTH_CHANNEL);
        proxy.getEventManager().register(this, new ProxyConnectionListener(this, proxy, maintenanceManager, whitelistManager, backendBridge, logger));
        proxy.getEventManager().register(this, new ProxyPingListener(maintenanceManager));
        proxy.getEventManager().register(this, new ProxyPluginMessageListener());

        CommandManager commands = proxy.getCommandManager();
        CommandMeta meta = commands.metaBuilder(Blazentenance.COMMAND)
                .aliases("blazentenance", "bmaint", "maintenance")
                .plugin(this)
                .build();
        commands.register(meta, new BlazeMaintenanceCommand(this, proxy, maintenanceManager, whitelistManager, uuidResolver));

        logger.info("{} loaded. Maintenance is currently {}.",
                Blazentenance.CONSOLE_PREFIX.trim(),
                maintenanceManager.enabled() ? "ENABLED" : "DISABLED");
        if (maintenanceManager.config().backendProtectionEnabled()) {
            logger.info("Backend handshake protection is enabled. Install Blazentenance-Paper on every Paper server and copy the shared secret.");
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (uuidResolver != null) {
            uuidResolver.close();
        }
        if (backendBridge != null) {
            backendBridge.shutdown();
        }
        if (whitelistManager != null) {
            try {
                whitelistManager.save();
            } catch (Exception exception) {
                logger.error("Failed to save the maintenance whitelist during shutdown.", exception);
            }
        }
        proxy.getChannelRegistrar().unregister(AUTH_CHANNEL);
        logger.info("Blazentenance has been disabled.");
    }

    public ProxyServer proxy() {
        return proxy;
    }

    public Logger logger() {
        return logger;
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public MaintenanceManager maintenance() {
        return maintenanceManager;
    }

    public WhitelistManager whitelist() {
        return whitelistManager;
    }
}
