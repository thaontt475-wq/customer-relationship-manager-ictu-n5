<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, com.crm.dto.permissions.MenuItem" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css?v=20261005">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css?v=20261005">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/responsive.css?v=20261005">
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
            case "ORGANIZATION": case "ORGANIZATION_MGT": paths = "<circle cx='12' cy='5' r='2'/><circle cx='6' cy='19' r='2'/><circle cx='18' cy='19' r='2'/><path d='M12 7v5m-6 5v-3a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v3'/>"; break;
            case "CHANGE_PASSWORD": paths = "<rect x='5' y='10' width='14' height='11' rx='2'/><path d='M8 10V7a4 4 0 0 1 8 0v3m-4 5v2'/>"; break;
            case "LOGOUT": paths = "<path d='M9 21H4V3h5m7 4 5 5-5 5M9 12h12'/>"; break;
            case "SALES_CONFIG": case "PIPELINE": paths = "<path d='m12 3 9 5v8l-9 5-9-5V8zm0 9 9-4m-9 4L3 8m9 4v9'/>"; break;
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
    jakarta.servlet.http.Cookie[] sidebarCookies = request.getCookies();
    boolean sidebarIsRail = false;
    if (sidebarCookies != null) {
        for (jakarta.servlet.http.Cookie c : sidebarCookies) {
            if ("crm_sidebar_rail".equals(c.getName()) && "true".equals(c.getValue())) {
                sidebarIsRail = true;
                break;
            }
        }
    }
%>

<!-- Checkbox hack cho Chế độ Thu gọn Sidebar Rail Mode 68px trên PC (Lưu trạng thái qua Cookie/localStorage) -->
<input type="checkbox" id="crm-sidebar-rail-cb" class="crm-sidebar-rail-cb" <%= sidebarIsRail ? "checked" : "" %> hidden>
<script>
(function() {
    var railCb = document.getElementById('crm-sidebar-rail-cb');
    if (!railCb) return;
    try {
        var savedRail = localStorage.getItem('crm_sidebar_rail');
        if (savedRail === 'true' && !railCb.checked) {
            railCb.checked = true;
            document.cookie = "crm_sidebar_rail=true;path=/;max-age=31536000;SameSite=Lax";
        } else if (savedRail === 'false' && railCb.checked) {
            railCb.checked = false;
            document.cookie = "crm_sidebar_rail=false;path=/;max-age=31536000;SameSite=Lax";
        }
    } catch (e) {}
    railCb.addEventListener('change', function() {
        var val = this.checked ? 'true' : 'false';
        try {
            localStorage.setItem('crm_sidebar_rail', val);
        } catch (e) {}
        document.cookie = "crm_sidebar_rail=" + val + ";path=/;max-age=31536000;SameSite=Lax";
    });
})();
</script>

<!-- Sidebar chính của hệ thống CRM -->
<aside class="sidebar" id="crmSidebar" aria-label="Sidebar navigation">
    <div class="sidebar__header">
        <div class="sidebar__brand" aria-label="CRM brand">
            <span class="sidebar__brand-mark">CRM</span>
            <span class="sidebar__brand-text">CRM System</span>
        </div>
        <!-- Nút thu gọn dạng thanh icon mỏng (Rail mode 68px) trên PC Desktop -->
        <label for="crm-sidebar-rail-cb" class="sidebar__rail-toggle" title="Thu gọn / Mở rộng thanh điều hướng" aria-label="Thu gọn / Mở rộng sidebar">
            <svg class="rail-chevron" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
        </label>
    </div>

    <!-- Khu vực danh sách điều hướng chức năng -->
    <nav class="sidebar__nav" aria-label="Điều hướng chính">
        <div class="sidebar__section">
            <span class="sidebar__section-label">Menu chức năng</span>

            <!-- Empty State khi không có mục menu nào thuộc quyền (AC 1) -->
            <div class="sidebar__empty <%= sidebarHasServerMenu ? "is-hidden" : "is-visible" %>" id="crmSidebarEmpty" role="status">
                <span class="sidebar__empty-icon" aria-hidden="true">&#8709;</span>
                <span class="sidebar__empty-text">Không có menu khả dụng cho tài khoản này</span>
            </div>

            <!-- Menu do server render; không sử dụng JavaScript -->
            <ul class="sidebar__menu <%= sidebarHasServerMenu ? "is-visible" : "is-hidden" %>" id="crmSidebarMenuList">
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
                                <input type="hidden" name="csrfToken" value="<%= com.crm.controller.ServerForms.csrf(request) %>">
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

    <!-- Footer Sidebar hiển thị Tên, Vai trò và Nhóm kinh doanh -->
    <%
    Object sidebarNameObj = request.getAttribute("currentUserDisplayName");
    if (sidebarNameObj == null || String.valueOf(sidebarNameObj).isBlank()) {
        sidebarNameObj = session == null ? null : session.getAttribute("displayName");
    }
    String sidebarDisplayName = (sidebarNameObj == null || String.valueOf(sidebarNameObj).isBlank()) ? "Tài khoản" : String.valueOf(sidebarNameObj);
    %>
    <div class="sidebar__footer">
        <div class="sidebar__user-card" id="crmSidebarUserCard">
            <div class="sidebar__user-avatar" id="crmSidebarAvatarContainer" style="overflow: hidden;">
                <img src="${pageContext.request.contextPath}/profile/avatar/thumbnail"
                     alt="Avatar"
                     class="sidebar__user-avatar-img"
                     onload="this.style.display='block'; var s=this.nextElementSibling; if(s) s.style.display='none';"
                     onerror="this.style.display='none'; var s=this.nextElementSibling; if(s) s.style.display='grid';"
                     style="display: none; width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">
                <span id="crmSidebarAvatarText"><%= sidebarEscapeHtml(sidebarDisplayName.substring(0, 1).toUpperCase(java.util.Locale.ROOT)) %></span>
            </div>
            <div class="sidebar__user-info">
                <div class="sidebar__user-name" id="crmSidebarUserName"><%= sidebarEscapeHtml(sidebarDisplayName) %></div>
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

<!-- Bottom Navigation Bar cố định ở đáy màn hình trên Mobile (< 768px, 4 icon chính) -->
<!-- Mặc định style="display: none;" để tuyệt đối không hiển thị trên Desktop kể cả khi chưa nạp CSS -->
<nav class="crm-bottom-nav" aria-label="Điều hướng nhanh mobile" style="display: none;">
    <a href="${pageContext.request.contextPath}/dashboard" class="crm-bottom-nav__item <%= sidebarUrlActive("/dashboard", sidebarCurrentUri, sidebarContextPath) ? "crm-bottom-nav__item--active" : "" %>">
        <svg class="crm-bottom-nav__icon" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="width: 22px; height: 22px; max-width: 22px; max-height: 22px;">
            <rect x="3" y="3" width="7" height="7" rx="1"/>
            <rect x="14" y="3" width="7" height="7" rx="1"/>
            <rect x="3" y="14" width="7" height="7" rx="1"/>
            <rect x="14" y="14" width="7" height="7" rx="1"/>
        </svg>
        <span class="crm-bottom-nav__label">Tổng quan</span>
    </a>
    <a href="${pageContext.request.contextPath}/customers" class="crm-bottom-nav__item <%= (sidebarUrlActive("/customers", sidebarCurrentUri, sidebarContextPath) || sidebarUrlActive("/users", sidebarCurrentUri, sidebarContextPath)) ? "crm-bottom-nav__item--active" : "" %>">
        <svg class="crm-bottom-nav__icon" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="width: 22px; height: 22px; max-width: 22px; max-height: 22px;">
            <circle cx="9" cy="8" r="3"/>
            <path d="M3 21v-3a6 6 0 0 1 12 0v3m1-16a3 3 0 0 1 0 6m2 4a5 5 0 0 1 3 5"/>
        </svg>
        <span class="crm-bottom-nav__label">Khách hàng</span>
    </a>
    <a href="${pageContext.request.contextPath}/opportunities" class="crm-bottom-nav__item <%= (sidebarUrlActive("/opportunities", sidebarCurrentUri, sidebarContextPath) || sidebarUrlActive("/pipeline", sidebarCurrentUri, sidebarContextPath)) ? "crm-bottom-nav__item--active" : "" %>">
        <svg class="crm-bottom-nav__icon" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="width: 22px; height: 22px; max-width: 22px; max-height: 22px;">
            <path d="m12 3 9 5v8l-9 5-9-5V8zm0 9 9-4m-9 4L3 8m9 4v9"/>
        </svg>
        <span class="crm-bottom-nav__label">Cơ hội</span>
    </a>
    <a href="${pageContext.request.contextPath}/activities" class="crm-bottom-nav__item <%= (sidebarUrlActive("/activities", sidebarCurrentUri, sidebarContextPath) || sidebarUrlActive("/dashboard#activities", sidebarCurrentUri, sidebarContextPath)) ? "crm-bottom-nav__item--active" : "" %>">
        <svg class="crm-bottom-nav__icon" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="width: 22px; height: 22px; max-width: 22px; max-height: 22px;">
            <rect x="3" y="5" width="18" height="16" rx="2"/>
            <path d="M7 3v4m10-4v4M3 11h18"/>
        </svg>
        <span class="crm-bottom-nav__label">Lịch</span>
    </a>
</nav>
