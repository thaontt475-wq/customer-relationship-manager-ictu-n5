<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.dto.excel.ImportReportResult, com.crm.dto.excel.ImportRowData, com.crm.dto.excel.ImportErrorDetail" %>
<%!
 private String esc(Object obj) {
     String s = obj == null ? "" : obj.toString();
     return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
             .replace("\"","&quot;").replace("'","&#39;");
 }
%>
<%
 ImportReportResult report = (ImportReportResult) request.getAttribute("report");
 boolean completed = Boolean.TRUE.equals(request.getAttribute("completed"));
 String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Nhập người dùng từ Excel - CRM</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/user-import.css">
<style>
.import-native{max-width:1050px;margin:20px auto;padding:24px}.import-native .panel{padding:20px;margin:18px 0;background:var(--card-bg,#fff);border:1px solid #aaa;border-radius:10px}.import-native table{width:100%;border-collapse:collapse}.import-native td,.import-native th{padding:9px;border-bottom:1px solid #aaa;text-align:left;vertical-align:top}.import-native .scroll{overflow-x:auto}.import-native button,.import-native a.action{display:inline-block;padding:10px 15px;margin:8px 8px 8px 0}.import-native .error{border-left:4px solid #a33;padding:12px}
</style></head><body class="crm-body users-admin">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout"><jsp:include page="/jsp/shared/sidebar.jsp"/>
<main class="import-native crm-page"><nav><a href="${pageContext.request.contextPath}/users">Quản lý người dùng</a> / Nhập Excel</nav>
<h1>Nhập danh sách người dùng từ Excel</h1>
<p>Xem trước và sửa các dòng lỗi trong tệp trước khi xác nhận. Tất cả thao tác chạy trên máy chủ, không cần JavaScript.</p>
<% if (error != null) { %><p class="error" role="alert"><%=esc(error)%></p><% } %>
<section class="panel"><h2>Bước 1: Tải tệp mẫu và chọn dữ liệu</h2>
<p><a class="action" href="${pageContext.request.contextPath}/api/users/import/template?format=xlsx">Tải mẫu XLSX</a>
<a class="action" href="${pageContext.request.contextPath}/api/users/import/template?format=csv">Tải mẫu CSV</a></p>
<form action="${pageContext.request.contextPath}/users/import/preview" method="post" enctype="multipart/form-data">
<input type="hidden" name="csrfToken" value="<%=esc(com.crm.controller.ServerForms.csrf(request))%>">
<label for="file">Tệp Excel/CSV</label> <input id="file" name="file" type="file" accept=".xlsx,.xls,.csv" required>
<button type="submit">Tải lên và xem trước</button></form></section>
<% if (report != null) { %><section class="panel">
<h2><%= completed ? "Bước 3: Kết quả nhập" : "Bước 2: Xem trước" %></h2>
<p>Tổng dòng: <strong><%=report.getTotalRows()%></strong> · Hợp lệ/thành công: <strong><%=completed ? report.getSuccessRows() : report.getValidCount()%></strong> · Lỗi/bỏ qua: <strong><%=completed ? report.getFailedRows() : report.getErrorCount()%></strong></p>
<% if (!completed) { %>
<% if (report.getValidRows()!=null && !report.getValidRows().isEmpty()) { %>
<h3>Dòng hợp lệ</h3><div class="scroll"><table><thead><tr><th>Dòng</th><th>Họ tên</th><th>Email</th><th>Tài khoản</th><th>Vai trò</th><th>Nhóm</th></tr></thead><tbody>
<% for (ImportRowData row : report.getValidRows()) { %><tr><td><%=row.getRowNum()%></td><td><%=esc(row.getFullName())%></td><td><%=esc(row.getEmail())%></td><td><%=esc(row.getUsername())%></td><td><%=esc(row.getRole())%></td><td><%=esc(row.getTeam())%></td></tr><% } %>
</tbody></table></div><% } %>
<% if (report.getErrors()!=null && !report.getErrors().isEmpty()) { %>
<h3>Lỗi cần kiểm tra</h3><div class="scroll"><table><thead><tr><th>Dòng</th><th>Trường</th><th>Thông báo</th></tr></thead><tbody>
<% for (ImportErrorDetail row : report.getErrors()) { %><tr><td><%=row.getRowNumber()%></td><td><%=esc(row.getField())%></td><td><%=esc(row.getMessage())%></td></tr><% } %>
</tbody></table></div><% } %>
<% if (report.getValidCount()>0) { %>
<form action="${pageContext.request.contextPath}/users/import/execute" method="post">
<input type="hidden" name="csrfToken" value="<%=esc(com.crm.controller.ServerForms.csrf(request))%>">
<p>Chỉ các dòng hợp lệ sẽ được nhập. Dòng lỗi bị bỏ qua.</p><button type="submit">Xác nhận nhập các dòng hợp lệ</button></form>
<% } else { %><p>Không có dòng hợp lệ. Hãy chỉnh sửa tệp rồi tải lại.</p><% } %>
<% } else { %><p>Quá trình nhập đã kết thúc. Kiểm tra tài khoản vừa tạo trong danh sách người dùng.</p><% } %>
</section><% } %>
<p><a href="${pageContext.request.contextPath}/users">Về quản lý người dùng</a></p></main></div>
<jsp:include page="/jsp/shared/footer.jsp"/></body></html>
