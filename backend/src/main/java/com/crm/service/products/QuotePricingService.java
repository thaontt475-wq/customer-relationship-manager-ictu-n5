package com.crm.service.products;

import com.crm.dao.products.QuotePricingDAO;
import com.crm.dao.audit.BusinessChangeDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.QuoteItem;
import com.crm.service.scope.*;
import com.crm.service.audit.AuditLogService;
import com.crm.util.DBConnection;
import java.sql.*;
import java.math.*;
import java.util.*;

public class QuotePricingService {
    private final QuotePricingDAO dao;
    private final AuditLogService audit;
    private final BusinessChangeDAO businessChangeDAO;
    private final UserDAO userDAO;
    private final AuditLogService.ConnectionProvider connectionProvider;

    public record Pricing(List<QuoteItem> items, BigDecimal discount, BigDecimal total, boolean requiresApproval, String status) { }

    public QuotePricingService() {
        this(new QuotePricingDAO(), new AuditLogService(), new BusinessChangeDAO(), new UserDAO(), DBConnection::getConnection);
    }

    public QuotePricingService(QuotePricingDAO dao, AuditLogService audit, BusinessChangeDAO businessChangeDAO, UserDAO userDAO, AuditLogService.ConnectionProvider connectionProvider) {
        this.dao = dao;
        this.audit = audit;
        this.businessChangeDAO = businessChangeDAO;
        this.userDAO = userDAO;
        this.connectionProvider = connectionProvider;
    }

    public static BigDecimal netUnitPrice(BigDecimal unitPrice, BigDecimal discount) {
        if (unitPrice == null) return BigDecimal.ZERO;
        if (discount == null || discount.compareTo(BigDecimal.ZERO) == 0) {
            return unitPrice.setScale(2, RoundingMode.HALF_UP);
        }
        return unitPrice.multiply(BigDecimal.ONE.subtract(discount.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean belowFloor(List<QuoteItem> items, BigDecimal discount) {
        if (items == null || items.isEmpty()) return false;
        return items.stream().anyMatch(i -> netUnitPrice(i.unitPrice(), discount).compareTo(i.floorPrice()) < 0);
    }

    private BusinessChangeDAO.QuoteDiscount authorize(Connection c, long actor, long quote) throws SQLException {
        BusinessChangeDAO.QuoteDiscount q = businessChangeDAO.quote(c, quote);
        if (q == null) {
            throw new IllegalArgumentException("Báo giá không tồn tại.");
        }
        if (!new ScopeAccessPolicy().canAccess(new DataScopeService().loadContext(c, actor), q.record())) {
            throw new SecurityException("Bạn không có quyền truy cập báo giá này.");
        }
        return q;
    }

    public Pricing read(long actor, long quote) throws SQLException {
        try (Connection c = connectionProvider.getConnection()) {
            BusinessChangeDAO.QuoteDiscount q = authorize(c, actor, quote);
            List<QuoteItem> items = dao.items(c, quote);
            BigDecimal discount = q.discount() != null ? q.discount() : BigDecimal.ZERO;
            BigDecimal total = items.stream()
                    .map(i -> netUnitPrice(i.unitPrice(), discount).multiply(i.quantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean reqApproval = belowFloor(items, discount);
            String status = dao.status(c, quote);
            return new Pricing(items, discount, total, reqApproval, status);
        }
    }

    /** Any price/discount edit invalidates previous approval in the same transaction. */
    public void reprice(Connection c, long quote, BigDecimal discount) throws SQLException {
        List<QuoteItem> items = dao.items(c, quote);
        String newStatus = belowFloor(items, discount) ? "PENDING_APPROVAL" : "DRAFT";
        dao.status(c, quote, newStatus, null);
    }

    public void addItem(long actor, long quote, long product, BigDecimal qty, BigDecimal price) throws SQLException {
        if (qty == null || qty.signum() <= 0 || qty.scale() > 2 || qty.compareTo(new BigDecimal("9999999999.99")) > 0
                || price == null || price.signum() < 0 || price.scale() > 2 || price.compareTo(new BigDecimal("9999999999999.99")) > 0) {
            throw new IllegalArgumentException("Số lượng hoặc đơn giá không hợp lệ.");
        }
        try (Connection c = connectionProvider.getConnection()) {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                BusinessChangeDAO.QuoteDiscount q = authorize(c, actor, quote);
                dao.add(c, quote, product, qty, price);
                reprice(c, quote, q.discount());
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollbackEx) { e.addSuppressed(rollbackEx); }
                throw e;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        }
    }

    public void approve(long actor, long quote) throws SQLException {
        try (Connection c = connectionProvider.getConnection()) {
            boolean autoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                authorize(c, actor, quote);
                List<String> roles = userDAO.findRoleNamesByUserId(c, actor);
                if (roles.stream().noneMatch(r -> "Admin".equalsIgnoreCase(r) || "Director".equalsIgnoreCase(r))) {
                    throw new SecurityException("Bạn không có quyền phê duyệt chiết khấu.");
                }
                String before = dao.status(c, quote);
                if (!"PENDING_APPROVAL".equals(before)) {
                    throw new IllegalArgumentException("Báo giá không ở trạng thái chờ phê duyệt.");
                }
                dao.status(c, quote, "APPROVED", actor);
                audit.recordChange(c, actor, "DISCOUNT_APPROVED", "QUOTE", quote, before, "APPROVED");
                c.commit();
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollbackEx) { e.addSuppressed(rollbackEx); }
                throw e;
            } finally {
                c.setAutoCommit(autoCommit);
            }
        }
    }
}
