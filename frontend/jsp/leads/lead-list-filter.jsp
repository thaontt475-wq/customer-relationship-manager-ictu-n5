<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh sách Lead & Bộ lọc tùy chỉnh | CRM ICTU</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/leads/lead-filter.css">
</head>
<body>
<div class="crm-layout">
    <jsp:include page="/jsp/shared/sidebar.jsp" />

    <div class="crm-main">
        <jsp:include page="/jsp/shared/header.jsp" />

        <main class="crm-content">
            <div class="page-header">
                <div class="page-title-box">
                    <nav class="page-breadcrumb" aria-label="breadcrumb">
                        <a href="#">Tiếp thị & Lead</a>
                        <span>/</span>
                        <span>Danh sách Lead</span>
                    </nav>
                    <h1>Bộ lọc Lead & Saved Filters</h1>
                </div>
                <div class="page-actions">
                    <a href="<%= request.getContextPath() %>/html/lead-capture.html" target="_blank" class="btn btn-secondary">
                        <span>🌐</span> Mở Form thu Lead ngoài Web
                    </a>
                    <button type="button" class="btn btn-primary" onclick="alert('Mở form tạo mới Lead thủ công')">
                        <span>➕</span> Thêm Lead mới
                    </button>
                </div>
            </div>

            <!-- Dynamic Filter + Saved Filters Component (S4-09 / CRM-123) -->
            <div class="lead-filter-box">
                <!-- Saved Filter Chips Bar -->
                <div class="saved-filter-bar">
                    <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
                        <span style="font-size: 0.85rem; font-weight: 700; color: var(--text-secondary);">Bộ lọc đã lưu:</span>
                        <div id="savedFilterChips" class="saved-filter-chips">
                            <!-- Populated dynamically by lead-filter.js -->
                        </div>
                    </div>
                    <button type="button" id="btnOpenSaveFilter" class="btn btn-secondary btn-sm">
                        <span>💾</span> Lưu bộ lọc hiện tại
                    </button>
                </div>

                <!-- Criteria Grid Inputs -->
                <div class="filter-criteria-grid">
                    <div class="form-group" style="margin-bottom: 0;">
                        <label class="form-label" for="filterSource">Nguồn Lead (Source)</label>
                        <select id="filterSource" class="form-select">
                            <option value="">-- Tất cả nguồn --</option>
                            <option value="WEB">Website Form</option>
                            <option value="FACEBOOK">Facebook Ads</option>
                            <option value="GOOGLE">Google Ads</option>
                            <option value="EVENT">Sự kiện Offline</option>
                        </select>
                    </div>

                    <div class="form-group" style="margin-bottom: 0;">
                        <label class="form-label" for="filterStatus">Trạng thái (Status)</label>
                        <select id="filterStatus" class="form-select">
                            <option value="">-- Tất cả trạng thái --</option>
                            <option value="NEW">Mới tiếp cận (New)</option>
                            <option value="WARM">Tiềm năng (Warm)</option>
                            <option value="WON">Đã chuyển đổi (Won)</option>
                        </select>
                    </div>

                    <div class="form-group" style="margin-bottom: 0;">
                        <label class="form-label" for="filterMinScore">Điểm Lead tối thiểu</label>
                        <input type="number" id="filterMinScore" class="form-control" placeholder="VD: 75" min="0" max="100">
                    </div>

                    <div class="filter-actions">
                        <button type="button" id="btnApplyFilter" class="btn btn-primary" style="flex: 1;">
                            <span>🔍</span> Áp dụng
                        </button>
                        <button type="button" id="btnResetFilter" class="btn btn-secondary" title="Đặt lại bộ lọc">
                            <span>🔄</span>
                        </button>
                    </div>
                </div>

                <div class="active-filter-summary">
                    <span>Kết quả lọc: <strong id="filterResultCount" style="color: var(--primary);">4</strong> Lead phù hợp</span>
                </div>
            </div>

            <!-- Leads Data Table -->
            <div class="card" style="padding: 24px;">
                <div class="crm-table-container">
                    <table class="crm-table">
                        <thead>
                            <tr>
                                <th>Mã Lead</th>
                                <th>Khách hàng</th>
                                <th>Doanh nghiệp</th>
                                <th>Nguồn</th>
                                <th>Điểm Lead</th>
                                <th>Trạng thái</th>
                                <th>Ngày tạo</th>
                                <th>Thao tác</th>
                            </tr>
                        </thead>
                        <tbody id="leadsTableBody">
                            <tr data-source="WEB" data-status="WARM" data-score="85">
                                <td><strong>LD-101</strong></td>
                                <td>
                                    <div style="font-weight: 700;">Đặng Quốc Cường</div>
                                    <div style="font-size: 0.76rem; color: var(--text-muted);">0912 888 777</div>
                                </td>
                                <td>Tập đoàn Logistics Miền Bắc</td>
                                <td><span class="badge badge-info">Website Form</span></td>
                                <td><span class="badge badge-success">85 điểm</span></td>
                                <td><span class="badge badge-purple">Tiềm năng (Warm)</span></td>
                                <td>03/10/2026</td>
                                <td><button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem LD-101')">Xem</button></td>
                            </tr>
                            <tr data-source="FACEBOOK" data-status="NEW" data-score="55">
                                <td><strong>LD-102</strong></td>
                                <td>
                                    <div style="font-weight: 700;">Nguyễn Mai Chi</div>
                                    <div style="font-size: 0.76rem; color: var(--text-muted);">0976 112 334</div>
                                </td>
                                <td>Chuỗi Bán lẻ Thời trang Nova</td>
                                <td><span class="badge badge-info">Facebook Ads</span></td>
                                <td><span class="badge badge-warning">55 điểm</span></td>
                                <td><span class="badge badge-gray">Mới tiếp cận (New)</span></td>
                                <td>02/10/2026</td>
                                <td><button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem LD-102')">Xem</button></td>
                            </tr>
                            <tr data-source="GOOGLE" data-status="WON" data-score="95">
                                <td><strong>LD-103</strong></td>
                                <td>
                                    <div style="font-weight: 700;">Trần Văn Hùng</div>
                                    <div style="font-size: 0.76rem; color: var(--text-muted);">0904 556 778</div>
                                </td>
                                <td>Công ty Cơ khí Chính xác Hùng Phát</td>
                                <td><span class="badge badge-info">Google Ads</span></td>
                                <td><span class="badge badge-success">95 điểm</span></td>
                                <td><span class="badge badge-success">Đã chuyển đổi (Won)</span></td>
                                <td>01/10/2026</td>
                                <td><button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem LD-103')">Xem</button></td>
                            </tr>
                            <tr data-source="WEB" data-status="WARM" data-score="82">
                                <td><strong>LD-104</strong></td>
                                <td>
                                    <div style="font-weight: 700;">Lê Minh Tú</div>
                                    <div style="font-size: 0.76rem; color: var(--text-muted);">0932 445 667</div>
                                </td>
                                <td>Dược phẩm An Bình</td>
                                <td><span class="badge badge-info">Website Form</span></td>
                                <td><span class="badge badge-success">82 điểm</span></td>
                                <td><span class="badge badge-purple">Tiềm năng (Warm)</span></td>
                                <td>03/10/2026</td>
                                <td><button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem LD-104')">Xem</button></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>
</div>

<!-- Modal Lưu Bộ Lọc -->
<div id="saveFilterModal" class="modal-overlay">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3 class="modal-title">Lưu bộ lọc tùy chỉnh</h3>
            <button type="button" id="closeSaveFilterModal" class="modal-close">&times;</button>
        </div>
        <form id="saveFilterForm">
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="savedFilterName">Tên bộ lọc <span class="required">*</span></label>
                    <input type="text" id="savedFilterName" class="form-control" placeholder="Ví dụ: Lead Website Nóng trên 80đ..." required>
                </div>
                <p style="font-size: 0.8rem; color: var(--text-muted);">Bộ lọc này sẽ được lưu vào danh sách truy cập nhanh của bạn.</p>
            </div>
            <div class="modal-footer">
                <button type="button" id="cancelSaveFilterModal" class="btn btn-secondary">Hủy</button>
                <button type="submit" class="btn btn-primary">Lưu bộ lọc</button>
            </div>
        </form>
    </div>
</div>

<script src="<%= request.getContextPath() %>/js/leads/lead-filter.js"></script>
</body>
</html>
