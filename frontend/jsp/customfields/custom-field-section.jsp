<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.util.Map,com.crm.model.CustomField" %>
<%!
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
%>
<%
    List<CustomField> customFields = (List<CustomField>) request.getAttribute("customFields");
    Map<String, String> customFieldValues = (Map<String, String>) request.getAttribute("customFieldValues");
    if (customFields != null && !customFields.isEmpty()) {
%>
<section class="crm-section custom-fields-section" aria-labelledby="customFieldsSectionHeading">
    <div class="crm-section-header">
        <h3 class="crm-section-title" id="customFieldsSectionHeading">Thông tin bổ sung</h3>
        <p class="crm-section-desc">Các trường dữ liệu tùy chỉnh mở rộng</p>
    </div>
    <div class="crm-form-grid custom-fields-grid">
        <%
            for (CustomField field : customFields) {
                if (field == null || !field.isActive() || !field.isInForm()) continue;
                String fieldName = field.getFieldName();
                String fieldLabel = field.getFieldLabel();
                String storageType = field.getStorageType() != null ? field.getStorageType() : "TEXT";
                boolean required = field.isRequired();
                String val = (customFieldValues != null && customFieldValues.containsKey(fieldName))
                        ? customFieldValues.get(fieldName) : "";
                if (val == null) val = "";
        %>
        <div class="crm-form-group custom-field-item">
            <label for="cf_<%= escapeHtml(fieldName) %>" class="crm-label">
                <%= escapeHtml(fieldLabel) %>
                <% if (required) { %><span class="crm-required" style="color: var(--crm-danger, #ef4444);">*</span><% } %>
            </label>
            <% if ("NUMBER".equalsIgnoreCase(storageType)) { %>
                <input type="number" step="any" id="cf_<%= escapeHtml(fieldName) %>" name="cf_<%= escapeHtml(fieldName) %>"
                       class="crm-input" value="<%= escapeHtml(val) %>"
                       placeholder="Nhập <%= escapeHtml(fieldLabel) %>"
                       <%= required ? "required" : "" %>
                       aria-describedby="feedback_cf_<%= escapeHtml(fieldName) %>">
            <% } else if ("DATE".equalsIgnoreCase(storageType)) { %>
                <input type="date" id="cf_<%= escapeHtml(fieldName) %>" name="cf_<%= escapeHtml(fieldName) %>"
                       class="crm-input" value="<%= escapeHtml(val) %>"
                       <%= required ? "required" : "" %>
                       aria-describedby="feedback_cf_<%= escapeHtml(fieldName) %>">
            <% } else if ("SELECT".equalsIgnoreCase(storageType) || "DROPDOWN".equalsIgnoreCase(storageType)) { %>
                <select id="cf_<%= escapeHtml(fieldName) %>" name="cf_<%= escapeHtml(fieldName) %>"
                        class="crm-select" <%= required ? "required" : "" %>
                        aria-describedby="feedback_cf_<%= escapeHtml(fieldName) %>">
                    <option value="">-- Chọn <%= escapeHtml(fieldLabel) %> --</option>
                    <%
                        List<String> options = field.getOptions();
                        if (options != null) {
                            for (String opt : options) {
                                boolean selected = opt != null && opt.equalsIgnoreCase(val);
                    %>
                    <option value="<%= escapeHtml(opt) %>" <%= selected ? "selected" : "" %>><%= escapeHtml(opt) %></option>
                    <%
                            }
                        }
                    %>
                </select>
            <% } else { %>
                <input type="text" id="cf_<%= escapeHtml(fieldName) %>" name="cf_<%= escapeHtml(fieldName) %>"
                       class="crm-input" value="<%= escapeHtml(val) %>"
                       placeholder="Nhập <%= escapeHtml(fieldLabel) %>"
                       maxlength="255"
                       <%= required ? "required" : "" %>
                       aria-describedby="feedback_cf_<%= escapeHtml(fieldName) %>">
            <% } %>
            <div class="crm-field-error" id="feedback_cf_<%= escapeHtml(fieldName) %>"></div>
        </div>
        <% } %>
    </div>
</section>
<% } %>
