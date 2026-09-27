package com.crm.service.scope;

public record ScopeContext(
        long userId,
        Long teamId,
        String dataScope) {
}