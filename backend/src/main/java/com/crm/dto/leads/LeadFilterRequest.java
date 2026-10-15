package com.crm.dto.leads;

public class LeadFilterRequest {
    private String keyword;
    private String search;
    private String status = "ALL";
    private String tier = "ALL";
    private String rating = "ALL";
    private String source = "ALL";
    private String owner = "ALL";
    private Long ownerId;
    private String dateRange = "ALL";
    private String createdFrom;
    private String createdTo;
    private String from;
    private String to;
    private boolean slaOverdueOnly = false;
    private String sort = "SLA_URGENT";
    private int page = 1;
    private int size = 10;

    public LeadFilterRequest() {
    }

    public String getKeyword() {
        if (keyword != null && !keyword.isBlank()) return keyword.trim();
        if (search != null && !search.isBlank()) return search.trim();
        return null;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getSearch() {
        return getKeyword();
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTier() {
        if (tier != null && !"ALL".equalsIgnoreCase(tier) && !tier.isBlank()) return tier.toUpperCase();
        if (rating != null && !"ALL".equalsIgnoreCase(rating) && !rating.isBlank()) return rating.toUpperCase();
        return "ALL";
    }

    public void setTier(String tier) {
        this.tier = tier;
        this.rating = tier;
    }

    public String getRating() {
        return getTier();
    }

    public void setRating(String rating) {
        setTier(rating);
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public String getEffectiveFrom() {
        if (createdFrom != null && !createdFrom.isBlank()) return createdFrom.trim();
        if (from != null && !from.isBlank()) return from.trim();
        return null;
    }

    public void setCreatedFrom(String createdFrom) {
        this.createdFrom = createdFrom;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getEffectiveTo() {
        if (createdTo != null && !createdTo.isBlank()) return createdTo.trim();
        if (to != null && !to.isBlank()) return to.trim();
        return null;
    }

    public void setCreatedTo(String createdTo) {
        this.createdTo = createdTo;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public boolean isSlaOverdueOnly() {
        return slaOverdueOnly;
    }

    public void setSlaOverdueOnly(boolean slaOverdueOnly) {
        this.slaOverdueOnly = slaOverdueOnly;
    }

    public String getSort() {
        return sort != null ? sort : "SLA_URGENT";
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page > 0 ? page : 1;
    }

    public void setPage(int page) {
        this.page = page > 0 ? page : 1;
    }

    public int getSize() {
        return size > 0 ? size : 10;
    }

    public void setSize(int size) {
        this.size = size > 0 ? size : 10;
    }
}
