package com.crm.service.customers;

import com.crm.dao.customers.SavedCustomerFilterDAO;
import com.crm.dao.customers.SavedCustomerFilterDAO.SavedFilter;
import com.crm.dto.customers.CustomerSearchFilter;
import com.crm.service.permissions.DataScopeService;
import java.util.*;

/** CUSTOMER saved-filter service. Proposed HTTP contract requires group confirmation. */
public class SavedCustomerFilterService {
    private final SavedCustomerFilterDAO dao;
    private final DataScopeService scopes;
    private final CustomerService customers;

    public SavedCustomerFilterService() {
        this(new SavedCustomerFilterDAO(), new DataScopeService(), new CustomerService());
    }

    public SavedCustomerFilterService(SavedCustomerFilterDAO dao, DataScopeService scopes, CustomerService customers) {
        this.dao = dao; this.scopes = scopes; this.customers = customers;
    }

    public SavedFilter create(long user, String name, CustomerSearchFilter filter) throws Exception {
        scopes.resolve(user, "customer", "read");
        String normalized = CustomerSearchFilter.text(name, "name", 100);
        if (normalized == null) throw new IllegalArgumentException("Tên bộ lọc là bắt buộc");
        if (filter == null) throw new IllegalArgumentException("Thiếu điều kiện bộ lọc");
        return dao.create(user, normalized, filter);
    }

    public List<SavedFilter> list(long user, int page, int size) throws Exception {
        scopes.resolve(user, "customer", "read");
        CustomerSearchFilter.pagination(page, size);
        return dao.list(user, page, size);
    }

    public SavedFilter get(long user, long id) throws Exception {
        scopes.resolve(user, "customer", "read");
        requireId(id); return dao.find(user, id);
    }

    public Map<String, Object> apply(long user, long id, int page, int size) throws Exception {
        // Only criteria survive saving. CustomerService resolves current role and scope again.
        SavedFilter saved = get(user, id);
        return customers.search(user, saved.filter(), page, size);
    }

    public void delete(long user, long id) throws Exception {
        scopes.resolve(user, "customer", "read");
        requireId(id); dao.delete(user, id);
    }

    private void requireId(long id) {
        if (id <= 0) throw new IllegalArgumentException("Filter ID phải là số nguyên dương");
    }
}
