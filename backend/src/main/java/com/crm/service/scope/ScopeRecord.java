package com.crm.service.scope;

public record ScopeRecord(
        long id,
        String label,
        long ownerUserId,
        Long ownerTeamId,
        String ownerName,
        String ownerTeamName,
        String createdAt) {

    public ScopeRecord(long id, String label, long ownerUserId, Long ownerTeamId) {
        this(id, label, ownerUserId, ownerTeamId, null, null, null);
    }
}