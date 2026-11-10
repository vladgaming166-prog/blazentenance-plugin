package net.blazemc.blazentenance.common.policy;

/**
 * Pure access rules used by both Velocity and tests.
 *
 * <p>Velocity remains the authority. Paper uses the same policy only as
 * defence-in-depth after a signed proxy handshake.
 */
public final class AccessPolicy {

    private AccessPolicy() {
    }

    public static AccessDecision decide(
            boolean maintenanceEnabled,
            boolean whitelistEnabled,
            boolean whitelisted,
            boolean bypassPermission
    ) {
        if (!maintenanceEnabled) {
            return AccessDecision.ALLOWED_OPEN;
        }
        if (whitelisted) {
            return AccessDecision.ALLOWED_WHITELIST;
        }
        if (bypassPermission) {
            return AccessDecision.ALLOWED_BYPASS;
        }
        if (!whitelistEnabled) {
            return AccessDecision.DENIED_MAINTENANCE;
        }
        return AccessDecision.DENIED_MAINTENANCE;
    }

    public static boolean canJoin(
            boolean maintenanceEnabled,
            boolean whitelistEnabled,
            boolean whitelisted,
            boolean bypassPermission
    ) {
        return decide(maintenanceEnabled, whitelistEnabled, whitelisted, bypassPermission).allowed();
    }

    public static boolean shouldKickExisting(
            boolean maintenanceEnabled,
            boolean kickExistingEnabled,
            boolean exemptWhitelisted,
            boolean whitelisted,
            boolean bypassPermission
    ) {
        if (!maintenanceEnabled || !kickExistingEnabled) {
            return false;
        }
        if (bypassPermission) {
            return false;
        }
        if (exemptWhitelisted && whitelisted) {
            return false;
        }
        return !whitelisted;
    }
}
