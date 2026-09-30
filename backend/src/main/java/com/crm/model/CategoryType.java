package com.crm.model;

/**
 * Các nhóm danh mục dùng chung trong CRM-44:
 * 1. Ngành nghề (Industries)
 * 2. Quy mô doanh nghiệp (Company Sizes)
 * 3. Nguồn Lead (Lead Sources)
 * 4. Loại hoạt động (Activity Types)
 */
public enum CategoryType {
    INDUSTRY("INDUSTRY", "Ngành nghề"),
    COMPANY_SIZE("COMPANY_SIZE", "Quy mô doanh nghiệp"),
    LEAD_SOURCE("LEAD_SOURCE", "Nguồn Lead"),
    ACTIVITY_TYPE("ACTIVITY_TYPE", "Loại hoạt động");

    private final String code;
    private final String displayName;

    CategoryType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a string to CategoryType safely, supporting various formats and plurals.
     * Examples: "industry", "industries", "company_size", "company-sizes", "lead_source", "lead_sources", "activity_type", "activity-types"
     */
    public static CategoryType fromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String clean = text.trim().toUpperCase().replace("-", "_").replace(" ", "_");

        for (CategoryType type : values()) {
            if (type.name().equalsIgnoreCase(clean) || type.code.equalsIgnoreCase(clean)) {
                return type;
            }
        }

        // Handle plurals and common aliases
        switch (clean) {
            case "INDUSTRIES":
            case "NGANH_NGHE":
                return INDUSTRY;
            case "COMPANY_SIZES":
            case "SIZES":
            case "QUY_MO":
                return COMPANY_SIZE;
            case "LEAD_SOURCES":
            case "SOURCES":
            case "NGUON_LEAD":
                return LEAD_SOURCE;
            case "ACTIVITY_TYPES":
            case "ACTIVITIES":
            case "LOAI_HOAT_DONG":
                return ACTIVITY_TYPE;
            default:
                return null;
        }
    }
}
