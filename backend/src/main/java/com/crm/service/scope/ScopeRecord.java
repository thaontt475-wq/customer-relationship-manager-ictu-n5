package com.crm.service.scope;

public record ScopeRecord(
        long id,
        String label,
        long ownerUserId,
        Long ownerTeamId) {
}