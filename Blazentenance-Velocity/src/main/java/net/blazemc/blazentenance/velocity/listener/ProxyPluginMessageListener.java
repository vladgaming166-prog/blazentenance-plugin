package net.blazemc.blazentenance.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import net.blazemc.blazentenance.velocity.BlazentenanceVelocity;

/**
 * Prevents clients from spoofing Blazentenance plugin messages to backends.
 */
public final class ProxyPluginMessageListener {

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!BlazentenanceVelocity.AUTH_CHANNEL.equals(event.getIdentifier())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (event.getSource() instanceof Player) {
            // A client attempted to impersonate the proxy. Drop the packet.
        }
    }
}
