package com.crm.service.leads;

import com.crm.dto.leads.LeadDuplicatePairDTO;
import java.time.Instant;
import java.util.*;

public class LeadDuplicateMatcher {

    public static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+84")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("84") && digits.length() >= 10) {
            digits = "0" + digits.substring(2);
        }
        digits = digits.replaceAll("[^0-9]", "");
        return digits.length() >= 8 && digits.length() <= 15 ? digits : null;
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        int atIdx = trimmed.indexOf('@');
        if (atIdx <= 0 || atIdx == trimmed.length() - 1 || !trimmed.substring(atIdx).contains(".")) {
            return null;
        }
        return trimmed;
    }

    public static String removeDiacritics(String str) {
        if (str == null) return null;
        String nfd = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        String stripped = nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return stripped.replace('đ', 'd').replace('Đ', 'D');
    }

    public static String normalizeCompanyName(String company) {
        if (company == null || company.isBlank()) return null;
        String s = removeDiacritics(company).toLowerCase(Locale.ROOT).trim();
        s = s.replaceAll("[^a-z0-9\\s]", " ");
        s = s.replaceAll("\\b(cong ty|cty|co phan|cp|tnhh|mtv|tap doan|group|corp|corporation|global|viet nam|vn)\\b", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() >= 2 ? s : null;
    }

    public static boolean isCompanyMatch(String c1, String c2) {
        if (c1 == null || c2 == null) return false;
        if (c1.equalsIgnoreCase(c2)) return true;
        if (c1.length() >= 3 && c2.length() >= 3 && (c1.contains(c2) || c2.contains(c1))) {
            return true;
        }
        Set<String> set1 = new HashSet<>(Arrays.asList(c1.split("\\s+")));
        Set<String> set2 = new HashSet<>(Arrays.asList(c2.split("\\s+")));
        if (set1.isEmpty() || set2.isEmpty()) return false;
        for (String t1 : set1) {
            if (t1.length() >= 3 && set2.contains(t1)) return true;
        }
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        double jaccard = (double) intersection.size() / union.size();
        return jaccard >= 0.70;
    }

    public static LeadDuplicatePairDTO matchLeads(Map<String, Object> a, Map<String, Object> b) {
        if (a == null || b == null) return null;
        Object idA = a.get("id");
        Object idB = b.get("id");
        if (Objects.equals(idA, idB)) return null;

        String phoneA = (String) a.get("phone");
        String phoneB = (String) b.get("phone");
        String emailA = (String) a.get("email");
        String emailB = (String) b.get("email");
        String compA = (String) a.get("companyName");
        String compB = (String) b.get("companyName");

        String normPhoneA = normalizePhone(phoneA);
        String normPhoneB = normalizePhone(phoneB);
        String normEmailA = normalizeEmail(emailA);
        String normEmailB = normalizeEmail(emailB);
        String normCompA = normalizeCompanyName(compA);
        String normCompB = normalizeCompanyName(compB);

        boolean phoneMatch = normPhoneA != null && normPhoneA.equals(normPhoneB);
        boolean emailMatch = normEmailA != null && normEmailA.equals(normEmailB);
        boolean compMatch = normCompA != null && normCompB != null && isCompanyMatch(normCompA, normCompB);

        if (!phoneMatch && !emailMatch && !compMatch) {
            return null;
        }

        List<String> matchedFields = new ArrayList<>();
        if (phoneMatch) {
            matchedFields.add("Số điện thoại trùng khớp (" + phoneA + ")");
        }
        if (emailMatch) {
            matchedFields.add("Địa chỉ Email trùng khớp (" + emailA + ")");
        }
        if (compMatch) {
            matchedFields.add("Cùng doanh nghiệp: " + (compA != null ? compA : compB));
        }

        String criterion;
        if (phoneMatch) {
            criterion = "PHONE";
        } else if (emailMatch) {
            criterion = "EMAIL";
        } else {
            criterion = "COMPANY";
        }

        int confidence;
        if (phoneMatch && emailMatch && compMatch) {
            confidence = 99;
        } else if (phoneMatch && emailMatch) {
            confidence = 98;
        } else if (phoneMatch && compMatch) {
            confidence = 96;
        } else if (emailMatch && compMatch) {
            confidence = 95;
        } else if (phoneMatch) {
            confidence = 95;
        } else if (emailMatch) {
            confidence = 94;
        } else {
            confidence = normCompA.equalsIgnoreCase(normCompB) ? 85 : 80;
        }

        String pairId = "DUP-LD-" + idA + "-" + idB;
        String now = Instant.now().toString();

        return new LeadDuplicatePairDTO(
                pairId,
                criterion,
                confidence,
                now,
                a,
                b,
                false,
                matchedFields
        );
    }

    public static LeadDuplicatePairDTO matchLeadAndCustomer(Map<String, Object> lead, Map<String, Object> customer) {
        if (lead == null || customer == null) return null;
        Object leadCustId = lead.get("customerId");
        Object custId = customer.get("id");
        if (leadCustId != null && custId != null && Objects.equals(String.valueOf(leadCustId), String.valueOf(custId))) {
            return null;
        }

        String leadPhone = (String) lead.get("phone");
        String custPhone = (String) customer.get("phone");
        String leadEmail = (String) lead.get("email");
        String custEmail = (String) customer.get("email");
        String leadComp = (String) lead.get("companyName");
        String custName = (String) customer.get("name");

        String normPhoneLead = normalizePhone(leadPhone);
        String normPhoneCust = normalizePhone(custPhone);
        String normEmailLead = normalizeEmail(leadEmail);
        String normEmailCust = normalizeEmail(custEmail);
        String normCompLead = normalizeCompanyName(leadComp);
        String normCompCust = normalizeCompanyName(custName);

        boolean phoneMatch = normPhoneLead != null && normPhoneLead.equals(normPhoneCust);
        boolean emailMatch = normEmailLead != null && normEmailLead.equals(normEmailCust);
        boolean compMatch = normCompLead != null && normCompCust != null && isCompanyMatch(normCompLead, normCompCust);

        if (!phoneMatch && !emailMatch && !compMatch) {
            return null;
        }

        List<String> matchedFields = new ArrayList<>();
        matchedFields.add("Trùng khớp Khách hàng CRM (Mã KH-" + custId + ")");
        if (phoneMatch) {
            matchedFields.add("SĐT: " + leadPhone);
        }
        if (emailMatch) {
            matchedFields.add("Email: " + leadEmail);
        }
        if (compMatch) {
            matchedFields.add("Doanh nghiệp: " + custName);
        }

        int confidence = (phoneMatch || emailMatch) ? 99 : 85;
        String pairId = "DUP-CUST-" + lead.get("id") + "-" + custId;
        String now = Instant.now().toString();

        // Prepare customer view for Record B
        Map<String, Object> customerRecordB = new LinkedHashMap<>();
        customerRecordB.put("id", "CUST-" + custId);
        customerRecordB.put("customerId", custId);
        customerRecordB.put("fullName", customer.get("name"));
        customerRecordB.put("companyName", customer.get("name"));
        customerRecordB.put("phone", customer.get("phone"));
        customerRecordB.put("email", customer.get("email"));
        customerRecordB.put("title", "Khách hàng CRM");
        customerRecordB.put("source", "Khách hàng CRM hiện hữu (Mã KH-" + custId + ")");
        customerRecordB.put("notes", customer.get("address") != null ? "Địa chỉ: " + customer.get("address") : "Khách hàng hiện hữu");
        customerRecordB.put("createdAt", customer.get("createdAt") != null ? customer.get("createdAt").toString() : now);

        return new LeadDuplicatePairDTO(
                pairId,
                "CUSTOMER",
                confidence,
                now,
                lead,
                customerRecordB,
                true,
                matchedFields
        );
    }
}
