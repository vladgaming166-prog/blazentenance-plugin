package net.blazemc.blazentenance.common.protocol;

/**
 * Plugin-message channels used between the Velocity proxy and Paper backends.
 *
 * <p>These channels are never forwarded from Minecraft clients. Velocity intercepts
 * them and only the proxy may emit signed payloads.
 */
public final class Channels {

    public static final String NAMESPACE = "blazentenance";
    public static final String AUTH_KEY = "auth";
    public static final String AUTH = NAMESPACE + ":" + AUTH_KEY;

    private Channels() {
    }
}
