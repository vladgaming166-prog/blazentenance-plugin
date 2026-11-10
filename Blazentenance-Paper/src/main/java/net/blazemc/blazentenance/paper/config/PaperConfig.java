package net.blazemc.blazentenance.paper.config;

import java.util.Map;
import net.blazemc.blazentenance.common.config.TextFormat;
import net.blazemc.blazentenance.common.config.YamlSupport;

public final class PaperConfig {

    private final boolean debug;
    private final boolean protectionEnabled;
    private final boolean preventDirectConnections;
    private final boolean requireProxyHandshake;
    private final int handshakeTimeoutSeconds;
    private final boolean requireVelocityForwarding;
    private final String sharedSecret;
    private final String prefix;
    private final TextFormat textFormat;
    private final String noPermission;
    private final String reload;
    private final String statusHeader;
    private final String directConnectionDenied;
    private final String handshakeTimeout;
    private final String forwardingDisabled;
    private final String missingSecret;
    private final String unauthorized;
    private final String proxyOnlyCommand;

    private PaperConfig(
            boolean debug,
            boolean protectionEnabled,
            boolean preventDirectConnections,
            boolean requireProxyHandshake,
            int handshakeTimeoutSeconds,
            boolean requireVelocityForwarding,
            String sharedSecret,
            String prefix,
            TextFormat textFormat,
            String noPermission,
            String reload,
            String statusHeader,
            String directConnectionDenied,
            String handshakeTimeout,
            String forwardingDisabled,
            String missingSecret,
            String unauthorized,
            String proxyOnlyCommand
    ) {
        this.debug = debug;
        this.protectionEnabled = protectionEnabled;
        this.preventDirectConnections = preventDirectConnections;
        this.requireProxyHandshake = requireProxyHandshake;
        this.handshakeTimeoutSeconds = handshakeTimeoutSeconds;
        this.requireVelocityForwarding = requireVelocityForwarding;
        this.sharedSecret = sharedSecret;
        this.prefix = prefix;
        this.textFormat = textFormat;
        this.noPermission = noPermission;
        this.reload = reload;
        this.statusHeader = statusHeader;
        this.directConnectionDenied = directConnectionDenied;
        this.handshakeTimeout = handshakeTimeout;
        this.forwardingDisabled = forwardingDisabled;
        this.missingSecret = missingSecret;
        this.unauthorized = unauthorized;
        this.proxyOnlyCommand = proxyOnlyCommand;
    }

    public static PaperConfig fromMap(Map<String, Object> root) {
        Map<String, Object> settings = YamlSupport.map(root, "settings");
        Map<String, Object> protection = YamlSupport.map(root, "backend-protection");
        Map<String, Object> security = YamlSupport.map(root, "security");
        Map<String, Object> messages = YamlSupport.map(root, "messages");
        int timeout = Math.max(1, Math.min(30, YamlSupport.integer(protection, "handshake-timeout-seconds", 5)));
        return new PaperConfig(
                YamlSupport.bool(settings, "debug", false),
                YamlSupport.bool(protection, "enabled", true),
                YamlSupport.bool(protection, "prevent-direct-connections", true),
                YamlSupport.bool(protection, "require-proxy-handshake", true),
                timeout,
                YamlSupport.bool(protection, "require-velocity-forwarding", true),
                YamlSupport.string(security, "shared-secret", ""),
                YamlSupport.string(messages, "prefix", "&8[&cBlazentenance&8] "),
                YamlSupport.enumerate(messages, "format", TextFormat.class, TextFormat.AUTO),
                YamlSupport.string(messages, "no-permission", "&cYou don't have permission to execute this command!"),
                YamlSupport.string(messages, "reload", "&aPaper configuration successfully reloaded!"),
                YamlSupport.string(messages, "status-header", "&cBlazentenance &7- Backend Protection"),
                YamlSupport.string(messages, "direct-connection-denied", "&cDirect connections to this backend are not allowed!"),
                YamlSupport.string(messages, "handshake-timeout", "&cCould not verify your connection with the Velocity proxy."),
                YamlSupport.string(messages, "forwarding-disabled", "&cThis backend requires Velocity modern forwarding."),
                YamlSupport.string(messages, "missing-secret", "&cBlazentenance is not configured. Direct access is blocked."),
                YamlSupport.string(messages, "unauthorized", "&cServerul BlazeMC Network este în lucru!"),
                YamlSupport.string(messages, "proxy-only-command", "&cMaintenance is controlled from the Velocity proxy. Use /blazemaintenance there.")
        );
    }

    public boolean debug() {
        return debug;
    }

    public boolean protectionEnabled() {
        return protectionEnabled;
    }

    public boolean preventDirectConnections() {
        return preventDirectConnections;
    }

    public boolean requireProxyHandshake() {
        return requireProxyHandshake;
    }

    public int handshakeTimeoutSeconds() {
        return handshakeTimeoutSeconds;
    }

    public boolean requireVelocityForwarding() {
        return requireVelocityForwarding;
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

    public String noPermission() {
        return noPermission;
    }

    public String reload() {
        return reload;
    }

    public String statusHeader() {
        return statusHeader;
    }

    public String directConnectionDenied() {
        return directConnectionDenied;
    }

    public String handshakeTimeout() {
        return handshakeTimeout;
    }

    public String forwardingDisabled() {
        return forwardingDisabled;
    }

    public String missingSecret() {
        return missingSecret;
    }

    public String unauthorized() {
        return unauthorized;
    }

    public String proxyOnlyCommand() {
        return proxyOnlyCommand;
    }
}
