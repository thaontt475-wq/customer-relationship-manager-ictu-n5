package com.crm.service.customers;

import com.crm.dao.audit.AuditLogDAO;
import com.crm.dao.categories.CategoryDAO;
import com.crm.dao.customfields.CustomFieldDAO;
import com.crm.dao.scope.ScopedEntityDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.AuditLog;
import com.crm.model.Category;
import com.crm.model.CustomField;
import com.crm.model.User;
import com.crm.service.scope.DataScopeService;
import com.crm.service.scope.ScopeContext;
import com.crm.service.scope.ScopeEntityType;
import com.crm.service.scope.ScopeRecord;
import com.crm.util.DBConnection;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Service for detecting duplicates and merging customer records safely with data scope enforcement and audit logging (Sprint 3).
 */
public class CustomerDuplicateService {

    private final DataScopeService scopeService;
    private final ScopedEntityDAO scopedEntityDAO;
    private final CustomFieldDAO customFieldDAO;
    private final CategoryDAO categoryDAO;
    private final UserDAO userDAO;
    private final AuditLogDAO auditLogDAO;

    public CustomerDuplicateService() {
        this(new DataScopeService(), new ScopedEntityDAO(), new CustomFieldDAO(),
                new CategoryDAO(), new UserDAO(), new AuditLogDAO());
    }

    public CustomerDuplicateService(
            DataScopeService scopeService,
            ScopedEntityDAO scopedEntityDAO,
            CustomFieldDAO customFieldDAO,
            CategoryDAO categoryDAO,
            UserDAO userDAO,
            AuditLogDAO auditLogDAO) {
        this.scopeService = scopeService != null ? scopeService : new DataScopeService();
        this.scopedEntityDAO = scopedEntityDAO != null ? scopedEntityDAO : new ScopedEntityDAO();
        this.customFieldDAO = customFieldDAO != null ? customFieldDAO : new CustomFieldDAO();
        this.categoryDAO = categoryDAO != null ? categoryDAO : new CategoryDAO();
        this.userDAO = userDAO != null ? userDAO : new UserDAO();
        this.auditLogDAO = auditLogDAO != null ? auditLogDAO : new AuditLogDAO();
    }

    public record CustomerDetails(
            long id,
            String name,
            long ownerUserId,
            String ownerName,
            Long ownerTeamId,
            String ownerTeamName,
            Long industryId,
            String industryName,
            Long companySizeId,
            String companySizeName,
            Timestamp createdAt,
            Map<String, String> customFields,
            long opportunityCount,
            long activityCount,
            long quoteCount,
            long leadCount
    ) {}

    public record DuplicateGroup(
            String matchCriteria,
            String matchValue,
            List<CustomerDetails> records
    ) {}

    /**
     * Finds duplicate customer candidates within the actor's data scope.
     */
    public List<DuplicateGroup> findDuplicates(long actorUserId, String query) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ScopeContext actor = scopeService.loadContext(conn, actorUserId);
            List<ScopeRecord> visibleList = scopedEntityDAO.findVisible(conn, ScopeEntityType.CUSTOMERS, actor, query);
            if (visibleList.isEmpty()) {
                return List.of();
            }

            Map<Long, CustomerDetails> detailsMap = new HashMap<>();
            for (ScopeRecord r : visibleList) {
                CustomerDetails details = loadCustomerDetails(conn, r.id());
                if (details != null) {
                    detailsMap.put(r.id(), details);
                }
            }

            Map<String, List<CustomerDetails>> nameGroups = new LinkedHashMap<>();
            for (CustomerDetails c : detailsMap.values()) {
                String normalizedName = normalizeForComparison(c.name());
                if (!normalizedName.isEmpty()) {
                    nameGroups.computeIfAbsent(normalizedName, k -> new ArrayList<>()).add(c);
                }
            }

            Map<String, List<CustomerDetails>> taxGroups = new LinkedHashMap<>();
            for (CustomerDetails c : detailsMap.values()) {
                String tax = c.customFields().get("tax_code");
                if (tax != null && !tax.trim().isEmpty()) {
                    String normTax = tax.trim().toLowerCase(Locale.ROOT);
                    taxGroups.computeIfAbsent(normTax, k -> new ArrayList<>()).add(c);
                }
            }

            List<DuplicateGroup> result = new ArrayList<>();

            for (Map.Entry<String, List<CustomerDetails>> entry : nameGroups.entrySet()) {
                if (entry.getValue().size() > 1) {
                    String val = entry.getValue().get(0).name();
                    result.add(new DuplicateGroup("Tên doanh nghiệp", val, entry.getValue()));
                }
            }

            for (Map.Entry<String, List<CustomerDetails>> entry : taxGroups.entrySet()) {
                if (entry.getValue().size() > 1) {
                    result.add(new DuplicateGroup("Mã số thuế", entry.getKey(), entry.getValue()));
                }
            }

            return result;
        }
    }

    public CustomerDetails getCustomerDetails(long actorUserId, long customerId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            var readRes = scopeService.read(actorUserId, ScopeEntityType.CUSTOMERS, customerId);
            if (readRes.status() != DataScopeService.ReadStatus.SUCCESS) {
                return null;
            }
            return loadCustomerDetails(conn, customerId);
        }
    }

    private CustomerDetails loadCustomerDetails(Connection conn, long id) throws SQLException {
        String sql = "SELECT c.id, c.name, c.owner_user_id, c.industry_id, c.company_size_id, c.created_at, "
                + "u.full_name AS owner_name, u.team_id AS owner_team_id, t.name AS team_name, "
                + "ind.name AS industry_name, cs.name AS company_size_name "
                + "FROM customers c "
                + "LEFT JOIN users u ON u.id = c.owner_user_id "
                + "LEFT JOIN teams t ON t.id = u.team_id "
                + "LEFT JOIN categories ind ON ind.id = c.industry_id "
                + "LEFT JOIN categories cs ON cs.id = c.company_size_id "
                + "WHERE c.id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;

                long ownerUserId = rs.getLong("owner_user_id");
                String ownerName = rs.getString("owner_name");
                long teamId = rs.getLong("owner_team_id");
                Long ownerTeamId = rs.wasNull() ? null : teamId;
                String teamName = rs.getString("team_name");
                long indId = rs.getLong("industry_id");
                Long industryId = rs.wasNull() ? null : indId;
                String industryName = rs.getString("industry_name");
                long sizeId = rs.getLong("company_size_id");
                Long companySizeId = rs.wasNull() ? null : sizeId;
                String companySizeName = rs.getString("company_size_name");
                Timestamp createdAt = rs.getTimestamp("created_at");

                Map<String, String> customFields = customFieldDAO.findValues(conn, "customers", id);

                long oppCount = countRelated(conn, "opportunities", "customer_id", id);
                long actCount = countRelated(conn, "activities", "customer_id", id);
                long quoteCount = countRelated(conn, "quotes", "customer_id", id);
                long leadCount = countRelated(conn, "leads", "converted_customer_id", id);

                return new CustomerDetails(
                        id, rs.getString("name"), ownerUserId, ownerName,
                        ownerTeamId, teamName, industryId, industryName,
                        companySizeId, companySizeName, createdAt,
                        customFields, oppCount, actCount, quoteCount, leadCount
                );
            }
        }
    }

    private long countRelated(Connection conn, String table, String col, long customerId) {
        try {
            String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + col + " = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
        } catch (SQLException ignored) {
        }
        return 0;
    }

    public enum MergeStatus {
        SUCCESS,
        SAME_RECORD,
        NOT_FOUND,
        FORBIDDEN,
        VALIDATION_ERROR
    }

    public record MergeResult(MergeStatus status, String message, Long masterId) {}

    /**
     * Merges duplicateRecordId into masterRecordId transactionally.
     */
    public MergeResult mergeCustomers(
            long actorUserId,
            long masterRecordId,
            long duplicateRecordId,
            Map<String, String> overrideValues
    ) throws SQLException {
        if (masterRecordId <= 0 || duplicateRecordId <= 0) {
            return new MergeResult(MergeStatus.VALIDATION_ERROR, "Mã khách hàng không hợp lệ.", null);
        }
        if (masterRecordId == duplicateRecordId) {
            return new MergeResult(MergeStatus.SAME_RECORD, "Không thể gộp bản ghi vào chính nó.", masterRecordId);
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                var masterRes = scopeService.read(actorUserId, ScopeEntityType.CUSTOMERS, masterRecordId);
                var dupRes = scopeService.read(actorUserId, ScopeEntityType.CUSTOMERS, duplicateRecordId);

                if (masterRes.status() == DataScopeService.ReadStatus.NOT_FOUND || dupRes.status() == DataScopeService.ReadStatus.NOT_FOUND) {
                    conn.rollback();
                    return new MergeResult(MergeStatus.NOT_FOUND, "Không tìm thấy một trong hai bản ghi khách hàng.", null);
                }
                if (masterRes.status() == DataScopeService.ReadStatus.FORBIDDEN || dupRes.status() == DataScopeService.ReadStatus.FORBIDDEN) {
                    conn.rollback();
                    return new MergeResult(MergeStatus.FORBIDDEN, "Bạn không có quyền thao tác trên một trong hai bản ghi này do giới hạn Data Scope.", null);
                }

                CustomerDetails masterDetails = loadCustomerDetails(conn, masterRecordId);
                CustomerDetails dupDetails = loadCustomerDetails(conn, duplicateRecordId);

                if (masterDetails == null || dupDetails == null) {
                    conn.rollback();
                    return new MergeResult(MergeStatus.NOT_FOUND, "Bản ghi khách hàng không còn tồn tại.", null);
                }

                // 1. Reassign child relationships
                reassignChildRecords(conn, "opportunities", "customer_id", duplicateRecordId, masterRecordId);
                reassignChildRecords(conn, "activities", "customer_id", duplicateRecordId, masterRecordId);
                reassignChildRecords(conn, "quotes", "customer_id", duplicateRecordId, masterRecordId);
                reassignChildRecords(conn, "leads", "converted_customer_id", duplicateRecordId, masterRecordId);

                // 2. Merge custom fields: master keeps its fields unless blank, fills with duplicate's, or overridden
                List<CustomField> definitions = customFieldDAO.findAll(conn, "customers");
                for (CustomField def : definitions) {
                    String fieldName = def.getFieldName();
                    String val = null;
                    if (overrideValues != null && overrideValues.containsKey(fieldName)) {
                        val = overrideValues.get(fieldName);
                    } else {
                        String mVal = masterDetails.customFields().get(fieldName);
                        String dVal = dupDetails.customFields().get(fieldName);
                        val = (mVal != null && !mVal.isBlank()) ? mVal : dVal;
                    }

                    if (val != null && !val.isBlank()) {
                        customFieldDAO.upsertValue(conn, def.getId(), masterRecordId, val.trim());
                    }
                }

                // 3. Delete duplicate's custom field values
                for (CustomField def : definitions) {
                    customFieldDAO.deleteValue(conn, def.getId(), duplicateRecordId);
                }

                // 4. Delete duplicate record from customers table
                scopedEntityDAO.delete(conn, ScopeEntityType.CUSTOMERS, duplicateRecordId);

                // 5. Write Audit Log
                JsonObject beforeJson = new JsonObject();
                beforeJson.addProperty("master_id", masterRecordId);
                beforeJson.addProperty("master_name", masterDetails.name());
                beforeJson.addProperty("duplicate_id", duplicateRecordId);
                beforeJson.addProperty("duplicate_name", dupDetails.name());

                JsonObject afterJson = new JsonObject();
                afterJson.addProperty("action", "MERGE_CUSTOMER");
                afterJson.addProperty("surviving_master_id", masterRecordId);
                afterJson.addProperty("deleted_duplicate_id", duplicateRecordId);

                AuditLog log = new AuditLog();
                log.setActorUserId(actorUserId);
                log.setAction("MERGE");
                log.setObjectType("CUSTOMER");
                log.setObjectId(masterRecordId);
                log.setBeforeValue(beforeJson);
                log.setAfterValue(afterJson);

                auditLogDAO.insert(conn, log);

                conn.commit();
                return new MergeResult(MergeStatus.SUCCESS, "Gộp khách hàng thành công!", masterRecordId);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void reassignChildRecords(Connection conn, String table, String col, long oldId, long newId) {
        try {
            String sql = "UPDATE " + table + " SET " + col + " = ? WHERE " + col + " = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, newId);
                stmt.setLong(2, oldId);
                stmt.executeUpdate();
            }
        } catch (SQLException ignored) {
        }
    }

    private String normalizeForComparison(String str) {
        if (str == null) return "";
        return str.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Punct}]+", " ");
    }
}
