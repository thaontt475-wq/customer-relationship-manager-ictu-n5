package com.crm.service.categories;

/**
 * Exception thrown when a master data category cannot be deleted
 * because it is referenced by other records (Customers, Leads, Activities, etc.).
 * Mapped to HTTP 409 Conflict / 400 Bad Request.
 */
public class CategoryInUseException extends Exception {
    private static final long serialVersionUID = 1L;

    private final String categoryType;
    private final long categoryId;

    public CategoryInUseException(String categoryType, long categoryId, String message) {
        super(message);
        this.categoryType = categoryType;
        this.categoryId = categoryId;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public long getCategoryId() {
        return categoryId;
    }
}
