package com.crm.dao.customfields;

import com.crm.config.DatabaseConfig;

import java.sql.*;
import java.util.*;

public class CustomFieldValueDAO {

    public Map<String, Object> getValues(String entityType, long recordId) throws SQLException {
        Map<String, Object> values = new LinkedHashMap<>();
        String sql = """
                SELECT cf.field_key, cf.field_type, cfv.field_value
                FROM custom_field_values cfv
                JOIN custom_fields cf ON cf.id = cfv.custom_field_id
                WHERE cf.entity_type = ? AND cfv.record_id = ? AND cf.active = TRUE
                """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entityType);
            stmt.setLong(2, recordId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    values.put(rs.getString("field_key"), rs.getString("field_value"));
                }
            }
        }
        return values;
    }

    public void saveValues(String entityType, long recordId, Map<String, Object> values) throws SQLException {
        if (values == null || values.isEmpty()) return;

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Find all active custom fields for entity
                Map<String, Long> fieldIds = new HashMap<>();
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT id, field_key FROM custom_fields WHERE entity_type = ? AND active = TRUE")) {
                    stmt.setString(1, entityType);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            fieldIds.put(rs.getString("field_key"), rs.getLong("id"));
                        }
                    }
                }

                String upsertSql = """
                        INSERT INTO custom_field_values (custom_field_id, record_id, field_value)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE field_value = VALUES(field_value)
                        """;
                try (PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
                    for (Map.Entry<String, Object> entry : values.entrySet()) {
                        Long fieldId = fieldIds.get(entry.getKey());
                        if (fieldId != null && entry.getValue() != null) {
                            stmt.setLong(1, fieldId);
                            stmt.setLong(2, recordId);
                            stmt.setString(3, String.valueOf(entry.getValue()));
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
