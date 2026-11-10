package net.blazemc.blazentenance.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.blazemc.blazentenance.common.config.PluginConfig;
import net.blazemc.blazentenance.velocity.maintenance.MaintenanceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ProxyPingListener {

    private final MaintenanceManager maintenance;

    public ProxyPingListener(MaintenanceManager maintenance) {
        this.maintenance = maintenance;
    }

    @Subscribe
    public void onPing(ProxyPingEvent event) {
        if (!maintenance.enabled()) {
            return;
        }
        PluginConfig config = maintenance.config();
        ServerPing.Builder builder = event.getPing().asBuilder();
        if (config.motdEnabled()) {
            builder.description(maintenance.formatter().lines(config.motd()));
        }
        if (config.versionNameEnabled()) {
            Component version = maintenance.formatter().deserialize(config.versionName());
            String plain = LegacyComponentSerializer.legacySection().serialize(version);
            builder.version(new ServerPing.Version(event.getPing().getVersion().getProtocol(), plain));
        }
        event.setPing(builder.build());
    }
}
