package net.blazemc.blazentenance.common.policy;

public enum AccessDecision {
    ALLOWED_OPEN,
    ALLOWED_WHITELIST,
    ALLOWED_BYPASS,
    DENIED_MAINTENANCE;

    public boolean allowed() {
        return this != DENIED_MAINTENANCE;
    }
}
