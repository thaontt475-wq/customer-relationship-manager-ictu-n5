<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Lead theo Chiến dịch | CRM ICTU</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/leads/lead-campaign.css">
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
                        <span>Chiến dịch</span>
                    </nav>
                    <h1>Lead theo Chiến dịch (Campaign Tracking)</h1>
                </div>
                <div class="page-actions">
                    <button type="button" id="openAssignModal" class="btn btn-primary">
                        <span>➕</span> Gán Lead vào Chiến dịch
                    </button>
                </div>
            </div>

            <!-- Campaign Selector Bar -->
            <div class="card campaign-selector-card">
                <div class="campaign-meta-left">
                    <span style="font-weight: 700; color: var(--text-secondary);">Chọn Chiến Dịch:</span>
                    <select id="campaignSelector" class="form-select" style="width: 320px; font-weight: 600;">
                        <option value="1">Chiến dịch Ra mắt Cloud ERP 2026</option>
                        <option value="2">Hội thảo Chuyển đổi số ICTU Q3</option>
                    </select>
                    <span id="campaignCodeBadge" class="campaign-tag-badge">CAMP-2026-Q4</span>
                </div>
                <div>
                    <span style="font-size: 0.85rem; color: var(--text-muted);">Ngân sách: <strong style="color: var(--text-primary);">50.000.000 ₫</strong></span>
                </div>
            </div>

            <!-- Campaign KPI Cards -->
            <div class="campaign-kpi-grid">
                <div class="card campaign-kpi-card">
                    <div class="campaign-kpi-header">
                        <span class="campaign-kpi-sub">Tổng số Lead</span>
                        <div class="campaign-kpi-icon" style="background: var(--primary-light); color: var(--primary);">👥</div>
                    </div>
                    <div id="kpiTotalLeads" class="campaign-kpi-val" style="color: var(--primary);">128</div>
                    <span style="color: var(--success); font-size: 0.76rem; font-weight: 700;">↑ 12% so với tuần trước</span>
                </div>

                <div class="card campaign-kpi-card">
                    <div class="campaign-kpi-header">
                        <span class="campaign-kpi-sub">Đã chuyển đổi (Won)</span>
                        <div class="campaign-kpi-icon" style="background: var(--success-light); color: var(--success);">🏆</div>
                    </div>
                    <div id="kpiConverted" class="campaign-kpi-val" style="color: var(--success);">24</div>
                    <span style="color: var(--text-muted); font-size: 0.76rem;">Khách hàng ký kết</span>
                </div>

                <div class="card campaign-kpi-card">
                    <div class="campaign-kpi-header">
                        <span class="campaign-kpi-sub">Tỷ lệ chuyển đổi</span>
                        <div class="campaign-kpi-icon" style="background: var(--purple-light); color: var(--purple-text);">📈</div>
                    </div>
                    <div id="kpiConversionRate" class="campaign-kpi-val" style="color: var(--purple-text);">18.75%</div>
                    <span style="color: var(--success); font-size: 0.76rem; font-weight: 700;">Đạt mục tiêu đề ra</span>
                </div>

                <div class="card campaign-kpi-card">
                    <div class="campaign-kpi-header">
                        <span class="campaign-kpi-sub">Doanh thu mang lại</span>
                        <div class="campaign-kpi-icon" style="background: var(--warning-light); color: var(--warning-text);">💰</div>
                    </div>
                    <div class="campaign-kpi-val" style="color: #b45309;">480.000.000 ₫</div>
                    <span style="color: var(--text-muted); font-size: 0.76rem;">ROI: 9.6x</span>
                </div>
            </div>

            <!-- Leads in Campaign Table -->
            <div class="card" style="padding: 24px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <div>
                        <h3 style="font-size: 1.15rem; font-weight: 700;">Danh sách Lead thuộc chiến dịch</h3>
                        <p style="font-size: 0.82rem; color: var(--text-muted); margin-top: 2px;">Lead được đồng bộ tự động qua UTM Tracking hoặc gán thủ công</p>
                    </div>
                </div>

                <div class="crm-table-container">
                    <table class="crm-table">
                        <thead>
                            <tr>
                                <th>Mã Lead</th>
                                <th>Họ tên & Số ĐT</th>
                                <th>Doanh nghiệp</th>
                                <th>Kênh nguồn (UTM)</th>
                                <th>Điểm Lead</th>
                                <th>Giai đoạn</th>
                                <th>Sales phụ trách</th>
                                <th>Thao tác</th>
                            </tr>
                        </thead>
                        <tbody id="campaignLeadTableBody">
                            <tr>
                                <td><strong>LEAD-901</strong></td>
                                <td>
                                    <div style="font-weight: 700; color: var(--text-primary);">Đặng Tuấn Anh</div>
                                    <div style="font-size: 0.76rem; color: var(--text-muted);">0912 345 678</div>
                                </td>
                                <td>Công ty CP Đầu tư Nam Việt</td>
                                <td><span class="badge badge-info">Facebook Ads</span></td>
                                <td><span class="lead-score-pill lead-score-high">85 pts</span></td>
                                <td><span class="badge badge-purple">Tiềm năng (Warm)</span></td>
                                <td>Hoàng Thắng</td>
                                <td>
                                    <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem chi tiết Lead LEAD-901')">Xem</button>
                                </td>
                            </tr>
                            <tr>
                                <td><strong>LEAD-902</strong></td>
                                <td>
                                    <div style="font-weight: 700; color: var(--text-primary);">Phạm Thu Trang</div>
                                    <div style="font-size: 0.76rem; color: var(--text-muted);">0988 765 432</div>
                                </td>
                                <td>Logistics Toàn Cầu</td>
                                <td><span class="badge badge-info">Google Search Ads</span></td>
                                <td><span class="lead-score-pill lead-score-high">92 pts</span></td>
                                <td><span class="badge badge-success">Đã chuyển đổi (Won)</span></td>
                                <td>Nguyễn Thắng</td>
                                <td>
                                    <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem chi tiết Lead LEAD-902')">Xem</button>
                                </td>
                            </tr>
                            <tr>
                                <td><strong>LEAD-903</strong></td>
                                <td>
                                    <div style="font-weight: 700; color: var(--text-primary);">Lê Hoàng Long</div>
                                    <div style="font-size: 0.76rem; color: var(--text-muted);">0903 112 233</div>
                                </td>
                                <td>Dệt may Đông Nam</td>
                                <td><span class="badge badge-info">Web Form</span></td>
                                <td><span class="lead-score-pill lead-score-mid">70 pts</span></td>
                                <td><span class="badge badge-gray">Mới tiếp cận (New)</span></td>
                                <td>Tiến</td>
                                <td>
                                    <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem chi tiết Lead LEAD-903')">Xem</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>
</div>

<!-- Modal Gán Lead vào Campaign -->
<div id="assignLeadModalOverlay" class="modal-overlay">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3 class="modal-title">Gán Lead vào Chiến dịch</h3>
            <button type="button" id="closeAssignModal" class="modal-close">&times;</button>
        </div>
        <form id="assignLeadForm">
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="assignLeadSelect">Chọn Lead cần gán <span class="required">*</span></label>
                    <select id="assignLeadSelect" class="form-select" required>
                        <option value="Hoàng Minh Đức (0945 889 123) - FPT Retail">Hoàng Minh Đức (0945 889 123) - FPT Retail</option>
                        <option value="Trần Bích Thủy (0911 223 344) - VNPT">Trần Bích Thủy (0911 223 344) - VNPT</option>
                        <option value="Lý Gia Thành (0982 777 999) - Bất động sản An Gia">Lý Gia Thành (0982 777 999) - Bất động sản An Gia</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label" for="assignNotes">Ghi chú bổ sung</label>
                    <textarea id="assignNotes" class="form-control" rows="2" placeholder="Lý do gắn lead hoặc nguồn thông tin..."></textarea>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" id="cancelAssignModal" class="btn btn-secondary">Hủy</button>
                <button type="submit" class="btn btn-primary">Xác nhận gán</button>
            </div>
        </form>
    </div>
</div>

<script src="<%= request.getContextPath() %>/js/leads/lead-campaign.js"></script>
</body>
</html>
