package com.crm.service.customers;

import com.crm.dao.customers.CustomerDAO;
import com.crm.dao.customfields.CustomFieldValueDAO;
import com.crm.dto.customers.CustomerWriteRequest;
import com.crm.dto.customers.CustomerSearchFilter;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;

import java.sql.SQLException;
import java.sql.Connection;
import java.util.*;

public class CustomerService {

    private final CustomerDAO customerDAO;
    private final CustomFieldValueDAO customFieldValueDAO = new CustomFieldValueDAO();
    private final DataScopeService dataScopeService;

    public CustomerService() { this(new CustomerDAO(), new DataScopeService()); }

    public CustomerService(CustomerDAO customerDAO, DataScopeService dataScopeService) {
        this.customerDAO = customerDAO;
        this.dataScopeService = dataScopeService;
    }

    public Map<String, Object> search(
            long currentUserId,
            String keyword,
            String status,
            int page,
            int size
    ) throws Exception {
        return search(currentUserId, new CustomerSearchFilter(keyword, status, null, null, null, null), page, size);
    }

    public Map<String, Object> search(long currentUserId, CustomerSearchFilter filter, int page, int size) throws Exception {
        CustomerSearchFilter.pagination(page, size);
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "customer", "read");
        try (Connection conn = customerDAO.open()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            conn.setAutoCommit(false);
            try {
                // A single snapshot keeps COUNT and the current page consistent during concurrent writes.
                long total = customerDAO.count(conn, scope, filter);
                List<Map<String, Object>> items = customerDAO.search(conn, scope, filter, page, size);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("items", items);
                result.put("page", page);
                result.put("size", size);
                result.put("totalItems", total);
                result.put("totalPages", (int) Math.ceil((double) total / size));
                result.put("scope", scope.scopeType().name());
                conn.commit();
                return result;
            } catch (Exception e) {
                try { conn.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }

    public Map<String, Object> getById(long currentUserId, long id) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "customer", "read");
        Map<String, Object> customer = customerDAO.findById(id);
        if (customer == null) {
            return null;
        }

        long ownerId = (Long) customer.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        Map<String, Object> customFields = customFieldValueDAO.getValues("CUSTOMER", id);
        customer.put("customFields", customFields);
        return customer;
    }

    public Map<String, Object> create(long currentUserId, CustomerWriteRequest req) throws Exception {
        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Tên khách hàng/công ty là bắt buộc.");
        }

        validateRegion(req);
        dataScopeService.resolve(currentUserId, "customer", "create");
        long customerId = customerDAO.create(req, currentUserId);

        if (req.getCustomFields() != null && !req.getCustomFields().isEmpty()) {
            customFieldValueDAO.saveValues("CUSTOMER", customerId, req.getCustomFields());
        }

        return getById(currentUserId, customerId);
    }

    public Map<String, Object> update(long currentUserId, long id, CustomerWriteRequest req) throws Exception {
        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Tên khách hàng/công ty là bắt buộc.");
        }

        validateRegion(req);
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "customer", "update");
        Map<String, Object> existing = customerDAO.findById(id);
        if (existing == null) {
            return null;
        }

        long ownerId = (Long) existing.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        customerDAO.update(id, req);

        if (req.getCustomFields() != null && !req.getCustomFields().isEmpty()) {
            customFieldValueDAO.saveValues("CUSTOMER", id, req.getCustomFields());
        }

        return getById(currentUserId, id);
    }

    private void validateRegion(CustomerWriteRequest req) {
        if (req.hasRegion()) req.setRegion(CustomerSearchFilter.text(req.getRegion(), "region", 20));
    }

    public void delete(long currentUserId, long id) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "customer", "delete");
        Map<String, Object> existing = customerDAO.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Khách hàng không tồn tại.");
        }

        long ownerId = (Long) existing.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        customerDAO.softDelete(id);
    }
}
