package com.crm.service.leads;

import com.crm.dao.leads.LeadDuplicateMergeDAO;
import com.crm.dto.leads.LeadAttachCustomerRequest;
import com.crm.dto.leads.LeadDuplicatePairDTO;
import com.crm.dto.leads.LeadMergeRequest;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;
import com.crm.util.JsonUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class LeadDuplicateMergeService {

    @FunctionalInterface
    public interface ScopeResolver {
        DataScopeContext resolve(long user, String module, String action) throws Exception;
    }

    private final LeadDuplicateMergeDAO dao;
    private final ScopeResolver scopes;

    public LeadDuplicateMergeService() {
        this(new LeadDuplicateMergeDAO(), (user, module, action) -> new DataScopeService().resolve(user, module, action));
    }

    public LeadDuplicateMergeService(LeadDuplicateMergeDAO dao, ScopeResolver scopes) {
        this.dao = dao;
        this.scopes = scopes;
    }

    public List<LeadDuplicatePairDTO> getDuplicates(long userId) throws Exception {
        DataScopeContext leadScope = scopes.resolve(userId, "lead", "read");
        DataScopeContext customerScope = null;
        try {
            customerScope = scopes.resolve(userId, "customer", "read");
        } catch (Exception ignored) {}

        List<LeadDuplicatePairDTO> duplicatePairs = new ArrayList<>();

        try (Connection c = dao.open()) {
            List<Map<String, Object>> leads = dao.findActiveLeads(c, leadScope, 1000);
            List<Map<String, Object>> customers = dao.findActiveCustomers(c, customerScope, 1000);

            // 1. Lead vs Lead comparisons
            for (int i = 0; i < leads.size(); i++) {
                Map<String, Object> leadA = leads.get(i);
                for (int j = i + 1; j < leads.size(); j++) {
                    Map<String, Object> leadB = leads.get(j);
                    LeadDuplicatePairDTO pair = LeadDuplicateMatcher.matchLeads(leadA, leadB);
                    if (pair != null) {
                        duplicatePairs.add(pair);
                    }
                }
            }

            // 2. Lead vs Customer comparisons
            for (Map<String, Object> lead : leads) {
                for (Map<String, Object> cust : customers) {
                    LeadDuplicatePairDTO custPair = LeadDuplicateMatcher.matchLeadAndCustomer(lead, cust);
                    if (custPair != null) {
                        duplicatePairs.add(custPair);
                    }
                }
            }
        }

        // Sort by confidence descending, then by detectedAt descending
        duplicatePairs.sort((p1, p2) -> {
            int cmp = Integer.compare(p2.getConfidence(), p1.getConfidence());
            if (cmp != 0) return cmp;
            return p2.getDetectedAt().compareTo(p1.getDetectedAt());
        });

        return duplicatePairs;
    }

    public Map<String, Object> merge(long userId, LeadMergeRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu yêu cầu merge");
        }
        if (request.masterId == null || request.masterId <= 0) {
            throw new IllegalArgumentException("masterId phải là số nguyên dương");
        }
        if (request.duplicateId == null || request.duplicateId <= 0) {
            throw new IllegalArgumentException("duplicateId phải là số nguyên dương");
        }
        if (request.masterId.equals(request.duplicateId)) {
            throw new IllegalArgumentException("Không thể gộp Lead với chính nó");
        }
        if (request.retainHistory != null && !request.retainHistory) {
            throw new IllegalArgumentException("Bảo toàn lịch sử là bắt buộc khi gộp Lead");
        }
        if (request.mergedData == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu mergedData");
        }

        String fullName = (String) request.mergedData.get("fullName");
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên Lead chính không được để trống");
        }
        fullName = fullName.trim();
        if (fullName.length() > 150) fullName = fullName.substring(0, 150);

        String email = (String) request.mergedData.get("email");
        if (email != null) {
            email = email.trim();
            if (email.length() > 255) email = email.substring(0, 255);
        }

        String phone = (String) request.mergedData.get("phone");
        if (phone != null) {
            phone = phone.trim();
            if (phone.length() > 50) phone = phone.substring(0, 50);
        }

        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            throw new IllegalArgumentException("Lead phải có ít nhất Email hoặc Số điện thoại");
        }

        String companyName = (String) request.mergedData.get("companyName");
        if (companyName != null) {
            companyName = companyName.trim();
            if (companyName.length() > 255) companyName = companyName.substring(0, 255);
        }

        String title = (String) request.mergedData.get("title");
        if (title != null) {
            title = title.trim();
            if (title.length() > 150) title = title.substring(0, 150);
        }

        DataScopeContext mergeScope = scopes.resolve(userId, "lead", "merge");

        try (Connection c = dao.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);

            try {
                // Lock both records ordered by id ascending to prevent deadlocks
                List<Map<String, Object>> locked = dao.lockLeadsForUpdate(c, request.masterId, request.duplicateId);
                if (locked.size() < 2) {
                    throw new NoSuchElementException("Một trong hai Lead không tồn tại");
                }

                Map<String, Object> master = null;
                Map<String, Object> duplicate = null;
                for (Map<String, Object> item : locked) {
                    long id = ((Number) item.get("id")).longValue();
                    if (id == request.masterId) {
                        master = item;
                    } else if (id == request.duplicateId) {
                        duplicate = item;
                    }
                }

                if (master == null || duplicate == null) {
                    throw new NoSuchElementException("Một trong hai Lead không tồn tại");
                }

                // Scope checks
                if (!mergeScope.canAccessOwner(((Number) master.get("ownerUserId")).longValue())) {
                    throw new SecurityException("Lead chính nằm ngoài phạm vi phân quyền của người dùng");
                }
                if (!mergeScope.canAccessOwner(((Number) duplicate.get("ownerUserId")).longValue())) {
                    throw new SecurityException("Lead phụ nằm ngoài phạm vi phân quyền của người dùng");
                }

                // Verify neither is already merged
                if ("MERGED".equals(master.get("status")) || master.get("mergedIntoId") != null) {
                    throw new IllegalStateException("Lead chính đã được gộp trước đó");
                }
                if ("MERGED".equals(duplicate.get("status")) || duplicate.get("mergedIntoId") != null) {
                    throw new IllegalStateException("Lead phụ đã được gộp trước đó");
                }

                String beforeMaster = JsonUtil.getGson().toJson(master);
                String beforeDuplicate = JsonUtil.getGson().toJson(duplicate);

                // Resolve source_id
                Long sourceId = null;
                if (request.mergedData.containsKey("source")) {
                    sourceId = dao.resolveSourceId(c, request.mergedData.get("source"));
                }
                if (sourceId == null && request.mergedData.containsKey("sourceId")) {
                    sourceId = dao.resolveSourceId(c, request.mergedData.get("sourceId"));
                }
                if (sourceId == null && master.get("sourceId") != null) {
                    sourceId = ((Number) master.get("sourceId")).longValue();
                }

                // Build notes preserving history
                String masterNotes = (String) master.get("notes");
                String dupNotes = (String) duplicate.get("notes");
                String reqNotes = (String) request.mergedData.get("notes");

                String finalNotes;
                if (reqNotes != null && !reqNotes.isBlank()) {
                    finalNotes = reqNotes;
                } else {
                    finalNotes = "[Gốc]: " + (masterNotes != null ? masterNotes : "Không có")
                            + "\n--- [Lịch sử gộp từ Lead #" + request.duplicateId + "]: "
                            + (dupNotes != null ? dupNotes : "Không có");
                }
                if (finalNotes.length() > 2000) {
                    finalNotes = finalNotes.substring(0, 1996) + "...";
                }

                Map<String, Object> updateFields = new LinkedHashMap<>();
                updateFields.put("fullName", fullName);
                updateFields.put("companyName", companyName);
                updateFields.put("email", email);
                updateFields.put("phone", phone);
                updateFields.put("title", title);
                if (sourceId != null) {
                    updateFields.put("sourceId", sourceId);
                }
                updateFields.put("notes", finalNotes);

                // 1. Update master lead
                dao.updateMasterLead(c, request.masterId, updateFields, userId);

                // 2. Archive duplicate lead
                dao.archiveDuplicateLead(c, request.duplicateId, request.masterId, userId);

                // 3. Snapshot after master
                Map<String, Object> afterMaster = dao.findLeadById(c, request.masterId);

                // 4. Save merge history
                dao.insertMergeHistory(
                        c,
                        request.masterId,
                        request.duplicateId,
                        userId,
                        beforeMaster,
                        beforeDuplicate,
                        JsonUtil.getGson().toJson(afterMaster),
                        JsonUtil.getGson().toJson(request.mergedData),
                        true
                );

                // 5. Audit log
                dao.insertAuditLog(
                        c,
                        userId,
                        "LEAD",
                        String.valueOf(request.masterId),
                        "MERGE",
                        "Gộp Lead #" + request.duplicateId + " vào Master Lead #" + request.masterId,
                        beforeMaster,
                        JsonUtil.getGson().toJson(afterMaster)
                );

                c.commit();

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("success", true);
                response.put("masterId", request.masterId);
                response.put("duplicateId", request.duplicateId);
                response.put("master", afterMaster);
                response.put("merged", true);
                return response;

            } catch (Exception e) {
                try {
                    c.rollback();
                } catch (SQLException rb) {
                    e.addSuppressed(rb);
                }
                throw e;
            }
        }
    }

    public Map<String, Object> attachToCustomer(long userId, LeadAttachCustomerRequest request) throws Exception {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu yêu cầu liên kết khách hàng");
        }
        if (request.leadId == null || request.leadId <= 0) {
            throw new IllegalArgumentException("leadId phải là số nguyên dương");
        }
        if (request.customerId == null || request.customerId <= 0) {
            throw new IllegalArgumentException("customerId phải là số nguyên dương");
        }

        DataScopeContext leadScope = scopes.resolve(userId, "lead", "merge");

        try (Connection c = dao.open()) {
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            c.setAutoCommit(false);

            try {
                Map<String, Object> lead = dao.findLeadById(c, request.leadId);
                if (lead == null) {
                    throw new NoSuchElementException("Lead không tồn tại");
                }
                if (!leadScope.canAccessOwner(((Number) lead.get("ownerUserId")).longValue())) {
                    throw new SecurityException("Lead nằm ngoài phạm vi phân quyền của người dùng");
                }
                if ("MERGED".equals(lead.get("status"))) {
                    throw new IllegalStateException("Lead đã được gộp, không thể liên kết");
                }

                Map<String, Object> customer = dao.findCustomerById(c, request.customerId);
                if (customer == null) {
                    throw new NoSuchElementException("Khách hàng không tồn tại");
                }

                dao.linkLeadToCustomer(c, request.leadId, request.customerId, userId, request.notes);
                dao.insertAuditLog(
                        c,
                        userId,
                        "LEAD",
                        String.valueOf(request.leadId),
                        "LINK_CUSTOMER",
                        "Liên kết Lead #" + request.leadId + " vào Khách hàng #" + request.customerId,
                        null,
                        null
                );

                c.commit();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("leadId", request.leadId);
                result.put("customerId", request.customerId);
                result.put("linked", true);
                return result;

            } catch (Exception e) {
                try {
                    c.rollback();
                } catch (SQLException rb) {
                    e.addSuppressed(rb);
                }
                throw e;
            }
        }
    }
}
