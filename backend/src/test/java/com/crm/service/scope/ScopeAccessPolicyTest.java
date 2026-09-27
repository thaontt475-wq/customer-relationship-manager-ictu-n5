package com.crm.service.scope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScopeAccessPolicyTest {

    private final ScopeAccessPolicy policy =
            new ScopeAccessPolicy();

    @Test
    void employeeAWithSelfScopeCannotReadEmployeeBCustomer() {
        ScopeContext employeeA =
                new ScopeContext(10L, 1L, "SELF");

        ScopeRecord customerOwnedByB =
                new ScopeRecord(
                        100L,
                        "Khách hàng của nhân viên B",
                        20L,
                        1L
                );

        assertFalse(
                policy.canAccess(
                        employeeA,
                        customerOwnedByB
                )
        );
    }

    @Test
    void teamScopeAllowsSameTeamButBlocksOtherTeam() {
        ScopeContext employeeA =
                new ScopeContext(10L, 1L, "TEAM");

        ScopeRecord sameTeam =
                new ScopeRecord(1L, "A", 20L, 1L);

        ScopeRecord otherTeam =
                new ScopeRecord(2L, "B", 30L, 2L);

        assertTrue(policy.canAccess(employeeA, sameTeam));
        assertFalse(policy.canAccess(employeeA, otherTeam));
    }

    @Test
    void allScopeAllowsOtherOwners() {
        ScopeContext employeeA =
                new ScopeContext(10L, 1L, "ALL");

        ScopeRecord otherOwner =
                new ScopeRecord(1L, "A", 999L, 99L);

        assertTrue(policy.canAccess(employeeA, otherOwner));
    }
}