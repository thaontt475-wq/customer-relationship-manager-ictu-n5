<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cơ cấu tổ chức kinh doanh | C-CRM ENTERPRISE</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/organization/organization.css">
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
                        <span>Cơ cấu tổ chức</span>
                    </nav>
                    <h1>Cơ cấu tổ chức kinh doanh (Org Tree)</h1>
                </div>
                <div class="page-actions">
                    <button type="button" id="btnOpenAddDept" class="btn btn-primary">
                        <span>➕</span> Thêm phòng ban mới
                    </button>
                </div>
            </div>

            <div class="org-container">
                <!-- Sơ đồ cây phân cấp -->
                <div class="org-tree-card">
                    <h3 style="font-size: 1.15rem; font-weight: 700; margin-bottom: 20px;">Sơ đồ phân cấp đơn vị</h3>
                    
                    <ul class="org-tree">
                        <!-- Node Root: Tổng công ty -->
                        <li class="org-node-item">
                            <div class="org-node-card selected" data-name="Tập đoàn C-CRM Enterprise" data-code="CORP-HQ" data-leader="Nguyễn Văn Tổng" data-members="68">
                                <div class="org-node-left">
                                    <div class="org-node-icon" style="background: #e0e7ff; color: #4338ca;">🏢</div>
                                    <div>
                                        <div class="org-node-name">Tập đoàn C-CRM Enterprise</div>
                                        <div class="org-node-meta">
                                            <span>Mã: CORP-HQ</span>
                                            <span>• 68 Nhân sự</span>
                                        </div>
                                    </div>
                                </div>
                                <div class="org-node-actions">
                                    <span class="org-action-icon" title="Sửa">✏️</span>
                                </div>
                            </div>

                            <!-- Nhánh cấp 1 -->
                            <ul>
                                <li class="org-node-item">
                                    <div class="org-node-card" data-name="Khối Kinh doanh Miền Bắc" data-code="BRANCH-HN" data-leader="Hoàng Thắng" data-members="25">
                                        <div class="org-node-left">
                                            <div class="org-node-icon">💼</div>
                                            <div>
                                                <div class="org-node-name">Khối Kinh doanh Miền Bắc</div>
                                                <div class="org-node-meta">
                                                    <span>Trưởng khối: Hoàng Thắng</span>
                                                    <span>• 25 Nhân sự</span>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="org-node-actions">
                                            <span class="org-action-icon" title="Sửa">✏️</span>
                                            <span class="org-action-icon" title="Xóa" onclick="deleteDept(event, 'Khối Kinh doanh Miền Bắc', 25)">🗑️</span>
                                        </div>
                                    </div>

                                    <!-- Nhánh cấp 2 -->
                                    <ul>
                                        <li class="org-node-item">
                                            <div class="org-node-card" data-name="Phòng Sales Doanh nghiệp B2B" data-code="DEPT-B2B" data-leader="Nguyễn Thắng" data-members="12">
                                                <div class="org-node-left">
                                                    <div class="org-node-icon" style="background: #ecfdf5; color: #065f46;">👥</div>
                                                    <div>
                                                        <div class="org-node-name">Phòng Sales Doanh nghiệp B2B</div>
                                                        <div class="org-node-meta">
                                                            <span>Trưởng phòng: Nguyễn Thắng</span>
                                                            <span>• 12 Nhân sự</span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="org-node-actions">
                                                    <span class="org-action-icon" title="Sửa">✏️</span>
                                                    <span class="org-action-icon" title="Xóa" onclick="deleteDept(event, 'Phòng Sales Doanh nghiệp B2B', 12)">🗑️</span>
                                                </div>
                                            </div>
                                        </li>
                                        <li class="org-node-item">
                                            <div class="org-node-card" data-name="Tổ Chăm sóc Khách hàng (CSKH)" data-code="TEAM-CS" data-leader="Chưa chỉ định" data-members="0">
                                                <div class="org-node-left">
                                                    <div class="org-node-icon" style="background: #fffbeb; color: #92400e;">🎧</div>
                                                    <div>
                                                        <div class="org-node-name">Tổ Chăm sóc Khách hàng (CSKH)</div>
                                                        <div class="org-node-meta">
                                                            <span>Chưa có nhân sự</span>
                                                            <span>• 0 Nhân sự</span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="org-node-actions">
                                                    <span class="org-action-icon" title="Sửa">✏️</span>
                                                    <span class="org-action-icon" title="Xóa" onclick="deleteDept(event, 'Tổ Chăm sóc Khách hàng (CSKH)', 0)">🗑️</span>
                                                </div>
                                            </div>
                                        </li>
                                    </ul>
                                </li>

                                <li class="org-node-item">
                                    <div class="org-node-card" data-name="Khối Kinh doanh Miền Nam" data-code="BRANCH-HCM" data-leader="Lê Minh Trí" data-members="18">
                                        <div class="org-node-left">
                                            <div class="org-node-icon">💼</div>
                                            <div>
                                                <div class="org-node-name">Khối Kinh doanh Miền Nam</div>
                                                <div class="org-node-meta">
                                                    <span>Trưởng khối: Lê Minh Trí</span>
                                                    <span>• 18 Nhân sự</span>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="org-node-actions">
                                            <span class="org-action-icon" title="Sửa">✏️</span>
                                            <span class="org-action-icon" title="Xóa" onclick="deleteDept(event, 'Khối Kinh doanh Miền Nam', 18)">🗑️</span>
                                        </div>
                                    </div>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </div>

                <!-- Cột Chi tiết Phòng ban bên phải -->
                <div class="org-detail-card">
                    <div class="org-detail-header">
                        <span class="badge badge-purple" style="margin-bottom: 8px;">Chi tiết đơn vị</span>
                        <h2 id="detailDeptName" class="org-detail-title">Tập đoàn C-CRM Enterprise</h2>
                    </div>

                    <div style="display: flex; flex-direction: column; gap: 14px;">
                        <div>
                            <div style="font-size: 0.78rem; color: var(--text-muted);">Mã đơn vị</div>
                            <div id="detailDeptCode" style="font-weight: 700; color: var(--primary);">CORP-HQ</div>
                        </div>
                        <div>
                            <div style="font-size: 0.78rem; color: var(--text-muted);">Người phụ trách / Trưởng đơn vị</div>
                            <div id="detailDeptLeader" style="font-weight: 700;">Nguyễn Văn Tổng</div>
                        </div>
                        <div>
                            <div style="font-size: 0.78rem; color: var(--text-muted);">Quy mô nhân sự</div>
                            <div id="detailDeptMembers" style="font-weight: 700; color: var(--success-text);">68 Nhân sự</div>
                        </div>
                        <div>
                            <div style="font-size: 0.78rem; color: var(--text-muted);">Chỉ tiêu doanh số giao (KPI)</div>
                            <div style="font-weight: 700;">10.000.000.000 ₫ / Năm</div>
                        </div>
                    </div>

                    <div style="margin-top: 24px; padding-top: 18px; border-top: 1px solid var(--border-color); display: flex; flex-direction: column; gap: 10px;">
                        <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Mở danh sách nhân viên trực thuộc...')">
                            👥 Xem danh sách nhân sự
                        </button>
                        <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Cấu hình phân quyền dữ liệu...')">
                            🔒 Phân quyền nhóm dữ liệu
                        </button>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>

<!-- Modal Thêm Phòng Ban Mới -->
<div id="addDeptModal" class="modal-overlay">
    <div class="modal-dialog">
        <div class="modal-header">
            <h3 class="modal-title">Thêm Đơn vị / Phòng ban mới</h3>
            <button type="button" id="closeDeptModal" class="modal-close">&times;</button>
        </div>
        <form id="addDeptForm">
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="deptParent">Đơn vị cấp trên <span class="required">*</span></label>
                    <select id="deptParent" class="form-select" required>
                        <option value="Tập đoàn C-CRM Enterprise">Tập đoàn C-CRM Enterprise</option>
                        <option value="Khối Kinh doanh Miền Bắc">Khối Kinh doanh Miền Bắc</option>
                        <option value="Khối Kinh doanh Miền Nam">Khối Kinh doanh Miền Nam</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label" for="deptName">Tên đơn vị / Phòng ban <span class="required">*</span></label>
                    <input type="text" id="deptName" class="form-control" placeholder="Ví dụ: Nhóm Sales Khách hàng VIP..." required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="deptCode">Mã phòng ban <span class="required">*</span></label>
                    <input type="text" id="deptCode" class="form-control" placeholder="Ví dụ: TEAM-VIP-01" required>
                </div>
                <div class="form-group">
                    <label class="form-label" for="deptLeader">Người đứng đầu</label>
                    <select id="deptLeader" class="form-select">
                        <option value="Hoàng Thắng">Hoàng Thắng (Frontend Lead)</option>
                        <option value="Nguyễn Thắng">Nguyễn Thắng</option>
                        <option value="Toàn">Toàn (Backend)</option>
                    </select>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" id="cancelDeptModal" class="btn btn-secondary">Hủy</button>
                <button type="submit" class="btn btn-primary">Xác nhận tạo mới</button>
            </div>
        </form>
    </div>
</div>

<script src="<%= request.getContextPath() %>/js/organization/organization.js"></script>
</body>
</html>