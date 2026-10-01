<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, com.crm.dto.permissions.MenuItem" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
<%!
    private String sidebarEscapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /* Presentation only: the backend still owns menu items and visibility. */
    private String sidebarIcon(String code) {
        String paths;
        switch (code == null ? "" : code) {
            case "DASHBOARD": paths = "<rect x='3' y='3' width='7' height='7' rx='1'/><rect x='14' y='3' width='7' height='7' rx='1'/><rect x='3' y='14' width='7' height='7' rx='1'/><rect x='14' y='14' width='7' height='7' rx='1'/>"; break;
            case "USERS": case "CUSTOMERS": paths = "<circle cx='9' cy='8' r='3'/><path d='M3 21v-3a6 6 0 0 1 12 0v3m1-16a3 3 0 0 1 0 6m2 4a5 5 0 0 1 3 5'/>"; break;
            case "PERMISSIONS": case "USERS_AUDIT": paths = "<path d='M12 3 3 7v5c0 5 9 9 9 9s9-4 9-9V7z'/><path d='m8 12 3 3 5-6'/>"; break;
            case "CHANGE_PASSWORD": paths = "<rect x='5' y='10' width='14' height='11' rx='2'/><path d='M8 10V7a4 4 0 0 1 8 0v3m-4 5v2'/>"; break;
            case "LOGOUT": paths = "<path d='M9 21H4V3h5m7 4 5 5-5 5M9 12h12'/>"; break;
            case "SALES_CONFIG": paths = "<path d='m12 3 9 5v8l-9 5-9-5V8zm0 9 9-4m-9 4L3 8m9 4v9'/>"; break;
            case "OPPORTUNITIES": case "KPI": case "REPORTS": paths = "<path d='M4 20V10m8 10V4m8 16v-7M2 22h20'/>"; break;
            case "ACTIVITIES": paths = "<rect x='3' y='5' width='18' height='16' rx='2'/><path d='M7 3v4m10-4v4M3 11h18'/>"; break;
            default: paths = "<rect x='4' y='4' width='16' height='16' rx='3'/><path d='M8 9h8m-8 6h8'/>";
        }
        return "<svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='1.7' stroke-linecap='round' stroke-linejoin='round' aria-hidden='true'>" + paths + "</svg>";
    }

    private String sidebarResolveUrl(String url, String contextPath) {
        if (url == null || url.trim().isEmpty()) return null;
        String normalized = url.trim();
        if (normalized.startsWith("#")) return normalized;
        if (!normalized.startsWith("/") || normalized.startsWith("//")
                || normalized.contains("\\") || normalized.indexOf('\r') >= 0
                || normalized.indexOf('\n') >= 0) return null;
        if (normalized.startsWith("/") && contextPath != null && !contextPath.isEmpty()
                && !"/".equals(contextPath) && !normalized.equals(contextPath)
                && !normalized.startsWith(contextPath + "/")) {
            return contextPath + normalized;
        }
        return normalized;
    }

    private boolean sidebarUrlActive(String url, String currentUri, String contextPath) {
        String resolved = sidebarResolveUrl(url, contextPath);
        if (resolved == null || currentUri == null) return false;
        String target = resolved.length() > 1 && resolved.endsWith("/")
                ? resolved.substring(0, resolved.length() - 1) : resolved;
        String current = currentUri.length() > 1 && currentUri.endsWith("/")
                ? currentUri.substring(0, currentUri.length() - 1) : currentUri;
        return current.equals(target) || (!"/".equals(target) && current.startsWith(target + "/"));
    }
%>
<%
    Object sidebarRawMenuItems = request.getAttribute("menuItems");
    List<MenuItem> sidebarServerMenuItems = null;
    if (sidebarRawMenuItems instanceof List<?>) {
        sidebarServerMenuItems = (List<MenuItem>) sidebarRawMenuItems;
    }
    boolean sidebarHasServerMenu = sidebarServerMenuItems != null && !sidebarServerMenuItems.isEmpty();
    String sidebarCurrentUri = (String) request.getAttribute("jakarta.servlet.forward.request_uri");
    if (sidebarCurrentUri == null || sidebarCurrentUri.isEmpty()) {
        sidebarCurrentUri = request.getRequestURI();
    }
    String sidebarContextPath = request.getContextPath();
%>
<!-- Backdrop overlay khi mở mobile sidebar drawer (AC 3) -->
<div class="sidebar__backdrop" id="crmSidebarBackdrop" aria-hidden="true"></div>

<!-- Sidebar chính của hệ thống CRM -->
<aside class="sidebar" id="crmSidebar" aria-label="Sidebar navigation">
    <div class="sidebar__header">
        <div class="sidebar__brand" aria-label="CRM brand">
            <span class="sidebar__brand-mark">CRM</span>
            <span class="sidebar__brand-text">CRM System</span>
        </div>
        <!-- Nút đóng Drawer trên thiết bị di động (AC 3) -->
        <a href="#" class="sidebar__close-btn" aria-label="Đóng menu điều hướng" title="Đóng menu">&#10005;</a>
    </div>

    <!-- Khu vực danh sách điều hướng chức năng -->
    <nav class="sidebar__nav" aria-label="Điều hướng chính">
        <div class="sidebar__section">
            <span class="sidebar__section-label">Menu chức năng</span>

            <!-- Empty State khi không có mục menu nào thuộc quyền (AC 1) -->
            <div class="sidebar__empty" id="crmSidebarEmpty" role="status" style="display: <%= sidebarHasServerMenu ? "none" : "flex" %>;">
                <span class="sidebar__empty-icon" aria-hidden="true">&#8709;</span>
                <span class="sidebar__empty-text">Không có menu khả dụng cho tài khoản này</span>
            </div>

            <!-- Menu do server render; không sử dụng Fetch API. -->
            <ul class="sidebar__menu" id="crmSidebarMenuList" style="display: <%= sidebarHasServerMenu ? "flex" : "none" %>;">
                <% if (sidebarHasServerMenu) {
                    for (MenuItem item : sidebarServerMenuItems) {
                        if (item == null) continue;
                        String itemCode = item.getCode();
                        String itemUrl = item.getUrl();
                        String itemResolvedUrl = sidebarResolveUrl(itemUrl, sidebarContextPath);
                        List<MenuItem> children = item.getChildren();
                        boolean hasChildren = children != null && !children.isEmpty();
                        boolean selfActive = sidebarUrlActive(itemUrl, sidebarCurrentUri, sidebarContextPath);
                        boolean childActive = false;
                        if (hasChildren) {
                            for (MenuItem child : children) {
                                if (child != null && sidebarUrlActive(child.getUrl(), sidebarCurrentUri, sidebarContextPath)) {
                                    childActive = true;
                                    break;
                                }
                            }
                        }
                        boolean logoutItem = "LOGOUT".equals(itemCode);
                %>
                    <li class="sidebar__item<%= (selfActive || childActive) ? " sidebar__item--active" : "" %><%= hasChildren ? " sidebar__item--has-children" : "" %>">
                        <% if (logoutItem) { %>
                            <form class="sidebar__logout-form" method="post" action="<%= sidebarEscapeHtml(itemResolvedUrl) %>">
                                <input type="hidden" name="redirectToLogin" value="true">
                                <button type="submit" class="sidebar__link sidebar__logout-button">
                                    <span class="sidebar__icon sidebar__icon--custom" aria-hidden="true"><%= sidebarIcon(itemCode) %></span>
                                    <span class="sidebar__text"><%= sidebarEscapeHtml(item.getLabel()) %></span>
                                </button>
                            </form>
                        <% } else if (itemResolvedUrl != null) { %>
                            <a href="<%= sidebarEscapeHtml(itemResolvedUrl) %>" class="sidebar__link<%= selfActive ? " sidebar__link--active" : "" %>">
                                <span class="sidebar__icon sidebar__icon--custom" aria-hidden="true"><%= sidebarIcon(itemCode) %></span>
                                <span class="sidebar__text"><%= sidebarEscapeHtml(item.getLabel()) %></span>
                            </a>
                        <% } else { %>
                            <div class="sidebar__link sidebar__link--disabled" aria-disabled="true">
                                <span class="sidebar__icon" aria-hidden="true"><%= sidebarIcon(itemCode) %></span>
                                <span class="sidebar__text"><%= sidebarEscapeHtml(item.getLabel()) %></span>
                            </div>
                        <% } %>

                        <% if (hasChildren) { %>
                            <ul class="sidebar__submenu">
                                <% for (MenuItem child : children) {
                                    if (child == null) continue;
                                    String childResolvedUrl = sidebarResolveUrl(child.getUrl(), sidebarContextPath);
                                    boolean childIsActive = sidebarUrlActive(child.getUrl(), sidebarCurrentUri, sidebarContextPath);
                                %>
                                    <li class="sidebar__subitem">
                                        <% if (childResolvedUrl != null) { %>
                                            <a href="<%= sidebarEscapeHtml(childResolvedUrl) %>" class="sidebar__link<%= childIsActive ? " sidebar__link--active" : "" %>">
                                                <span class="sidebar__icon" aria-hidden="true"><%= sidebarIcon(child.getCode()) %></span>
                                                <span class="sidebar__text"><%= sidebarEscapeHtml(child.getLabel()) %></span>
                                            </a>
                                        <% } else { %>
                                            <div class="sidebar__link sidebar__link--disabled" aria-disabled="true">
                                                <span class="sidebar__icon" aria-hidden="true"><%= sidebarIcon(child.getCode()) %></span>
                                                <span class="sidebar__text"><%= sidebarEscapeHtml(child.getLabel()) %></span>
                                            </div>
                                        <% } %>
                                    </li>
                                <% } %>
                            </ul>
                        <% } %>
                    </li>
                <%  }
                } %>
            </ul>
        </div>
    </nav>

    <!-- Footer Sidebar hiển thị Tên, Vai trò và Nhóm kinh doanh (AC 2) -->
    <div class="sidebar__footer">
        <div class="sidebar__user-card" id="crmSidebarUserCard">
            <div class="sidebar__user-avatar" id="crmSidebarAvatarText"><%= sidebarEscapeHtml(request.getAttribute("currentUserDisplayName") == null || String.valueOf(request.getAttribute("currentUserDisplayName")).isBlank() ? "U" : String.valueOf(request.getAttribute("currentUserDisplayName")).substring(0,1).toUpperCase(java.util.Locale.ROOT)) %></div>
            <div class="sidebar__user-info">
                <div class="sidebar__user-name" id="crmSidebarUserName"><%= sidebarEscapeHtml(request.getAttribute("currentUserDisplayName") == null ? "Tài khoản" : String.valueOf(request.getAttribute("currentUserDisplayName"))) %></div>
                <div class="sidebar__user-sub">
                    <span class="sidebar__role-tag" id="crmSidebarUserRole"><%= sidebarEscapeHtml(request.getAttribute("currentUserRoleLabel") == null ? "Người dùng" : String.valueOf(request.getAttribute("currentUserRoleLabel"))) %></span>
                    <span class="sidebar__team-tag" id="crmSidebarUserTeam"><%= sidebarEscapeHtml(request.getAttribute("currentUserTeamName") == null ? "Chưa phân nhóm" : String.valueOf(request.getAttribute("currentUserTeamName"))) %></span>
                </div>
            </div>
        </div>

        <div class="sidebar__status">
            <span class="sidebar__status-dot"></span>
            <span>Hệ thống trực tuyến</span>
        </div>
    </div>
</aside>

<!-- Menu động và responsive drawer được xử lý bằng JSP/CSS, không cần JavaScript. -->
