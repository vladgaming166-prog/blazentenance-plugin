package net.blazemc.blazentenance.common.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccessPolicyTest {

    @Test
    void openNetworkAllowsEveryone() {
        assertTrue(AccessPolicy.canJoin(false, true, false, false));
    }

    @Test
    void maintenanceBlocksUnauthorizedPlayers() {
        assertFalse(AccessPolicy.canJoin(true, true, false, false));
    }

    @Test
    void whitelistAllowsJoinDuringMaintenance() {
        assertTrue(AccessPolicy.canJoin(true, true, true, false));
    }

    @Test
    void bypassPermissionAllowsJoinDuringMaintenance() {
        assertTrue(AccessPolicy.canJoin(true, true, false, true));
    }

    @Test
    void existingPlayersAreNotKickedByDefault() {
        assertFalse(AccessPolicy.shouldKickExisting(true, false, true, false, false));
    }

    @Test
    void existingUnauthorizedPlayersCanBeKicked() {
        assertTrue(AccessPolicy.shouldKickExisting(true, true, true, false, false));
    }

    @Test
    void whitelistedPlayersAreNeverKickedWhenExempt() {
        assertFalse(AccessPolicy.shouldKickExisting(true, true, true, true, false));
    }

    @Test
    void bypassPlayersAreNeverKicked() {
        assertFalse(AccessPolicy.shouldKickExisting(true, true, true, false, true));
    }
}
