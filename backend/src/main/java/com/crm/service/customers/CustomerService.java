package com.crm.service.customers;

import com.crm.dao.customers.CustomerDAO;
import com.crm.dao.customers.CustomerImportDAO;
import com.crm.dao.customfields.CustomFieldValueDAO;
import com.crm.dto.customers.CustomerWriteRequest;
import com.crm.dto.customers.CustomerSearchFilter;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class CustomerService {

    private final CustomerDAO customerDAO;
    private final CustomFieldValueDAO customFieldValueDAO;
    private final DataScopeService dataScopeService;
    private final CustomerImportDAO references;

    public CustomerService() {
        this(new CustomerDAO(), new CustomFieldValueDAO(), new DataScopeService(), new CustomerImportDAO());
    }

    public CustomerService(CustomerDAO customers, CustomFieldValueDAO fields,
                           DataScopeService scopes, CustomerImportDAO references) {
        this.customerDAO = customers;
        this.customFieldValueDAO = fields;
        this.dataScopeService = scopes;
        this.references = references;
    }

    public CustomerService(CustomerDAO customerDAO, DataScopeService dataScopeService) {
        this(customerDAO, new CustomFieldValueDAO(), dataScopeService, new CustomerImportDAO());
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
        requireId(id);
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "customer", "read");
        try (Connection conn = customerDAO.open()) {
            conn.setAutoCommit(false);
            try {
                Map<String, Object> customer = customerDAO.findById(conn, id, true);
                if (customer != null) {
                    requireOwner(scope, ((Number) customer.get("ownerUserId")).longValue());
                    customer.put("customFields", customFieldValueDAO.getValues(conn, "CUSTOMER", id));
                }
                conn.commit();
                return customer;
            } catch (Exception e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    private Map<String, Object> profile(Connection conn, long id) throws SQLException {
        Map<String, Object> customer = customerDAO.findById(conn, id, false);
        if (customer == null) {
            return null;
        }
        customer.put("customFields", customFieldValueDAO.getValues(conn, "CUSTOMER", id));
        return customer;
    }

    public Map<String, Object> create(long currentUserId, CustomerWriteRequest req) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "customer", "create");
        validateRegion(req);
        CustomerValidation.normalize(req);
        long owner = req.getOwnerUserId() == null ? currentUserId : req.getOwnerUserId();
        requireOwner(scope, owner);
        DataScopeContext read = dataScopeService.resolve(currentUserId, "customer", "read");
        requireOwner(read, owner);
        try (Connection conn = customerDAO.open()) {
            conn.setAutoCommit(false);
            try {
                validateReferences(conn, req, owner);
                long id = customerDAO.create(conn, req, owner);
                customFieldValueDAO.saveValues(conn, "CUSTOMER", id, req.getCustomFields());
                Map<String, Object> result = profile(conn, id);
                conn.commit();
                return result;
            } catch (Exception e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    public Map<String, Object> update(long currentUserId, long id, CustomerWriteRequest req) throws Exception {
        requireId(id);
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "customer", "update");
        validateRegion(req);
        CustomerValidation.normalize(req);
        DataScopeContext read = dataScopeService.resolve(currentUserId, "customer", "read");
        try (Connection conn = customerDAO.open()) {
            conn.setAutoCommit(false);
            try {
                Map<String, Object> existing = customerDAO.findById(conn, id, true);
                if (existing == null) { conn.commit(); return null; }
                long oldOwner = ((Number) existing.get("ownerUserId")).longValue();
                requireOwner(scope, oldOwner);
                requireOwner(read, oldOwner);
                long owner = req.getOwnerUserId() == null ? oldOwner : req.getOwnerUserId();
                requireOwner(scope, owner);
                requireOwner(read, owner);
                validateReferences(conn, req, owner);
                customerDAO.update(conn, id, req);
                customFieldValueDAO.saveValues(conn, "CUSTOMER", id, req.getCustomFields());
                Map<String, Object> result = profile(conn, id);
                conn.commit();
                return result;
            } catch (Exception e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    private void validateRegion(CustomerWriteRequest req) {
        if (req.hasRegion()) req.setRegion(CustomerSearchFilter.text(req.getRegion(), "region", 20));
    }

    public void delete(long currentUserId, long id) throws Exception {
        requireId(id);
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "customer", "delete");
        try (Connection conn = customerDAO.open()) {
            conn.setAutoCommit(false);
            try {
                Map<String, Object> existing = customerDAO.findById(conn, id, true);
                if (existing == null) throw new NoSuchElementException("Khách hàng không tồn tại.");
                requireOwner(scope, ((Number) existing.get("ownerUserId")).longValue());
                customerDAO.softDelete(conn, id);
                conn.commit();
            } catch (Exception e) {
                rollback(conn, e);
                throw e;
            }
        }
    }

    private void requireOwner(DataScopeContext scope, long ownerId) {
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }
    }

    private void validateReferences(Connection conn, CustomerWriteRequest req, long owner) throws SQLException {
        // Reuse S3-06's schema guard: the database arbitrates concurrent duplicate writes.
        references.requireUniqueTaxCode(conn);
        if (!references.referenceExists(conn, req.getIndustryId(), "industry")) {
            throw new IllegalArgumentException("industryId không tồn tại, không hoạt động hoặc sai danh mục");
        }
        if (!references.referenceExists(conn, req.getCompanySizeId(), "company-size")) {
            throw new IllegalArgumentException("companySizeId không tồn tại, không hoạt động hoặc sai danh mục");
        }
        if (!customerDAO.activeOwner(conn, owner)) {
            throw new IllegalArgumentException("Sales Owner không tồn tại hoặc không hoạt động");
        }
    }

    private void requireId(long id) {
        if (id <= 0) throw new IllegalArgumentException("Customer ID phải là số nguyên dương");
    }

    private void rollback(Connection conn, Exception failure) {
        try { conn.rollback(); } catch (SQLException e) { failure.addSuppressed(e); }
    }
}
