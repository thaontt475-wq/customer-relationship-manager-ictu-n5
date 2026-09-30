package com.crm.service.products;

/**
 * Exception thrown when a product cannot be deleted because it is already referenced
 * by business transactions (Quotes, Opportunities, Contracts, Orders, etc.).
 * Mapped to HTTP 409 Conflict / 400 Bad Request.
 */
public class ProductInUseException extends Exception {
    private static final long serialVersionUID = 1L;

    private final long productId;

    public ProductInUseException(long productId, String message) {
        super(message);
        this.productId = productId;
    }

    public long getProductId() {
        return productId;
    }
}
