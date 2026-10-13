package com.crm.service.customers;

import com.crm.dto.customers.CustomerWriteRequest;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/** Business statuses retain the existing API codes; DA_GOP belongs only to merge. */
public final class CustomerValidation {
    public static final Set<String> STATUSES = Set.of(
            "TIEM_NANG", "DANG_GIAO_DICH", "CHINH_THUC", "NGUNG_HOP_TAC");

    private CustomerValidation() {}

    public static String status(String value) {
        String result = optional(value, "status", 50);
        if (result != null && !STATUSES.contains(result)) {
            throw new IllegalArgumentException("status phải là TIEM_NANG, DANG_GIAO_DICH, CHINH_THUC hoặc NGUNG_HOP_TAC");
        }
        return result;
    }

    public static void normalize(CustomerWriteRequest request) {
        if (request == null) throw new IllegalArgumentException("Thiếu hồ sơ khách hàng");
        String name = optional(request.getName(), "name", 255);
        if (name == null) throw new IllegalArgumentException("Tên khách hàng/công ty là bắt buộc.");
        request.setName(name);
        String tax = optional(request.getTaxCode(), "taxCode", 50);
        if (tax != null && !tax.matches("[0-9]{10}(-?[0-9]{3})?")) {
            throw new IllegalArgumentException("taxCode phải gồm 10 chữ số hoặc mã chi nhánh 13 chữ số");
        }
        request.setTaxCode(tax == null ? null : tax.replace("-", ""));
        request.setStatus(status(request.getStatus()));
        String email = optional(request.getEmail(), "email", 255);
        if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("email không đúng định dạng");
        }
        request.setEmail(email);
        String phone = optional(request.getPhone(), "phone", 50);
        if (phone != null && (!phone.matches("[+()0-9 .-]+")
                || phone.replaceAll("\\D", "").length() < 6
                || phone.replaceAll("\\D", "").length() > 20)) {
            throw new IllegalArgumentException("phone không đúng định dạng");
        }
        request.setPhone(phone);
        String website = optional(request.getWebsite(), "website", 255);
        if (website != null) {
            try {
                URI uri = URI.create(website);
                if (!Set.of("http", "https").contains(String.valueOf(uri.getScheme()).toLowerCase(Locale.ROOT))
                        || uri.getHost() == null || uri.getUserInfo() != null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("website phải là URL http/https hợp lệ");
            }
        }
        request.setWebsite(website);
        request.setAddress(optional(request.getAddress(), "address", 500));
        positive(request.getIndustryId(), "industryId");
        positive(request.getCompanySizeId(), "companySizeId");
        positive(request.getOwnerUserId(), "ownerUserId");
    }

    private static String optional(String value, String field, int maximum) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.codePointCount(0, trimmed.length()) > maximum) {
            throw new IllegalArgumentException(field + ": tối đa " + maximum + " ký tự");
        }
        return trimmed;
    }

    private static void positive(Long value, String field) {
        if (value != null && value <= 0) throw new IllegalArgumentException(field + " phải là số nguyên dương");
    }
}
