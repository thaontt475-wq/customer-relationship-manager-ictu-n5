package com.crm.dto.customers;

import java.util.Set;

/** Only search criteria are persisted. Permissions and pagination are resolved per request. */
public record CustomerSearchFilter(String search, String status, Long industryId,
                                   Long companySizeId, String region, Long ownerId, String industry) {
    private static final Set<String> STATUSES = Set.of("TIEM_NANG", "DANG_GIAO_DICH", "CHINH_THUC", "NGUNG_HOP_TAC");

    public CustomerSearchFilter {
        search = text(search, "search", 255);
        status = text(status, "status", 50);
        region = text(region, "region", 20);
        industry = text(industry, "industry", 100);
        if (status != null && !STATUSES.contains(status)) throw new IllegalArgumentException("Trạng thái khách hàng không hợp lệ");
        positive(industryId, "industryId");
        positive(companySizeId, "companySizeId");
        positive(ownerId, "ownerId");
    }

    public CustomerSearchFilter(String search, String status, Long industryId,
                                Long companySizeId, String region, Long ownerId) {
        this(search, status, industryId, companySizeId, region, ownerId, null);
    }

    public static String text(String value, String field, int limit) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim();
        if (result.codePointCount(0, result.length()) > limit) throw new IllegalArgumentException(field + " quá dài (tối đa " + limit + " ký tự)");
        return result;
    }

    public static Long id(String value, String field) {
        String result = text(value, field, 19);
        if (result == null) return null;
        if (!result.matches("[1-9][0-9]*")) throw new IllegalArgumentException(field + " phải là số nguyên dương");
        try { return Long.valueOf(result); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(field + " vượt giới hạn số nguyên"); }
    }

    public static void pagination(int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new IllegalArgumentException("page phải >= 1; size phải từ 1 đến 100");
    }

    private static void positive(Long id, String field) {
        if (id != null && id <= 0) throw new IllegalArgumentException(field + " phải là số nguyên dương");
    }
}
