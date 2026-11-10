package net.blazemc.blazentenance.paper.listener;

import net.blazemc.blazentenance.common.security.Secrets;
import net.blazemc.blazentenance.paper.BlazentenancePaper;
import net.blazemc.blazentenance.paper.config.PaperConfig;
import net.blazemc.blazentenance.paper.protection.HandshakeGuard;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BackendConnectionListener implements Listener {

    private final BlazentenancePaper plugin;

    public BackendConnectionListener(BlazentenancePaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        PaperConfig config = plugin.configuration();
        if (!config.protectionEnabled() || !config.preventDirectConnections()) {
            return;
        }
        if (config.requireVelocityForwarding() && !plugin.forwardingStatus().ready()) {
            deny(event, config.forwardingDisabled());
            return;
        }
        if (config.requireProxyHandshake() && !Secrets.isConfigured(config.sharedSecret())) {
            deny(event, config.missingSecret());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        PaperConfig config = plugin.configuration();
        if (!config.protectionEnabled() || !config.preventDirectConnections()) {
            return;
        }
        if (config.requireVelocityForwarding() && !plugin.forwardingStatus().ready()) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, component(config.forwardingDisabled()));
            return;
        }
        HandshakeGuard.Decision decision = plugin.handshakeGuard().peek(event.getPlayer().getUniqueId());
        if (decision != null && decision.valid() && !decision.authorized()) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, component(config.unauthorized()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        PaperConfig config = plugin.configuration();
        if (!config.protectionEnabled() || !config.requireProxyHandshake()) {
            return;
        }
        Player player = event.getPlayer();
        HandshakeGuard.Decision existing = plugin.handshakeGuard().peek(player.getUniqueId());
        if (existing != null && existing.valid()) {
            if (!existing.authorized()) {
                player.kick(component(config.unauthorized()));
            }
            return;
        }
        long ticks = config.handshakeTimeoutSeconds() * 20L;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            HandshakeGuard.Decision decision = plugin.handshakeGuard().peek(player.getUniqueId());
            if (decision != null && decision.valid()) {
                if (!decision.authorized()) {
                    player.kick(component(config.unauthorized()));
                }
                return;
            }
            plugin.debug("Handshake timed out for " + player.getName());
            player.kick(component(config.handshakeTimeout()));
        }, ticks);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.handshakeGuard().forget(event.getPlayer().getUniqueId());
    }

    private void deny(AsyncPlayerPreLoginEvent event, String message) {
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, component(message));
    }

    private Component component(String message) {
        return plugin.formatter().prefixed(plugin.configuration().prefix(), message);
    }
}
