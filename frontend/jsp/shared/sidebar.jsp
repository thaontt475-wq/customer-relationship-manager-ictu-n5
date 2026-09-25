<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, com.crm.dto.permissions.MenuItem" %>
<%!
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    private boolean isUrlActive(String itemUrl, String currentUri, String contextPath) {
        if (itemUrl == null || itemUrl.trim().isEmpty() || currentUri == null || currentUri.trim().isEmpty()) {
            return false;
        }
        String u = itemUrl.trim();
        String c = currentUri.trim();

        String uNorm = (u.length() > 1 && u.endsWith("/")) ? u.substring(0, u.length() - 1) : u;
        String cNorm = (c.length() > 1 && c.endsWith("/")) ? c.substring(0, c.length() - 1) : c;

        if (uNorm.equals(cNorm)) {
            return true;
        }

        if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath)) {
            String cpNorm = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
            String withCp = uNorm.startsWith("/") ? (cpNorm + uNorm) : (cpNorm + "/" + uNorm);
            if (withCp.equals(cNorm)) {
                return true;
            }
            if (cNorm.startsWith(cpNorm)) {
                String cWithoutCp = cNorm.substring(cpNorm.length());
                if (uNorm.equals(cWithoutCp)) {
                    return true;
                }
            }
        }

        if (!"/".equals(uNorm) && !uNorm.isEmpty()) {
            if (cNorm.startsWith(uNorm + "/")) {
                return true;
            }
            if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath)) {
                String cpNorm = contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
                String withCp = uNorm.startsWith("/") ? (cpNorm + uNorm) : (cpNorm + "/" + uNorm);
                if (cNorm.startsWith(withCp + "/")) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasActiveDescendant(MenuItem item, String currentUri, String contextPath) {
        if (item == null || item.getChildren() == null) {
            return false;
        }
        for (MenuItem child : item.getChildren()) {
            if (child == null) continue;
            if (isUrlActive(child.getUrl(), currentUri, contextPath)) {
                return true;
            }
            if (hasActiveDescendant(child, currentUri, contextPath)) {
                return true;
            }
        }
        return false;
    }

    private String resolveUrl(String url, String contextPath) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String u = url.trim();
        if (u.startsWith("http://") || u.startsWith("https://") || u.startsWith("//") || u.startsWith("#")) {
            return u;
        }
        if (u.startsWith("/")) {
            if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath)) {
                if (!u.startsWith(contextPath + "/") && !u.equals(contextPath)) {
                    return contextPath + u;
                }
            }
        }
        return u;
    }
%>
<%
    Object rawMenuItems = request.getAttribute("menuItems");
    List<MenuItem> menuItems = null;
    if (rawMenuItems instanceof List<?>) {
        menuItems = (List<MenuItem>) rawMenuItems;
    }

    String currentUri = (String) request.getAttribute("jakarta.servlet.forward.request_uri");
    if (currentUri == null || currentUri.isEmpty()) {
        currentUri = request.getRequestURI();
    }
    String contextPath = request.getContextPath();
%>
<aside class="sidebar" aria-label="Sidebar navigation">
    <div class="sidebar__header">
        <div class="sidebar__brand" aria-label="CRM brand">
            <span class="sidebar__brand-mark">CRM</span>
            <span class="sidebar__brand-text">CRM</span>
        </div>
    </div>

    <nav class="sidebar__nav" aria-label="Primary navigation">
        <div class="sidebar__section">
            <span class="sidebar__section-label">Menu</span>
            <% if (menuItems == null || menuItems.isEmpty()) { %>
                <div class="sidebar__empty" role="status">
                    <span class="sidebar__empty-icon" aria-hidden="true">∅</span>
                    <span class="sidebar__empty-text">Không có menu khả dụng</span>
                </div>
            <% } else { %>
                <ul class="sidebar__menu">
                    <% for (MenuItem item : menuItems) {
                        if (item == null) continue;
                        String label = item.getLabel();
                        String url = item.getUrl();
                        String icon = item.getIcon();
                        List<MenuItem> children = item.getChildren();
                        boolean hasChildren = children != null && !children.isEmpty();
                        boolean hasUrl = url != null && !url.trim().isEmpty();
                        boolean isSelfActive = hasUrl && isUrlActive(url, currentUri, contextPath);
                        boolean isParentActive = hasChildren && hasActiveDescendant(item, currentUri, contextPath);
                        boolean isItemActive = isSelfActive || isParentActive;
                        String resolvedUrl = hasUrl ? resolveUrl(url, contextPath) : null;
                    %>
                        <li class="sidebar__item<%= isItemActive ? " sidebar__item--active" : "" %><%= hasChildren ? " sidebar__item--has-children" : "" %>">
                            <% if (hasUrl) { %>
                                <a href="<%= escapeHtml(resolvedUrl) %>" class="sidebar__link<%= isSelfActive ? " sidebar__link--active" : "" %>">
                                    <% if (icon != null && !icon.trim().isEmpty()) { %>
                                        <span class="sidebar__icon sidebar__icon--custom" aria-hidden="true"><%= escapeHtml(icon.trim()) %></span>
                                    <% } else { %>
                                        <span class="sidebar__icon" aria-hidden="true">•</span>
                                    <% } %>
                                    <span class="sidebar__text"><%= escapeHtml(label) %></span>
                                </a>
                            <% } else { %>
                                <div class="sidebar__link sidebar__link--disabled" aria-disabled="true">
                                    <% if (icon != null && !icon.trim().isEmpty()) { %>
                                        <span class="sidebar__icon sidebar__icon--custom" aria-hidden="true"><%= escapeHtml(icon.trim()) %></span>
                                    <% } else { %>
                                        <span class="sidebar__icon" aria-hidden="true">•</span>
                                    <% } %>
                                    <span class="sidebar__text"><%= escapeHtml(label) %></span>
                                    <% if (!hasChildren) { %>
                                        <span class="sidebar__badge sidebar__badge--disabled">Chưa có route</span>
                                    <% } %>
                                </div>
                            <% } %>

                            <% if (hasChildren) { %>
                                <ul class="sidebar__submenu">
                                    <% for (MenuItem child : children) {
                                        if (child == null) continue;
                                        String childLabel = child.getLabel();
                                        String childUrl = child.getUrl();
                                        String childIcon = child.getIcon();
                                        boolean childHasUrl = childUrl != null && !childUrl.trim().isEmpty();
                                        boolean childIsActive = childHasUrl && isUrlActive(childUrl, currentUri, contextPath);
                                        String childResolvedUrl = childHasUrl ? resolveUrl(childUrl, contextPath) : null;
                                    %>
                                        <li class="sidebar__subitem">
                                            <% if (childHasUrl) { %>
                                                <a href="<%= escapeHtml(childResolvedUrl) %>" class="sidebar__link<%= childIsActive ? " sidebar__link--active" : "" %>">
                                                    <% if (childIcon != null && !childIcon.trim().isEmpty()) { %>
                                                        <span class="sidebar__icon sidebar__icon--custom" aria-hidden="true"><%= escapeHtml(childIcon.trim()) %></span>
                                                    <% } else { %>
                                                        <span class="sidebar__icon" aria-hidden="true">•</span>
                                                    <% } %>
                                                    <span class="sidebar__text"><%= escapeHtml(childLabel) %></span>
                                                </a>
                                            <% } else { %>
                                                <div class="sidebar__link sidebar__link--disabled" aria-disabled="true">
                                                    <% if (childIcon != null && !childIcon.trim().isEmpty()) { %>
                                                        <span class="sidebar__icon sidebar__icon--custom" aria-hidden="true"><%= escapeHtml(childIcon.trim()) %></span>
                                                    <% } else { %>
                                                        <span class="sidebar__icon" aria-hidden="true">•</span>
                                                    <% } %>
                                                    <span class="sidebar__text"><%= escapeHtml(childLabel) %></span>
                                                    <span class="sidebar__badge sidebar__badge--disabled">Chưa có route</span>
                                                </div>
                                            <% } %>
                                        </li>
                                    <% } %>
                                </ul>
                            <% } %>
                        </li>
                    <% } %>
                </ul>
            <% } %>
        </div>
    </nav>

    <div class="sidebar__footer"></div>
</aside>
