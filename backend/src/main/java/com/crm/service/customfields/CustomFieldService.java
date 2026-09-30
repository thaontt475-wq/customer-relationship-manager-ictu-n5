package com.crm.service.customfields;

import com.crm.dao.customfields.CustomFieldDAO;
import com.crm.model.CustomField;
import com.crm.model.CustomFieldValues;
import com.crm.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Business rules and transactions for CRM-46 custom fields. */
public class CustomFieldService {
    private static final Set<String> ENTITIES = Set.of("CUSTOMER", "OPPORTUNITY");
    private static final Set<String> TYPES = Set.of("TEXT", "NUMBER", "DATE", "SELECT");
    private static final Pattern FIELD_NAME = Pattern.compile("^[a-z][a-z0-9_]*$");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT);

    private final CustomFieldDAO dao;
    private final ConnectionProvider connectionProvider;

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    public enum DeleteOutcome { DELETED, DEACTIVATED }

    public CustomFieldService() {
        this(new CustomFieldDAO(), DBConnection::getConnection);
    }

    public CustomFieldService(CustomFieldDAO dao, ConnectionProvider connectionProvider) {
        this.dao = dao;
        this.connectionProvider = connectionProvider;
    }

    public List<CustomField> getDefinitions(String entityType) throws SQLException {
        String entity = normalizeEntity(entityType);
        try (Connection connection = connectionProvider.getConnection()) {
            return dao.findAll(connection, entity);
        }
    }

    public CustomField getDefinition(long id) throws SQLException {
        requirePositiveId(id, "ID trường tùy chỉnh");
        try (Connection connection = connectionProvider.getConnection()) {
            return dao.findById(connection, id);
        }
    }

    public CustomField createDefinition(CustomField field) throws SQLException {
        normalizeAndValidate(field);
        try (Connection connection = connectionProvider.getConnection()) {
            return inTransaction(connection, () -> {
                if (dao.existsByName(connection, field.getEntityType(), field.getFieldName(), null)) {
                    throw new IllegalStateException("Mã hệ thống đã tồn tại trong cùng đối tượng áp dụng");
                }
                if (field.getSortOrder() <= 0) {
                    field.setSortOrder(dao.findAll(connection, field.getEntityType()).size() + 1);
                }
                long id = dao.insert(connection, field);
                field.setId(id);
                dao.replaceOptions(connection, id, field.getOptions());
                return dao.findById(connection, id);
            });
        }
    }

    public CustomField updateDefinition(long id, CustomField field) throws SQLException {
        requirePositiveId(id, "ID trường tùy chỉnh");
        field.setId(id);
        normalizeAndValidate(field);
        try (Connection connection = connectionProvider.getConnection()) {
            return inTransaction(connection, () -> {
                CustomField existing = dao.findById(connection, id);
                if (existing == null) throw new NotFoundException("Không tìm thấy trường tùy chỉnh");
                if (!existing.getEntityType().equals(field.getEntityType())
                        || !existing.getFieldName().equals(field.getFieldName())) {
                    throw new IllegalArgumentException("Không được thay đổi đối tượng áp dụng hoặc mã hệ thống");
                }
                if (dao.existsByName(connection, field.getEntityType(), field.getFieldName(), id)) {
                    throw new IllegalStateException("Mã hệ thống đã tồn tại trong cùng đối tượng áp dụng");
                }
                if (existing.getUsageCount() > 0
                        && (!existing.getStorageType().equals(field.getStorageType())
                        || !existing.getOptions().equals(field.getOptions()))) {
                    throw new IllegalStateException("Không thể đổi kiểu hoặc tùy chọn của trường đang có dữ liệu");
                }
                if (field.getSortOrder() <= 0) field.setSortOrder(existing.getSortOrder());
                dao.update(connection, field);
                dao.replaceOptions(connection, id, field.getOptions());
                return dao.findById(connection, id);
            });
        }
    }

    public DeleteOutcome deleteDefinition(long id) throws SQLException {
        requirePositiveId(id, "ID trường tùy chỉnh");
        try (Connection connection = connectionProvider.getConnection()) {
            return inTransaction(connection, () -> {
                CustomField existing = dao.findById(connection, id);
                if (existing == null) throw new NotFoundException("Không tìm thấy trường tùy chỉnh");
                if (existing.getUsageCount() > 0) {
                    dao.deactivate(connection, id);
                    return DeleteOutcome.DEACTIVATED;
                }
                dao.delete(connection, id);
                return DeleteOutcome.DELETED;
            });
        }
    }

    public CustomFieldValues getValues(String entityType, long recordId) throws SQLException {
        String entity = normalizeEntity(entityType);
        requirePositiveId(recordId, "ID bản ghi");
        try (Connection connection = connectionProvider.getConnection()) {
            if (!dao.recordExists(connection, entity, recordId)) {
                throw new NotFoundException("Không tìm thấy bản ghi cần đọc custom field");
            }
            return new CustomFieldValues(entity, recordId, dao.findValues(connection, entity, recordId));
        }
    }

    /** Patch semantics: supplied values replace/delete existing values; required validation uses the merged result. */
    public CustomFieldValues saveValues(String entityType, long recordId, Map<String, String> submitted)
            throws SQLException {
        String entity = normalizeEntity(entityType);
        requirePositiveId(recordId, "ID bản ghi");
        if (submitted == null) throw new IllegalArgumentException("Danh sách giá trị không được để trống");

        try (Connection connection = connectionProvider.getConnection()) {
            return inTransaction(connection, () -> {
                if (!dao.recordExists(connection, entity, recordId)) {
                    throw new NotFoundException("Không tìm thấy bản ghi cần lưu custom field");
                }
                List<CustomField> definitions = dao.findActive(connection, entity);
                Map<String, CustomField> byName = new HashMap<>();
                for (CustomField definition : definitions) byName.put(definition.getFieldName(), definition);

                for (String name : submitted.keySet()) {
                    if (!byName.containsKey(name)) {
                        throw new IllegalArgumentException(
                                "Custom field không tồn tại hoặc đã ngừng kích hoạt: " + name);
                    }
                }

                Map<String, String> merged = new LinkedHashMap<>(dao.findValues(connection, entity, recordId));
                Map<String, String> normalized = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry : submitted.entrySet()) {
                    String value = validateValue(byName.get(entry.getKey()), entry.getValue());
                    normalized.put(entry.getKey(), value);
                    if (value == null) merged.remove(entry.getKey()); else merged.put(entry.getKey(), value);
                }
                for (CustomField definition : definitions) {
                    if (definition.isRequired() && isBlank(merged.get(definition.getFieldName()))) {
                        throw new IllegalArgumentException(
                                "Trường bắt buộc không được để trống: " + definition.getFieldLabel());
                    }
                }
                for (Map.Entry<String, String> entry : normalized.entrySet()) {
                    CustomField definition = byName.get(entry.getKey());
                    if (entry.getValue() == null) {
                        dao.deleteValue(connection, definition.getId(), recordId);
                    } else {
                        dao.upsertValue(connection, definition.getId(), recordId, entry.getValue());
                    }
                }
                return new CustomFieldValues(entity, recordId, merged);
            });
        }
    }

    private void normalizeAndValidate(CustomField field) {
        if (field == null) throw new IllegalArgumentException("Dữ liệu trường tùy chỉnh không hợp lệ");
        field.setEntityType(normalizeEntity(field.getEntityType()));
        String name = trim(field.getFieldName());
        if (name == null || !FIELD_NAME.matcher(name).matches() || name.length() > 100) {
            throw new IllegalArgumentException(
                    "Mã hệ thống phải bắt đầu bằng chữ thường và chỉ gồm chữ thường, số, dấu gạch dưới");
        }
        field.setFieldName(name);
        String label = trim(field.getFieldLabel());
        if (label == null || label.length() > 255) {
            throw new IllegalArgumentException("Nhãn hiển thị là bắt buộc và không quá 255 ký tự");
        }
        field.setFieldLabel(label);
        String type = field.getStorageType();
        type = type == null ? null : type.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Kiểu custom field không hợp lệ");
        field.setFieldType(type);
        field.setOptions(validateOptions(type, field.getOptions()));
        if (field.getSortOrder() < 0) throw new IllegalArgumentException("Thứ tự hiển thị không hợp lệ");
    }

    private List<String> validateOptions(String type, List<String> options) {
        List<String> cleaned = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        if (options != null) {
            for (String option : options) {
                String value = trim(option);
                if (value == null || value.length() > 255) {
                    throw new IllegalArgumentException("Tùy chọn SELECT không được rỗng và không quá 255 ký tự");
                }
                if (!unique.add(value.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Tùy chọn SELECT không được trùng nhau");
                }
                cleaned.add(value);
            }
        }
        if ("SELECT".equals(type) && cleaned.size() < 2) {
            throw new IllegalArgumentException("Trường SELECT cần ít nhất 2 tùy chọn hợp lệ");
        }
        if (!"SELECT".equals(type) && !cleaned.isEmpty()) {
            throw new IllegalArgumentException("Chỉ trường SELECT mới được khai báo tùy chọn");
        }
        return cleaned;
    }

    private String validateValue(CustomField definition, String rawValue) {
        String value = trim(rawValue);
        if (value == null) {
            if (definition.isRequired()) {
                throw new IllegalArgumentException(
                        "Trường bắt buộc không được để trống: " + definition.getFieldLabel());
            }
            return null;
        }
        return switch (definition.getStorageType()) {
            case "TEXT" -> {
                if (value.length() > 65_535) {
                    throw new IllegalArgumentException("Giá trị TEXT vượt quá 65535 ký tự");
                }
                yield value;
            }
            case "NUMBER" -> parseNumber(value, definition.getFieldLabel());
            case "DATE" -> parseDate(value, definition.getFieldLabel());
            case "SELECT" -> validateSelect(value, definition);
            default -> throw new IllegalArgumentException("Kiểu custom field không hợp lệ");
        };
    }

    private String parseNumber(String value, String label) {
        try {
            return new BigDecimal(value).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá trị NUMBER không hợp lệ cho trường: " + label);
        }
    }

    private String parseDate(String value, String label) {
        try {
            return LocalDate.parse(value, DATE_FORMAT).toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Giá trị DATE phải đúng định dạng yyyy-MM-dd cho trường: " + label);
        }
    }

    private String validateSelect(String value, CustomField definition) {
        if (!definition.getOptions().contains(value)) {
            throw new IllegalArgumentException("Giá trị SELECT không thuộc danh sách tùy chọn: " + value);
        }
        return value;
    }

    private String normalizeEntity(String value) {
        String entity = trim(value);
        entity = entity == null ? null : entity.toUpperCase(Locale.ROOT);
        if (!ENTITIES.contains(entity)) {
            throw new IllegalArgumentException("Đối tượng áp dụng chỉ hỗ trợ CUSTOMER hoặc OPPORTUNITY");
        }
        return entity;
    }

    private static String trim(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private static boolean isBlank(String value) { return value == null || value.isBlank(); }

    private static void requirePositiveId(long id, String label) {
        if (id <= 0) throw new IllegalArgumentException(label + " không hợp lệ");
    }

    private <T> T inTransaction(Connection connection, SqlWork<T> work) throws SQLException {
        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.run();
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            try { connection.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            throw e;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> { T run() throws SQLException; }

    public static class NotFoundException extends IllegalArgumentException {
        public NotFoundException(String message) { super(message); }
    }
}
