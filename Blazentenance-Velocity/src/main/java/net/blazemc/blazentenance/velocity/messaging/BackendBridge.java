package net.blazemc.blazentenance.velocity.messaging;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.concurrent.TimeUnit;
import net.blazemc.blazentenance.common.security.AuthToken;
import net.blazemc.blazentenance.common.security.AuthTokenCodec;
import net.blazemc.blazentenance.velocity.BlazentenanceVelocity;
import net.blazemc.blazentenance.velocity.maintenance.MaintenanceManager;
import net.blazemc.blazentenance.velocity.whitelist.WhitelistManager;
import org.slf4j.Logger;

/**
 * Sends signed authorization tokens to Paper backends. Tokens are never
 * accepted from Minecraft clients.
 */
public final class BackendBridge {

    private final BlazentenanceVelocity plugin;
    private final ProxyServer proxy;
    private final MaintenanceManager maintenance;
    private final WhitelistManager whitelist;
    private final Logger logger;
    private volatile boolean shutdown;

    public BackendBridge(
            BlazentenanceVelocity plugin,
            ProxyServer proxy,
            MaintenanceManager maintenance,
            WhitelistManager whitelist,
            Logger logger
    ) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.maintenance = maintenance;
        this.whitelist = whitelist;
        this.logger = logger;
    }

    public void sendHandshake(Player player) {
        if (shutdown || !maintenance.config().backendProtectionEnabled()) {
            return;
        }
        sendNow(player);
        proxy.getScheduler().buildTask(plugin, () -> sendNow(player))
                .delay(250, TimeUnit.MILLISECONDS)
                .schedule();
        proxy.getScheduler().buildTask(plugin, () -> sendNow(player))
                .delay(1, TimeUnit.SECONDS)
                .schedule();
    }

    public void shutdown() {
        this.shutdown = true;
    }

    private void sendNow(Player player) {
        if (shutdown || !player.isActive()) {
            return;
        }
        player.getCurrentServer().ifPresent(connection -> send(player, connection));
    }

    private void send(Player player, ServerConnection connection) {
        try {
            boolean authorized = maintenance.canJoin(player, whitelist.isWhitelisted(player));
            AuthToken token = AuthToken.issue(
                    player.getUniqueId(),
                    player.getUsername(),
                    authorized,
                    maintenance.enabled(),
                    AuthTokenCodec.randomNonce()
            );
            byte[] payload = maintenance.authTokenCodec().encode(token);
            boolean sent = connection.sendPluginMessage(BlazentenanceVelocity.AUTH_CHANNEL, payload);
            if (!sent) {
                RegisteredServer server = connection.getServer();
                server.sendPluginMessage(BlazentenanceVelocity.AUTH_CHANNEL, payload);
            }
            if (maintenance.config().debug()) {
                logger.info("Sent backend handshake for {} to {} (authorized={}, maintenance={})",
                        player.getUsername(),
                        connection.getServerInfo().getName(),
                        authorized,
                        maintenance.enabled());
            }
        } catch (RuntimeException exception) {
            logger.warn("Failed to send backend handshake for {}.", player.getUsername(), exception);
        }
    }
}
