<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.crm.dto.users.UserImportResult" %>
<%@ page import="com.crm.dto.users.UserImportRow" %>
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
    String errorMessage = (String) request.getAttribute("errorMessage");
    UserImportResult previewResult = (UserImportResult) request.getAttribute("previewResult");
    UserImportResult importReport = (UserImportResult) request.getAttribute("importReport");
    String fileName = (String) request.getAttribute("fileName");

    // Xác định bước hiện tại: 1 - Upload, 2 - Preview, 3 - Report
    int currentStep = 1;
    if (importReport != null && importReport.isExecuted()) {
        currentStep = 3;
    } else if (previewResult != null) {
        currentStep = 2;
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nhập người dùng hàng loạt từ Excel - CRM</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Import Excel (S2-01) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/user-import.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính của màn hình Nhập người dùng từ Excel -->
        <main class="import-page" id="importApp" role="main">
            <div class="import-container">

                <!-- Breadcrumb điều hướng -->
                <nav class="import-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <a href="${pageContext.request.contextPath}/users">Quản lý người dùng</a>
                    <span class="separator">/</span>
                    <span class="active">Nhập hàng loạt từ Excel</span>
                </nav>

                <!-- Header màn hình -->
                <header class="import-header">
                    <div class="import-header-info">
                        <h1>Nhập danh sách người dùng từ tệp Excel</h1>
                        <p>Tạo tài khoản người dùng hàng loạt cho khối kinh doanh từ bảng tính Excel (.xlsx hoặc .csv).</p>
                    </div>

                    <div class="import-header-badges">
                        <span class="import-badge-feature" title="User Story S2-01">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                                <line x1="16" y1="13" x2="8" y2="13"></line>
                                <line x1="16" y1="17" x2="8" y2="17"></line>
                                <polyline points="10 9 9 9 8 9"></polyline>
                            </svg>
                            S2-01 / Nhập người dùng Excel
                        </span>
                    </div>
                </header>

                <!-- Stepper 3 bước -->
                <nav class="import-stepper" aria-label="Các bước thực hiện">
                    <div class="import-step <%= currentStep >= 1 ? "active" : "" %> <%= currentStep > 1 ? "completed" : "" %>">
                        <div class="import-step-circle"><%= currentStep > 1 ? "✓" : "1" %></div>
                        <div class="import-step-info">
                            <span class="import-step-title">Tải lên tệp Excel</span>
                            <span class="import-step-desc">Tải mẫu & chọn tệp</span>
                        </div>
                    </div>

                    <div class="import-step-divider"></div>

                    <div class="import-step <%= currentStep >= 2 ? "active" : "" %> <%= currentStep > 2 ? "completed" : "" %>">
                        <div class="import-step-circle"><%= currentStep > 2 ? "✓" : "2" %></div>
                        <div class="import-step-info">
                            <span class="import-step-title">Xem trước & Kiểm tra</span>
                            <span class="import-step-desc">Phát hiện lỗi theo từng dòng</span>
                        </div>
                    </div>

                    <div class="import-step-divider"></div>

                    <div class="import-step <%= currentStep == 3 ? "active completed" : "" %>">
                        <div class="import-step-circle">3</div>
                        <div class="import-step-info">
                            <span class="import-step-title">Báo cáo kết quả</span>
                            <span class="import-step-desc">Tổng kết hợp lệ & lỗi</span>
                        </div>
                    </div>
                </nav>

                <!-- Hiển thị thông báo lỗi hệ thống nếu có -->
                <% if (errorMessage != null && !errorMessage.isBlank()) { %>
                    <div class="import-alert import-alert-danger" role="alert">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        <div>
                            <strong>Đã xảy ra lỗi:</strong> <%= escapeHtml(errorMessage) %>
                        </div>
                    </div>
                <% } %>

                <!-- ==================================================================== -->
                <!-- GIAI ĐOẠN 1: TẢI FILE MẪU & TẢI LÊN TỆP EXCEL                       -->
                <!-- ==================================================================== -->
                <% if (currentStep == 1) { %>
                    <!-- Khung Tải file Excel mẫu -->
                    <section class="import-card" aria-label="Tải tệp mẫu">
                        <h2 class="import-card-title">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                <polyline points="7 10 12 15 17 10"></polyline>
                                <line x1="12" y1="15" x2="12" y2="3"></line>
                            </svg>
                            Bước 1: Tải tệp Excel mẫu chuẩn
                        </h2>

                        <div class="import-template-box">
                            <div class="import-template-info">
                                <div class="import-excel-icon" aria-hidden="true">XLS</div>
                                <div class="import-template-text">
                                    <h4>Tệp Excel mẫu nhập người dùng</h4>
                                    <p>Bao gồm các cột chuẩn: Tên đăng nhập, Họ và tên, Email, Số điện thoại, Vai trò, Nhóm kinh doanh, Phạm vi dữ liệu, Mật khẩu.</p>
                                </div>
                            </div>

                            <div class="import-template-actions">
                                <a href="${pageContext.request.contextPath}/users/import/template"
                                   class="import-btn import-btn-excel"
                                   id="btnDownloadXlsx">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                        <polyline points="7 10 12 15 17 10"></polyline>
                                        <line x1="12" y1="15" x2="12" y2="3"></line>
                                    </svg>
                                    Tải mẫu Excel (.xlsx)
                                </a>

                                <a href="${pageContext.request.contextPath}/users/import/template?format=csv"
                                   class="import-btn import-btn-secondary"
                                   id="btnDownloadCsv">
                                    Tải mẫu CSV
                                </a>
                            </div>
                        </div>
                    </section>

                    <!-- Khung Upload tệp Excel -->
                    <section class="import-card" aria-label="Tải lên tệp">
                        <h2 class="import-card-title">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                <polyline points="17 8 12 3 7 8"></polyline>
                                <line x1="12" y1="3" x2="12" y2="15"></line>
                            </svg>
                            Bước 2: Tải lên tệp Excel của bạn
                        </h2>

                        <form action="${pageContext.request.contextPath}/users/import/preview"
                              method="post"
                              enctype="multipart/form-data"
                              id="uploadForm">

                            <label class="import-dropzone" for="excelFileInput">
                                <svg class="import-dropzone-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <path d="M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242"></path>
                                    <path d="M12 12v9"></path>
                                    <path d="m16 16-4-4-4 4"></path>
                                </svg>
                                <div class="import-dropzone-text">Nhấp vào đây để chọn tệp hoặc kéo thả tệp vào khung này</div>
                                <div class="import-dropzone-hint">Hỗ trợ tệp định dạng Microsoft Excel (.xlsx, .xls) và bảng tính (.csv) dung lượng tối đa 10MB</div>

                                <input type="file"
                                       name="file"
                                       id="excelFileInput"
                                       class="import-file-input"
                                       accept=".xlsx, .xls, .csv"
                                       required
                                       onchange="handleFileSelect(this)">

                                <div class="import-selected-file" id="selectedFileInfo" style="display: none;">
                                    <span>Tệp đã chọn:</span>
                                    <strong id="selectedFileName"></strong>
                                </div>
                            </label>

                            <div style="margin-top: 20px; display: flex; justify-content: flex-end; gap: 12px;">
                                <a href="${pageContext.request.contextPath}/users" class="import-btn import-btn-secondary">
                                    Hủy bỏ
                                </a>
                                <button type="submit" class="import-btn import-btn-primary" id="btnPreview">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <circle cx="11" cy="11" r="8"></circle>
                                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                    </svg>
                                    Tải lên & Xem trước dữ liệu
                                </button>
                            </div>
                        </form>
                    </section>
                <% } %>

                <!-- ==================================================================== -->
                <!-- GIAI ĐOẠN 2: MÀN HÌNH XEM TRƯỚC (PREVIEW) & KIỂM TRA LỖI THEO DÒNG    -->
                <!-- ==================================================================== -->
                <% if (currentStep == 2 && previewResult != null) { %>
                    <section class="import-card" aria-label="Xem trước dữ liệu">
                        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
                            <h2 class="import-card-title">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                    <circle cx="12" cy="12" r="3"></circle>
                                </svg>
                                Màn hình xem trước dữ liệu (Tệp: <%= escapeHtml(fileName) %>)
                            </h2>

                            <a href="${pageContext.request.contextPath}/users/import?clear=1" class="import-btn import-btn-secondary">
                                Tải lại tệp khác
                            </a>
                        </div>

                        <!-- Thống kê tổng hợp trước khi nhập -->
                        <div class="import-stats-grid">
                            <div class="import-stat-card total">
                                <span class="import-stat-label">Tổng số dòng phát hiện</span>
                                <span class="import-stat-value"><%= previewResult.getTotalRows() %></span>
                            </div>

                            <div class="import-stat-card valid">
                                <span class="import-stat-label">Dòng hợp lệ (Sẽ được nhập)</span>
                                <span class="import-stat-value"><%= previewResult.getValidRows() %></span>
                            </div>

                            <div class="import-stat-card error">
                                <span class="import-stat-label">Dòng lỗi (Sẽ bị bỏ qua)</span>
                                <span class="import-stat-value"><%= previewResult.getErrorRows() %></span>
                            </div>
                        </div>

                        <!-- Bảng xem trước dữ liệu -->
                        <div class="import-table-wrap">
                            <table class="import-table" aria-label="Bảng xem trước dữ liệu người dùng">
                                <thead>
                                    <tr>
                                        <th style="width: 50px;">Dòng</th>
                                        <th>Tên đăng nhập</th>
                                        <th>Họ và tên</th>
                                        <th>Email</th>
                                        <th>Số điện thoại</th>
                                        <th>Vai trò</th>
                                        <th>Nhóm</th>
                                        <th>Phạm vi</th>
                                        <th style="width: 140px;">Trạng thái</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% List<UserImportRow> rows = previewResult.getRows(); %>
                                    <% if (rows != null && !rows.isEmpty()) { %>
                                        <% for (UserImportRow r : rows) { %>
                                            <tr class="<%= r.isValid() ? "row-valid" : "row-error" %>" id="row_<%= r.getRowNumber() %>">
                                                <td><strong>#<%= r.getRowNumber() %></strong></td>
                                                <td><%= escapeHtml(r.getUsername()) %></td>
                                                <td><strong><%= escapeHtml(r.getFullName()) %></strong></td>
                                                <td><%= escapeHtml(r.getEmail()) %></td>
                                                <td><%= escapeHtml(r.getPhone() != null ? r.getPhone() : "—") %></td>
                                                <td><span style="font-weight: 600;"><%= escapeHtml(r.getRoleName()) %></span></td>
                                                <td><%= escapeHtml(r.getTeamName() != null && !r.getTeamName().isBlank() ? r.getTeamName() : "—") %></td>
                                                <td><code><%= escapeHtml(r.getDataScope()) %></code></td>
                                                <td>
                                                    <% if (r.isValid()) { %>
                                                        <span class="import-tag import-tag-success">
                                                            ✓ Hợp lệ
                                                        </span>
                                                    <% } else { %>
                                                        <span class="import-tag import-tag-danger">
                                                            ✗ Lỗi
                                                        </span>
                                                        <ul class="import-error-list">
                                                            <% for (String err : r.getErrorMessages()) { %>
                                                                <li><%= escapeHtml(err) %></li>
                                                            <% } %>
                                                        </ul>
                                                    <% } %>
                                                </td>
                                            </tr>
                                        <% } %>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>

                        <!-- Form xác nhận thực thi -->
                        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-top: 10px;">
                            <span style="font-size: 14px; color: var(--import-text-muted);">
                                * Lưu ý: Các dòng lỗi sẽ tự động được bỏ qua an toàn mà không ảnh hưởng đến các dòng hợp lệ.
                            </span>

                            <form action="${pageContext.request.contextPath}/users/import/execute" method="post">
                                <button type="submit"
                                        class="import-btn import-btn-success"
                                        id="btnConfirmImport"
                                        <%= previewResult.getValidRows() == 0 ? "disabled" : "" %>>
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                        <polyline points="20 6 9 17 4 12"></polyline>
                                    </svg>
                                    Xác nhận nhập <%= previewResult.getValidRows() %> người dùng hợp lệ
                                </button>
                            </form>
                        </div>
                    </section>
                <% } %>

                <!-- ==================================================================== -->
                <!-- GIAI ĐOẠN 3: BÁO CÁO TỔNG KẾT SAU KHI XỬ LÝ (REPORT)                  -->
                <!-- ==================================================================== -->
                <% if (currentStep == 3 && importReport != null) { %>
                    <section class="import-card" aria-label="Báo cáo kết quả nhập">
                        <div class="import-alert import-alert-success" role="status">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                                <polyline points="22 4 12 14.01 9 11.01"></polyline>
                            </svg>
                            <div>
                                <h3 style="margin: 0 0 4px 0; font-size: 16px; font-weight: 700;">Đã hoàn thành nhập danh sách người dùng!</h3>
                                <div>Hệ thống đã xử lý xong tệp dữ liệu. Tài khoản hợp lệ đã được khởi tạo an toàn vào cơ sở dữ liệu.</div>
                            </div>
                        </div>

                        <!-- Thẻ thống kê Báo cáo -->
                        <div class="import-stats-grid">
                            <div class="import-stat-card total">
                                <span class="import-stat-label">Tổng số dòng trong tệp</span>
                                <span class="import-stat-value"><%= importReport.getTotalRows() %></span>
                            </div>

                            <div class="import-stat-card success">
                                <span class="import-stat-label">Số dòng hợp lệ (Đã tạo thành công)</span>
                                <span class="import-stat-value"><%= importReport.getImportedCount() %></span>
                            </div>

                            <div class="import-stat-card error">
                                <span class="import-stat-label">Số dòng lỗi (Đã bỏ qua)</span>
                                <span class="import-stat-value"><%= importReport.getSkippedCount() %></span>
                            </div>
                        </div>

                        <!-- Chi tiết các dòng bị bỏ qua do lỗi (nếu có) -->
                        <% List<UserImportRow> failedRows = importReport.getFailedRows(); %>
                        <% if (failedRows != null && !failedRows.isEmpty()) { %>
                            <div style="margin-top: 10px;">
                                <h3 style="font-size: 16px; font-weight: 700; color: var(--import-danger); margin-bottom: 10px;">
                                    Chi tiết các dòng bị bỏ qua do lỗi (<%= failedRows.size() %> dòng)
                                </h3>

                                <div class="import-table-wrap">
                                    <table class="import-table" aria-label="Bảng các dòng bị lỗi">
                                        <thead>
                                            <tr>
                                                <th style="width: 50px;">Dòng</th>
                                                <th>Tên đăng nhập</th>
                                                <th>Email</th>
                                                <th>Họ và tên</th>
                                                <th>Chi tiết lỗi phát hiện</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <% for (UserImportRow fr : failedRows) { %>
                                                <tr class="row-error">
                                                    <td><strong>#<%= fr.getRowNumber() %></strong></td>
                                                    <td><%= escapeHtml(fr.getUsername()) %></td>
                                                    <td><%= escapeHtml(fr.getEmail()) %></td>
                                                    <td><%= escapeHtml(fr.getFullName()) %></td>
                                                    <td>
                                                        <ul class="import-error-list">
                                                            <% for (String err : fr.getErrorMessages()) { %>
                                                                <li><%= escapeHtml(err) %></li>
                                                            <% } %>
                                                        </ul>
                                                    </td>
                                                </tr>
                                            <% } %>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        <% } %>

                        <!-- Nút điều hướng sau khi xong -->
                        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 10px;">
                            <a href="${pageContext.request.contextPath}/users/import?clear=1" class="import-btn import-btn-secondary">
                                Nhập tiếp tệp khác
                            </a>
                            <a href="${pageContext.request.contextPath}/users" class="import-btn import-btn-primary" id="btnBackToUsers">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                    <circle cx="9" cy="7" r="4"></circle>
                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                </svg>
                                Về danh sách người dùng
                            </a>
                        </div>
                    </section>
                <% } %>

            </div>
        </main>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp" />

    <script>
        function handleFileSelect(input) {
            if (input.files && input.files[0]) {
                var file = input.files[0];
                var nameDisplay = document.getElementById('selectedFileName');
                var infoBox = document.getElementById('selectedFileInfo');
                if (nameDisplay && infoBox) {
                    nameDisplay.textContent = file.name + ' (' + (file.size / 1024).toFixed(1) + ' KB)';
                    infoBox.style.display = 'inline-flex';
                }
            }
        }
    </script>
</body>
</html>
