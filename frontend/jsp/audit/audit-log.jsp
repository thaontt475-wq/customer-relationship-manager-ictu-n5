<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nhật ký thay đổi - CRM ICTU</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/audit/audit.css">
</head>
<body class="crm-body">
    <jsp:include page="/jsp/shared/header.jsp" />
    <div class="crm-main-layout">
        <jsp:include page="/jsp/shared/sidebar.jsp" />
        <main class="audit-page crm-page" id="auditApp" role="main" aria-labelledby="auditPageTitle" data-context-path="${pageContext.request.contextPath}">
            <div class="audit-container crm-page-container">
                <nav class="audit-breadcrumb crm-breadcrumb" aria-label="Đường dẫn trang">
                    <a href="${pageContext.request.contextPath}/">CRM</a><span aria-hidden="true">/</span><span>Hệ thống</span><span aria-hidden="true">/</span><span aria-current="page">Nhật ký thay đổi</span>
                </nav>
                <header class="audit-header crm-page-header">
                    <div class="audit-header-info">
                        <h1 class="crm-page-title" id="auditPageTitle">Nhật ký thay đổi</h1>
                        <p class="crm-page-description">Tra cứu người thực hiện, thời điểm và giá trị trước/sau mỗi thay đổi.</p>
                    </div>
                    <button type="button" class="crm-btn crm-btn-secondary" id="btnRefreshAudit">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 7v5h-5M4 17v-5h5"/><path d="M6 6a8 8 0 0 1 13 3M18 18A8 8 0 0 1 5 15"/></svg>
                        Làm mới
                    </button>
                </header>
                <section class="audit-card crm-card" aria-labelledby="auditCardTitle">
                    <div class="crm-card-header"><h2 class="crm-card-title" id="auditCardTitle">Lịch sử thay đổi</h2><span class="crm-badge">Chỉ đọc</span></div>
                    <div class="crm-card-body">
                        <form id="auditFilterForm" class="audit-filter-bar crm-toolbar">
                            <div class="audit-filter-item crm-form-group">
                                <label class="audit-filter-label crm-label" for="filterActor">Người thực hiện (ID)</label>
                                <input type="text" inputmode="numeric" pattern="[1-9][0-9]*" id="filterActor" class="audit-input crm-input" placeholder="Mã người dùng">
                            </div>
                            <div class="audit-filter-item crm-form-group">
                                <label class="audit-filter-label crm-label" for="filterEntityType">Loại đối tượng</label>
                                <input type="text" id="filterEntityType" class="audit-input crm-input" list="auditObjectTypes" placeholder="Tất cả đối tượng">
                                <datalist id="auditObjectTypes"><option value="USER">Người dùng</option><option value="CUSTOMER">Khách hàng</option><option value="OPPORTUNITY">Cơ hội</option><option value="ACTIVITY">Hoạt động</option><option value="QUOTE">Báo giá</option></datalist>
                            </div>
                            <div class="audit-filter-item crm-form-group">
                                <label class="crm-label" for="filterObjectId">Mã bản ghi</label>
                                <input type="text" inputmode="numeric" pattern="[1-9][0-9]*" id="filterObjectId" class="crm-input" placeholder="Tất cả bản ghi">
                            </div>
                            <div class="audit-filter-item crm-form-group">
                                <label class="audit-filter-label crm-label" for="filterFromDate">Từ ngày</label>
                                <input type="date" id="filterFromDate" class="audit-input crm-input">
                            </div>
                            <div class="audit-filter-item crm-form-group">
                                <label class="audit-filter-label crm-label" for="filterToDate">Đến ngày</label>
                                <input type="date" id="filterToDate" class="audit-input crm-input">
                            </div>
                            <div class="audit-filter-actions">
                                <button type="submit" class="crm-btn crm-btn-primary" id="btnFilter">Lọc nhật ký</button>
                                <button type="button" class="crm-btn crm-btn-secondary" id="btnResetFilter">Đặt lại</button>
                            </div>
                        </form>
                        <div id="auditFilterError" class="audit-filter-error crm-alert crm-alert-danger" hidden role="alert"></div>
                        <div id="auditLoading" class="crm-state crm-state-loading" hidden role="status" aria-live="polite">Đang tải nhật ký…</div>
                        <div id="auditErrorState" class="audit-error-state crm-state crm-state-error" hidden role="alert">
                            <h3 class="crm-state-title">Không thể tải nhật ký</h3>
                            <p id="auditErrorMessage"></p>
                            <button type="button" class="crm-btn crm-btn-secondary" id="btnRetryAudit">Thử lại</button>
                        </div>
                        <div class="audit-table-responsive crm-table-wrap" id="auditTableWrap" hidden tabindex="0" role="region" aria-label="Bảng nhật ký thay đổi; có thể cuộn ngang">
                            <table class="audit-table crm-table" id="auditTable">
                                <caption class="audit-sr-only">Người thực hiện, đối tượng và giá trị trước/sau của các thay đổi</caption>
                                <thead><tr>
                                    <th scope="col">Thời điểm</th><th scope="col">Người thực hiện</th><th scope="col">Loại đối tượng</th><th scope="col">Mã bản ghi</th><th scope="col">Hành động</th><th scope="col">Giá trị trước</th><th scope="col">Giá trị sau</th>
                                </tr></thead>
                                <tbody id="auditTableBody"></tbody>
                            </table>
                        </div>
                        <div id="auditEmptyState" class="crm-state" hidden role="status">
                            <svg class="audit-state-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 3h6v3H9zM9 11h6M9 15h4"/></svg>
                            <h3 class="crm-state-title">Chưa có nhật ký phù hợp</h3>
                            <p>Thử điều chỉnh người thực hiện, đối tượng hoặc khoảng thời gian.</p>
                            <button type="button" class="crm-btn crm-btn-secondary" id="btnEmptyReset">Đặt lại bộ lọc</button>
                        </div>
                        <p id="auditLimitNotice" class="audit-limit-notice" hidden>Đang hiển thị tối đa 500 bản ghi gần nhất. Hãy thu hẹp bộ lọc để tìm các thay đổi khác.</p>
                        <nav id="auditPagination" class="audit-pagination" hidden aria-label="Phân trang nhật ký">
                            <span id="auditPaginationInfo" role="status" aria-live="polite"></span>
                            <div id="auditPaginationControls" class="audit-pagination-controls"></div>
                        </nav>
                    </div>
                </section>
            </div>
        </main>
    </div>
    <jsp:include page="/jsp/shared/footer.jsp" />
    <script src="${pageContext.request.contextPath}/js/audit/audit-log.js" defer></script>
</body>
</html>
