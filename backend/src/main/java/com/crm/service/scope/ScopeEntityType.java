package com.crm.service.scope;

public enum ScopeEntityType {
    CUSTOMERS("customers", "name", "Khách hàng", "customers"),
    OPPORTUNITIES("opportunities", "name", "Cơ hội", "opportunities"),
    ACTIVITIES("activities", "subject", "Hoạt động", "activities"),
    QUOTES("quotes", "quote_number", "Báo giá", "quotes");

    private final String tableName;
    private final String labelColumn;
    private final String displayName;
    private final String slug;

    ScopeEntityType(String tableName, String labelColumn, String displayName, String slug) {
        this.tableName = tableName;
        this.labelColumn = labelColumn;
        this.displayName = displayName;
        this.slug = slug;
    }

    public String tableName() {
        return tableName;
    }

    public String labelColumn() {
        return labelColumn;
    }

    public String displayName() {
        return displayName;
    }

    public String slug() {
        return slug;
    }

    public static ScopeEntityType fromServletPath(String path) {
        if (path == null) {
            return null;
        }

        String normalized = path.endsWith("/") && path.length() > 1
                ? path.substring(0, path.length() - 1)
                : path;

        return switch (normalized) {
            case "/api/customers", "/customers" -> CUSTOMERS;
            case "/api/opportunities", "/opportunities" -> OPPORTUNITIES;
            case "/api/activities", "/activities" -> ACTIVITIES;
            case "/api/quotes", "/quotes" -> QUOTES;
            default -> null;
        };
    }

    public static ScopeEntityType fromTypeName(String name) {
        if (name == null || name.isBlank()) {
            return CUSTOMERS;
        }

        String lower = name.trim().toLowerCase();
        return switch (lower) {
            case "customers", "customer", "khach-hang" -> CUSTOMERS;
            case "opportunities", "opportunity", "co-hoi" -> OPPORTUNITIES;
            case "activities", "activity", "hoat-dong" -> ACTIVITIES;
            case "quotes", "quote", "bao-gia" -> QUOTES;
            default -> CUSTOMERS;
        };
    }
}