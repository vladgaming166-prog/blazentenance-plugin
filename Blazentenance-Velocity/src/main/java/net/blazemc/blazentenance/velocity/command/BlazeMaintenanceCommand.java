package net.blazemc.blazentenance.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.blazemc.blazentenance.common.Blazentenance;
import net.blazemc.blazentenance.common.config.PluginConfig;
import net.blazemc.blazentenance.common.message.Placeholders;
import net.blazemc.blazentenance.common.storage.WhitelistedPlayer;
import net.blazemc.blazentenance.common.storage.YamlWhitelistStore;
import net.blazemc.blazentenance.common.util.PlayerName;
import net.blazemc.blazentenance.common.util.Uuids;
import net.blazemc.blazentenance.velocity.BlazentenanceVelocity;
import net.blazemc.blazentenance.velocity.maintenance.MaintenanceManager;
import net.blazemc.blazentenance.velocity.resolve.UuidResolver;
import net.blazemc.blazentenance.velocity.whitelist.WhitelistManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class BlazeMaintenanceCommand implements SimpleCommand {

    private static final List<String> ROOT = List.of("on", "off", "add", "remove", "list", "status", "reload");

    private final BlazentenanceVelocity plugin;
    private final ProxyServer proxy;
    private final MaintenanceManager maintenance;
    private final WhitelistManager whitelist;
    private final UuidResolver uuidResolver;

    public BlazeMaintenanceCommand(
            BlazentenanceVelocity plugin,
            ProxyServer proxy,
            MaintenanceManager maintenance,
            WhitelistManager whitelist,
            UuidResolver uuidResolver
    ) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.maintenance = maintenance;
        this.whitelist = whitelist;
        this.uuidResolver = uuidResolver;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!maintenance.isAdmin(source)) {
            sendPrefixed(source, maintenance.config().messages().noPermission());
            return;
        }
        String[] args = invocation.arguments();
        if (args.length == 0) {
            sendHelp(source);
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "on" -> toggle(source, true);
            case "off" -> toggle(source, false);
            case "add" -> add(source, args);
            case "remove" -> remove(source, args);
            case "list" -> list(source);
            case "status" -> status(source);
            case "reload" -> reload(source);
            default -> sendPrefixed(source, maintenance.config().messages().invalidSubcommand());
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!maintenance.isAdmin(invocation.source())) {
            return List.of();
        }
        String[] args = invocation.arguments();
        if (args.length == 0) {
            return ROOT;
        }
        if (args.length == 1) {
            return prefix(ROOT, args[0]);
        }
        if (args.length == 2 && (equalsIgnore(args[0], "add") || equalsIgnore(args[0], "remove"))) {
            return prefix(List.of("player"), args[1]);
        }
        if (args.length == 3 && equalsIgnore(args[1], "player")
                && (equalsIgnore(args[0], "add") || equalsIgnore(args[0], "remove"))) {
            List<String> names = proxy.getAllPlayers().stream().map(Player::getUsername).sorted().toList();
            return prefix(names, args[2]);
        }
        return List.of();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(suggest(invocation));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

    private void toggle(CommandSource source, boolean enable) {
        try {
            boolean changed = enable ? maintenance.enable(whitelist) : maintenance.disable();
            PluginConfig.Messages messages = maintenance.config().messages();
            if (!changed) {
                sendPrefixed(source, enable ? messages.alreadyEnabled() : messages.alreadyDisabled());
                return;
            }
            sendPrefixed(source, enable ? messages.enabled() : messages.disabled());
        } catch (Exception exception) {
            source.sendMessage(Component.text("Failed to update maintenance state.", NamedTextColor.RED));
            plugin.logger().error("Failed to toggle maintenance.", exception);
        }
    }

    private void add(CommandSource source, String[] args) {
        if (args.length < 3 || !equalsIgnore(args[1], "player")) {
            sendPrefixed(source, maintenance.config().messages().usageAdd());
            return;
        }
        String raw = args[2];
        if (Uuids.parse(raw).isEmpty() && !PlayerName.isValid(raw)) {
            sendPrefixed(source, Placeholders.apply(maintenance.config().messages().invalidName(), Map.of("player", raw)));
            return;
        }
        sendPrefixed(source, Placeholders.apply(maintenance.config().messages().resolvingUuid(), Map.of("player", raw)));
        uuidResolver.resolve(raw).whenComplete((resolved, error) -> proxy.getScheduler().buildTask(plugin, () -> {
            if (error != null || resolved.isEmpty()) {
                sendPrefixed(source, Placeholders.apply(maintenance.config().messages().uuidResolveFailed(), Map.of("player", raw)));
                return;
            }
            try {
                UuidResolver.ResolvedProfile profile = resolved.get();
                YamlWhitelistStore.AddResult result = whitelist.add(profile.uuid(), profile.name());
                if (result == YamlWhitelistStore.AddResult.ALREADY_PRESENT) {
                    sendPrefixed(source, Placeholders.apply(
                            maintenance.config().messages().playerAlreadyAdded(),
                            Map.of("player", profile.name(), "uuid", profile.uuid().toString())
                    ));
                    return;
                }
                sendPrefixed(source, Placeholders.apply(
                        maintenance.config().messages().playerAdded(),
                        Map.of("player", profile.name(), "uuid", profile.uuid().toString())
                ));
                source.sendMessage(maintenance.formatter().deserialize(
                        maintenance.config().prefix() + "&7UUID: &f" + profile.uuid()
                ));
            } catch (Exception exception) {
                source.sendMessage(Component.text("Failed to update the whitelist.", NamedTextColor.RED));
                plugin.logger().error("Failed to add a whitelist entry.", exception);
            }
        }).schedule());
    }

    private void remove(CommandSource source, String[] args) {
        if (args.length < 3 || !equalsIgnore(args[1], "player")) {
            sendPrefixed(source, maintenance.config().messages().usageRemove());
            return;
        }
        String raw = args[2];
        try {
            var removed = whitelist.remove(raw);
            if (removed.isEmpty()) {
                sendPrefixed(source, Placeholders.apply(maintenance.config().messages().playerNotFound(), Map.of("player", raw)));
                return;
            }
            WhitelistedPlayer player = removed.get();
            sendPrefixed(source, Placeholders.apply(
                    maintenance.config().messages().playerRemoved(),
                    Map.of("player", player.name(), "uuid", player.uuid().toString())
            ));
        } catch (Exception exception) {
            source.sendMessage(Component.text("Failed to update the whitelist.", NamedTextColor.RED));
            plugin.logger().error("Failed to remove a whitelist entry.", exception);
        }
    }

    private void list(CommandSource source) {
        List<WhitelistedPlayer> players = whitelist.list();
        if (players.isEmpty()) {
            sendPrefixed(source, maintenance.config().messages().whitelistEmpty());
            return;
        }
        sendPrefixed(source, "&7Maintenance whitelist (&f" + players.size() + "&7):");
        for (WhitelistedPlayer player : players) {
            source.sendMessage(maintenance.formatter().deserialize(
                    "&8- &f" + player.name() + " &8(&7" + player.uuid() + "&8)"
            ));
        }
    }

    private void status(CommandSource source) {
        boolean on = maintenance.enabled();
        sendPrefixed(source, on ? maintenance.config().messages().statusOn() : maintenance.config().messages().statusOff());
        source.sendMessage(maintenance.formatter().deserialize(
                maintenance.config().prefix() + "&7Whitelisted players: &f" + whitelist.list().size()
        ));
        source.sendMessage(maintenance.formatter().deserialize(
                maintenance.config().prefix() + "&7Kick existing players: &f" + maintenance.config().kickExistingEnabled()
        ));
    }

    private void reload(CommandSource source) {
        try {
            maintenance.reload();
            whitelist.load();
            sendPrefixed(source, maintenance.config().messages().reload());
        } catch (Exception exception) {
            source.sendMessage(Component.text("Failed to reload Blazentenance.", NamedTextColor.RED));
            plugin.logger().error("Failed to reload configuration.", exception);
        }
    }

    private void sendHelp(CommandSource source) {
        sendPrefixed(source, maintenance.config().messages().helpHeader());
        source.sendMessage(maintenance.formatter().deserialize("&8/&fblazemaintenance on &8- &7Enable global maintenance"));
        source.sendMessage(maintenance.formatter().deserialize("&8/&fblazemaintenance off &8- &7Disable global maintenance"));
        source.sendMessage(maintenance.formatter().deserialize("&8/&fblazemaintenance add player <name> &8- &7Whitelist a player"));
        source.sendMessage(maintenance.formatter().deserialize("&8/&fblazemaintenance remove player <name> &8- &7Remove a player"));
        source.sendMessage(maintenance.formatter().deserialize("&8/&fblazemaintenance list &8- &7Show authorized players"));
        source.sendMessage(maintenance.formatter().deserialize("&8/&fblazemaintenance status &8- &7Show maintenance status"));
        source.sendMessage(maintenance.formatter().deserialize("&8/&fblazemaintenance reload &8- &7Reload configuration"));
    }

    private void sendPrefixed(CommandSource source, String message) {
        source.sendMessage(maintenance.formatter().prefixed(maintenance.config().prefix(), message));
    }

    private static boolean equalsIgnore(String left, String right) {
        return left.equalsIgnoreCase(right);
    }

    private static List<String> prefix(List<String> values, String token) {
        String needle = token.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(needle)).toList();
    }
}
