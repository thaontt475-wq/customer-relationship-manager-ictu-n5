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
        return switch (path) {
            case "/api/customers" -> CUSTOMERS;
            case "/api/opportunities" -> OPPORTUNITIES;
            case "/api/activities" -> ACTIVITIES;
            case "/api/quotes" -> QUOTES;
            default -> null;
        };
    }
}