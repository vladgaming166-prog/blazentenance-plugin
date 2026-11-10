package net.blazemc.blazentenance.common.config;

import java.util.List;
import java.util.Map;
import net.blazemc.blazentenance.common.Blazentenance;

public final class PluginConfig {

    private final boolean debug;
    private final boolean checkUpdates;
    private final UuidMode uuidMode;
    private final boolean maintenanceEnabled;
    private final List<String> kickMessage;
    private final boolean whitelistEnabled;
    private final String adminPermission;
    private final String bypassPermission;
    private final boolean backendProtectionEnabled;
    private final boolean preventDirectConnections;
    private final boolean kickExistingEnabled;
    private final boolean kickExistingExemptWhitelisted;
    private final String storageFile;
    private final String sharedSecret;
    private final String prefix;
    private final TextFormat textFormat;
    private final boolean joinTitleEnabled;
    private final String joinTitle;
    private final String joinSubtitle;
    private final boolean motdEnabled;
    private final List<String> motd;
    private final boolean versionNameEnabled;
    private final String versionName;
    private final Messages messages;

    private PluginConfig(
            boolean debug,
            boolean checkUpdates,
            UuidMode uuidMode,
            boolean maintenanceEnabled,
            List<String> kickMessage,
            boolean whitelistEnabled,
            String adminPermission,
            String bypassPermission,
            boolean backendProtectionEnabled,
            boolean preventDirectConnections,
            boolean kickExistingEnabled,
            boolean kickExistingExemptWhitelisted,
            String storageFile,
            String sharedSecret,
            String prefix,
            TextFormat textFormat,
            boolean joinTitleEnabled,
            String joinTitle,
            String joinSubtitle,
            boolean motdEnabled,
            List<String> motd,
            boolean versionNameEnabled,
            String versionName,
            Messages messages
    ) {
        this.debug = debug;
        this.checkUpdates = checkUpdates;
        this.uuidMode = uuidMode;
        this.maintenanceEnabled = maintenanceEnabled;
        this.kickMessage = kickMessage;
        this.whitelistEnabled = whitelistEnabled;
        this.adminPermission = adminPermission;
        this.bypassPermission = bypassPermission;
        this.backendProtectionEnabled = backendProtectionEnabled;
        this.preventDirectConnections = preventDirectConnections;
        this.kickExistingEnabled = kickExistingEnabled;
        this.kickExistingExemptWhitelisted = kickExistingExemptWhitelisted;
        this.storageFile = storageFile;
        this.sharedSecret = sharedSecret;
        this.prefix = prefix;
        this.textFormat = textFormat;
        this.joinTitleEnabled = joinTitleEnabled;
        this.joinTitle = joinTitle;
        this.joinSubtitle = joinSubtitle;
        this.motdEnabled = motdEnabled;
        this.motd = motd;
        this.versionNameEnabled = versionNameEnabled;
        this.versionName = versionName;
        this.messages = messages;
    }

    public static PluginConfig fromMap(Map<String, Object> root) {
        Map<String, Object> settings = YamlSupport.map(root, "settings");
        Map<String, Object> maintenance = YamlSupport.map(root, "maintenance");
        Map<String, Object> whitelist = YamlSupport.map(maintenance, "whitelist");
        Map<String, Object> permissions = YamlSupport.map(maintenance, "permissions");
        Map<String, Object> backend = YamlSupport.map(maintenance, "backend-protection");
        Map<String, Object> kickExisting = YamlSupport.map(maintenance, "kick-existing-players");
        Map<String, Object> storage = YamlSupport.map(root, "storage");
        Map<String, Object> security = YamlSupport.map(root, "security");
        Map<String, Object> messages = YamlSupport.map(root, "messages");
        Map<String, Object> title = YamlSupport.map(maintenance, "join-title");
        Map<String, Object> serverList = YamlSupport.map(maintenance, "server-list");

        List<String> defaultKick = List.of(
                "",
                "&c&lBLAZEMC NETWORK",
                "",
                "&cServerul BlazeMC Network este în lucru!",
                "&7Revenim în curând!",
                "",
                "&eMulțumim pentru înțelegere!",
                ""
        );

        return new PluginConfig(
                YamlSupport.bool(settings, "debug", false),
                YamlSupport.bool(settings, "check-updates", true),
                YamlSupport.enumerate(settings, "uuid-mode", UuidMode.class, UuidMode.AUTO),
                YamlSupport.bool(maintenance, "enabled", false),
                YamlSupport.stringList(maintenance, "kick-message", defaultKick),
                YamlSupport.bool(whitelist, "enabled", true),
                YamlSupport.string(permissions, "admin", Blazentenance.PERMISSION_ADMIN),
                YamlSupport.string(permissions, "bypass", Blazentenance.PERMISSION_BYPASS),
                YamlSupport.bool(backend, "enabled", true),
                YamlSupport.bool(backend, "prevent-direct-connections", true),
                YamlSupport.bool(kickExisting, "enabled", false),
                YamlSupport.bool(kickExisting, "exempt-whitelisted", true),
                YamlSupport.string(storage, "file", "players.yml"),
                YamlSupport.string(security, "shared-secret", ""),
                YamlSupport.string(messages, "prefix", "&8[&cBlazentenance&8] "),
                YamlSupport.enumerate(messages, "format", TextFormat.class, TextFormat.AUTO),
                YamlSupport.bool(title, "enabled", true),
                YamlSupport.string(title, "title", "&c&lMAINTENANCE"),
                YamlSupport.string(title, "subtitle", "&7Authorized access only"),
                YamlSupport.bool(serverList, "change-motd", true),
                YamlSupport.stringList(serverList, "motd", List.of("&c&lBLAZEMC NETWORK", "&7Currently under maintenance")),
                YamlSupport.bool(serverList, "change-version", true),
                YamlSupport.string(serverList, "version", "&cUnder Maintenance"),
                Messages.fromMap(messages)
        );
    }

    public boolean debug() {
        return debug;
    }

    public boolean checkUpdates() {
        return checkUpdates;
    }

    public UuidMode uuidMode() {
        return uuidMode;
    }

    public boolean maintenanceEnabled() {
        return maintenanceEnabled;
    }

    public List<String> kickMessage() {
        return kickMessage;
    }

    public boolean whitelistEnabled() {
        return whitelistEnabled;
    }

    public String adminPermission() {
        return adminPermission;
    }

    public String bypassPermission() {
        return bypassPermission;
    }

    public boolean backendProtectionEnabled() {
        return backendProtectionEnabled;
    }

    public boolean preventDirectConnections() {
        return preventDirectConnections;
    }

    public boolean kickExistingEnabled() {
        return kickExistingEnabled;
    }

    public boolean kickExistingExemptWhitelisted() {
        return kickExistingExemptWhitelisted;
    }

    public String storageFile() {
        return storageFile;
    }

    public String sharedSecret() {
        return sharedSecret;
    }

    public String prefix() {
        return prefix;
    }

    public TextFormat textFormat() {
        return textFormat;
    }

    public boolean joinTitleEnabled() {
        return joinTitleEnabled;
    }

    public String joinTitle() {
        return joinTitle;
    }

    public String joinSubtitle() {
        return joinSubtitle;
    }

    public boolean motdEnabled() {
        return motdEnabled;
    }

    public List<String> motd() {
        return motd;
    }

    public boolean versionNameEnabled() {
        return versionNameEnabled;
    }

    public String versionName() {
        return versionName;
    }

    public Messages messages() {
        return messages;
    }

    public PluginConfig withMaintenanceEnabled(boolean enabled) {
        return new PluginConfig(
                debug,
                checkUpdates,
                uuidMode,
                enabled,
                kickMessage,
                whitelistEnabled,
                adminPermission,
                bypassPermission,
                backendProtectionEnabled,
                preventDirectConnections,
                kickExistingEnabled,
                kickExistingExemptWhitelisted,
                storageFile,
                sharedSecret,
                prefix,
                textFormat,
                joinTitleEnabled,
                joinTitle,
                joinSubtitle,
                motdEnabled,
                motd,
                versionNameEnabled,
                versionName,
                messages
        );
    }

    public PluginConfig withSharedSecret(String secret) {
        return new PluginConfig(
                debug,
                checkUpdates,
                uuidMode,
                maintenanceEnabled,
                kickMessage,
                whitelistEnabled,
                adminPermission,
                bypassPermission,
                backendProtectionEnabled,
                preventDirectConnections,
                kickExistingEnabled,
                kickExistingExemptWhitelisted,
                storageFile,
                secret,
                prefix,
                textFormat,
                joinTitleEnabled,
                joinTitle,
                joinSubtitle,
                motdEnabled,
                motd,
                versionNameEnabled,
                versionName,
                messages
        );
    }

    public record Messages(
            String enabled,
            String disabled,
            String alreadyEnabled,
            String alreadyDisabled,
            String playerAdded,
            String playerRemoved,
            String playerNotFound,
            String playerAlreadyAdded,
            String noPermission,
            String whitelistEmpty,
            String reload,
            String usage,
            String invalidSubcommand,
            String usageAdd,
            String usageRemove,
            String resolvingUuid,
            String uuidResolveFailed,
            String invalidName,
            String statusOn,
            String statusOff,
            String helpHeader
    ) {
        static Messages fromMap(Map<String, Object> map) {
            return new Messages(
                    YamlSupport.string(map, "enabled", "&aMaintenance mode has been ENABLED!"),
                    YamlSupport.string(map, "disabled", "&aMaintenance mode has been DISABLED!"),
                    YamlSupport.string(map, "already-enabled", "&cMaintenance is already enabled!"),
                    YamlSupport.string(map, "already-disabled", "&cMaintenance is already disabled!"),
                    YamlSupport.string(map, "player-added", "&aPlayer &f{player} &ahas been added to the maintenance whitelist."),
                    YamlSupport.string(map, "player-removed", "&aPlayer &f{player} &ahas been removed from the maintenance whitelist."),
                    YamlSupport.string(map, "player-not-found", "&cPlayer &f{player} &cwas not found."),
                    YamlSupport.string(map, "player-already-added", "&cPlayer &f{player} &cis already whitelisted."),
                    YamlSupport.string(map, "no-permission", "&cYou don't have permission to execute this command!"),
                    YamlSupport.string(map, "whitelist-empty", "&cThe maintenance whitelist is empty!"),
                    YamlSupport.string(map, "reload", "&aConfiguration successfully reloaded!"),
                    YamlSupport.string(map, "usage", "&cUsage: /blazemaintenance <on|off|add|remove|list|status|reload>"),
                    YamlSupport.string(map, "invalid-subcommand", "&cUnknown subcommand. Use /blazemaintenance for help."),
                    YamlSupport.string(map, "usage-add", "&cUsage: /blazemaintenance add player <playername>"),
                    YamlSupport.string(map, "usage-remove", "&cUsage: /blazemaintenance remove player <playername>"),
                    YamlSupport.string(map, "resolving-uuid", "&7Resolving UUID for &f{player}&7..."),
                    YamlSupport.string(map, "uuid-resolve-failed", "&cCould not resolve UUID for &f{player}&c. Check the name or uuid-mode setting."),
                    YamlSupport.string(map, "invalid-name", "&c'{player}' is not a valid Minecraft username or UUID."),
                    YamlSupport.string(map, "status-on", "&cMaintenance is currently &lON"),
                    YamlSupport.string(map, "status-off", "&aMaintenance is currently &lOFF"),
                    YamlSupport.string(map, "help-header", "&cBlazentenance &7- Network Maintenance")
            );
        }
    }
}
