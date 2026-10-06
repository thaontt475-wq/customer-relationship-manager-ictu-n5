package com.crm.service.quotes;

import com.crm.dao.products.ProductDAO;
import com.crm.dao.quotes.QuoteDAO;
import com.crm.service.audit.AuditLogService;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public class QuoteService {

    private final QuoteDAO quoteDAO = new QuoteDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final DataScopeService dataScopeService = new DataScopeService();
    private final AuditLogService auditLogService = new AuditLogService();

    public List<Map<String, Object>> search(
            long currentUserId,
            String keyword,
            String status
    ) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "quote", "read");
        return quoteDAO.search(scope, keyword, status);
    }

    public Map<String, Object> getById(long currentUserId, long id) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "quote", "read");
        Map<String, Object> quote = quoteDAO.findById(id);
        if (quote == null) return null;

        long ownerId = (Long) quote.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }
        return quote;
    }

    public Map<String, Object> create(
            long currentUserId,
            String title,
            Long customerId,
            Long opportunityId,
            BigDecimal discountPercent,
            List<Map<String, Object>> items
    ) throws Exception {
        dataScopeService.resolve(currentUserId, "quote", "create");

        String quoteNumber = "BG-" + System.currentTimeMillis() % 1000000;
        if (discountPercent == null) discountPercent = BigDecimal.ZERO;

        BigDecimal subtotal = BigDecimal.ZERO;
        boolean requiresApproval = false;

        if (items != null) {
            for (Map<String, Object> it : items) {
                Long prodId = ((Number) it.get("productId")).longValue();
                int qty = it.get("quantity") != null ? ((Number) it.get("quantity")).intValue() : 1;
                BigDecimal unitPrice = it.get("unitPrice") != null ? new BigDecimal(String.valueOf(it.get("unitPrice"))) : BigDecimal.ZERO;
                BigDecimal itemDiscount = it.get("discountPercent") != null ? new BigDecimal(String.valueOf(it.get("discountPercent"))) : BigDecimal.ZERO;

                // CRM-44 Tiêu chí 2: Giá sàn là ngưỡng để xác định báo giá có cần duyệt chiết khấu hay không
                Map<String, Object> prod = productDAO.findById(prodId);
                if (prod != null && prod.get("floorPrice") != null) {
                    BigDecimal floorPrice = (BigDecimal) prod.get("floorPrice");
                    if (floorPrice != null && unitPrice.compareTo(floorPrice) < 0) {
                        requiresApproval = true;
                    }
                }

                BigDecimal lineAmount = unitPrice.multiply(BigDecimal.valueOf(qty));
                if (itemDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discAmt = lineAmount.multiply(itemDiscount).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    lineAmount = lineAmount.subtract(discAmt);
                }
                it.put("amount", lineAmount);
                subtotal = subtotal.add(lineAmount);
            }
        }

        BigDecimal totalAmount = subtotal;
        if (discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discAmt = subtotal.multiply(discountPercent).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            totalAmount = subtotal.subtract(discAmt);
            if (discountPercent.compareTo(new BigDecimal("15")) > 0) {
                requiresApproval = true;
            }
        }

        String status = requiresApproval ? "PENDING_APPROVAL" : "DRAFT";

        long quoteId = quoteDAO.create(
                quoteNumber, title, customerId, opportunityId,
                discountPercent, subtotal, totalAmount, status, requiresApproval,
                currentUserId, items
        );

        // CRM-39: Ghi audit log thay đổi trên chiết khấu
        if (discountPercent.compareTo(BigDecimal.ZERO) > 0 || requiresApproval) {
            try {
                auditLogService.log(
                        currentUserId,
                        "DISCOUNT",
                        quoteNumber,
                        "CREATE_QUOTE_DISCOUNT",
                        "Áp dụng chiết khấu báo giá " + quoteNumber + " (" + discountPercent + "%)",
                        "0%",
                        discountPercent + "%" + (requiresApproval ? " [Yêu cầu duyệt]" : "")
                );
            } catch (Exception ignored) {}
        }

        return getById(currentUserId, quoteId);
    }

    public void delete(long currentUserId, long id) throws Exception {
        dataScopeService.resolve(currentUserId, "quote", "update");
        Map<String, Object> existing = quoteDAO.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Báo giá không tồn tại.");
        }

        long ownerId = (Long) existing.get("ownerUserId");
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "quote", "update");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        quoteDAO.softDelete(id);
    }

    public Map<String, Object> updateStatus(long currentUserId, long id, String status) throws Exception {
        dataScopeService.resolve(currentUserId, "quote", "update");
        Map<String, Object> existing = quoteDAO.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Báo giá không tồn tại.");
        }

        long ownerId = (Long) existing.get("ownerUserId");
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "quote", "update");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        quoteDAO.updateStatus(id, status);

        // Audit log on approval / rejection
        try {
            auditLogService.log(
                    currentUserId,
                    "QUOTE",
                    String.valueOf(existing.get("quoteNumber")),
                    "UPDATE_QUOTE_STATUS",
                    "Cập nhật trạng thái báo giá sang " + status,
                    String.valueOf(existing.get("status")),
                    status
            );
        } catch (Exception ignored) {}

        return getById(currentUserId, id);
    }
}
