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
                if (items.isEmpty()) {
                    ensureDefaultMasterData(connection, type);
                    try (PreparedStatement query = connection.prepareStatement(
                            "SELECT id,code,name FROM master_data WHERE type=? AND active=1 ORDER BY display_order,id")) {
                        query.setString(1, type);
                        try (ResultSet rs = query.executeQuery()) {
                            while (rs.next()) items.add(Map.of("id", rs.getLong(1), "code", rs.getString(2), "name", rs.getString(3)));
                        }
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

    private void ensureDefaultMasterData(Connection connection, String type) {
        List<String[]> defaults = switch (type) {
            case "industry" -> List.of(
                    new String[]{"CNTT", "Công nghệ thông tin"},
                    new String[]{"RETAIL", "Bán lẻ / Thương mại"},
                    new String[]{"MANUFACTURING", "Sản xuất / Chế tạo"},
                    new String[]{"FINANCE", "Tài chính / Ngân hàng / Bảo hiểm"},
                    new String[]{"REAL_ESTATE", "Bất động sản / Xây dựng"},
                    new String[]{"LOGISTICS", "Vận tải / Kho bãi / Logistics"},
                    new String[]{"HEALTHCARE", "Y tế / Dược phẩm"},
                    new String[]{"EDUCATION", "Giáo dục / Đào tạo"},
                    new String[]{"FNB", "Thực phẩm / Đồ uống (F&B)"},
                    new String[]{"SERVICES", "Dịch vụ / Du lịch / Khách sạn"},
                    new String[]{"OTHER", "Ngành nghề khác"}
            );
            case "company-size" -> List.of(
                    new String[]{"MICRO", "Dưới 10 nhân sự (Siêu nhỏ)"},
                    new String[]{"SMALL", "10 - 50 nhân sự (Nhỏ)"},
                    new String[]{"MEDIUM", "50 - 200 nhân sự (Vừa)"},
                    new String[]{"LARGE", "200 - 500 nhân sự (Lớn)"},
                    new String[]{"ENTERPRISE", "Trên 500 nhân sự (Tập đoàn)"}
            );
            default -> List.of();
        };

        if (defaults.isEmpty()) return;

        String sql = "INSERT IGNORE INTO master_data(type, code, name, active, display_order) VALUES (?, ?, ?, 1, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int order = 1;
            for (String[] def : defaults) {
                ps.setString(1, type);
                ps.setString(2, def[0]);
                ps.setString(3, def[1]);
                ps.setInt(4, order++);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ignored) {
        }
    }
}
