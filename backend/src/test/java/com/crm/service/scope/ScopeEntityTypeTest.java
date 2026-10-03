package com.crm.service.scope;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopeEntityTypeTest {

    @Test
    void allFourScopedApiFamiliesResolveToAllowlistedEntities() {
        Map<String, ScopeEntityType> routes = Map.of(
                "/api/customers", ScopeEntityType.CUSTOMERS,
                "/api/opportunities", ScopeEntityType.OPPORTUNITIES,
                "/api/activities", ScopeEntityType.ACTIVITIES,
                "/api/quotes", ScopeEntityType.QUOTES
        );

        routes.forEach((route, expected) ->
                assertEquals(expected, ScopeEntityType.fromServletPath(route)));
    }

    @Test
    void selfScopeDeniesAnotherOwnerForEveryEntityFamily() {
        ScopeAccessPolicy policy = new ScopeAccessPolicy();
        ScopeContext employeeA = new ScopeContext(10L, 1L, "SELF");

        for (ScopeEntityType entityType : ScopeEntityType.values()) {
            ScopeRecord ownedByEmployeeB = new ScopeRecord(100L, "Bản ghi của B (" + entityType + ")", 20L, 1L);
            assertFalse(policy.canAccess(employeeA, ownedByEmployeeB));
        }
    }

    @Test
    void teamScopeAllowsSameTeamAndDeniesOtherTeamForEveryEntityFamily() {
        ScopeAccessPolicy policy = new ScopeAccessPolicy();
        ScopeContext employeeA = new ScopeContext(10L, 1L, "TEAM");

        for (ScopeEntityType entityType : ScopeEntityType.values()) {
            ScopeRecord sameTeam = new ScopeRecord(100L, "Bản ghi cùng nhóm (" + entityType + ")", 20L, 1L);
            ScopeRecord otherTeam = new ScopeRecord(101L, "Bản ghi khác nhóm (" + entityType + ")", 30L, 2L);

            assertTrue(policy.canAccess(employeeA, sameTeam));
            assertFalse(policy.canAccess(employeeA, otherTeam));
        }
    }

    @Test
    void allScopeAllowsEveryOwnerForEveryEntityFamily() {
        ScopeAccessPolicy policy = new ScopeAccessPolicy();
        ScopeContext employeeA = new ScopeContext(10L, 1L, "ALL");

        for (ScopeEntityType entityType : ScopeEntityType.values()) {
            ScopeRecord otherOwner = new ScopeRecord(100L, "Bản ghi bất kỳ (" + entityType + ")", 999L, 99L);
            assertTrue(policy.canAccess(employeeA, otherOwner));
        }
    }
}
