package com.crm.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Entity representing a Common Master Data Category (Danh mục dùng chung) in CRM-44.
 *
 * Types:
 * - INDUSTRY: Ngành nghề
 * - COMPANY_SIZE: Quy mô doanh nghiệp
 * - LEAD_SOURCE: Nguồn Lead
 * - ACTIVITY_TYPE: Loại hoạt động
 */
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private CategoryType type;
    private String code;
    private String name;
    private String description;
    private int displayOrder = 0;
    private boolean active = true;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Category() {
    }

    public Category(Long id, CategoryType type, String code, String name,
                    String description, int displayOrder, boolean active) {
        this.id = id;
        this.type = type;
        this.code = code;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public String getTypeCode() {
        return type != null ? type.getCode() : null;
    }

    public void setTypeCode(String typeCode) {
        this.type = CategoryType.fromString(typeCode);
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
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
        return "Category{" +
                "id=" + id +
                ", type=" + type +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", displayOrder=" + displayOrder +
                ", active=" + active +
                '}';
    }
}
