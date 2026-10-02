package com.crm.service.audit;

import com.crm.dao.audit.BusinessChangeDAO;
import com.crm.dao.users.UserDAO;
import com.crm.service.scope.*;
import com.crm.util.DBConnection;
import java.math.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Business writes and audit entries share one connection and transaction.
 */
public class BusinessChangeService {
    private final BusinessChangeDAO dao;
    private final AuditLogService audit;
    private final AuditLogService.ConnectionProvider connections;

    public BusinessChangeService() {
        this(new BusinessChangeDAO(), new AuditLogService(), DBConnection::getConnection);
    }

    public BusinessChangeService(BusinessChangeDAO dao, AuditLogService audit, AuditLogService.ConnectionProvider connections) {
        this.dao = dao;
        this.audit = audit;
        this.connections = connections;
    }

    public void changeDiscount(long actor, long quote, BigDecimal amount) throws SQLException {
        decimal(amount, new BigDecimal("100"));
        try (Connection c = connections.getConnection()) {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                BusinessChangeDAO.QuoteDiscount before = dao.quote(c, quote);
                if (before == null) {
                    throw new IllegalArgumentException("Báo giá không tồn tại.");
                }
                if (!new ScopeAccessPolicy().canAccess(new DataScopeService().loadContext(c, actor), before.record())) {
                    throw new SecurityException("Bạn không có quyền thay đổi báo giá này.");
                }
                if (before.discount().compareTo(amount) != 0) {
                    dao.discount(c, quote, amount);
                    audit.recordDiscountChange(c, actor, "QUOTE", quote, before.discount(), amount);
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollbackEx) { e.addSuppressed(rollbackEx); }
                throw e;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        }
    }

    public void setTarget(long actor, long user, LocalDate month, BigDecimal amount) throws SQLException {
        decimal(amount, new BigDecimal("9999999999999.99"));
        if (month == null || month.getDayOfMonth() != 1) {
            throw new IllegalArgumentException("Kỳ chỉ tiêu phải là đầu tháng.");
        }
        try (Connection c = connections.getConnection()) {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                requireAdmin(c, actor);
                dao.lockUser(c, user);
                BusinessChangeDAO.Target before = dao.target(c, user, month);
                if (before == null || before.amount().compareTo(amount) != 0) {
                    long id = dao.saveTarget(c, before, user, month, amount);
                    audit.recordTargetChange(c, actor, "SALES_TARGET", id, before == null ? null : before.amount(), amount);
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollbackEx) { e.addSuppressed(rollbackEx); }
                throw e;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        }
    }

    public List<BusinessChangeDAO.Target> targets(long actor) throws SQLException {
        try (Connection c = connections.getConnection()) {
            requireAdmin(c, actor);
            return dao.targets(c);
        }
    }

    private void requireAdmin(Connection c, long actor) throws SQLException {
        boolean allowed = new UserDAO().findRoleNamesByUserId(c, actor).stream()
                .anyMatch(r -> "Admin".equalsIgnoreCase(r) || "Director".equalsIgnoreCase(r));
        if (!allowed) {
            throw new SecurityException("Bạn không có quyền quản lý chỉ tiêu.");
        }
    }

    private void decimal(BigDecimal value, BigDecimal max) {
        if (value == null || value.signum() < 0 || value.compareTo(max) > 0 || value.scale() > 2) {
            throw new IllegalArgumentException("Giá trị không hợp lệ; tối đa hai chữ số thập phân.");
        }
    }
}
