package net.blazemc.blazentenance.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import java.util.UUID;
import net.blazemc.blazentenance.common.policy.AccessDecision;
import net.blazemc.blazentenance.common.policy.AccessPolicy;
import net.blazemc.blazentenance.velocity.BlazentenanceVelocity;
import net.blazemc.blazentenance.velocity.maintenance.MaintenanceManager;
import net.blazemc.blazentenance.velocity.messaging.BackendBridge;
import net.blazemc.blazentenance.velocity.whitelist.WhitelistManager;
import net.kyori.adventure.title.Title;
import org.slf4j.Logger;

public final class ProxyConnectionListener {

    private final MaintenanceManager maintenance;
    private final WhitelistManager whitelist;
    private final BackendBridge backendBridge;
    private final Logger logger;

    public ProxyConnectionListener(
            BlazentenanceVelocity plugin,
            com.velocitypowered.api.proxy.ProxyServer proxy,
            MaintenanceManager maintenance,
            WhitelistManager whitelist,
            BackendBridge backendBridge,
            Logger logger
    ) {
        this.maintenance = maintenance;
        this.whitelist = whitelist;
        this.backendBridge = backendBridge;
        this.logger = logger;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPreLogin(PreLoginEvent event) {
        if (!maintenance.enabled() || !event.getResult().isAllowed()) {
            return;
        }
        String username = event.getUsername();
        UUID uuid = event.getUniqueId();
        boolean whitelisted = whitelist.isWhitelisted(uuid, username);
        if (whitelisted) {
            return;
        }
        if (maintenance.config().debug()) {
            logger.info("Pre-login for {} will be decided after authentication so bypass permissions can be applied.", username);
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onLogin(LoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }
        Player player = event.getPlayer();
        boolean whitelisted = whitelist.isWhitelisted(player);
        AccessDecision decision = AccessPolicy.decide(
                maintenance.enabled(),
                maintenance.config().whitelistEnabled(),
                whitelisted,
                maintenance.isBypass(player)
        );
        if (decision.allowed()) {
            if (maintenance.config().debug()) {
                logger.info("Allowed {} during login ({})", player.getUsername(), decision);
            }
            return;
        }
        event.setResult(ResultedEvent.ComponentResult.denied(maintenance.kickComponent()));
        if (maintenance.config().debug()) {
            logger.info("Denied {} because maintenance is enabled.", player.getUsername());
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (!maintenance.enabled() || !maintenance.config().joinTitleEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        player.showTitle(Title.title(
                maintenance.formatter().deserialize(maintenance.config().joinTitle()),
                maintenance.formatter().deserialize(maintenance.config().joinSubtitle())
        ));
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (!event.getResult().isAllowed() || !maintenance.enabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (maintenance.canJoin(player, whitelist.isWhitelisted(player))) {
            return;
        }
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        player.disconnect(maintenance.kickComponent());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        backendBridge.sendHandshake(event.getPlayer());
    }
}
