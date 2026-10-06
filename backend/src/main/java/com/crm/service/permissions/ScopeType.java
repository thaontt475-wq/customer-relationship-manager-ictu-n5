package com.crm.service.permissions;

public enum ScopeType {
    SELF, TEAM, ALL;

    public static ScopeType widest(ScopeType left, ScopeType right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
