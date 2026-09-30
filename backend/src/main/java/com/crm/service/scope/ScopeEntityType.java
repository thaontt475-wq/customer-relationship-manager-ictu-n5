package com.crm.service.scope;

public enum ScopeEntityType {
    CUSTOMERS("customers", "name"),
    OPPORTUNITIES("opportunities", "name"),
    ACTIVITIES("activities", "subject"),
    QUOTES("quotes", "quote_number");

    private final String tableName;
    private final String labelColumn;

    ScopeEntityType(String tableName, String labelColumn) {
        this.tableName = tableName;
        this.labelColumn = labelColumn;
    }

    public String tableName() {
        return tableName;
    }

    public String labelColumn() {
        return labelColumn;
    }

    public static ScopeEntityType fromServletPath(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("/api/customers")) return CUSTOMERS;
        if (path.startsWith("/api/opportunities")) return OPPORTUNITIES;
        if (path.startsWith("/api/activities")) return ACTIVITIES;
        if (path.startsWith("/api/quotes")) return QUOTES;
        return null;
    }
}