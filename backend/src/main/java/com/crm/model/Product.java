package com.crm.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Entity representing a Product in CRM-39 (Sản phẩm & bảng giá).
 *
 * Managed fields:
 * - code: Mã sản phẩm (unique, non-blank)
 * - name: Tên sản phẩm (non-blank)
 * - category: Loại sản phẩm (hàng hóa, dịch vụ, phần mềm, ...)
 * - unit: Đơn vị tính (cái, chiếc, bộ, gói, tháng, ...)
 * - listPrice: Giá niêm yết (list_price >= floor_price)
 * - floorPrice: Giá sàn (floor_price <= list_price)
 * - costPrice: Giá vốn (chỉ Giám đốc/Director/Admin được xem và sửa)
 * - description: Mô tả sản phẩm
 * - active: Trạng thái kinh doanh / hoạt động
 */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String category;
    private String unit;
    private BigDecimal listPrice = BigDecimal.ZERO;
    private BigDecimal floorPrice = BigDecimal.ZERO;
    private BigDecimal costPrice; // Nullable when masked for non-director roles
    private String description;
    private boolean active = true;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Product() {
    }

    public Product(Long id, String code, String name, String category, String unit,
                   BigDecimal listPrice, BigDecimal floorPrice, BigDecimal costPrice,
                   String description, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.listPrice = listPrice != null ? listPrice : BigDecimal.ZERO;
        this.floorPrice = floorPrice != null ? floorPrice : BigDecimal.ZERO;
        this.costPrice = costPrice;
        this.description = description;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getListPrice() {
        return listPrice;
    }

    public void setListPrice(BigDecimal listPrice) {
        this.listPrice = listPrice != null ? listPrice : BigDecimal.ZERO;
    }

    public BigDecimal getFloorPrice() {
        return floorPrice;
    }

    public void setFloorPrice(BigDecimal floorPrice) {
        this.floorPrice = floorPrice != null ? floorPrice : BigDecimal.ZERO;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", unit='" + unit + '\'' +
                ", listPrice=" + listPrice +
                ", floorPrice=" + floorPrice +
                ", costPrice=" + costPrice +
                ", active=" + active +
                '}';
    }
}