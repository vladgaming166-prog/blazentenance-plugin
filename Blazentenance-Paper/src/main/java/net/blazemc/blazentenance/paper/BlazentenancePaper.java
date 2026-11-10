package net.blazemc.blazentenance.paper;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.blazemc.blazentenance.common.Blazentenance;
import net.blazemc.blazentenance.common.config.ConfigException;
import net.blazemc.blazentenance.common.config.ConfigFiles;
import net.blazemc.blazentenance.common.message.MessageFormatter;
import net.blazemc.blazentenance.common.protocol.Channels;
import net.blazemc.blazentenance.paper.command.BackendCommand;
import net.blazemc.blazentenance.paper.config.PaperConfig;
import net.blazemc.blazentenance.paper.listener.BackendConnectionListener;
import net.blazemc.blazentenance.paper.listener.BackendPluginMessageListener;
import net.blazemc.blazentenance.paper.protection.ForwardingInspector;
import net.blazemc.blazentenance.paper.protection.HandshakeGuard;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class BlazentenancePaper extends JavaPlugin {

    private PaperConfig config;
    private MessageFormatter formatter;
    private HandshakeGuard handshakeGuard;
    private ForwardingInspector.ForwardingStatus forwardingStatus;

    @Override
    public void onEnable() {
        this.handshakeGuard = new HandshakeGuard();
        try {
            reloadPluginConfig();
        } catch (ConfigException exception) {
            getLogger().log(Level.SEVERE, "Failed to load Blazentenance configuration. The backend will refuse connections.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.forwardingStatus = new ForwardingInspector().inspect();
        logForwardingStatus();

        getServer().getMessenger().registerIncomingPluginChannel(this, Channels.AUTH, new BackendPluginMessageListener(this));
        getServer().getMessenger().registerOutgoingPluginChannel(this, Channels.AUTH);
        getServer().getPluginManager().registerEvents(new BackendConnectionListener(this), this);

        registerBackendCommand();

        getLogger().info(Blazentenance.CONSOLE_PREFIX + "Paper backend protection is enabled.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this, Channels.AUTH);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, Channels.AUTH);
        if (handshakeGuard != null) {
            handshakeGuard.reconfigure(config == null ? PaperConfig.fromMap(Map.of()) : config);
        }
        getLogger().info("Blazentenance backend protection has been disabled.");
    }

    public void reloadPluginConfig() throws ConfigException {
        Map<String, Object> raw = ConfigFiles.loadOrCreate(getDataFolder().toPath().resolve("config.yml"), getClass(), "paper-config.yml");
        this.config = PaperConfig.fromMap(raw);
        this.formatter = new MessageFormatter(config.textFormat());
        this.handshakeGuard.reconfigure(config);
        this.forwardingStatus = new ForwardingInspector().inspect();
    }

    public PaperConfig configuration() {
        return config;
    }

    public MessageFormatter formatter() {
        return formatter;
    }

    public HandshakeGuard handshakeGuard() {
        return handshakeGuard;
    }

    public ForwardingInspector.ForwardingStatus forwardingStatus() {
        return forwardingStatus;
    }

    public void debug(String message) {
        if (config != null && config.debug()) {
            getLogger().info(message);
        }
    }

    private void registerBackendCommand() {
        BackendCommand command = new BackendCommand(this);
        PluginCommand pluginCommand = getCommand("blazemaintenance");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
            return;
        }
        Command fallback = new Command("blazemaintenance") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                return command.onCommand(sender, this, label, args);
            }

            @Override
            public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                List<String> suggestions = command.onTabComplete(sender, this, alias, args);
                return suggestions == null ? List.of() : suggestions;
            }
        };
        fallback.setAliases(List.of("blazentenance", "bmaint"));
        fallback.setDescription("Shows Blazentenance backend protection status.");
        fallback.setPermission(Blazentenance.PERMISSION_ADMIN);
        getServer().getCommandMap().register("blazentenance", fallback);
    }

    private void logForwardingStatus() {
        if (!forwardingStatus.paperApiFound()) {
            getLogger().warning("Could not detect Paper Velocity forwarding settings. Configure proxies.velocity in config/paper-global.yml.");
            return;
        }
        if (!forwardingStatus.velocityEnabled()) {
            getLogger().severe("Velocity modern forwarding is DISABLED. Direct backend connections will be blocked by Blazentenance.");
            getLogger().severe("Set proxies.velocity.enabled to true in config/paper-global.yml and copy forwarding.secret from Velocity.");
            return;
        }
        if (!forwardingStatus.secretConfigured()) {
            getLogger().severe("Velocity forwarding is enabled but the forwarding secret is empty. Direct connections can be spoofed.");
            return;
        }
        getLogger().info("Detected Velocity modern forwarding from " + forwardingStatus.source() + ".");
    }
}
