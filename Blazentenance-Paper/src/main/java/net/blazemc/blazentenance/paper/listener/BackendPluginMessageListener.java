package net.blazemc.blazentenance.paper.listener;

import net.blazemc.blazentenance.common.protocol.Channels;
import net.blazemc.blazentenance.paper.BlazentenancePaper;
import net.blazemc.blazentenance.paper.protection.HandshakeGuard;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public final class BackendPluginMessageListener implements PluginMessageListener {

    private final BlazentenancePaper plugin;

    public BackendPluginMessageListener(BlazentenancePaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!Channels.AUTH.equals(channel)) {
            return;
        }
        HandshakeGuard.Decision decision = plugin.handshakeGuard().accept(message, player.getUniqueId(), player.getName());
        if (!decision.valid()) {
            plugin.debug("Rejected handshake for " + player.getName() + " (" + decision.reason() + ")");
            return;
        }
        plugin.debug("Accepted proxy handshake for " + player.getName() + " authorized=" + decision.authorized());
        if (plugin.configuration().protectionEnabled() && !decision.authorized()) {
            player.kick(plugin.formatter().deserialize(plugin.configuration().unauthorized()));
        }
    }
}
