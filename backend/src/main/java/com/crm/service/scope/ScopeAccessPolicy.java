package com.crm.service.scope;

import java.util.Locale;
import java.util.Objects;

public class ScopeAccessPolicy {

    public boolean canAccess(ScopeContext actor, ScopeRecord record) {
        if (actor == null || record == null) {
            return false;
        }

        String scope = actor.dataScope() == null
                ? "SELF"
                : actor.dataScope().trim().toUpperCase(Locale.ROOT);

        return switch (scope) {
            case "ALL" -> true;
            case "TEAM" -> actor.teamId() != null
                    && Objects.equals(actor.teamId(), record.ownerTeamId());
            case "SELF" -> record.ownerUserId() == actor.userId();
            default -> false;
        };
    }
}