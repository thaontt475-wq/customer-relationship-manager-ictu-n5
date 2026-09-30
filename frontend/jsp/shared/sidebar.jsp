<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List, com.crm.dto.permissions.MenuItem" %>
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

    private String sidebarResolveUrl(String url, String contextPath) {
        if (url == null || url.trim().isEmpty()) return null;
        String normalized = url.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")
                || normalized.startsWith("//") || normalized.startsWith("#")) {
            return normalized;
        }
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
        <button type="button" class="sidebar__close-btn" id="crmSidebarCloseBtn" aria-label="Đóng menu điều hướng" title="Đóng menu">
            &times;
        </button>
    </div>

    <!-- Khu vực danh sách điều hướng chức năng -->
    <nav class="sidebar__nav" aria-label="Primary navigation">
        <div class="sidebar__section">
            <span class="sidebar__section-label">Menu chức năng</span>

            <!-- Menu server-render từ MenuService là fallback; API CRM-26 sẽ đồng bộ khi khả dụng. -->
            <div class="sidebar__loading" id="crmSidebarLoading" aria-hidden="true"<%= sidebarHasServerMenu ? " style=\"display: none;\"" : "" %>>
                <div class="sidebar__skeleton-item"></div>
                <div class="sidebar__skeleton-item"></div>
                <div class="sidebar__skeleton-item"></div>
                <div class="sidebar__skeleton-item"></div>
                <div class="sidebar__skeleton-item"></div>
            </div>

            <!-- Empty State khi không có mục menu nào thuộc quyền (AC 1) -->
            <div class="sidebar__empty" id="crmSidebarEmpty" role="status" style="display: none;">
                <span class="sidebar__empty-icon" aria-hidden="true">&#8709;</span>
                <span class="sidebar__empty-text">Không có menu khả dụng cho tài khoản này</span>
            </div>

            <!-- Một danh sách duy nhất: render server trước, sau đó API có thể thay nội dung. -->
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
                                    <span class="sidebar__icon sidebar__icon--custom" aria-hidden="true"><%= sidebarEscapeHtml(item.getIcon()) %></span>
                                    <span class="sidebar__text"><%= sidebarEscapeHtml(item.getLabel()) %></span>
                                </button>
                            </form>
                        <% } else if (itemResolvedUrl != null) { %>
                            <a href="<%= sidebarEscapeHtml(itemResolvedUrl) %>" class="sidebar__link<%= selfActive ? " sidebar__link--active" : "" %>">
                                <span class="sidebar__icon sidebar__icon--custom" aria-hidden="true"><%= sidebarEscapeHtml(item.getIcon()) %></span>
                                <span class="sidebar__text"><%= sidebarEscapeHtml(item.getLabel()) %></span>
                            </a>
                        <% } else { %>
                            <div class="sidebar__link sidebar__link--disabled" aria-disabled="true">
                                <span class="sidebar__icon" aria-hidden="true">•</span>
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
                                                <span class="sidebar__icon" aria-hidden="true">•</span>
                                                <span class="sidebar__text"><%= sidebarEscapeHtml(child.getLabel()) %></span>
                                            </a>
                                        <% } else { %>
                                            <div class="sidebar__link sidebar__link--disabled" aria-disabled="true">
                                                <span class="sidebar__icon" aria-hidden="true">•</span>
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
            <div class="sidebar__user-avatar" id="crmSidebarAvatarText">U</div>
            <div class="sidebar__user-info">
                <div class="sidebar__user-name" id="crmSidebarUserName">Đang tải...</div>
                <div class="sidebar__user-sub">
                    <span class="sidebar__role-tag" id="crmSidebarUserRole">Vai trò</span>
                    <span class="sidebar__team-tag" id="crmSidebarUserTeam">Nhóm</span>
                </div>
            </div>
        </div>

        <div class="sidebar__status">
            <span class="sidebar__status-dot"></span>
            <span>Hệ thống trực tuyến</span>
        </div>
    </div>
</aside>

<!-- Script xử lý phân quyền Menu động & Responsive Drawer (CRM-26) -->
<script>
(function () {
    'use strict';

    var contextPath = '${pageContext.request.contextPath}';

    // Bảng định nghĩa icon SVG và đường dẫn route mặc định cho từng mã module
    var MODULE_CONFIGS = {
        'SALES_CONFIG': {
            defaultUrl: '/products',
            iconSvg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>'
        },
        'CUSTOMERS': {
            defaultUrl: '/customers',
            iconSvg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>'
        },
        'LEADS': {
            defaultUrl: '/leads',
            iconSvg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon></svg>'
        },
        'OPPORTUNITIES': {
            defaultUrl: '/pipeline',
            iconSvg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>'
        },
        'ACTIVITIES': {
            defaultUrl: '/activities',
            iconSvg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>'
        },
        'QUOTES_CONTRACTS': {
            defaultUrl: '/quotes',
            iconSvg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>'
        },
        'KPI': {
            defaultUrl: '/kpi',
            iconSvg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="7"></circle><polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"></polyline></svg>'
        },
        'REPORTS': {
            defaultUrl: '/winloss',
            iconSvg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21.21 15.89A10 10 0 1 1 8 2.83"></path><path d="M22 12A10 10 0 0 0 12 2v10z"></path></svg>'
        },
        'AUTOMATION': {
            defaultUrl: '/automation',
            iconSvg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg>'
        },
        'USERS_AUDIT': {
            defaultUrl: '/users',
            iconSvg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>',
            children: [
                { label: 'Quản lý người dùng', url: '/users' },
                { label: 'Phân quyền & Dữ liệu', url: '/permissions' },
                { label: 'Nhật ký kiểm toán', url: '/audit' }
            ]
        }
    };

    // DOM Elements
    var sidebarEl = document.getElementById('crmSidebar');
    var backdropEl = document.getElementById('crmSidebarBackdrop');
    var closeBtnEl = document.getElementById('crmSidebarCloseBtn');
    var toggleBtnEl = document.getElementById('crmHeaderToggleBtn');
    var loadingEl = document.getElementById('crmSidebarLoading');
    var emptyEl = document.getElementById('crmSidebarEmpty');
    var menuListEl = document.getElementById('crmSidebarMenuList');
    var hasServerRenderedMenu = menuListEl && menuListEl.children.length > 0;
    var sessionRoles = [];
    var sessionProfileLoaded = false;

    // User Profile Elements - Sidebar & Header (AC 2)
    var sidebarAvatar = document.getElementById('crmSidebarAvatarText');
    var sidebarName = document.getElementById('crmSidebarUserName');
    var sidebarRole = document.getElementById('crmSidebarUserRole');
    var sidebarTeam = document.getElementById('crmSidebarUserTeam');

    var headerAvatar = document.getElementById('crmHeaderAvatarText');
    var headerName = document.getElementById('crmHeaderUserName');
    var headerRole = document.getElementById('crmHeaderUserRole');
    var headerTeam = document.getElementById('crmHeaderUserTeam');

    // 1. Tải và hiển thị thông tin User Profile, Role, Team từ Session API (AC 2)
    async function loadUserProfile() {
        try {
            var response = await fetch(contextPath + '/api/auth/session', {
                headers: { 'Accept': 'application/json' }
            });

            if (!response.ok) return;
            sessionProfileLoaded = true;

            var resBody = await response.json();
            var data = resBody && resBody.data ? resBody.data : {};
            var user = data.currentUser || {};
            var roles = Array.isArray(data.roles) ? data.roles : [];
            sessionRoles = roles.slice();

            // Họ tên hiển thị
            var displayName = user.fullName || user.username || 'Tài khoản CRM';
            var firstChar = displayName.trim().charAt(0).toUpperCase() || 'U';

            if (sidebarAvatar) sidebarAvatar.textContent = firstChar;
            if (headerAvatar) headerAvatar.textContent = firstChar;
            if (sidebarName) sidebarName.textContent = displayName;
            if (headerName) headerName.textContent = displayName;

            // Vai trò hiển thị
            var roleText = roles.length > 0 ? roles.join(', ') : (user.role || 'Người dùng');
            if (sidebarRole) sidebarRole.textContent = roleText;
            if (headerRole) {
                headerRole.textContent = roleText;
                headerRole.style.display = 'inline-block';
            }

            // Nhóm kinh doanh hiển thị
            var teamText = user.teamName || user.team;
            if (teamText && teamText.trim() !== '') {
                if (sidebarTeam) sidebarTeam.textContent = teamText;
                if (headerTeam) {
                    headerTeam.textContent = teamText;
                    headerTeam.style.display = 'inline-block';
                }
            } else {
                if (sidebarTeam) sidebarTeam.textContent = 'Chưa phân nhóm';
                if (headerTeam) {
                    headerTeam.textContent = 'Chưa phân nhóm';
                    headerTeam.style.display = 'inline-block';
                }
            }

        } catch (err) {
            console.error('Không thể nạp thông tin phiên làm việc:', err);
        }
    }

    // 2. Tải cấu trúc Menu theo quyền người dùng từ Menu API (AC 1)
    async function loadNavigationMenu() {
        loadingEl.style.display = hasServerRenderedMenu ? 'none' : 'flex';
        emptyEl.style.display = 'none';
        menuListEl.style.display = hasServerRenderedMenu ? 'flex' : 'none';

        try {
            var response = await fetch(contextPath + '/api/navigation/menu', {
                headers: { 'Accept': 'application/json' }
            });

            loadingEl.style.display = 'none';

            if (!response.ok) {
                if (!hasServerRenderedMenu) emptyEl.style.display = 'flex';
                return;
            }

            var resBody = await response.json();
            var menuData = resBody && resBody.data ? resBody.data : {};
            var menuItems = Array.isArray(menuData.menuItems) ? menuData.menuItems : [];

            // Không thay menu server đã được permission-filter nếu session API chưa xác nhận role.
            if (hasServerRenderedMenu && !sessionProfileLoaded) {
                return;
            }
            menuItems = mergeDevelopNavigation(menuItems);

            // AC 1: Không có quyền thì không hiển thị
            if (menuItems.length === 0) {
                if (!hasServerRenderedMenu) emptyEl.style.display = 'flex';
                return;
            }

            renderMenu(menuItems);
            menuListEl.style.display = 'flex';

        } catch (err) {
            console.error('Không thể nạp menu điều hướng:', err);
            loadingEl.style.display = 'none';
            if (!hasServerRenderedMenu) emptyEl.style.display = 'flex';
        }
    }

    function normalizeMenuUrl(url) {
        if (!url) return '';
        var normalized = String(url).trim();
        if (contextPath && normalized.indexOf(contextPath + '/') === 0) {
            normalized = normalized.substring(contextPath.length);
        }
        if (normalized.length > 1 && normalized.endsWith('/')) {
            normalized = normalized.substring(0, normalized.length - 1);
        }
        return normalized;
    }

    function menuContainsUrl(items, targetUrl) {
        var target = normalizeMenuUrl(targetUrl);
        return items.some(function (item) {
            var config = MODULE_CONFIGS[item.code || ''] || {};
            var itemUrl = normalizeMenuUrl(item.url || config.defaultUrl);
            if (itemUrl && itemUrl === target) return true;

            var children = (Array.isArray(item.children) && item.children.length > 0)
                ? item.children
                : (config.children || []);
            return children.some(function (child) {
                return normalizeMenuUrl(child.url) === target;
            });
        });
    }

    // Bổ sung các route mới từ develop nhưng khử trùng với module/children CRM-26.
    function mergeDevelopNavigation(apiItems) {
        var merged = apiItems.slice();
        var normalizedRoles = sessionRoles.map(function (role) {
            return String(role || '').trim().toLowerCase();
        });
        var canAdministerPermissions = normalizedRoles.includes('admin') || normalizedRoles.includes('director');

        var leadingItems = [
            { code: 'DASHBOARD', label: 'Tổng quan', url: '/dashboard', icon: '⌂', children: [] }
        ];
        var trailingItems = [];
        if (canAdministerPermissions) {
            trailingItems.push({ code: 'USERS', label: 'Quản lý người dùng', url: '/users', icon: '👥', children: [] });
            trailingItems.push({ code: 'PERMISSIONS', label: 'Phân quyền & vai trò', url: '/permissions', icon: '⚿', children: [] });
        }
        trailingItems.push({ code: 'CHANGE_PASSWORD', label: 'Đổi mật khẩu', url: '/change-password', icon: '●', children: [] });
        trailingItems.push({ code: 'LOGOUT', label: 'Đăng xuất', url: '/api/auth/logout', icon: '↪', children: [] });

        leadingItems.reverse().forEach(function (item) {
            if (!menuContainsUrl(merged, item.url)) merged.unshift(item);
        });
        trailingItems.forEach(function (item) {
            if (!menuContainsUrl(merged, item.url)) merged.push(item);
        });
        return merged;
    }

    // 3. Render danh sách các mục Menu động vào DOM
    function renderMenu(items) {
        menuListEl.innerHTML = '';
        var currentPath = window.location.pathname;

        items.forEach(function (item) {
            var code = item.code || '';
            var config = MODULE_CONFIGS[code] || {};
            var label = item.label || code;

            // Xác định URL điều hướng
            var relativeUrl = item.url || config.defaultUrl;
            var fullUrl = relativeUrl ? (relativeUrl.startsWith('/') ? (contextPath + relativeUrl) : relativeUrl) : null;

            // Xác định Icon
            var iconSvg = item.icon || config.iconSvg || '<span class="sidebar__icon" aria-hidden="true">•</span>';

            // Xác định Submenu (nếu có từ backend hoặc config)
            var children = (Array.isArray(item.children) && item.children.length > 0) ? item.children : (config.children || []);
            var hasChildren = children.length > 0;

            // Kiểm tra trạng thái Active
            var isSelfActive = Boolean(fullUrl && (currentPath === fullUrl || currentPath === (fullUrl + '/')));
            var isParentActive = false;
            if (hasChildren) {
                isParentActive = children.some(function (child) {
                    var cUrl = child.url ? (child.url.startsWith('/') ? (contextPath + child.url) : child.url) : '';
                    return cUrl && (currentPath === cUrl || currentPath.startsWith(cUrl + '/'));
                });
            }
            var isItemActive = isSelfActive || isParentActive;

            var li = document.createElement('li');
            li.className = 'sidebar__item' + (isItemActive ? ' sidebar__item--active' : '') + (hasChildren ? ' sidebar__item--has-children' : '');

            if (code === 'LOGOUT' && fullUrl) {
                var logoutForm = document.createElement('form');
                logoutForm.className = 'sidebar__logout-form';
                logoutForm.method = 'post';
                logoutForm.action = fullUrl;
                logoutForm.innerHTML = '<input type="hidden" name="redirectToLogin" value="true">' +
                    '<button type="submit" class="sidebar__link sidebar__logout-button">' +
                        '<span class="sidebar__icon sidebar__icon--custom" aria-hidden="true">' + iconSvg + '</span>' +
                        '<span class="sidebar__text">' + escapeHtml(label) + '</span>' +
                    '</button>';
                li.appendChild(logoutForm);
            } else if (fullUrl) {
                var a = document.createElement('a');
                a.href = fullUrl;
                a.className = 'sidebar__link' + (isSelfActive ? ' sidebar__link--active' : '');
                a.innerHTML = '<span class="sidebar__icon sidebar__icon--custom" aria-hidden="true">' + iconSvg + '</span>' +
                              '<span class="sidebar__text">' + escapeHtml(label) + '</span>';

                // Tự động đóng Mobile Drawer khi click link trên mobile
                a.addEventListener('click', function () {
                    if (window.innerWidth <= 768) {
                        closeMobileDrawer();
                    }
                });

                li.appendChild(a);
            } else {
                var div = document.createElement('div');
                div.className = 'sidebar__link sidebar__link--disabled';
                div.setAttribute('aria-disabled', 'true');
                div.innerHTML = '<span class="sidebar__icon sidebar__icon--custom" aria-hidden="true">' + iconSvg + '</span>' +
                                '<span class="sidebar__text">' + escapeHtml(label) + '</span>' +
                                (hasChildren ? '' : '<span class="sidebar__badge sidebar__badge--disabled">Chưa có route</span>');
                li.appendChild(div);
            }

            // Render Submenu nếu có
            if (hasChildren) {
                var subUl = document.createElement('ul');
                subUl.className = 'sidebar__submenu';

                children.forEach(function (child) {
                    var subLabel = child.label || child.name || '';
                    var subRelative = child.url || '';
                    var subFull = subRelative ? (subRelative.startsWith('/') ? (contextPath + subRelative) : subRelative) : null;
                    var isSubActive = Boolean(subFull && (currentPath === subFull || currentPath.startsWith(subFull + '/')));

                    var subLi = document.createElement('li');
                    subLi.className = 'sidebar__subitem';

                    if (subFull) {
                        var subA = document.createElement('a');
                        subA.href = subFull;
                        subA.className = 'sidebar__link' + (isSubActive ? ' sidebar__link--active' : '');
                        subA.innerHTML = '<span class="sidebar__icon" aria-hidden="true">•</span>' +
                                         '<span class="sidebar__text">' + escapeHtml(subLabel) + '</span>';

                        subA.addEventListener('click', function () {
                            if (window.innerWidth <= 768) {
                                closeMobileDrawer();
                            }
                        });

                        subLi.appendChild(subA);
                    }
                    subUl.appendChild(subLi);
                });

                li.appendChild(subUl);
            }

            menuListEl.appendChild(li);
        });
    }

    // 4. Xử lý Drawer Offcanvas cho Mobile / Tablet (AC 3)
    function openMobileDrawer() {
        sidebarEl.classList.add('sidebar--open');
        backdropEl.classList.add('is-active');
        document.body.style.overflow = 'hidden';
    }

    function closeMobileDrawer() {
        sidebarEl.classList.remove('sidebar--open');
        backdropEl.classList.remove('is-active');
        document.body.style.overflow = '';
    }

    if (toggleBtnEl) {
        toggleBtnEl.addEventListener('click', function (e) {
            e.preventDefault();
            if (sidebarEl.classList.contains('sidebar--open')) {
                closeMobileDrawer();
            } else {
                openMobileDrawer();
            }
        });
    }

    if (closeBtnEl) {
        closeBtnEl.addEventListener('click', closeMobileDrawer);
    }

    if (backdropEl) {
        backdropEl.addEventListener('click', closeMobileDrawer);
    }

    // Đóng drawer khi nhấn phím Escape
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && sidebarEl.classList.contains('sidebar--open')) {
            closeMobileDrawer();
        }
    });

    // Tự động đóng drawer khi màn hình resize lớn hơn 768px
    window.addEventListener('resize', function () {
        if (window.innerWidth > 768 && sidebarEl.classList.contains('sidebar--open')) {
            closeMobileDrawer();
        }
    });

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    // Khởi chạy tuần tự để role từ session được dùng khi ghép menu develop.
    (async function initializeSidebar() {
        await loadUserProfile();
        await loadNavigationMenu();
    })();
})();
</script>
