<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hỗ trợ sau bán & Cảnh báo rủi ro | CRM ICTU</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/customer/after-sales.css">
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
                        <a href="<%= request.getContextPath() %>/jsp/customer/customer-360.jsp">Khách hàng</a>
                        <span>/</span>
                        <span>Sau bán & Rủi ro rời bỏ</span>
                    </nav>
                    <h1>Hỗ trợ sau bán & Cảnh báo rủi ro (Customer Health)</h1>
                </div>
                <div class="page-actions">
                    <button type="button" id="openTicketModal" class="btn btn-primary">
                        <span>➕</span> Tạo Ticket hỗ trợ
                    </button>
                </div>
            </div>

            <!-- Risk Banner Alert -->
            <div class="risk-alert-banner">
                <div class="risk-alert-info">
                    <span class="risk-alert-badge">RỦI RO CAO</span>
                    <div>
                        <strong style="color: #9f1239; font-size: 1rem;">Phát hiện 2 khách hàng trọng điểm có nguy cơ ngừng dịch vụ (Churn Risk)</strong>
                        <div class="risk-alert-factors">
                            <span class="risk-factor-item">⚠️ Có ticket khiếu nại quá hạn SLA > 48h</span>
                            <span class="risk-factor-item">⏳ Chưa có tương tác sau bán > 30 ngày</span>
                        </div>
                    </div>
                </div>
                <button type="button" class="btn btn-sm btn-danger" onclick="alert('Đã gửi thông báo khẩn tới trưởng bộ phận CSKH & Sales!')">
                    Gửi cảnh báo khẩn cấp
                </button>
            </div>

            <!-- Overview KPIs -->
            <div class="risk-overview-grid">
                <div class="card risk-kpi-card">
                    <div class="risk-kpi-icon danger">⚠️</div>
                    <div>
                        <div class="risk-kpi-val" style="color: var(--danger);">5</div>
                        <div class="risk-kpi-label">Khách hàng rủi ro cao</div>
                    </div>
                </div>

                <div class="card risk-kpi-card">
                    <div class="risk-kpi-icon warning">⏳</div>
                    <div>
                        <div class="risk-kpi-val" style="color: var(--warning);">12</div>
                        <div class="risk-kpi-label">Ticket đang mở</div>
                    </div>
                </div>

                <div class="card risk-kpi-card">
                    <div class="risk-kpi-icon danger">🚨</div>
                    <div>
                        <div class="risk-kpi-val" style="color: var(--danger);">2</div>
                        <div class="risk-kpi-label">Ticket vi phạm SLA</div>
                    </div>
                </div>

                <div class="card risk-kpi-card">
                    <div class="risk-kpi-icon success">🛡️</div>
                    <div>
                        <div class="risk-kpi-val" style="color: var(--success);">94.2%</div>
                        <div class="risk-kpi-label">Tỷ lệ hài lòng (CSAT)</div>
                    </div>
                </div>
            </div>

            <!-- Ticket Table -->
            <div class="card" style="padding: 24px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <div>
                        <h3 style="font-size: 1.15rem; font-weight: 700;">Danh sách Ticket hỗ trợ kỹ thuật & khiếu nại</h3>
                        <p style="font-size: 0.82rem; color: var(--text-muted); margin-top: 2px;">Theo dõi tiến độ giải quyết và cam kết thời hạn phục vụ (SLA)</p>
                    </div>
                </div>

                <div class="crm-table-container">
                    <table class="crm-table">
                        <thead>
                            <tr>
                                <th>Mã Ticket</th>
                                <th>Vấn đề / Tiêu đề</th>
                                <th>Loại dịch vụ</th>
                                <th>Mức độ</th>
                                <th>Trạng thái</th>
                                <th>Phụ trách</th>
                                <th>Thời hạn SLA</th>
                                <th>Hành động</th>
                            </tr>
                        </thead>
                        <tbody id="ticketTableBody">
                            <tr>
                                <td><strong>TCK-1088</strong></td>
                                <td>Lỗi đồng bộ hóa đơn điện tử sang phần mềm kế toán</td>
                                <td>Khiếu nại kỹ thuật</td>
                                <td><span class="badge badge-danger">Khẩn cấp</span></td>
                                <td><span class="badge badge-warning">Đang xử lý</span></td>
                                <td>Nguyễn Thắng</td>
                                <td><span class="sla-overdue">⚠️ Quá hạn 4h</span></td>
                                <td>
                                    <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem chi tiết ticket TCK-1088')">Xem</button>
                                </td>
                            </tr>
                            <tr>
                                <td><strong>TCK-1085</strong></td>
                                <td>Yêu cầu đào tạo lại nhân sự chi nhánh mới</td>
                                <td>Đào tạo sau bán</td>
                                <td><span class="badge badge-info">Bình thường</span></td>
                                <td><span class="badge badge-success">Đã hoàn thành</span></td>
                                <td>Hoàng Thắng</td>
                                <td><span>Đúng hạn</span></td>
                                <td>
                                    <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem chi tiết ticket TCK-1085')">Xem</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>
</div>

<!-- Modal Tạo Ticket mới -->
<div id="ticketModalOverlay" class="modal-overlay">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3 class="modal-title">Tạo Ticket hỗ trợ sau bán</h3>
            <button type="button" id="closeTicketModal" class="modal-close">&times;</button>
        </div>
        <form id="createTicketForm">
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="ticketCustomer">Khách hàng <span class="required">*</span></label>
                    <select id="ticketCustomer" class="form-select" required>
                        <option value="1">Tập đoàn Công nghệ VNG (CUST-2026-089)</option>
                        <option value="2">Công ty Cổ phần MISA</option>
                        <option value="3">Viettel Telecom</option>
                    </select>
                </div>

                <div class="form-group">
                    <label class="form-label" for="ticketSubject">Tiêu đề yêu cầu <span class="required">*</span></label>
                    <input type="text" id="ticketSubject" class="form-control" placeholder="Tóm tắt ngắn gọn vấn đề cần hỗ trợ..." required>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                    <div class="form-group">
                        <label class="form-label" for="ticketCategory">Loại hỗ trợ</label>
                        <select id="ticketCategory" class="form-select">
                            <option value="Hỗ trợ kỹ thuật">Hỗ trợ kỹ thuật</option>
                            <option value="Khiếu nại dịch vụ">Khiếu nại dịch vụ</option>
                            <option value="Đào tạo sử dụng">Đào tạo sử dụng</option>
                            <option value="Bảo hành định kỳ">Bảo hành định kỳ</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="ticketPriority">Mức độ ưu tiên</label>
                        <select id="ticketPriority" class="form-select">
                            <option value="MEDIUM">Trung bình</option>
                            <option value="HIGH">Khẩn cấp (SLA 4h)</option>
                            <option value="LOW">Thấp (SLA 48h)</option>
                        </select>
                    </div>
                </div>

                <div class="form-group">
                    <label class="form-label" for="ticketDesc">Mô tả chi tiết</label>
                    <textarea id="ticketDesc" class="form-control" rows="3" placeholder="Chi tiết lỗi phát sinh, hình ảnh hoặc các bước tái hiện..."></textarea>
                </div>
            </div>

            <div class="modal-footer">
                <button type="button" id="cancelTicketModal" class="btn btn-secondary">Hủy</button>
                <button type="submit" class="btn btn-primary">Xác nhận tạo Ticket</button>
            </div>
        </form>
    </div>
</div>

<script src="<%= request.getContextPath() %>/js/customer/after-sales.js"></script>
</body>
</html>
