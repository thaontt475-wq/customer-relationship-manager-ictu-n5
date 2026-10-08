<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Lý do Thắng/Thua & Đối thủ cạnh tranh | C-CRM ENTERPRISE</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/winloss/winloss.css">
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
                        <a href="#">Quản trị hệ thống</a>
                        <span>/</span>
                        <span>Win/Loss & Đối thủ</span>
                    </nav>
                    <h1>Cấu hình Win/Loss & Đối thủ cạnh tranh</h1>
                </div>
                <div class="page-actions">
                    <button type="button" id="btnOpenAddReason" class="btn btn-primary">
                        <span>➕</span> Thêm lý do Thắng/Thua
                    </button>
                </div>
            </div>

            <!-- Tabs Navigation -->
            <div class="wl-tab-nav">
                <button type="button" class="wl-tab-link active" data-tab="tab-reasons">
                    🎯 Danh mục Lý do Thắng / Thua
                </button>
                <button type="button" class="wl-tab-link" data-tab="tab-competitors">
                    🏢 Hồ sơ Đối thủ cạnh tranh
                </button>
            </div>

            <!-- Tab 1: Danh mục Lý do Thắng / Thua -->
            <div id="tab-reasons" class="wl-tab-content active">
                <div class="wl-columns-grid">
                    <!-- Khối Lý do Thắng -->
                    <div class="wl-column-card">
                        <div class="wl-column-header">
                            <span class="wl-column-title" style="color: var(--success-text);">
                                <span>🏆</span> Lý do chốt đơn thành công (Won)
                            </span>
                            <span class="badge badge-success">4 Lý do</span>
                        </div>
                        <div>
                            <div class="wl-list-item">
                                <div>
                                    <div style="font-weight: 700;">Giá cả cạnh tranh & Chiết khấu tốt</div>
                                    <div style="font-size: 0.75rem; color: var(--text-muted);">Khách hàng đánh giá gói giá phù hợp ngân sách</div>
                                </div>
                                <span class="badge badge-success" style="cursor: pointer;" onclick="toggleReasonStatus(this, 'Giá cả cạnh tranh')">Đang áp dụng</span>
                            </div>
                            <div class="wl-list-item">
                                <div>
                                    <div style="font-weight: 700;">Tính năng đáp ứng sát nghiệp vụ</div>
                                    <div style="font-size: 0.75rem; color: var(--text-muted);">Tính năng CRM 360 và Automation vượt trội</div>
                                </div>
                                <span class="badge badge-success" style="cursor: pointer;" onclick="toggleReasonStatus(this, 'Tính năng')">Đang áp dụng</span>
                            </div>
                            <div class="wl-list-item">
                                <div>
                                    <div style="font-weight: 700;">Dịch vụ hỗ trợ kỹ thuật 24/7 uy tín</div>
                                    <div style="font-size: 0.75rem; color: var(--text-muted);">Đội ngũ CSKH cam kết SLA phản hồi dưới 15 phút</div>
                                </div>
                                <span class="badge badge-success" style="cursor: pointer;" onclick="toggleReasonStatus(this, 'Dịch vụ')">Đang áp dụng</span>
                            </div>
                        </div>
                    </div>

                    <!-- Khối Lý do Thua -->
                    <div class="wl-column-card">
                        <div class="wl-column-header">
                            <span class="wl-column-title" style="color: var(--danger-text);">
                                <span>⚠️</span> Lý do mất đơn hàng (Lost)
                            </span>
                            <span class="badge badge-danger">4 Lý do</span>
                        </div>
                        <div>
                            <div class="wl-list-item">
                                <div>
                                    <div style="font-weight: 700;">Vượt quá ngân sách của khách hàng</div>
                                    <div style="font-size: 0.75rem; color: var(--text-muted);">Giá triển khai cao hơn khả năng chi trả</div>
                                </div>
                                <span class="badge badge-danger" style="cursor: pointer;" onclick="toggleReasonStatus(this, 'Vượt ngân sách')">Đang áp dụng</span>
                            </div>
                            <div class="wl-list-item">
                                <div>
                                    <div style="font-weight: 700;">Mất khách hàng về tay Đối thủ cạnh tranh</div>
                                    <div style="font-size: 0.75rem; color: var(--text-muted);">Đối thủ giảm giá sốc hoặc quan hệ nội bộ tốt hơn</div>
                                </div>
                                <span class="badge badge-danger" style="cursor: pointer;" onclick="toggleReasonStatus(this, 'Mất về đối thủ')">Đang áp dụng</span>
                            </div>
                            <div class="wl-list-item">
                                <div>
                                    <div style="font-weight: 700;">Khách hàng hoãn hoặc hủy dự án</div>
                                    <div style="font-size: 0.75rem; color: var(--text-muted);">Tái cơ cấu nội bộ hoặc cắt giảm kế hoạch chuyển đổi số</div>
                                </div>
                                <span class="badge badge-danger" style="cursor: pointer;" onclick="toggleReasonStatus(this, 'Hoãn dự án')">Đang áp dụng</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Tab 2: Hồ sơ Đối thủ cạnh tranh -->
            <div id="tab-competitors" class="wl-tab-content">
                <div class="comp-card-grid">
                    <div class="comp-card">
                        <div class="comp-header">
                            <div class="comp-icon">MS</div>
                            <div>
                                <h3 style="font-size: 1.05rem; font-weight: 700;">MISA AMIS CRM</h3>
                                <span style="font-size: 0.78rem; color: var(--text-muted);">Phân khúc: Doanh nghiệp Vừa và Nhỏ (SME)</span>
                            </div>
                        </div>
                        <div style="font-size: 0.85rem; line-height: 1.6; margin-bottom: 16px;">
                            <div><strong style="color: var(--success-text);">Điểm mạnh:</strong> Thương hiệu uy tín lâu năm tại VN, tích hợp sẵn hệ sinh thái kế toán MISA.</div>
                            <div style="margin-top: 6px;"><strong style="color: var(--danger-text);">Điểm yếu:</strong> Khó tùy biến quy trình sâu cho B2B Enterprise, chi phí license tính theo user khá đắt khi mở rộng.</div>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--border-color); padding-top: 12px;">
                            <span class="badge badge-warning">Đụng độ 8 Deals</span>
                            <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem chi tiết các deals thua trước đối thủ này')">Xem chi tiết</button>
                        </div>
                    </div>

                    <div class="comp-card">
                        <div class="comp-header">
                            <div class="comp-icon" style="background: #ecfdf5; color: #065f46;">FS</div>
                            <div>
                                <h3 style="font-size: 1.05rem; font-weight: 700;">Fast Business CRM</h3>
                                <span style="font-size: 0.78rem; color: var(--text-muted);">Phân khúc: Doanh nghiệp Sản xuất & Bán buôn</span>
                            </div>
                        </div>
                        <div style="font-size: 0.85rem; line-height: 1.6; margin-bottom: 16px;">
                            <div><strong style="color: var(--success-text);">Điểm mạnh:</strong> Quản lý kho vận và công nợ bán hàng chặt chẽ.</div>
                            <div style="margin-top: 6px;"><strong style="color: var(--danger-text);">Điểm yếu:</strong> Giao diện cổ điển, tính năng Lead & Marketing Automation hạn chế.</div>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--border-color); padding-top: 12px;">
                            <span class="badge badge-warning">Đụng độ 4 Deals</span>
                            <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem chi tiết')">Xem chi tiết</button>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>

<!-- Modal Thêm Lý Do Thắng / Thua -->
<div id="addReasonModal" class="modal-overlay">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3 class="modal-title">Thêm Lý do Thắng / Thua mới</h3>
            <button type="button" id="closeReasonModal" class="modal-close">&times;</button>
        </div>
        <form id="addReasonForm">
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="reasonType">Phân loại <span class="required">*</span></label>
                    <select id="reasonType" class="form-select" required>
                        <option value="WIN">Thắng đơn hàng (Won)</option>
                        <option value="LOSS">Thất bại / Mất đơn hàng (Lost)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label" for="reasonName">Tên lý do <span class="required">*</span></label>
                    <input type="text" id="reasonName" class="form-control" placeholder="Ví dụ: Khách hàng cắt giảm ngân sách..." required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="reasonDesc">Mô tả chi tiết</label>
                    <textarea id="reasonDesc" class="form-control" rows="2" placeholder="Ghi chú thêm về lý do này..."></textarea>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" id="cancelReasonModal" class="btn btn-secondary">Hủy</button>
                <button type="submit" class="btn btn-primary">Xác nhận tạo</button>
            </div>
        </form>
    </div>
</div>

<script src="<%= request.getContextPath() %>/js/winloss/winloss.js"></script>
</body>
</html>