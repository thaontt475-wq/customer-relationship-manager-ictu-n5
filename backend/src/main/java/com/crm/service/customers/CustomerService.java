package com.crm.service.customers;

import com.crm.dao.customers.CustomerDAO;
import com.crm.dao.customfields.CustomFieldValueDAO;
import com.crm.dto.customers.CustomerWriteRequest;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;

import java.sql.SQLException;
import java.util.*;

public class CustomerService {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final CustomFieldValueDAO customFieldValueDAO = new CustomFieldValueDAO();
    private final DataScopeService dataScopeService = new DataScopeService();

    public Map<String, Object> search(
            long currentUserId,
            String keyword,
            String status,
            int page,
            int size
    ) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "customer", "read");
        long total = customerDAO.count(scope, keyword, status);
        int totalPages = (int) Math.ceil((double) total / size);

        List<Map<String, Object>> items = customerDAO.search(scope, keyword, status, page, size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("size", size);
        result.put("totalItems", total);
        result.put("totalPages", totalPages);
        result.put("scope", scope.scopeType().name());
        return result;
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
