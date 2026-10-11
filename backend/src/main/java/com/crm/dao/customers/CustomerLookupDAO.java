package com.crm.dao.customers;

import com.crm.config.DatabaseConfig;
import com.crm.service.permissions.DataScopeContext;
import java.sql.*;
import java.util.*;

public class CustomerLookupDAO {
    public Map<String, Object> list(DataScopeContext scope) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection connection = DatabaseConfig.getConnection()) {
            for (String type : List.of("industry", "company-size")) {
                List<Map<String, Object>> items = new ArrayList<>();
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT id,code,name FROM master_data WHERE type=? AND active=1 ORDER BY display_order,id")) {
                    query.setString(1, type);
                    try (ResultSet rs = query.executeQuery()) {
                        while (rs.next()) items.add(Map.of("id", rs.getLong(1), "code", rs.getString(2), "name", rs.getString(3)));
                    }
                }
                result.put(type.equals("industry") ? "industries" : "companySizes", items);
            }
            StringBuilder where = new StringBuilder(" WHERE u.status='ACTIVE'");
            List<Object> parameters = new ArrayList<>();
            scope.appendOwnerPredicate("u.id", where, parameters);
            List<Map<String, Object>> owners = new ArrayList<>();
            try (PreparedStatement query = connection.prepareStatement("SELECT u.id,u.full_name FROM users u" + where + " ORDER BY u.full_name,u.id")) {
                for (int i=0; i<parameters.size(); i++) query.setObject(i+1, parameters.get(i));
                try (ResultSet rs = query.executeQuery()) {
                    while (rs.next()) owners.add(Map.of("id", rs.getLong(1), "fullName", rs.getString(2)));
                }
            }
            result.put("owners", owners);
            result.put("scope", scope.scopeType().name());
        }
        return result;
    }
}
