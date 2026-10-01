package com.crm.dao.customfields;

import com.crm.model.CustomField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JDBC persistence for CRM-46 custom field definitions and values. */
public class CustomFieldDAO {
    private static final String DEFINITION_COLUMNS =
            "d.id, d.entity_type, d.field_name, d.field_label, d.field_type, d.is_required, "
                    + "d.is_active, d.show_on_form, d.usable_in_filter, d.exportable, d.sort_order, "
                    + "d.created_at, d.updated_at, "
                    + "(SELECT COUNT(*) FROM custom_field_values v WHERE v.custom_field_id = d.id) usage_count";

    public List<CustomField> findAll(Connection connection, String entityType) throws SQLException {
        return findDefinitions(connection, entityType, false);
    }

    public List<CustomField> findActive(Connection connection, String entityType) throws SQLException {
        return findDefinitions(connection, entityType, true);
    }

    private List<CustomField> findDefinitions(Connection connection, String entityType, boolean activeOnly)
            throws SQLException {
        String sql = "SELECT " + DEFINITION_COLUMNS + " FROM custom_field_definitions d "
                + "WHERE d.entity_type = ?" + (activeOnly ? " AND d.is_active = TRUE" : "")
                + " ORDER BY d.sort_order, d.id";
        List<CustomField> fields = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entityType);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) fields.add(mapDefinition(connection, rs));
            }
        }
        return fields;
    }

    public CustomField findById(Connection connection, long id) throws SQLException {
        String sql = "SELECT " + DEFINITION_COLUMNS + " FROM custom_field_definitions d WHERE d.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapDefinition(connection, rs) : null;
            }
        }
    }

    public boolean existsByName(Connection connection, String entityType, String fieldName, Long excludeId)
            throws SQLException {
        String sql = "SELECT 1 FROM custom_field_definitions WHERE entity_type = ? AND field_name = ?"
                + (excludeId == null ? "" : " AND id <> ?") + " LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entityType);
            statement.setString(2, fieldName);
            if (excludeId != null) statement.setLong(3, excludeId);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    public long insert(Connection connection, CustomField field) throws SQLException {
        String sql = "INSERT INTO custom_field_definitions "
                + "(entity_type, field_name, field_label, field_type, is_required, is_active, show_on_form, "
                + "usable_in_filter, exportable, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindDefinition(statement, field);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không lấy được ID trường tùy chỉnh vừa tạo");
                return keys.getLong(1);
            }
        }
    }

    public int update(Connection connection, CustomField field) throws SQLException {
        String sql = "UPDATE custom_field_definitions SET entity_type = ?, field_name = ?, field_label = ?, "
                + "field_type = ?, is_required = ?, is_active = ?, show_on_form = ?, usable_in_filter = ?, "
                + "exportable = ?, sort_order = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindDefinition(statement, field);
            statement.setLong(11, field.getId());
            return statement.executeUpdate();
        }
    }

    private void bindDefinition(PreparedStatement statement, CustomField field) throws SQLException {
        statement.setString(1, field.getEntityType());
        statement.setString(2, field.getFieldName());
        statement.setString(3, field.getFieldLabel());
        statement.setString(4, field.getStorageType());
        statement.setBoolean(5, field.isRequired());
        statement.setBoolean(6, field.isActive());
        statement.setBoolean(7, field.isInForm());
        statement.setBoolean(8, field.isInFilter());
        statement.setBoolean(9, field.isInExport());
        statement.setInt(10, field.getSortOrder());
    }

    public void replaceOptions(Connection connection, long fieldId, List<String> options) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM custom_field_options WHERE custom_field_id = ?")) {
            delete.setLong(1, fieldId);
            delete.executeUpdate();
        }
        if (options == null || options.isEmpty()) return;
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO custom_field_options (custom_field_id, option_value, sort_order) VALUES (?, ?, ?)")) {
            for (int i = 0; i < options.size(); i++) {
                insert.setLong(1, fieldId);
                insert.setString(2, options.get(i));
                insert.setInt(3, i + 1);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    public int delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM custom_field_definitions WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate();
        }
    }

    public int deactivate(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE custom_field_definitions SET is_active = FALSE WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate();
        }
    }

    public boolean recordExists(Connection connection, String entityType, long recordId) throws SQLException {
        String table = switch (entityType) {
            case "CUSTOMER" -> "customers";
            case "OPPORTUNITY" -> "opportunities";
            default -> throw new IllegalArgumentException("Đối tượng áp dụng không hợp lệ");
        };
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM " + table + " WHERE id = ? LIMIT 1")) {
            statement.setLong(1, recordId);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    public Map<String, String> findValues(Connection connection, String entityType, long recordId)
            throws SQLException {
        String sql = "SELECT d.field_name, v.field_value FROM custom_field_values v "
                + "JOIN custom_field_definitions d ON d.id = v.custom_field_id "
                + "WHERE d.entity_type = ? AND d.is_active = TRUE AND v.record_id = ? "
                + "ORDER BY d.sort_order, d.id";
        Map<String, String> values = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entityType);
            statement.setLong(2, recordId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) values.put(rs.getString("field_name"), rs.getString("field_value"));
            }
        }
        return values;
    }

    public void upsertValue(Connection connection, long fieldId, long recordId, String value) throws SQLException {
        String sql = "INSERT INTO custom_field_values (custom_field_id, record_id, field_value) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE field_value = VALUES(field_value), updated_at = CURRENT_TIMESTAMP";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, fieldId);
            statement.setLong(2, recordId);
            statement.setString(3, value);
            statement.executeUpdate();
        }
    }

    public void deleteValue(Connection connection, long fieldId, long recordId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM custom_field_values WHERE custom_field_id = ? AND record_id = ?")) {
            statement.setLong(1, fieldId);
            statement.setLong(2, recordId);
            statement.executeUpdate();
        }
    }

    private CustomField mapDefinition(Connection connection, ResultSet rs) throws SQLException {
        CustomField field = new CustomField();
        field.setId(rs.getLong("id"));
        field.setEntityType(rs.getString("entity_type"));
        field.setFieldName(rs.getString("field_name"));
        field.setFieldLabel(rs.getString("field_label"));
        field.setFieldType(rs.getString("field_type"));
        field.setRequired(rs.getBoolean("is_required"));
        field.setActive(rs.getBoolean("is_active"));
        field.setInForm(rs.getBoolean("show_on_form"));
        field.setInFilter(rs.getBoolean("usable_in_filter"));
        field.setInExport(rs.getBoolean("exportable"));
        field.setSortOrder(rs.getInt("sort_order"));
        field.setUsageCount(rs.getLong("usage_count"));
        field.setCreatedAt(rs.getTimestamp("created_at"));
        field.setUpdatedAt(rs.getTimestamp("updated_at"));
        field.setOptions(findOptions(connection, field.getId()));
        return field;
    }

    private List<String> findOptions(Connection connection, long fieldId) throws SQLException {
        List<String> options = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT option_value FROM custom_field_options WHERE custom_field_id = ? ORDER BY sort_order, id")) {
            statement.setLong(1, fieldId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) options.add(rs.getString("option_value"));
            }
        }
        return options;
    }
}
