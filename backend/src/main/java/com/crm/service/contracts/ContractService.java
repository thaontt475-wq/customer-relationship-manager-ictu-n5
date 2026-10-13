package com.crm.service.contracts;

import com.crm.config.DatabaseConfig;
import com.crm.dao.contracts.ContractDAO;
import com.crm.service.audit.AuditLogService;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.*;

public class ContractService {

    private final ContractDAO contractDAO = new ContractDAO();
    private final DataScopeService dataScopeService = new DataScopeService();
    private final AuditLogService auditLogService = new AuditLogService();

    private DataScopeContext resolveScopeSafe(long currentUserId, String action) {
        try {
            return dataScopeService.resolve(currentUserId, "contract", action);
        } catch (Exception e) {
            // Fallback for setups where contract role_module_scopes may be unseeded
            return null;
        }
    }

    public List<Map<String, Object>> search(
            long currentUserId,
            String keyword,
            String status,
            Long customerId
    ) throws Exception {
        DataScopeContext scope = resolveScopeSafe(currentUserId, "read");
        return contractDAO.search(scope, keyword, status, customerId);
    }

    public Map<String, Object> getById(long currentUserId, long id) throws Exception {
        DataScopeContext scope = resolveScopeSafe(currentUserId, "read");
        Map<String, Object> contract = contractDAO.findById(id);
        if (contract == null) return null;

        if (scope != null) {
            long ownerId = (Long) contract.get("ownerUserId");
            if (!scope.canAccessOwner(ownerId)) {
                throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
            }
        }
        return contract;
    }

    public Map<String, Object> create(
            long currentUserId,
            String contractNumber,
            String title,
            Long customerId,
            Long opportunityId,
            Long quoteId,
            BigDecimal totalAmount,
            LocalDate signedDate,
            LocalDate startDate,
            LocalDate endDate,
            String paymentTerms,
            String status,
            String notes
    ) throws Exception {
        resolveScopeSafe(currentUserId, "create");

        // Validate required fields
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên hợp đồng.");
        }
        title = title.trim();

        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn khách hàng hợp lệ.");
        }

        // Verify dates
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không thể sau ngày kết thúc.");
        }

        if (contractNumber == null || contractNumber.trim().isEmpty()) {
            contractNumber = "HD-" + (System.currentTimeMillis() % 1000000);
        } else {
            contractNumber = contractNumber.trim().toUpperCase();
        }

        if (contractDAO.existsByContractNumber(contractNumber, null)) {
            throw new IllegalArgumentException("Số hợp đồng '" + contractNumber + "' đã tồn tại trong hệ thống.");
        }

        // Verify customer exists
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM customers WHERE id = ? AND is_deleted = 0")) {
            stmt.setLong(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Khách hàng không tồn tại hoặc đã bị xóa.");
                }
            }
        }

        // If quote is linked, verify quote is APPROVED
        if (quoteId != null && quoteId > 0) {
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT status FROM quotes WHERE id = ? AND is_deleted = 0")) {
                stmt.setLong(1, quoteId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Báo giá liên kết không tồn tại hoặc đã bị xóa.");
                    }
                    String qStatus = rs.getString("status");
                    if (!"APPROVED".equalsIgnoreCase(qStatus)) {
                        throw new IllegalArgumentException("Báo giá chưa được phê duyệt nên không thể tạo hợp đồng từ báo giá này.");
                    }
                }
            }
        } else {
            quoteId = null;
        }

        // Verify dates
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không thể sau ngày kết thúc.");
        }

        if (status == null || status.trim().isEmpty()) {
            status = "DRAFT";
        }

        long contractId = contractDAO.create(
                contractNumber,
                title,
                customerId,
                opportunityId,
                quoteId,
                totalAmount != null ? totalAmount : BigDecimal.ZERO,
                signedDate,
                startDate,
                endDate,
                paymentTerms,
                status,
                notes,
                currentUserId
        );

        // Audit log
        try {
            auditLogService.log(
                    currentUserId,
                    "CONTRACT",
                    contractNumber,
                    "CREATE_CONTRACT",
                    "Tạo hợp đồng mới: " + title,
                    null,
                    status
            );
        } catch (Exception ignored) {}

        return getById(currentUserId, contractId);
    }

    public Map<String, Object> update(
            long currentUserId,
            long id,
            String contractNumber,
            String title,
            Long customerId,
            Long opportunityId,
            Long quoteId,
            BigDecimal totalAmount,
            LocalDate signedDate,
            LocalDate startDate,
            LocalDate endDate,
            String paymentTerms,
            String status,
            String notes
    ) throws Exception {
        DataScopeContext scope = resolveScopeSafe(currentUserId, "update");
        Map<String, Object> existing = contractDAO.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Hợp đồng không tồn tại.");
        }

        if (scope != null) {
            long ownerId = (Long) existing.get("ownerUserId");
            if (!scope.canAccessOwner(ownerId)) {
                throw new SecurityException("Bạn không có quyền chỉnh sửa hợp đồng này do giới hạn phạm vi sở hữu.");
            }
        }

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên hợp đồng.");
        }
        title = title.trim();

        if (contractNumber == null || contractNumber.trim().isEmpty()) {
            contractNumber = String.valueOf(existing.get("contractNumber"));
        } else {
            contractNumber = contractNumber.trim().toUpperCase();
        }

        if (contractDAO.existsByContractNumber(contractNumber, id)) {
            throw new IllegalArgumentException("Số hợp đồng '" + contractNumber + "' đã tồn tại trong hệ thống.");
        }

        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Vui lòng chọn khách hàng hợp lệ.");
        }

        // Verify customer exists
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM customers WHERE id = ? AND is_deleted = 0")) {
            stmt.setLong(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Khách hàng không tồn tại hoặc đã bị xóa.");
                }
            }
        }

        // If quote is linked, verify quote is APPROVED
        if (quoteId != null && quoteId > 0) {
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT status FROM quotes WHERE id = ? AND is_deleted = 0")) {
                stmt.setLong(1, quoteId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Báo giá liên kết không tồn tại hoặc đã bị xóa.");
                    }
                    String qStatus = rs.getString("status");
                    if (!"APPROVED".equalsIgnoreCase(qStatus)) {
                        throw new IllegalArgumentException("Báo giá chưa được phê duyệt nên không thể liên kết vào hợp đồng.");
                    }
                }
            }
        } else {
            quoteId = null;
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không thể sau ngày kết thúc.");
        }

        if (status == null || status.trim().isEmpty()) {
            status = String.valueOf(existing.get("status"));
        }

        contractDAO.update(
                id,
                contractNumber,
                title,
                customerId,
                opportunityId,
                quoteId,
                totalAmount != null ? totalAmount : BigDecimal.ZERO,
                signedDate,
                startDate,
                endDate,
                paymentTerms,
                status,
                notes
        );

        // Audit log
        try {
            auditLogService.log(
                    currentUserId,
                    "CONTRACT",
                    contractNumber,
                    "UPDATE_CONTRACT",
                    "Cập nhật hợp đồng: " + title,
                    String.valueOf(existing.get("status")),
                    status
            );
        } catch (Exception ignored) {}

        return getById(currentUserId, id);
    }

    public void delete(long currentUserId, long id) throws Exception {
        DataScopeContext scope = resolveScopeSafe(currentUserId, "delete");
        if (scope == null) {
            scope = resolveScopeSafe(currentUserId, "update");
        }

        Map<String, Object> existing = contractDAO.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Hợp đồng không tồn tại.");
        }

        if (scope != null) {
            long ownerId = (Long) existing.get("ownerUserId");
            if (!scope.canAccessOwner(ownerId)) {
                throw new SecurityException("Bạn không có quyền xóa hợp đồng này do giới hạn phạm vi sở hữu.");
            }
        }

        contractDAO.softDelete(id);

        try {
            auditLogService.log(
                    currentUserId,
                    "CONTRACT",
                    String.valueOf(existing.get("contractNumber")),
                    "DELETE_CONTRACT",
                    "Xóa hợp đồng " + existing.get("title"),
                    String.valueOf(existing.get("status")),
                    "DELETED"
            );
        } catch (Exception ignored) {}
    }
}
