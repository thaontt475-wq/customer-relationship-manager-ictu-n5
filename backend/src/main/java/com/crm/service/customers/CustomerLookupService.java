package com.crm.service.customers;

import com.crm.dao.customers.CustomerLookupDAO;
import com.crm.service.permissions.DataScopeService;
import java.sql.SQLException;
import java.util.Map;

public class CustomerLookupService {
    public Map<String, Object> list(long userId) throws SQLException {
        return new CustomerLookupDAO().list(new DataScopeService().resolve(userId, "customer", "read"));
    }
}
