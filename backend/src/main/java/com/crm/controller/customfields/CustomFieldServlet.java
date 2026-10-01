package com.crm.controller.customfields;

import com.crm.model.CustomField;
import com.crm.model.Role;
import com.crm.model.User;
import com.crm.service.customfields.CustomFieldService;
import com.crm.service.customfields.CustomFieldService.DeleteOutcome;
import com.crm.service.customfields.CustomFieldService.NotFoundException;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** REST API for CRM-46 definitions and per-record values. */
@WebServlet({"/api/custom-fields", "/api/custom-fields/*"})
public class CustomFieldServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(CustomFieldServlet.class.getName());
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final Set<String> DEFINITION_ROLES =
            Set.of("ADMIN", "ADMINISTRATOR", "SYSTEM_ADMIN", "DIRECTOR",
                    "GIÁM ĐỐC", "GIAM DOC", "QUẢN TRỊ VIÊN", "QUAN TRI VIEN");

    private final CustomFieldService service;

    public CustomFieldServlet() { this(new CustomFieldService()); }
    public CustomFieldServlet(CustomFieldService service) { this.service = service; }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepare(request);
        if (!isAuthenticated(request)) {
            writeJson(response, 401, false, "Yêu cầu đăng nhập", null);
            return;
        }
        try {
            if (isValuesPath(request.getPathInfo())) {
                String entity = firstNonBlank(request.getParameter("entity"), request.getParameter("entityType"));
                long recordId = parsePositiveLong(request.getParameter("recordId"), "recordId");
                writeJson(response, 200, true, "Lấy giá trị custom field thành công",
                        service.getValues(entity, recordId));
                return;
            }
            if (!requireDefinitionAdmin(request, response)) return;
            Long id = parsePathId(request.getPathInfo());
            if (id != null) {
                CustomField field = service.getDefinition(id);
                if (field == null) throw new NotFoundException("Không tìm thấy trường tùy chỉnh");
                writeJson(response, 200, true, "Lấy trường tùy chỉnh thành công", field);
                return;
            }
            String entity = firstNonBlank(request.getParameter("entity"), request.getParameter("entityType"));
            writeJson(response, 200, true, "Lấy danh sách trường tùy chỉnh thành công",
                    service.getDefinitions(entity));
        } catch (Exception e) {
            handleException(response, "tải custom field", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepare(request);
        if (!requireDefinitionAdmin(request, response)) return;
        try {
            JsonObject json = readObject(request);
            CustomField field = buildDefinition(json, null, true);
            writeJson(response, 201, true, "Tạo trường tùy chỉnh thành công",
                    service.createDefinition(field));
        } catch (Exception e) {
            handleException(response, "tạo custom field", e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepare(request);
        try {
            if (isValuesPath(request.getPathInfo())) {
                if (!isAuthenticated(request)) {
                    writeJson(response, 401, false, "Yêu cầu đăng nhập", null);
                    return;
                }
                JsonObject json = readObject(request);
                String entity = getString(json, "entityType");
                if (entity == null) entity = getString(json, "entity");
                long recordId = getPositiveLong(json, "recordId");
                Map<String, String> values = readValues(json);
                writeJson(response, 200, true, "Lưu giá trị custom field thành công",
                        service.saveValues(entity, recordId, values));
                return;
            }
            if (!requireDefinitionAdmin(request, response)) return;
            Long id = parsePathId(request.getPathInfo());
            if (id == null) throw new IllegalArgumentException("Thiếu ID trường tùy chỉnh cần cập nhật");
            CustomField existing = service.getDefinition(id);
            if (existing == null) throw new NotFoundException("Không tìm thấy trường tùy chỉnh");
            CustomField merged = buildDefinition(readObject(request), existing, false);
            writeJson(response, 200, true, "Cập nhật trường tùy chỉnh thành công",
                    service.updateDefinition(id, merged));
        } catch (Exception e) {
            handleException(response, "cập nhật custom field", e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepare(request);
        if (!requireDefinitionAdmin(request, response)) return;
        try {
            Long id = parsePathId(request.getPathInfo());
            if (id == null) throw new IllegalArgumentException("Thiếu ID trường tùy chỉnh cần xóa");
            DeleteOutcome result = service.deleteDefinition(id);
            String message = result == DeleteOutcome.DEACTIVATED
                    ? "Trường đang có dữ liệu nên đã được chuyển sang ngừng kích hoạt"
                    : "Xóa trường tùy chỉnh thành công";
            writeJson(response, 200, true, message, Map.of("outcome", result.name()));
        } catch (Exception e) {
            handleException(response, "xóa custom field", e);
        }
    }

    private CustomField buildDefinition(JsonObject json, CustomField existing, boolean creating) {
        CustomField field = existing == null ? new CustomField() : copy(existing);
        if (has(json, "entityType")) field.setEntityType(getString(json, "entityType"));
        if (has(json, "fieldName")) field.setFieldName(getString(json, "fieldName"));
        if (has(json, "fieldLabel")) field.setFieldLabel(getString(json, "fieldLabel"));
        if (has(json, "fieldType")) field.setFieldType(getString(json, "fieldType"));
        if (has(json, "isRequired")) field.setRequired(getBoolean(json, "isRequired"));
        if (has(json, "required")) field.setRequired(getBoolean(json, "required"));
        if (has(json, "options")) field.setOptions(getStringList(json, "options"));
        if (has(json, "sortOrder")) field.setSortOrder(getInt(json, "sortOrder"));
        if (has(json, "active")) field.setActive(getBoolean(json, "active"));
        if (has(json, "inForm")) field.setInForm(getBoolean(json, "inForm"));
        if (has(json, "showOnForm")) field.setInForm(getBoolean(json, "showOnForm"));
        if (has(json, "inFilter")) field.setInFilter(getBoolean(json, "inFilter"));
        if (has(json, "usableInFilter")) field.setInFilter(getBoolean(json, "usableInFilter"));
        if (has(json, "inExport")) field.setInExport(getBoolean(json, "inExport"));
        if (has(json, "exportable")) field.setInExport(getBoolean(json, "exportable"));
        if (creating && (field.getEntityType() == null || field.getFieldName() == null
                || field.getFieldLabel() == null || field.getFieldType() == null)) {
            throw new IllegalArgumentException("Thiếu entityType, fieldName, fieldLabel hoặc fieldType");
        }
        return field;
    }

    private CustomField copy(CustomField source) {
        CustomField copy = new CustomField();
        copy.setId(source.getId());
        copy.setEntityType(source.getEntityType());
        copy.setFieldName(source.getFieldName());
        copy.setFieldLabel(source.getFieldLabel());
        copy.setFieldType(source.getFieldType());
        copy.setRequired(source.isRequired());
        copy.setOptions(source.getOptions());
        copy.setSortOrder(source.getSortOrder());
        copy.setActive(source.isActive());
        copy.setInForm(source.isInForm());
        copy.setInFilter(source.isInFilter());
        copy.setInExport(source.isInExport());
        copy.setUsageCount(source.getUsageCount());
        return copy;
    }

    private Map<String, String> readValues(JsonObject json) {
        if (!json.has("values") || !json.get("values").isJsonObject()) {
            throw new IllegalArgumentException("values phải là một JSON object");
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("values").entrySet()) {
            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull()) values.put(entry.getKey(), null);
            else if (value.isJsonPrimitive()) values.put(entry.getKey(), value.getAsString());
            else throw new IllegalArgumentException("Giá trị custom field phải là kiểu đơn giản hoặc null");
        }
        return values;
    }

    private boolean requireDefinitionAdmin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (!isAuthenticated(request)) {
            writeJson(response, 401, false, "Yêu cầu đăng nhập", null);
            return false;
        }
        if (!hasDefinitionRole(request.getSession(false))) {
            writeJson(response, 403, false, "Chỉ quản trị viên được cấu hình custom field", null);
            return false;
        }
        return true;
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        Object userId = session.getAttribute("userId");
        if (userId instanceof Number number && number.longValue() > 0) return true;
        if (userId instanceof String text) {
            try { if (Long.parseLong(text) > 0) return true; } catch (NumberFormatException ignored) { }
        }
        Object current = session.getAttribute(SessionKey.CURRENT_USER);
        if (current instanceof User user) return user.getId() > 0;
        if (current instanceof Map<?, ?> map) {
            Object id = map.get("id");
            return id instanceof Number number && number.longValue() > 0;
        }
        return false;
    }

    private boolean hasDefinitionRole(HttpSession session) {
        if (session == null) return false;
        List<String> roles = new ArrayList<>();
        collectRoles(session.getAttribute(SessionKey.ROLES), roles);
        Object current = session.getAttribute(SessionKey.CURRENT_USER);
        if (current instanceof User user) {
            collectRoles(user.getRoles(), roles);
            collectRoles(user.getRole(), roles);
        } else if (current instanceof Map<?, ?> map) {
            collectRoles(map.get("roles"), roles);
            collectRoles(map.get("role"), roles);
        }
        return roles.stream().map(this::normalizeRole).anyMatch(DEFINITION_ROLES::contains);
    }

    private void collectRoles(Object value, List<String> result) {
        if (value == null) return;
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> collectRoles(item, result));
        } else if (value instanceof Role role) {
            result.add(role.getName());
        } else if (value instanceof Map<?, ?> map && map.get("name") != null) {
            result.add(String.valueOf(map.get("name")));
        } else {
            result.add(String.valueOf(value));
        }
    }

    private String normalizeRole(String role) {
        String result = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        return result.startsWith("ROLE_") ? result.substring(5) : result;
    }

    private void handleException(HttpServletResponse response, String operation, Exception error)
            throws IOException {
        if (error instanceof NotFoundException) {
            writeJson(response, 404, false, error.getMessage(), null);
        } else if (error instanceof IllegalStateException) {
            writeJson(response, 409, false, error.getMessage(), null);
        } else if (error instanceof IllegalArgumentException || error instanceof JsonParseException) {
            writeJson(response, 400, false, error.getMessage(), null);
        } else if (error instanceof SQLException) {
            LOGGER.log(Level.SEVERE, "CRM-46 database error while " + operation, error);
            writeJson(response, 500, false, "Lỗi hệ thống khi " + operation, null);
        } else {
            LOGGER.log(Level.SEVERE, "CRM-46 unexpected error while " + operation, error);
            writeJson(response, 500, false, "Lỗi hệ thống khi " + operation, null);
        }
    }

    private JsonObject readObject(HttpServletRequest request) throws IOException {
        JsonElement element = GSON.fromJson(request.getReader(), JsonElement.class);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("Body phải là JSON object");
        }
        return element.getAsJsonObject();
    }

    private void prepare(HttpServletRequest request) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    }

    private boolean isValuesPath(String path) { return "/values".equals(path) || "/values/".equals(path); }

    private Long parsePathId(String path) {
        if (path == null || path.isBlank() || "/".equals(path) || isValuesPath(path)) return null;
        String value = path.startsWith("/") ? path.substring(1) : path;
        int slash = value.indexOf('/');
        if (slash >= 0) value = value.substring(0, slash);
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID trường tùy chỉnh không hợp lệ");
        }
    }

    private long parsePositiveLong(String value, String name) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (Exception e) {
            throw new IllegalArgumentException(name + " không hợp lệ");
        }
    }

    private long getPositiveLong(JsonObject object, String name) {
        if (!has(object, name)) throw new IllegalArgumentException("Thiếu " + name);
        return parsePositiveLong(object.get(name).getAsString(), name);
    }

    private boolean has(JsonObject object, String name) {
        return object.has(name) && !object.get(name).isJsonNull();
    }

    private String getString(JsonObject object, String name) {
        return has(object, name) ? object.get(name).getAsString() : null;
    }

    private boolean getBoolean(JsonObject object, String name) {
        return object.get(name).getAsBoolean();
    }

    private int getInt(JsonObject object, String name) { return object.get(name).getAsInt(); }

    private List<String> getStringList(JsonObject object, String name) {
        if (!object.get(name).isJsonArray()) throw new IllegalArgumentException(name + " phải là mảng");
        List<String> values = new ArrayList<>();
        object.getAsJsonArray(name).forEach(element -> values.add(element.getAsString()));
        return values;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private void writeJson(HttpServletResponse response, int status, boolean success,
                           String message, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        GSON.toJson(new ApiResponse(success, message, data), response.getWriter());
    }

    private record ApiResponse(boolean success, String message, Object data) { }
}
