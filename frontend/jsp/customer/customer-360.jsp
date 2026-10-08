<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Customer 360 & Phân cấp Tập đoàn | CRM ICTU</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/customer/customer-360.css">
</head>
<body>
<div class="crm-layout">
    <!-- Sidebar Navigation -->
    <jsp:include page="/jsp/shared/sidebar.jsp" />

    <!-- Main Content Shell -->
    <div class="crm-main">
        <!-- Top Header -->
        <jsp:include page="/jsp/shared/header.jsp" />

        <main class="crm-content">
            <!-- Header Bar with Breadcrumb, Customer Switcher & Top Actions -->
            <div class="c360-header-bar">
                <div class="page-title-box">
                    <nav class="page-breadcrumb" aria-label="breadcrumb">
                        <a href="#">Khách hàng</a>
                        <span>/</span>
                        <span id="breadcrumbCustName">Hồ sơ 360</span>
                    </nav>
                    <h1>Hồ sơ khách hàng Customer 360</h1>
                </div>

                <!-- Chuyển đổi khách hàng nhanh (Customer Switcher) -->
                <div class="c360-switcher-box">
                    <span class="c360-switcher-label">🏢 Khách hàng:</span>
                    <select id="customerSwitcher" class="c360-switcher-select" aria-label="Chọn khách hàng">
                        <option value="CUST-001" selected>Tập đoàn Công nghệ VNG (Mẹ)</option>
                        <option value="CUST-002">VNG Cloud Solutions (Công ty con)</option>
                        <option value="CUST-003">ZaloPay - Zion JSC (Công ty con)</option>
                        <option value="CUST-004">Tập đoàn Bán lẻ Masan Group</option>
                        <option value="CUST-005">Tổng công ty Viễn thông Viettel</option>
                    </select>
                </div>

                <div class="page-actions">
                    <a href="<%= request.getContextPath() %>/jsp/customer/customer-care.jsp" class="btn btn-warning">
                        <span>📞</span> DS Cần chăm sóc (<span id="carePendingCount">3</span>)
                    </a>
                    <a href="<%= request.getContextPath() %>/jsp/customer/after-sales-risk.jsp" class="btn btn-secondary">
                        <span>🛡️</span> Rủi ro sau bán
                    </a>
                    <button type="button" class="btn btn-primary" id="btnOpenEditCustModal">
                        <span>✏️</span> Chỉnh sửa
                    </button>
                </div>
            </div>

            <!-- Customer 360 Top Profile Card -->
            <div class="card c360-profile-card">
                <div class="c360-profile-main">
                    <div class="c360-avatar" id="custAvatar">VNG</div>
                    <div class="c360-info">
                        <h2>
                            <span id="custName">Tập đoàn Công nghệ VNG</span>
                            <span id="custBadge" class="badge badge-success">Khách hàng VIP</span>
                        </h2>
                        <div class="c360-meta-tags">
                            <span class="c360-meta-item">🏢 Mã KH: <strong id="custCode">CUST-2026-089</strong></span>
                            <span class="c360-meta-item">🌐 Lĩnh vực: <strong id="custIndustry">Công nghệ & Game</strong></span>
                            <span class="c360-meta-item">👤 Phụ trách: <strong>Hoàng Văn Thắng</strong></span>
                            <span class="c360-meta-item">📍 Quy mô: <strong id="custEmployees">4.500 Nhân sự</strong></span>
                        </div>

                        <!-- Dropdown Chọn Công ty Mẹ (Parent Company Dropdown) -->
                        <div class="c360-parent-select-box">
                            <span>🏛️ <strong>Công ty mẹ:</strong></span>
                            <select id="parentCompanySelect" title="Chọn công ty mẹ quản lý">
                                <option value="NONE">-- Độc lập / Là công ty Mẹ (Holding) --</option>
                                <option value="VNG" selected>Tập đoàn Công nghệ VNG (VNG Corp)</option>
                                <option value="MASAN">Tập đoàn Masan Group</option>
                                <option value="VIETTEL">Tập đoàn Công nghiệp - Viễn thông Viettel</option>
                                <option value="FPT">Tập đoàn FPT Corporation</option>
                            </select>
                            <span id="parentSavedNotice" style="display:none; color: var(--success); font-weight: 700; font-size: 0.8rem;">✓ Đã lưu</span>
                        </div>
                    </div>
                </div>

                <!-- Financial & Pipeline KPIs (Chuẩn S3-03) -->
                <div class="c360-profile-stats">
                    <div class="c360-stat-box">
                        <div class="c360-stat-label">Tổng giá trị đã ký</div>
                        <div class="c360-stat-val highlight" id="kpiRevenue">1.250.000.000 ₫</div>
                        <div class="c360-stat-sub" id="kpiSignedSub" style="font-size: 0.72rem; color: var(--text-muted); margin-top: 2px;">2 Hợp đồng • 1 Deal Won</div>
                    </div>
                    <div class="c360-stat-box">
                        <div class="c360-stat-label">Giá trị cơ hội đang mở</div>
                        <div class="c360-stat-val" style="color: var(--primary);" id="kpiOpenDeals">655.000.000 ₫</div>
                        <div class="c360-stat-sub" id="kpiOpenDealsCount" style="font-size: 0.72rem; color: var(--text-muted); margin-top: 2px;">3 Cơ hội đang mở</div>
                    </div>
                    <div class="c360-stat-box">
                        <div class="c360-stat-label">Điểm tín nhiệm & Sức khỏe</div>
                        <div class="c360-stat-val" style="color: var(--success);" id="kpiHealthScore">95 / 100</div>
                        <div class="c360-stat-sub" style="font-size: 0.72rem; color: var(--success); font-weight: 600; margin-top: 2px;">Tín nhiệm cao (VIP)</div>
                    </div>
                </div>
            </div>

            <!-- CARD TỔNG GIÁ TRỊ TẬP ĐOÀN (Group Total Value Card) -->
            <div class="c360-group-kpi-card" id="groupKpiCard">
                <div class="c360-group-primary">
                    <span class="c360-group-badge">🌐 Hệ sinh thái Tập đoàn (Group Level)</span>
                    <div class="c360-group-val" id="groupTotalValueText">5.480.000.000 ₫</div>
                    <div class="c360-group-desc">
                        Tổng giá trị hợp đồng & cơ hội toàn tập đoàn (Gồm công ty mẹ + <span id="subsidiaryCountBadge">3</span> công ty con trực thuộc)
                    </div>
                </div>
                <div class="c360-group-col">
                    <div class="c360-group-col-label">Đóng góp của đơn vị hiện tại</div>
                    <div class="c360-group-col-val" id="groupContributionVal">1.250.000.000 ₫ (<span id="groupContributionPct">22.8%</span>)</div>
                    <div class="c360-group-progress-wrap">
                        <div class="c360-group-progress-bar">
                            <div class="c360-group-progress-fill" id="groupContributionBar" style="width: 22.8%;"></div>
                        </div>
                    </div>
                </div>
                <div class="c360-group-col">
                    <div class="c360-group-col-label">Quy mô hệ sinh thái</div>
                    <div class="c360-group-col-val" id="groupSubsidiariesTotal">1 Mẹ + 3 Công ty con</div>
                    <div style="font-size: 0.8rem; color: #cbd5e1; margin-top: 4px;">
                        Tổng số Deals: <strong>14 Deals</strong> • <strong>8 HĐ</strong>
                    </div>
                </div>
            </div>

            <!-- Quick Action Bar -->
            <div class="c360-action-bar">
                <button type="button" class="btn btn-secondary btn-sm" id="btnQuickCall">
                    <span>📞</span> Gọi điện
                </button>
                <button type="button" class="btn btn-secondary btn-sm" id="btnQuickEmail">
                    <span>✉️</span> Gửi Email
                </button>
                <button type="button" class="btn btn-secondary btn-sm" id="btnOpenNewDeal">
                    <span>💼</span> Tạo cơ hội (Deal)
                </button>
                <button type="button" class="btn btn-secondary btn-sm" id="btnOpenUploadAttachment">
                    <span>📎</span> Đính kèm tài liệu
                </button>
                <button type="button" class="btn btn-secondary btn-sm" id="btnOpenNewContact">
                    <span>👤</span> Thêm người liên hệ
                </button>
                <button type="button" class="btn btn-secondary btn-sm" onclick="window.location.href='<%= request.getContextPath() %>/jsp/customer/customer-care.jsp'">
                    <span>📋</span> Chăm sóc định kỳ
                </button>
            </div>

            <!-- 2-Column Grid -->
            <div class="c360-grid">
                <!-- Left Sidebar Details -->
                <div class="c360-side-col">
                    <!-- Card 1: Thông tin doanh nghiệp -->
                    <div class="card c360-side-card">
                        <div class="c360-section-title">
                            <span>Thông tin pháp lý & Liên hệ</span>
                            <span style="font-size: 0.8rem; color: var(--primary); cursor: pointer;" id="btnEditSideInfo">Sửa</span>
                        </div>
                        <div class="c360-detail-row">
                            <div class="c360-detail-label">Số điện thoại</div>
                            <div class="c360-detail-val" id="sidePhone">028 3962 3888</div>
                        </div>
                        <div class="c360-detail-row">
                            <div class="c360-detail-label">Email doanh nghiệp</div>
                            <div class="c360-detail-val" id="sideEmail">contact@vng.com.vn</div>
                        </div>
                        <div class="c360-detail-row">
                            <div class="c360-detail-label">Mã số thuế</div>
                            <div class="c360-detail-val" id="sideTaxCode">0303538466</div>
                        </div>
                        <div class="c360-detail-row">
                            <div class="c360-detail-label">Địa chỉ trụ sở</div>
                            <div class="c360-detail-val" id="sideAddress">Z06 Đường số 13, KCX Tân Thuận, Quận 7, TP. HCM</div>
                        </div>
                        <div class="c360-detail-row">
                            <div class="c360-detail-label">Website</div>
                            <div class="c360-detail-val" id="sideWebsite"><a href="https://vng.com.vn" target="_blank">https://vng.com.vn</a></div>
                        </div>
                    </div>

                    <!-- Card 2: Danh sách người liên hệ (Contacts) -->
                    <div class="card c360-side-card">
                        <div class="c360-section-title">
                            <span>Người liên hệ (Contacts)</span>
                            <span class="badge badge-purple" id="contactCountBadge">3 Liên hệ</span>
                        </div>
                        <div id="contactList" style="display: flex; flex-direction: column; gap: 10px;">
                            <!-- Dynamic rendered via JS API -->
                        </div>
                        <button type="button" class="btn btn-secondary btn-sm" id="btnSideAddContact" style="margin-top: 12px; width: 100%;">
                            <span>➕</span> Thêm liên hệ mới
                        </button>
                    </div>

                    <!-- Card 3: Danh sách công ty con tóm tắt (Subsidiaries Summary) -->
                    <div class="card c360-side-card">
                        <div class="c360-section-title">
                            <span>Công ty con trực thuộc</span>
                            <span class="badge badge-info" id="sideSubsidiaryCountBadge">3 Đơn vị</span>
                        </div>
                        <div id="sideSubsidiariesList" style="display: flex; flex-direction: column; gap: 10px;">
                            <!-- Rendered by JS -->
                        </div>
                    </div>
                </div>

                <!-- Right Tabs Section -->
                <div class="c360-tabs-wrapper">
                    <nav class="c360-tabs-header" aria-label="Customer 360 Tabs">
                        <button type="button" class="c360-tab-btn active" data-tab="tab-hierarchy">
                            <span>🏛️ Sơ đồ Tập đoàn & Công ty con</span>
                            <span class="c360-tab-badge" id="tabBadgeHierarchy">4</span>
                        </button>
                        <button type="button" class="c360-tab-btn" data-tab="tab-timeline">
                            <span>🕒 Lịch sử tương tác</span>
                            <span class="c360-tab-badge" id="tabBadgeTimeline">8</span>
                        </button>
                        <button type="button" class="c360-tab-btn" data-tab="tab-deals">
                            <span>💼 Cơ hội (Mở & Đóng)</span>
                            <span class="c360-tab-badge" id="tabBadgeDeals">5</span>
                        </button>
                        <button type="button" class="c360-tab-btn" data-tab="tab-attachments">
                            <span>📎 Tài liệu đính kèm</span>
                            <span class="c360-tab-badge" id="tabBadgeAttachments">4</span>
                        </button>
                        <button type="button" class="c360-tab-btn" data-tab="tab-contracts">
                            <span>📜 Hợp đồng & Đơn hàng</span>
                            <span class="c360-tab-badge" id="tabBadgeContracts">2</span>
                        </button>
                    </nav>

                    <!-- TAB 1: SƠ ĐỒ HIERARCHY CÔNG TY MẸ - CON VÀ DANH SÁCH CÔNG TY CON -->
                    <div id="tab-hierarchy" class="c360-tab-pane active">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                            <div>
                                <h3 style="font-size: 1.15rem; font-weight: 800; color: var(--text-primary);">
                                    Cấu trúc phân cấp Tập đoàn (Group Hierarchy)
                                </h3>
                                <p style="font-size: 0.82rem; color: var(--text-muted); margin-top: 2px;">
                                    Mối quan hệ pháp nhân giữa công ty mẹ, các công ty con và giá trị hợp đồng đóng góp
                                </p>
                            </div>
                            <button type="button" class="btn btn-secondary btn-sm" id="btnToggleAddSubsidiary">
                                <span>➕</span> Gán công ty con mới
                            </button>
                        </div>

                        <!-- Sơ đồ trực quan Cây Hierarchy -->
                        <div class="c360-hierarchy-box">
                            <ul class="c360-tree-root" id="hierarchyTree">
                                <!-- Dynamic Rendered by JS -->
                            </ul>
                        </div>

                        <!-- Bảng chi tiết danh sách công ty con -->
                        <h4 style="font-size: 1.05rem; font-weight: 700; margin-bottom: 12px; display: flex; align-items: center; gap: 8px;">
                            <span>🏢 Danh sách công ty con thành viên</span>
                            <span class="badge badge-purple" id="subsidiaryListTableCount">3 Công ty</span>
                        </h4>

                        <div class="crm-table-container">
                            <table class="crm-table">
                                <thead>
                                    <tr>
                                        <th>Tên công ty con</th>
                                        <th>Mã KH</th>
                                        <th>Lĩnh vực</th>
                                        <th>Đại diện phụ trách</th>
                                        <th>Giá trị đóng góp</th>
                                        <th>Tỷ trọng %</th>
                                        <th>Hành động</th>
                                    </tr>
                                </thead>
                                <tbody id="subsidiaryTableBody">
                                    <!-- Rendered by JS -->
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <!-- TAB 2: TIMELINE TƯƠNG TÁC (Tối ưu tải < 1.5s với 500 hoạt động theo S3-03) -->
                    <div id="tab-timeline" class="c360-tab-pane">
                        <!-- Benchmark & Performance Test Bar -->
                        <div class="c360-benchmark-bar" style="display: flex; justify-content: space-between; align-items: center; background: #eff6ff; border: 1px solid #bfdbfe; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; flex-wrap: wrap; gap: 10px;">
                            <div class="c360-benchmark-info" style="display: flex; align-items: center; gap: 10px;">
                                <span style="font-size: 0.86rem; color: #1e40af;">⚡ <strong>Yêu cầu S3-03:</strong> Tải dưới 1.5s với 500 hoạt động</span>
                                <span id="benchmarkBadge" class="badge badge-success" style="font-size: 0.8rem; padding: 4px 10px;">
                                    ⏱️ Tải trong: <strong id="benchmarkTime">18.5 ms</strong> (<span id="benchmarkCount">4</span> hoạt động)
                                </span>
                            </div>
                            <div class="c360-benchmark-actions" style="display: flex; gap: 8px;">
                                <button type="button" class="btn btn-warning btn-sm" id="btnBenchmark500">
                                    ⚡ Nạp & Đo tải 500 hoạt động
                                </button>
                                <button type="button" class="btn btn-secondary btn-sm" id="btnResetTimeline">
                                    Mặc định
                                </button>
                            </div>
                        </div>

                        <!-- Quick Log Input -->
                        <div class="c360-quick-log">
                            <div class="c360-log-types">
                                <button type="button" class="c360-log-type-btn active" data-type="note">📝 Ghi chú</button>
                                <button type="button" class="c360-log-type-btn" data-type="call">📞 Cuộc gọi</button>
                                <button type="button" class="c360-log-type-btn" data-type="meeting">🤝 Họp</button>
                                <button type="button" class="c360-log-type-btn" data-type="email">✉️ Email</button>
                                <button type="button" class="c360-log-type-btn" data-type="care">❤️ Chăm sóc định kỳ</button>
                            </div>
                            <textarea id="quickLogText" class="form-control" rows="2" placeholder="Nhập nhanh nội dung trao đổi, kết quả cuộc gọi, thỏa thuận hoặc ghi chú..."></textarea>
                            <div style="display: flex; justify-content: flex-end; margin-top: 10px;">
                                <button type="button" id="quickLogSubmit" class="btn btn-primary btn-sm">
                                    <span>💾</span> Lưu vào Timeline
                                </button>
                            </div>
                        </div>

                        <!-- Timeline Filters -->
                        <div class="c360-timeline-filters" style="display: flex; gap: 8px; margin-bottom: 14px; flex-wrap: wrap;">
                            <button type="button" class="c360-deal-filter-btn active" data-tfilter="ALL">Tất cả (<span id="timelineCountFilter">4</span>)</button>
                            <button type="button" class="c360-deal-filter-btn" data-tfilter="meeting">🤝 Cuộc họp</button>
                            <button type="button" class="c360-deal-filter-btn" data-tfilter="call">📞 Cuộc gọi</button>
                            <button type="button" class="c360-deal-filter-btn" data-tfilter="email">✉️ Email</button>
                            <button type="button" class="c360-deal-filter-btn" data-tfilter="note">📝 Ghi chú</button>
                            <button type="button" class="c360-deal-filter-btn" data-tfilter="care">❤️ Chăm sóc</button>
                        </div>

                        <!-- Timeline List -->
                        <div id="activityTimeline" class="c360-timeline">
                            <!-- Dynamic Rendered via JS -->
                        </div>
                    </div>

                    <!-- TAB 3: OPPORTUNITY MỞ & ĐÓNG -->
                    <div id="tab-deals" class="c360-tab-pane">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 10px;">
                            <div class="c360-deal-subtabs">
                                <button type="button" class="c360-deal-filter-btn active" data-status="ALL">Tất cả (<span id="dealCountAll">5</span>)</button>
                                <button type="button" class="c360-deal-filter-btn" data-status="OPEN">Đang mở (<span id="dealCountOpen">3</span>)</button>
                                <button type="button" class="c360-deal-filter-btn" data-status="CLOSED_WON">Đã thắng (Won - <span id="dealCountWon">1</span>)</button>
                                <button type="button" class="c360-deal-filter-btn" data-status="CLOSED_LOST">Đã thua (Lost - <span id="dealCountLost">1</span>)</button>
                            </div>
                            <button type="button" class="btn btn-primary btn-sm" id="btnCreateNewDealTop">
                                <span>➕</span> Tạo cơ hội mới
                            </button>
                        </div>

                        <div class="crm-table-container">
                            <table class="crm-table">
                                <thead>
                                    <tr>
                                        <th>Tên cơ hội (Deal)</th>
                                        <th>Trạng thái / Giai đoạn</th>
                                        <th>Giá trị dự kiến</th>
                                        <th>Xác suất</th>
                                        <th>Ngày dự kiến chốt / Đóng</th>
                                        <th>Ghi chú kết quả</th>
                                    </tr>
                                </thead>
                                <tbody id="dealsTableBody">
                                    <!-- Dynamic Rendered via JS -->
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <!-- TAB 4: ATTACHMENTS (TỆP ĐÍNH KÈM) -->
                    <div id="tab-attachments" class="c360-tab-pane">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                            <div>
                                <h3 style="font-size: 1.15rem; font-weight: 800;">Tài liệu & Hồ sơ đính kèm</h3>
                                <p style="font-size: 0.82rem; color: var(--text-muted); margin-top: 2px;">
                                    Hợp đồng ký kết, báo giá đề xuất, hồ sơ năng lực và biên bản bàn giao
                                </p>
                            </div>
                            <button type="button" class="btn btn-primary btn-sm" id="btnUploadAttachmentTrigger">
                                <span>📤</span> Tải lên tài liệu
                            </button>
                        </div>

                        <div class="c360-attachment-grid" id="attachmentGrid">
                            <!-- Dynamic Rendered via JS -->
                        </div>
                    </div>

                    <!-- TAB 5: CONTRACTS -->
                    <div id="tab-contracts" class="c360-tab-pane">
                        <div class="crm-table-container">
                            <table class="crm-table">
                                <thead>
                                    <tr>
                                        <th>Số hợp đồng</th>
                                        <th>Tên hợp đồng</th>
                                        <th>Giá trị hợp đồng</th>
                                        <th>Ngày ký kết</th>
                                        <th>Hiệu lực đến</th>
                                        <th>Trạng thái</th>
                                    </tr>
                                </thead>
                                <tbody id="contractsTableBody">
                                    <!-- Dynamic Rendered via JS -->
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>

<!-- MODAL: TẠO LIÊN HỆ MỚI (CONTACT) -->
<div id="modalContact" class="modal-overlay" style="display:none;">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3 class="modal-title">Thêm người liên hệ mới</h3>
            <button type="button" class="modal-close" data-close="modalContact">&times;</button>
        </div>
        <form id="formAddContact">
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="contactNameInput">Họ và tên <span class="required">*</span></label>
                    <input type="text" id="contactNameInput" class="form-control" placeholder="Nguyễn Văn A" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="contactRoleInput">Chức vụ / Vị trí</label>
                    <input type="text" id="contactRoleInput" class="form-control" placeholder="Giám đốc CNTT / Trưởng phòng thu mua">
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label class="form-label" for="contactPhoneInput">Số điện thoại <span class="required">*</span></label>
                        <input type="tel" id="contactPhoneInput" class="form-control" placeholder="0912345678" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="contactEmailInput">Email</label>
                        <input type="email" id="contactEmailInput" class="form-control" placeholder="email@company.vn">
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label" for="contactDecisionRole">Vai trò trong quyết định mua</label>
                    <select id="contactDecisionRole" class="form-select">
                        <option value="Người quyết định chính (Decision Maker)">Người quyết định chính (Decision Maker)</option>
                        <option value="Người đánh giá kỹ thuật (Technical Evaluator)">Người đánh giá kỹ thuật (Technical Evaluator)</option>
                        <option value="Người sử dụng trực tiếp (End User)">Người sử dụng trực tiếp (End User)</option>
                        <option value="Ký hợp đồng & Thanh toán (Finance/Legal)">Ký hợp đồng & Thanh toán (Finance/Legal)</option>
                    </select>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-close="modalContact">Hủy</button>
                <button type="submit" class="btn btn-primary">Lưu người liên hệ</button>
            </div>
        </form>
    </div>
</div>

<!-- MODAL: TẠO CƠ HỘI (DEAL) MỚI -->
<div id="modalDeal" class="modal-overlay" style="display:none;">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3 class="modal-title">Tạo cơ hội bán hàng (Deal)</h3>
            <button type="button" class="modal-close" data-close="modalDeal">&times;</button>
        </div>
        <form id="formAddDeal">
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="dealNameInput">Tên cơ hội <span class="required">*</span></label>
                    <input type="text" id="dealNameInput" class="form-control" placeholder="Triển khai phần mềm Cloud ERP 2026..." required>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label class="form-label" for="dealValueInput">Giá trị dự kiến (VNĐ) <span class="required">*</span></label>
                        <input type="number" id="dealValueInput" class="form-control" placeholder="250000000" required>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="dealProbabilityInput">Xác suất thành công (%)</label>
                        <input type="number" id="dealProbabilityInput" class="form-control" value="70" min="0" max="100">
                    </div>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label class="form-label" for="dealStageSelect">Giai đoạn</label>
                        <select id="dealStageSelect" class="form-select">
                            <option value="Đề xuất giải pháp">Đề xuất giải pháp</option>
                            <option value="Đàm phán hợp đồng">Đàm phán hợp đồng</option>
                            <option value="Chờ ký kết">Chờ ký kết</option>
                            <option value="Closed Won">Closed Won (Thành công)</option>
                            <option value="Closed Lost">Closed Lost (Thất bại)</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="dealCloseDateInput">Ngày dự kiến chốt</label>
                        <input type="date" id="dealCloseDateInput" class="form-control" value="2026-11-15">
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-close="modalDeal">Hủy</button>
                <button type="submit" class="btn btn-primary">Xác nhận tạo Deal</button>
            </div>
        </form>
    </div>
</div>

<!-- MODAL: TẢI LÊN TÀI LIỆU (ATTACHMENT) -->
<div id="modalAttachment" class="modal-overlay" style="display:none;">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3 class="modal-title">Tải lên tài liệu đính kèm</h3>
            <button type="button" class="modal-close" data-close="modalAttachment">&times;</button>
        </div>
        <form id="formAddAttachment">
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="attachNameInput">Tên tài liệu / Tiêu đề <span class="required">*</span></label>
                    <input type="text" id="attachNameInput" class="form-control" placeholder="Hợp đồng dịch vụ bảo trì 2026.pdf" required>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label class="form-label" for="attachTypeSelect">Định dạng / Loại file</label>
                        <select id="attachTypeSelect" class="form-select">
                            <option value="pdf">Tệp PDF (.pdf)</option>
                            <option value="doc">Tài liệu Word (.docx)</option>
                            <option value="xls">Bảng tính Excel (.xlsx)</option>
                            <option value="img">Hình ảnh chứng từ</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="attachSizeInput">Dung lượng ước lượng</label>
                        <input type="text" id="attachSizeInput" class="form-control" value="2.4 MB">
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-close="modalAttachment">Hủy</button>
                <button type="submit" class="btn btn-primary">Tải lên ngay</button>
            </div>
        </form>
    </div>
</div>

<!-- Toast Container -->
<div id="c360Toast" class="c360-toast" style="display:none;">
    <span id="c360ToastIcon">✅</span>
    <span id="c360ToastMsg">Đã cập nhật thành công!</span>
</div>

<script src="<%= request.getContextPath() %>/js/customer/customer-360.js"></script>
</body>
</html>
