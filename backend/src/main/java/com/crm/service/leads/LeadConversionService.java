package com.crm.service.leads;

import com.crm.dao.leads.LeadConversionDAO;
import com.crm.dto.leads.*;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;
import com.crm.service.permissions.ScopeType;
import com.crm.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class LeadConversionService {

    @FunctionalInterface
    public interface DataScopeResolver {
        DataScopeContext resolve(long userId, String module, String action) throws SQLException;
    }

    private final LeadConversionDAO dao;
    private final DataScopeResolver scopeResolver;

    public LeadConversionService() {
        this(new LeadConversionDAO(), new DataScopeService()::resolve);
    }

    public LeadConversionService(LeadConversionDAO dao, DataScopeResolver scopeResolver) {
        this.dao = dao;
        this.scopeResolver = scopeResolver;
    }

    public boolean canAccessLead(DataScopeContext scope, Map<String, Object> lead) {
        if (scope == null) return false;
        if (scope.scopeType() == ScopeType.ALL) return true;

        Long assignedUserId = extractAssignedUserId(lead);
        if (assignedUserId != null) {
            if (assignedUserId == scope.currentUserId() || scope.canAccessOwner(assignedUserId)) {
                return true;
            }
        }

        long ownerId = lead.get("ownerUserId") instanceof Number n ? n.longValue() : 0L;
        if (ownerId > 0 && (ownerId == scope.currentUserId() || scope.canAccessOwner(ownerId))) {
            return true;
        }

        return false;
    }

    private Long extractAssignedUserId(Map<String, Object> lead) {
        Object atUser = lead.get("assignedToUserId");
        if (atUser instanceof Number n) {
            return n.longValue();
        }
        Object atObj = lead.get("assignedTo");
        if (atObj instanceof Number n) {
            return n.longValue();
        }
        if (atObj != null) {
            String s = atObj.toString().trim();
            if (s.startsWith("USR-")) s = s.substring(4);
            if (s.matches("^[0-9]+$")) {
                try { return Long.parseLong(s); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public LeadConvertDuplicateCheckDTO checkConvertDuplicates(long currentUserId, long leadId) throws Exception {
        DataScopeContext scope = scopeResolver.resolve(currentUserId, "lead", "read");

        try (Connection conn = dao.open()) {
            Map<String, Object> lead = dao.findLeadById(conn, leadId);
            if (lead == null) {
                throw new NoSuchElementException("Không tìm thấy thông tin Lead");
            }

            if (!canAccessLead(scope, lead)) {
                throw new SecurityException("Bạn không có quyền truy cập dữ liệu Lead này");
            }

            String taxCode = (String) lead.get("taxCode");
            if (taxCode != null && !taxCode.isBlank()) {
                Map<String, Object> dup = dao.findCustomerByTaxCode(conn, taxCode);
                if (dup != null) {
                    return new LeadConvertDuplicateCheckDTO(true, dup, "Trùng Mã số thuế (" + dup.get("taxCode") + ")");
                }
            }

            String companyName = (String) lead.get("companyName");
            if (companyName != null && !companyName.isBlank()) {
                Map<String, Object> dup = dao.findCustomerByName(conn, companyName);
                if (dup != null) {
                    return new LeadConvertDuplicateCheckDTO(true, dup, "Trùng Tên doanh nghiệp (" + dup.get("name") + ")");
                }
            }

            return new LeadConvertDuplicateCheckDTO(false, null, null);
        }
    }

    public LeadConversionResultDTO convert(long currentUserId, long leadId, LeadConversionRequest request) throws Exception {
        validateRequest(request);

        DataScopeContext scope = scopeResolver.resolve(currentUserId, "lead", "convert");

        try (Connection conn = dao.open()) {
            conn.setAutoCommit(false);
            try {
                // 1. Lock and retrieve Lead
                Map<String, Object> lead = dao.lockLeadForConversion(conn, leadId);
                if (lead == null) {
                    throw new NoSuchElementException("Không tìm thấy thông tin Lead để chuyển đổi.");
                }

                // 2. Data Scope enforcement
                if (!canAccessLead(scope, lead)) {
                    throw new SecurityException("Bạn không có quyền chuyển đổi Lead này");
                }

                // 3. Status and Lock check
                Boolean isLocked = (Boolean) lead.get("isLocked");
                String status = (String) lead.get("status");
                if (Boolean.TRUE.equals(isLocked) || "CONVERTED".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Bản ghi Lead này đã được chuyển đổi trước đó và đang bị khóa.");
                }
                if ("MERGED".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Lead đã bị gộp vào Lead khác, không thể chuyển đổi.");
                }

                // 4. Handle Customer
                long customerId;
                String customerName;
                String customerInfo;

                if ("MERGE_EXISTING".equalsIgnoreCase(request.customerMode)) {
                    DataScopeContext customerScope = scopeResolver.resolve(currentUserId, "customer", "update");
                    Map<String, Object> existing = dao.findCustomerById(conn, request.existingCustomerId);
                    if (existing == null) {
                        throw new NoSuchElementException("Không tìm thấy khách hàng được chọn để gộp.");
                    }
                    long customerOwnerId = existing.get("ownerUserId") instanceof Number n ? n.longValue() : 0L;
                    if (!customerScope.canAccessOwner(customerOwnerId)) {
                        throw new SecurityException("Bạn không có quyền thao tác trên Khách hàng được chọn để gộp");
                    }
                    customerId = ((Number) existing.get("id")).longValue();
                    customerName = (String) existing.get("name");
                    String tax = (String) existing.get("taxCode");
                    customerInfo = "Gộp vào khách hàng có sẵn (MST: " + (tax != null ? tax : "N/A") + ")";
                } else {
                    if (request.customer.taxCode != null && !request.customer.taxCode.isBlank()) {
                        Map<String, Object> dupTax = dao.findCustomerByTaxCode(conn, request.customer.taxCode);
                        if (dupTax != null) {
                            throw new IllegalStateException("Mã số thuế " + request.customer.taxCode + " đã tồn tại trên hệ thống. Vui lòng chọn gộp vào công ty có sẵn.");
                        }
                    }
                    customerId = dao.createCustomer(conn, request.customer, currentUserId);
                    customerName = request.customer.name.trim();
                    String ind = request.customer.industry != null && !request.customer.industry.isBlank() ? request.customer.industry.trim() : "B2B";
                    String tax = request.customer.taxCode != null && !request.customer.taxCode.isBlank() ? request.customer.taxCode.trim() : "N/A";
                    customerInfo = "MST: " + tax + " - " + ind;
                }

                // 5. Handle Contact
                long contactId = dao.createContact(conn, customerId, request.contact);
                String contactName = request.contact.fullName.trim();
                String contactTitle = request.contact.title != null && !request.contact.title.isBlank() ? request.contact.title.trim() : "Người đại diện";
                String contactInfo = contactTitle + " - " + request.contact.phone.trim();

                // 6. Handle Deal (Opportunity)
                Long dealId = null;
                String dealName = "";
                String dealInfo = "Không tạo cơ hội";
                if (request.deal != null && request.deal.createDeal) {
                    dealId = dao.createOpportunity(conn, customerId, contactName, request.deal, currentUserId);
                    dealName = request.deal.name.trim();
                    dealInfo = String.format("%,d ₫ - Dự kiến chốt: %s", request.deal.amount.longValue(), request.deal.closeDate.trim());
                }

                // 7. Update Lead as Converted & Locked
                dao.updateLeadAsConverted(conn, leadId, currentUserId, customerId, contactId, dealId);

                // 8. Transfer and relink activities to Customer timeline
                String leadNotes = (String) lead.get("notes");
                dao.transferLeadActivitiesToCustomer(conn, leadId, customerId, dealId, currentUserId, leadNotes);

                // 9. Record Conversion History & Audit Logs
                String payloadJson = JsonUtil.getGson().toJson(request);
                String snapshotJson = JsonUtil.getGson().toJson(lead);
                dao.insertConversionHistory(conn, leadId, customerId, contactId, dealId, currentUserId, request.customerMode, payloadJson, snapshotJson);
                dao.insertAuditLog(conn, currentUserId, "CONVERT", "LEAD", leadId,
                        "Chuyển đổi thành công: Customer #" + customerId + ", Contact #" + contactId + (dealId != null ? ", Deal #" + dealId : ""));

                // 10. Commit transaction
                conn.commit();

                // 10. Assemble return result
                LeadConversionResultDTO result = new LeadConversionResultDTO();
                result.success = true;
                result.customerId = customerId;
                result.customerName = customerName;
                result.customerInfo = customerInfo;
                result.contactId = contactId;
                result.contactName = contactName;
                result.contactInfo = contactInfo;
                result.dealId = dealId;
                result.dealName = dealName;
                result.dealInfo = dealInfo;
                result.leadCode = "LEAD-" + String.format("%05d", leadId);
                result.convertedAt = java.time.Instant.now().toString();

                return result;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<LeadDTO> getLeads(long currentUserId, String status, String keyword, int limit, int offset) throws Exception {
        DataScopeContext scope = scopeResolver.resolve(currentUserId, "lead", "read");
        try (Connection conn = dao.open()) {
            return dao.searchLeads(conn, scope, status, keyword, limit, offset);
        }
    }

    public Map<String, Object> getLeadById(long currentUserId, long leadId) throws Exception {
        DataScopeContext scope = scopeResolver.resolve(currentUserId, "lead", "read");
        try (Connection conn = dao.open()) {
            Map<String, Object> lead = dao.findLeadById(conn, leadId);
            if (lead == null) {
                return null;
            }
            if (!canAccessLead(scope, lead)) {
                throw new SecurityException("Bạn không có quyền truy cập dữ liệu Lead này");
            }
            return lead;
        }
    }

    private void validateRequest(LeadConversionRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu chuyển đổi không được để trống");
        }

        String mode = req.customerMode;
        if (mode == null || (!mode.equalsIgnoreCase("CREATE_NEW") && !mode.equalsIgnoreCase("MERGE_EXISTING"))) {
            throw new IllegalArgumentException("Chế độ khách hàng không hợp lệ (CREATE_NEW hoặc MERGE_EXISTING)");
        }

        if ("MERGE_EXISTING".equalsIgnoreCase(mode)) {
            if (req.existingCustomerId == null || req.existingCustomerId <= 0) {
                throw new IllegalArgumentException("Vui lòng chọn một Khách hàng doanh nghiệp đã có để gộp");
            }
        } else {
            if (req.customer == null || req.customer.name == null || req.customer.name.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập Tên công ty / Doanh nghiệp");
            }
        }

        if (req.contact == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu Người liên hệ");
        }
        if (req.contact.fullName == null || req.contact.fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập Họ tên Người liên hệ");
        }
        if (req.contact.phone == null || req.contact.phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập Số điện thoại người liên hệ");
        }
        if (req.contact.email == null || req.contact.email.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập Email công vụ của người liên hệ");
        }
        if (!req.contact.email.contains("@") || !req.contact.email.contains(".")) {
            throw new IllegalArgumentException("Email người liên hệ không đúng định dạng");
        }

        if (req.deal != null && req.deal.createDeal) {
            if (req.deal.name == null || req.deal.name.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập Tên cơ hội bán hàng");
            }
            if (req.deal.amount == null || req.deal.amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Vui lòng nhập Giá trị hợp đồng dự kiến hợp lệ (> 0đ)");
            }
            if (req.deal.closeDate == null || req.deal.closeDate.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn Ngày dự kiến chốt hợp đồng");
            }
            try {
                LocalDate.parse(req.deal.closeDate.trim());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Ngày dự kiến chốt hợp đồng không đúng định dạng (YYYY-MM-DD)");
            }
        }
    }
}
