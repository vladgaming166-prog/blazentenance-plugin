package net.blazemc.blazentenance.paper.command;

import java.util.List;
import java.util.Locale;
import net.blazemc.blazentenance.common.Blazentenance;
import net.blazemc.blazentenance.common.config.ConfigException;
import net.blazemc.blazentenance.paper.BlazentenancePaper;
import net.blazemc.blazentenance.paper.config.PaperConfig;
import net.blazemc.blazentenance.paper.protection.ForwardingInspector;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BackendCommand implements CommandExecutor, TabCompleter {

    private final BlazentenancePaper plugin;

    public BackendCommand(BlazentenancePaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isAdmin(sender)) {
            sender.sendMessage(plugin.formatter().prefixed(plugin.configuration().prefix(), plugin.configuration().noPermission()));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sendStatus(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            try {
                plugin.reloadPluginConfig();
                sender.sendMessage(plugin.formatter().prefixed(plugin.configuration().prefix(), plugin.configuration().reload()));
            } catch (ConfigException exception) {
                sender.sendMessage(plugin.formatter().deserialize("&cFailed to reload the Paper configuration."));
                plugin.getLogger().severe(exception.getMessage());
            }
            return true;
        }
        sender.sendMessage(plugin.formatter().prefixed(plugin.configuration().prefix(), plugin.configuration().proxyOnlyCommand()));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!isAdmin(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            String token = args[0].toLowerCase(Locale.ROOT);
            return List.of("status", "reload").stream().filter(value -> value.startsWith(token)).toList();
        }
        return List.of();
    }

    private void sendStatus(CommandSender sender) {
        PaperConfig config = plugin.configuration();
        ForwardingInspector.ForwardingStatus forwarding = plugin.forwardingStatus();
        sender.sendMessage(plugin.formatter().prefixed(config.prefix(), config.statusHeader()));
        sender.sendMessage(plugin.formatter().deserialize("&7Maintenance commands are handled by Velocity."));
        sender.sendMessage(plugin.formatter().deserialize("&7Backend protection: &f" + onOff(config.protectionEnabled())));
        sender.sendMessage(plugin.formatter().deserialize("&7Require handshake: &f" + onOff(config.requireProxyHandshake())));
        sender.sendMessage(plugin.formatter().deserialize("&7Shared secret configured: &f" + onOff(plugin.handshakeGuard().ready())));
        sender.sendMessage(plugin.formatter().deserialize("&7Velocity forwarding enabled: &f" + onOff(forwarding.velocityEnabled())));
        sender.sendMessage(plugin.formatter().deserialize("&7Velocity forwarding secret set: &f" + onOff(forwarding.secretConfigured())));
        sender.sendMessage(plugin.formatter().deserialize("&8Use /" + Blazentenance.COMMAND + " on the Velocity console to enable or disable maintenance."));
    }

    private boolean isAdmin(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender.hasPermission(Blazentenance.PERMISSION_ADMIN);
    }

    private static String onOff(boolean value) {
        return value ? "yes" : "no";
    }
}
