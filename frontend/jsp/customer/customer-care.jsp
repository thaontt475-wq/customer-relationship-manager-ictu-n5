<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh sách khách hàng cần chăm sóc | CRM ICTU</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/customer/customer-care.css">
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
            <!-- Breadcrumb & Top Page Header -->
            <div class="page-header">
                <div class="page-title-box">
                    <nav class="page-breadcrumb" aria-label="breadcrumb">
                        <a href="<%= request.getContextPath() %>/jsp/customer/customer-360.jsp">Khách hàng</a>
                        <span>/</span>
                        <span>Chăm sóc khách hàng</span>
                    </nav>
                    <h1>Danh sách khách hàng cần chăm sóc (Customer Follow-up)</h1>
                </div>
                <div class="page-actions">
                    <a href="<%= request.getContextPath() %>/jsp/customer/customer-360.jsp" class="btn btn-secondary">
                        <span>🏢</span> Hồ sơ Customer 360
                    </a>
                    <a href="<%= request.getContextPath() %>/jsp/customer/after-sales-risk.jsp" class="btn btn-secondary">
                        <span>🛡️</span> Cảnh báo Churn Risk
                    </a>
                    <button type="button" class="btn-refresh" id="btnRefreshList" title="Tải lại dữ liệu mới nhất">
                        <span class="refresh-icon">🔄</span> Refresh danh sách
                    </button>
                </div>
            </div>

            <!-- KPI Summary Cards -->
            <div class="care-kpi-grid">
                <div class="care-kpi-card">
                    <div class="care-kpi-icon warning">⏳</div>
                    <div>
                        <div class="care-kpi-val" id="kpiTotalPending" style="color: var(--warning);">7</div>
                        <div class="care-kpi-label">Cần chăm sóc (Theo bộ lọc N ngày)</div>
                    </div>
                </div>

                <div class="care-kpi-card">
                    <div class="care-kpi-icon danger">🚨</div>
                    <div>
                        <div class="care-kpi-val" id="kpiOverdue30" style="color: var(--danger);">3</div>
                        <div class="care-kpi-label">Báo động: Chưa liên hệ > 30 ngày</div>
                    </div>
                </div>

                <div class="care-kpi-card">
                    <div class="care-kpi-icon primary">💰</div>
                    <div>
                        <div class="care-kpi-val" id="kpiValueAtRisk" style="color: var(--primary);">4.850.000.000 ₫</div>
                        <div class="care-kpi-label">Tổng giá trị hợp đồng cần giữ chân</div>
                    </div>
                </div>

                <div class="care-kpi-card">
                    <div class="care-kpi-icon success">✅</div>
                    <div>
                        <div class="care-kpi-val" id="kpiContactedToday" style="color: var(--success);">2</div>
                        <div class="care-kpi-label">Đã liên hệ trong ngày hôm nay</div>
                    </div>
                </div>
            </div>

            <!-- Filter Card: Input N ngày, Presets, Sort, Search -->
            <div class="care-filter-card">
                <div class="care-filter-row">
                    <!-- INPUT N NGÀY -->
                    <div class="care-n-days-box">
                        <span class="care-n-days-label">
                            <span>⏱️</span> Chưa tương tác quá:
                        </span>
                        <input type="number" id="inputNDays" class="care-n-days-input" min="0" max="365" value="15" title="Nhập số N ngày chưa có liên hệ">
                        <span class="care-n-days-unit">ngày</span>
                    </div>

                    <!-- PRESETS CHỌN NHANH -->
                    <div class="care-presets-list">
                        <button type="button" class="care-preset-chip" data-days="7">&gt; 7 ngày</button>
                        <button type="button" class="care-preset-chip active" data-days="15">&gt; 15 ngày</button>
                        <button type="button" class="care-preset-chip" data-days="30">&gt; 30 ngày (Cảnh báo)</button>
                        <button type="button" class="care-preset-chip" data-days="60">&gt; 60 ngày (Khẩn cấp)</button>
                        <button type="button" class="care-preset-chip" data-days="0">Tất cả khách hàng</button>
                    </div>
                </div>

                <!-- Toolbar: Search & Sort -->
                <div class="care-toolbar">
                    <div class="care-search-box">
                        <span>🔍</span>
                        <input type="text" id="careSearchInput" placeholder="Tìm theo tên khách hàng, mã KH, người phụ trách...">
                    </div>

                    <!-- SORT (SẮP XẾP) -->
                    <div class="care-sort-box">
                        <span class="care-sort-label">Sắp xếp theo:</span>
                        <select id="careSortSelect" class="care-sort-select">
                            <option value="days_desc" selected>⏳ Lâu nhất chưa liên hệ (Ngày giảm dần)</option>
                            <option value="days_asc">⌛ Mới liên hệ gần đây nhất</option>
                            <option value="value_desc">💰 Giá trị hợp đồng cao nhất</option>
                            <option value="value_asc">📉 Giá trị hợp đồng thấp nhất</option>
                            <option value="name_asc">🔤 Tên khách hàng (A ➔ Z)</option>
                            <option value="risk_desc">🚨 Mức độ rủi ro cao nhất</option>
                        </select>
                    </div>
                </div>
            </div>

            <!-- BẢNG DANH SÁCH KHÁCH HÀNG CẦN CHĂM SÓC -->
            <div class="card" style="padding: 24px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                    <div>
                        <h3 style="font-size: 1.15rem; font-weight: 700;">
                            Khách hàng thỏa mãn điều kiện chăm sóc
                        </h3>
                        <p style="font-size: 0.82rem; color: var(--text-muted); margin-top: 2px;">
                            Ưu tiên liên hệ các khách hàng có giá trị hợp đồng lớn và đã quá lâu chưa có tương tác
                        </p>
                    </div>
                    <span class="badge badge-purple" id="tableRecordCountBadge">Đang hiển thị: 7 khách hàng</span>
                </div>

                <div class="crm-table-container">
                    <table class="crm-table">
                        <thead>
                            <tr>
                                <th>Khách hàng & Mã KH</th>
                                <th>Phụ trách</th>
                                <th>Lần tương tác gần nhất</th>
                                <th>Số ngày chưa liên hệ</th>
                                <th>Giá trị hợp đồng</th>
                                <th>Mức độ ưu tiên</th>
                                <th style="text-align: right;">Hành động</th>
                            </tr>
                        </thead>
                        <tbody id="careTableBody">
                            <!-- Dynamic rendered by customer-care.js -->
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>
</div>

<!-- MODAL GHI CHÚ "ĐÃ LIÊN HỆ" -->
<div id="modalMarkContacted" class="modal-overlay" style="display:none;">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3 class="modal-title">Ghi nhận liên hệ chăm sóc khách hàng</h3>
            <button type="button" class="modal-close" id="btnCloseContactModal">&times;</button>
        </div>
        <form id="formMarkContacted">
            <div class="modal-body">
                <input type="hidden" id="modalCustId">
                <div class="form-group">
                    <label class="form-label">Khách hàng được chăm sóc:</label>
                    <div style="font-weight: 800; font-size: 1.05rem; color: var(--primary);" id="modalCustNameDisplay">
                        Tập đoàn Công nghệ VNG
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label class="form-label" for="contactChannelSelect">Hình thức liên hệ <span class="required">*</span></label>
                        <select id="contactChannelSelect" class="form-select">
                            <option value="Cuộc gọi điện thoại">📞 Cuộc gọi điện thoại</option>
                            <option value="Họp trực tiếp">🤝 Gặp gỡ / Họp trực tiếp</option>
                            <option value="Gửi Email">✉️ Gửi Email CSKH</option>
                            <option value="Tin nhắn Zalo/Chat">💬 Tin nhắn Zalo / OA</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="contactResultSelect">Kết quả trao đổi</label>
                        <select id="contactResultSelect" class="form-select">
                            <option value="Khách hàng hài lòng, tiếp tục gia hạn">Khách hàng hài lòng, tiếp tục gia hạn</option>
                            <option value="Có nhu cầu nâng cấp gói mới">Có nhu cầu nâng cấp gói mới</option>
                            <option value="Có thắc mắc cần hỗ trợ kỹ thuật">Có thắc mắc cần hỗ trợ kỹ thuật</option>
                            <option value="Chưa nghe máy, cần gọi lại sau">Chưa nghe máy, cần gọi lại sau</option>
                        </select>
                    </div>
                </div>

                <div class="form-group">
                    <label class="form-label" for="contactNoteText">Nội dung tóm tắt cuộc trao đổi <span class="required">*</span></label>
                    <textarea id="contactNoteText" class="form-control" rows="3" placeholder="Ghi chú nội dung đã trao đổi cùng khách hàng..." required>Đã liên hệ hỏi thăm tình hình sử dụng dịch vụ và tiếp nhận ý kiến đóng góp.</textarea>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" id="btnCancelContactModal">Hủy</button>
                <button type="submit" class="btn btn-primary">Xác nhận "Đã liên hệ"</button>
            </div>
        </form>
    </div>
</div>

<!-- Toast Notification -->
<div id="careToast" class="care-toast" style="display:none;">
    <span id="careToastIcon">✅</span>
    <span id="careToastMsg">Đã cập nhật thành công!</span>
</div>

<script src="<%= request.getContextPath() %>/js/customer/customer-care.js"></script>
</body>
</html>
