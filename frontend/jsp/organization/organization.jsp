<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.Collection,java.util.Locale,com.crm.util.SessionKey" %>
<%
    boolean canManageOrganization = false;
    Object organizationRoles = session.getAttribute(SessionKey.ROLES);
    if (organizationRoles instanceof Collection<?>) {
        for (Object role : (Collection<?>) organizationRoles) {
            if (role instanceof String) {
                String normalized = ((String) role).trim().toLowerCase(Locale.ROOT);
                if ("admin".equals(normalized) || "director".equals(normalized)) {
                    canManageOrganization = true;
                    break;
                }
            }
        }
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cơ cấu tổ chức - CRM ICTU</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/organization/organization.css">
</head>
<body class="crm-body">
    <jsp:include page="/jsp/shared/header.jsp" />
    <div class="crm-main-layout">
        <jsp:include page="/jsp/shared/sidebar.jsp" />
        <main class="crm-page org-page" id="orgApp" data-context-path="${pageContext.request.contextPath}" data-can-manage="<%= canManageOrganization %>">
            <div class="crm-page-container org-container">
                <nav class="crm-breadcrumb" aria-label="Đường dẫn">
                    <a href="${pageContext.request.contextPath}/dashboard">Trang chủ</a><span aria-hidden="true">/</span>
                    <span aria-current="page">Cơ cấu tổ chức</span>
                </nav>
                <header class="crm-page-header org-header">
                    <div>
                        <h1 class="crm-page-title">Cơ cấu tổ chức</h1>
                        <p class="crm-page-description">Theo dõi các đơn vị, trưởng đơn vị và thành viên theo từng khu vực.</p>
                    </div>
                    <button type="button" class="crm-btn crm-btn-primary" id="btnOpenCreateUnitModal" <%= canManageOrganization ? "" : "hidden" %>>
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg>Thêm đơn vị
                    </button>
                </header>
                <div id="orgAlertsArea" class="org-alerts" aria-live="polite">
                    <div class="crm-alert crm-alert-danger org-alert" id="globalErrorAlert" hidden role="alert">
                        <div><strong id="globalErrorTitle">Không thể hoàn tất</strong><div id="globalErrorMessage"></div></div>
                        <button type="button" class="org-icon-btn" data-dismiss="globalErrorAlert" aria-label="Đóng thông báo lỗi">&times;</button>
                    </div>
                    <div class="crm-alert crm-alert-success org-alert" id="globalSuccessAlert" hidden role="status">
                        <div><strong id="globalSuccessTitle">Đã lưu thay đổi</strong><div id="globalSuccessMessage"></div></div>
                        <button type="button" class="org-icon-btn" data-dismiss="globalSuccessAlert" aria-label="Đóng thông báo thành công">&times;</button>
                    </div>
                </div>
                <div class="crm-toolbar org-toolbar">
                    <div class="org-search-wrap">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4.5 4.5"/></svg>
                        <input type="search" id="orgSearchInput" class="crm-input" placeholder="Tìm đơn vị hoặc trưởng đơn vị" aria-label="Tìm đơn vị hoặc trưởng đơn vị">
                    </div>
                    <select id="filterRegion" class="crm-select org-filter-select" aria-label="Lọc theo khu vực">
                        <option value="">Tất cả khu vực</option><option value="NORTH">Miền Bắc</option><option value="CENTRAL">Miền Trung</option><option value="SOUTH">Miền Nam</option><option value="NATIONAL">Toàn quốc</option><option value="OVERSEAS">Quốc tế</option>
                    </select>
                    <div class="org-view-switch" role="group" aria-label="Chế độ xem">
                        <button type="button" class="crm-btn crm-btn-secondary active" id="btnViewTree" aria-pressed="true">Sơ đồ cây</button>
                        <button type="button" class="crm-btn crm-btn-secondary" id="btnViewTable" aria-pressed="false">Danh sách</button>
                    </div>
                    <button type="button" class="crm-btn crm-btn-secondary" id="btnReloadUnits">Làm mới</button>
                </div>
                <div class="org-workspace">
                    <section class="crm-card org-card" aria-labelledby="orgCardTitle">
                        <header class="crm-card-header">
                            <div><h2 class="crm-card-title" id="orgCardTitle">Các đơn vị</h2><p class="org-help" id="orgUnitCount" aria-live="polite">Đang tải cơ cấu tổ chức…</p></div>
                            <div class="org-tree-actions">
                                <button type="button" class="org-text-btn" id="btnExpandAll">Mở rộng</button>
                                <button type="button" class="org-text-btn" id="btnCollapseAll">Thu gọn</button>
                            </div>
                        </header>
                        <div class="crm-state crm-state-loading" id="orgLoadingOverlay" hidden role="status">Đang tải cơ cấu tổ chức…</div>
                        <div class="org-tree-view-wrapper" id="orgTreeViewArea">
                            <ul class="org-tree" id="orgTreeRoot" aria-label="Danh sách đơn vị theo phân cấp"></ul>
                        </div>
                        <div class="crm-table-wrap org-table-responsive" id="orgTableViewArea" hidden tabindex="0" role="region" aria-label="Danh sách đơn vị, cuộn ngang để xem đủ thông tin">
                            <table class="crm-table org-table" id="orgTable"><thead><tr><th scope="col">STT</th><th scope="col">Đơn vị</th><th scope="col">Trưởng đơn vị</th><th scope="col">Khu vực</th><th scope="col">Đơn vị cấp trên</th><th scope="col">Thành viên</th><th scope="col">Trạng thái</th><th scope="col">Thao tác</th></tr></thead><tbody id="orgTableBody"></tbody></table>
                        </div>
                    </section>
                    <aside class="crm-card org-detail" aria-label="Thông tin đơn vị">
                        <div class="crm-state" id="orgDetailEmpty">
                            <div class="org-empty-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="7" y="3" width="10" height="6" rx="1.5"/><path d="M12 9v5M5 14h14M5 14v3m14-3v3"/><rect x="2" y="17" width="6" height="4" rx="1"/><rect x="16" y="17" width="6" height="4" rx="1"/></svg></div>
                            <h2 class="crm-state-title">Thông tin đơn vị</h2>
                            <p>Chọn một đơn vị để xem trưởng đơn vị, khu vực và danh sách thành viên.</p>
                        </div>
                        <section id="unitDrawer" hidden aria-labelledby="drawerTitle">
                            <header class="crm-card-header org-detail-header">
                                <div><p class="org-eyebrow">Chi tiết đơn vị</p><h2 class="crm-card-title" id="drawerTitle" tabindex="-1"></h2><p class="org-help" id="drawerSubtitle"></p></div>
                                <button type="button" class="org-icon-btn" id="btnCloseDrawer" aria-label="Bỏ chọn đơn vị">&times;</button>
                            </header>
                            <div class="crm-card-body org-detail-body">
                                <dl class="org-detail-meta"><div><dt>Đơn vị cấp trên</dt><dd id="drawerParent"></dd></div><div><dt>Khu vực</dt><dd id="drawerRegion"></dd></div></dl>
                                <section class="org-leader" aria-labelledby="orgLeaderTitle">
                                    <h3 id="orgLeaderTitle" class="org-section-title">Trưởng đơn vị</h3>
                                    <div class="org-manager-info"><span class="org-avatar" id="drawerManagerAvatar" aria-hidden="true"></span><div class="org-profile-text"><strong id="drawerManagerName"></strong><p class="org-help" id="drawerManagerRole"></p></div></div>
                                </section>
                                <section aria-labelledby="orgMembersTitle">
                                    <h3 class="org-section-title" id="orgMembersTitle">Thành viên <span class="crm-badge" id="drawerMemberCount">0</span></h3>
                                    <ul class="org-members-list" id="drawerMembersList"></ul>
                                </section>
                                <div class="org-detail-actions" id="orgDetailActions" <%= canManageOrganization ? "" : "hidden" %>>
                                    <button type="button" class="crm-btn crm-btn-primary" id="btnEditSelected" data-action="edit">Sửa đơn vị</button>
                                    <button type="button" class="crm-btn crm-btn-secondary" id="btnAddSelectedChild" data-action="add-child">Thêm đơn vị con</button>
                                </div>
                            </div>
                        </section>
                    </aside>
                </div>
            </div>
            <div class="crm-modal-overlay" id="unitModal" hidden role="dialog" aria-modal="true" aria-labelledby="unitModalTitle">
                <div class="crm-modal-card">
                    <header class="crm-modal-header"><h2 class="crm-modal-title" id="unitModalTitle"><span id="unitModalHeading">Thêm đơn vị</span></h2><button type="button" class="crm-modal-close" id="btnCloseUnitModal" aria-label="Đóng biểu mẫu">&times;</button></header>
                    <form id="unitForm" novalidate>
                        <input type="hidden" id="formUnitId" name="id">
                        <div class="crm-modal-body">
                            <div class="crm-alert crm-alert-danger" id="unitFormError" hidden role="alert"></div>
                            <p class="org-help org-form-intro">Thông tin có dấu <span class="org-required">*</span> là bắt buộc.</p>
                            <div class="crm-form-group"><label for="formUnitName" class="crm-label">Tên đơn vị <span class="org-required">*</span></label><input type="text" id="formUnitName" name="name" class="crm-input form-control" placeholder="Nhập tên đơn vị" required aria-describedby="feedbackUnitName"><div class="crm-field-error form-feedback" id="feedbackUnitName"></div></div>
                            <div class="crm-form-group"><label for="formUnitParent" class="crm-label">Đơn vị cấp trên</label><select id="formUnitParent" name="parentId" class="crm-select form-control" aria-describedby="feedbackUnitParent"><option value="">Không có đơn vị cấp trên</option></select><p class="crm-field-help">Để trống nếu đây là đơn vị gốc.</p><div class="crm-field-error form-feedback" id="feedbackUnitParent"></div></div>
                            <div class="crm-form-group"><label for="formUnitManager" class="crm-label">Trưởng đơn vị <span class="org-required">*</span></label><select id="formUnitManager" name="managerId" class="crm-select form-control" required aria-describedby="feedbackUnitManager managerLoadStatus"><option value="">Chọn trưởng đơn vị</option></select><p class="crm-field-help" id="managerLoadStatus">Chọn nhân sự đang hoạt động. Mỗi đơn vị có một trưởng đơn vị.</p><div class="crm-field-error form-feedback" id="feedbackUnitManager"></div></div>
                            <div class="crm-form-group"><label for="formUnitRegion" class="crm-label">Khu vực <span class="org-required">*</span></label><select id="formUnitRegion" name="region" class="crm-select form-control" required aria-describedby="feedbackUnitRegion"><option value="NORTH">Miền Bắc</option><option value="CENTRAL">Miền Trung</option><option value="SOUTH">Miền Nam</option><option value="NATIONAL">Toàn quốc</option><option value="OVERSEAS">Quốc tế</option></select><div class="crm-field-error form-feedback" id="feedbackUnitRegion"></div></div>
                        </div>
                        <footer class="crm-modal-footer"><button type="button" class="crm-btn crm-btn-secondary" id="btnCancelUnitModal">Hủy</button><button type="submit" class="crm-btn crm-btn-primary" id="btnSaveUnit">Lưu</button></footer>
                    </form>
                </div>
            </div>
        </main>
    </div>
    <jsp:include page="/jsp/shared/footer.jsp" />
    <script src="${pageContext.request.contextPath}/js/organization/organization.js" defer></script>
</body>
</html>
