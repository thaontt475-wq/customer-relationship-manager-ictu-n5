<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.crm.service.scope.ScopeEntityType" %>
<%@ page import="com.crm.service.scope.ScopeRecord" %>
<%@ page import="com.crm.service.scope.DataScopeService.UserContextData" %>
<%@ page import="com.crm.model.User" %>
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
    ScopeEntityType currentType = (ScopeEntityType) request.getAttribute("entityType");
    if (currentType == null) {
        currentType = ScopeEntityType.CUSTOMERS;
    }

    List<?> rawItems = (List<?>) request.getAttribute("items");
    List<ScopeRecord> items = null;
    if (rawItems != null) {
        items = (List<ScopeRecord>) rawItems;
    }

    String searchKeyword = (String) request.getAttribute("search");
    if (searchKeyword == null) searchKeyword = "";

    String accessDeniedError = (String) request.getAttribute("accessDeniedError");
    String notFoundError = (String) request.getAttribute("notFoundError");
    ScopeRecord viewRecord = (ScopeRecord) request.getAttribute("viewRecord");

    UserContextData userCtx = (UserContextData) request.getAttribute("userContext");
    User currentUser = (User) request.getAttribute("currentUser");

    String dataScope = "SELF";
    if (userCtx != null && userCtx.context() != null && userCtx.context().dataScope() != null) {
        dataScope = userCtx.context().dataScope().trim().toUpperCase();
    } else if (currentUser != null && currentUser.getDataScope() != null) {
        dataScope = currentUser.getDataScope().trim().toUpperCase();
    }

    String scopeTitle;
    String scopePillClass;
    String scopeDescription;

    switch (dataScope) {
        case "ALL" -> {
            scopeTitle = "Tất cả (ALL)";
            scopePillClass = "scope-pill-all";
            scopeDescription = "Bạn có quyền Giám đốc kinh doanh: xem toàn bộ dữ liệu trên toàn hệ thống.";
        }
        case "TEAM" -> {
            scopeTitle = "Của nhóm tôi (TEAM)";
            scopePillClass = "scope-pill-team";
            scopeDescription = "Bạn có quyền Trưởng nhóm: xem toàn bộ dữ liệu của các thành viên trong nhóm kinh doanh.";
        }
        default -> {
            scopeTitle = "Của tôi (SELF)";
            scopePillClass = "scope-pill-self";
            scopeDescription = "Bạn có quyền Nhân viên: chỉ xem và thao tác trên dữ liệu do chính bạn phụ trách.";
        }
    }

    String currentUrlSlug = currentType.slug();
    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= escapeHtml(currentType.displayName()) %> - Phân quyền dữ liệu - CRM</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Phạm vi Dữ liệu Sở hữu (S1-05 / CRM-25) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/scope/scope.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar điều hướng của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Danh mục theo phạm vi dữ liệu -->
        <main class="scope-page" id="scopeApp" role="main">
            <div class="scope-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="scope-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <span>Nghiệp vụ bán hàng</span>
                    <span class="separator">/</span>
                    <span class="active"><%= escapeHtml(currentType.displayName()) %></span>
                </nav>

                <!-- Header màn hình & thông tin phân quyền phạm vi -->
                <header class="scope-header">
                    <div class="scope-header-info">
                        <h1>Quản lý <%= escapeHtml(currentType.displayName()) %></h1>
                        <p><%= escapeHtml(scopeDescription) %></p>
                    </div>

                    <div class="scope-badges-group">
                        <span class="scope-badge-feature" title="User Story S1-05">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                            </svg>
                            S1-05 / Phân quyền dữ liệu
                        </span>

                        <div class="scope-pill <%= scopePillClass %>" title="Phạm vi dữ liệu đang áp dụng cho tài khoản">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <circle cx="12" cy="12" r="10"></circle>
                                <polyline points="12 6 12 12 16 14"></polyline>
                            </svg>
                            <span>Phạm vi: <strong><%= escapeHtml(scopeTitle) %></strong></span>
                        </div>
                    </div>
                </header>

                <!-- Khu vực hiển thị thông báo lỗi / quyền truy cập -->
                <% if (accessDeniedError != null && !accessDeniedError.isBlank()) { %>
                    <div class="scope-alert scope-alert-danger" role="alert" id="accessDeniedAlert">
                        <svg class="scope-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                            <line x1="12" y1="8" x2="12" y2="12"/>
                            <line x1="12" y1="16" x2="12.01" y2="16"/>
                        </svg>
                        <div>
                            <div class="scope-alert-title">Từ chối truy cập dữ liệu (403 Forbidden)</div>
                            <div><%= escapeHtml(accessDeniedError) %></div>
                            <div style="font-size: 13px; margin-top: 4px; opacity: 0.9;">
                                Tài khoản của bạn đang có phạm vi <strong><%= escapeHtml(scopeTitle) %></strong> và không được phép xem bản ghi thuộc nhân sự hoặc nhóm kinh doanh khác.
                            </div>
                        </div>
                    </div>
                <% } %>

                <% if (notFoundError != null && !notFoundError.isBlank()) { %>
                    <div class="scope-alert scope-alert-warning" role="alert">
                        <svg class="scope-alert-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div>
                            <div style="font-weight: 600;"><%= escapeHtml(notFoundError) %></div>
                        </div>
                    </div>
                <% } %>

                <!-- Thanh Tabs chuyển đổi giữa 4 loại dữ liệu áp dụng phạm vi -->
                <nav class="scope-tabs" aria-label="Các loại dữ liệu theo phạm vi">
                    <a href="${pageContext.request.contextPath}/customers"
                       class="scope-tab <%= currentType == ScopeEntityType.CUSTOMERS ? "active" : "" %>"
                       id="tabCustomers">
                        <span>Khách hàng</span>
                        <% if (currentType == ScopeEntityType.CUSTOMERS && items != null) { %>
                            <span class="scope-tab-count"><%= items.size() %></span>
                        <% } %>
                    </a>

                    <a href="${pageContext.request.contextPath}/opportunities"
                       class="scope-tab <%= currentType == ScopeEntityType.OPPORTUNITIES ? "active" : "" %>"
                       id="tabOpportunities">
                        <span>Cơ hội</span>
                        <% if (currentType == ScopeEntityType.OPPORTUNITIES && items != null) { %>
                            <span class="scope-tab-count"><%= items.size() %></span>
                        <% } %>
                    </a>

                    <a href="${pageContext.request.contextPath}/activities"
                       class="scope-tab <%= currentType == ScopeEntityType.ACTIVITIES ? "active" : "" %>"
                       id="tabActivities">
                        <span>Hoạt động</span>
                        <% if (currentType == ScopeEntityType.ACTIVITIES && items != null) { %>
                            <span class="scope-tab-count"><%= items.size() %></span>
                        <% } %>
                    </a>

                    <a href="${pageContext.request.contextPath}/quotes"
                       class="scope-tab <%= currentType == ScopeEntityType.QUOTES ? "active" : "" %>"
                       id="tabQuotes">
                        <span>Báo giá</span>
                        <% if (currentType == ScopeEntityType.QUOTES && items != null) { %>
                            <span class="scope-tab-count"><%= items.size() %></span>
                        <% } %>
                    </a>
                </nav>

                <!-- Thẻ danh sách dữ liệu -->
                <section class="scope-card" aria-label="Bảng dữ liệu">
                    <!-- Toolbar: Tìm kiếm & Xuất Excel -->
                    <div class="scope-toolbar">
                        <form class="scope-search-form" method="get" action="${pageContext.request.contextPath}/<%= currentUrlSlug %>">
                            <div class="scope-search-input-wrap">
                                <svg class="scope-search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                                <input type="text"
                                       name="search"
                                       class="scope-search-input"
                                       placeholder="Tìm kiếm <%= escapeHtml(currentType.displayName().toLowerCase()) %>..."
                                       value="<%= escapeHtml(searchKeyword) %>"
                                       aria-label="Tìm kiếm">
                            </div>
                            <button type="submit" class="scope-btn scope-btn-primary" id="btnSearch">
                                Tìm kiếm
                            </button>
                            <% if (!searchKeyword.isEmpty()) { %>
                                <a href="${pageContext.request.contextPath}/<%= currentUrlSlug %>" class="scope-btn scope-btn-secondary" title="Xóa bộ lọc tìm kiếm">
                                    Xóa
                                </a>
                            <% } %>
                        </form>

                        <div class="scope-actions-group">
                            <!-- Nút Xuất Excel (tuân thủ phạm vi dữ liệu) -->
                            <a href="${pageContext.request.contextPath}/<%= currentUrlSlug %>?action=export<%= !searchKeyword.isEmpty() ? "&search=" + java.net.URLEncoder.encode(searchKeyword, "UTF-8") : "" %>"
                               class="scope-btn scope-btn-success"
                               id="btnExportExcel"
                               title="Xuất danh sách ra tệp Excel (CSV UTF-8)">
                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                    <polyline points="7 10 12 15 17 10"></polyline>
                                    <line x1="12" y1="15" x2="12" y2="3"></line>
                                </svg>
                                Xuất Excel
                            </a>

                            <!-- Nút Làm mới -->
                            <a href="${pageContext.request.contextPath}/<%= currentUrlSlug %>"
                               class="scope-btn scope-btn-secondary"
                               title="Tải lại danh sách">
                                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <polyline points="23 4 23 10 17 10"></polyline>
                                    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                                </svg>
                                Làm mới
                            </a>
                        </div>
                    </div>

                    <!-- Bảng dữ liệu danh sách bản ghi -->
                    <div class="scope-table-wrap">
                        <table class="scope-table" aria-label="Danh sách <%= escapeHtml(currentType.displayName()) %>">
                            <thead>
                                <tr>
                                    <th style="width: 80px;">Mã</th>
                                    <th><%= currentType == ScopeEntityType.QUOTES ? "Số hiệu báo giá" : "Tên / Tiêu đề" %></th>
                                    <th>Người phụ trách</th>
                                    <th>Nhóm kinh doanh</th>
                                    <th>Thời gian tạo</th>
                                    <th style="text-align: right; width: 120px;">Hành động</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (items == null || items.isEmpty()) { %>
                                    <tr>
                                        <td colspan="6">
                                            <div class="scope-empty-state">
                                                <svg class="scope-empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                                    <circle cx="12" cy="12" r="10"></circle>
                                                    <line x1="12" y1="8" x2="12" y2="12"></line>
                                                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                                                </svg>
                                                <div class="scope-empty-title">Không tìm thấy bản ghi nào</div>
                                                <p>Không có dữ liệu <%= escapeHtml(currentType.displayName().toLowerCase()) %> phù hợp trong phạm vi được phân quyền của bạn.</p>
                                            </div>
                                        </td>
                                    </tr>
                                <% } else { %>
                                    <% for (ScopeRecord item : items) {
                                        String owner = item.ownerName() != null && !item.ownerName().isBlank() ? item.ownerName() : "Người dùng #" + item.ownerUserId();
                                        String team = item.ownerTeamName() != null && !item.ownerTeamName().isBlank() ? item.ownerTeamName() : "Chưa phân nhóm";
                                        String date = item.createdAt() != null ? item.createdAt() : "—";
                                        String initial = owner.substring(0, 1).toUpperCase();
                                    %>
                                        <tr id="row_<%= item.id() %>">
                                            <td>
                                                <span class="scope-id-badge">#<%= item.id() %></span>
                                            </td>
                                            <td class="scope-label-cell">
                                                <%= escapeHtml(item.label()) %>
                                            </td>
                                            <td>
                                                <div class="scope-user-badge">
                                                    <div class="scope-user-avatar" aria-hidden="true"><%= escapeHtml(initial) %></div>
                                                    <span><%= escapeHtml(owner) %></span>
                                                </div>
                                            </td>
                                            <td>
                                                <span class="scope-team-tag"><%= escapeHtml(team) %></span>
                                            </td>
                                            <td class="scope-date-cell">
                                                <%= escapeHtml(date) %>
                                            </td>
                                            <td style="text-align: right;">
                                                <a href="${pageContext.request.contextPath}/<%= currentUrlSlug %>?viewId=<%= item.id() %><%= !searchKeyword.isEmpty() ? "&search=" + java.net.URLEncoder.encode(searchKeyword, "UTF-8") : "" %>"
                                                   class="scope-btn scope-btn-secondary"
                                                   style="padding: 4px 10px; font-size: 12px;"
                                                   title="Xem chi tiết bản ghi">
                                                    Xem chi tiết
                                                </a>
                                            </td>
                                        </tr>
                                    <% } %>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </section>

                <!-- Modal / Khung hiển thị chi tiết bản ghi (khi có viewRecord) -->
                <% if (viewRecord != null) {
                    String vOwner = viewRecord.ownerName() != null && !viewRecord.ownerName().isBlank() ? viewRecord.ownerName() : "ID #" + viewRecord.ownerUserId();
                    String vTeam = viewRecord.ownerTeamName() != null && !viewRecord.ownerTeamName().isBlank() ? viewRecord.ownerTeamName() : "Chưa phân nhóm";
                    String vDate = viewRecord.createdAt() != null && !viewRecord.createdAt().isBlank() ? viewRecord.createdAt() : "Chưa cập nhật";
                %>
                    <div class="scope-modal-backdrop" id="viewDetailModal" role="dialog" aria-modal="true" aria-labelledby="modalTitle">
                        <div class="scope-modal">
                            <div class="scope-modal-header">
                                <h2 class="scope-modal-title" id="modalTitle">Chi tiết <%= escapeHtml(currentType.displayName()) %> #<%= viewRecord.id() %></h2>
                                <a href="${pageContext.request.contextPath}/<%= currentUrlSlug %><%= !searchKeyword.isEmpty() ? "?search=" + java.net.URLEncoder.encode(searchKeyword, "UTF-8") : "" %>"
                                   class="scope-modal-close"
                                   aria-label="Đóng">&times;</a>
                            </div>

                            <div class="scope-modal-body">
                                <div class="scope-detail-row">
                                    <span class="scope-detail-label">Loại bản ghi</span>
                                    <span class="scope-detail-value"><%= escapeHtml(currentType.displayName()) %></span>
                                </div>

                                <div class="scope-detail-row">
                                    <span class="scope-detail-label"><%= currentType == ScopeEntityType.QUOTES ? "Số hiệu báo giá" : "Tên / Tiêu đề" %></span>
                                    <span class="scope-detail-value" style="font-weight: 700; color: var(--scope-primary); font-size: 16px;">
                                        <%= escapeHtml(viewRecord.label()) %>
                                    </span>
                                </div>

                                <div class="scope-detail-row">
                                    <span class="scope-detail-label">Người sở hữu / Phụ trách</span>
                                    <span class="scope-detail-value">
                                        <%= escapeHtml(vOwner) %> (ID: <%= viewRecord.ownerUserId() %>)
                                    </span>
                                </div>

                                <div class="scope-detail-row">
                                    <span class="scope-detail-label">Nhóm kinh doanh</span>
                                    <span class="scope-detail-value"><%= escapeHtml(vTeam) %></span>
                                </div>

                                <div class="scope-detail-row">
                                    <span class="scope-detail-label">Thời gian tạo</span>
                                    <span class="scope-detail-value"><%= escapeHtml(vDate) %></span>
                                </div>

                                <div class="scope-detail-row">
                                    <span class="scope-detail-label">Trạng thái quyền truy cập</span>
                                    <span class="scope-detail-value" style="color: var(--scope-success); font-weight: 600;">
                                        ✓ Bản ghi nằm trong phạm vi dữ liệu cho phép (<%= escapeHtml(scopeTitle) %>)
                                    </span>
                                </div>
                            </div>

                            <div class="scope-modal-footer">
                                <a href="${pageContext.request.contextPath}/<%= currentUrlSlug %><%= !searchKeyword.isEmpty() ? "?search=" + java.net.URLEncoder.encode(searchKeyword, "UTF-8") : "" %>"
                                   class="scope-btn scope-btn-secondary">
                                    Đóng
                                </a>
                            </div>
                        </div>
                    </div>
                <% } %>

            </div>
        </main>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

</body>
</html>
