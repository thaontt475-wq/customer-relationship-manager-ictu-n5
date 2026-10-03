<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.crm.model.CustomField,com.crm.controller.ServerForms" %>
<%!
private String esc(Object raw) {
    if (raw == null) return "";
    return raw.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
private String options(CustomField f) { return f == null ? "" : String.join("\n", f.getOptions()); }
private String check(boolean value) { return value ? " checked" : ""; }
%>
<%
if (request.getAttribute("fields") == null) { response.sendRedirect(request.getContextPath()+"/customfields/page"); return; }
List<CustomField> fields = (List<CustomField>)request.getAttribute("fields");
CustomField edit = (CustomField) request.getAttribute("editField");
String entity = (String)request.getAttribute("entity");
String prefix = request.getContextPath();
%>
<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Trường tùy chỉnh - CRM</title>
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/common.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/layout.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/header.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/sidebar.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/components.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/customfields/customfields.css">
<style>
.cf-main{max-width:1150px;width:100%;padding:20px;margin:0 auto}.cf-card{border:1px solid #aaa;border-radius:7px;margin:16px 0;padding:18px}.cf-fields{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px}.cf-fields label{display:flex;flex-direction:column;gap:6px}.cf-fields input,.cf-fields textarea,.cf-fields select{padding:8px;border:1px solid #999;border-radius:5px;background:inherit;color:inherit}.cf-table-wrap{overflow-x:auto}.cf-table{border-collapse:collapse;width:100%}.cf-table th,.cf-table td{border-bottom:1px solid #bbb;text-align:left;padding:8px}.cf-btn{display:inline-block;border:1px solid #999;padding:7px 12px;border-radius:5px;background:inherit;color:inherit;text-decoration:none;cursor:pointer}.cf-primary{background:#174f7a;color:white}.cf-actions{display:flex;gap:8px;flex-wrap:wrap;align-items:center}.cf-checks{display:flex;gap:16px;flex-wrap:wrap;margin:12px 0}.cf-notice{padding:12px;border:1px solid #397d43}.cf-muted{opacity:.8}
</style></head>
<body class="crm-body"><jsp:include page="/jsp/shared/header.jsp"/><div class="crm-main-layout"><jsp:include page="/jsp/shared/sidebar.jsp"/>
<main class="crm-page cf-main">
<a href="<%=esc(prefix)%>/dashboard">Trang chủ</a> / Trường tùy chỉnh
<h1>Trường tùy chỉnh</h1>
<p>Quản lý trường của Khách hàng và Cơ hội bán hàng bằng biểu mẫu HTML, không dùng JavaScript.</p>
<% if (request.getAttribute("notice") != null) { %><p class="cf-notice" role="status"><%=esc(request.getAttribute("notice"))%></p><% } %>
<nav class="cf-actions" aria-label="Đối tượng áp dụng">
<a class="cf-btn <%= "CUSTOMER".equals(entity)?"cf-primary":"" %>" href="<%=esc(prefix)%>/customfields/page?entity=CUSTOMER">Khách hàng</a>
<a class="cf-btn <%= "OPPORTUNITY".equals(entity)?"cf-primary":"" %>" href="<%=esc(prefix)%>/customfields/page?entity=OPPORTUNITY">Cơ hội bán hàng</a>
</nav>
<section class="cf-card"><h2>Danh sách trường</h2><div class="cf-table-wrap"><table class="cf-table"><thead><tr><th>Thứ tự</th><th>Mã</th><th>Nhãn</th><th>Kiểu</th><th>Bắt buộc</th><th>Trạng thái</th><th>Có dữ liệu</th><th>Thao tác</th></tr></thead><tbody>
<% for (CustomField row:fields) { %>
<tr><td><%=row.getSortOrder()%></td><td><%=esc(row.getFieldName())%></td><td><%=esc(row.getFieldLabel())%></td><td><%=esc(row.getFieldType())%></td><td><%=row.isRequired()?"Có":"Không"%></td><td><%=row.isActive()?"Hoạt động":"Ngừng"%></td><td><%=row.getUsageCount()%></td><td>
<a href="<%=esc(prefix)%>/customfields/page?entity=<%=esc(entity)%>&amp;edit=<%=row.getId()%>">Sửa</a>
<form action="<%=esc(prefix)%>/customfields/page" method="post" class="cf-actions">
<input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>">
<input type="hidden" name="action" value="delete"><input type="hidden" name="entity" value="<%=esc(entity)%>"><input type="hidden" name="id" value="<%=row.getId()%>">
<label><input type="checkbox" name="confirm" value="yes" required> Xác nhận <%=row.getUsageCount()>0?"ngừng kích hoạt":"xóa"%></label><button type="submit" class="cf-btn">Thực hiện</button>
</form></td></tr><% } %>
<% if (fields.isEmpty()) { %><tr><td colspan="8">Chưa có trường tùy chỉnh.</td></tr><% } %>
</tbody></table></div></section>
<section class="cf-card"><h2><%=edit==null?"Thêm trường mới":"Sửa trường"%></h2>
<%if(edit!=null){%><p class="cf-muted">Mã hệ thống và đối tượng áp dụng không được sửa sau khi tạo. Nếu trường đã có dữ liệu, không thể đổi kiểu hay các tùy chọn.</p><%}%>
<form method="post" action="<%=esc(prefix)%>/customfields/page">
<input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>">
<input type="hidden" name="action" value="<%=edit==null?"create":"update"%>">
<input type="hidden" name="entity" value="<%=esc(entity)%>">
<%if(edit!=null){%><input type="hidden" name="id" value="<%=edit.getId()%>"><%}%>
<div class="cf-fields">
<label>Mã hệ thống<input name="fieldName" pattern="[a-z][a-z0-9_]*" maxlength="100" required value="<%=esc(edit==null?"":edit.getFieldName())%>" <%=edit==null?"":"readonly"%>></label>
<label>Nhãn hiển thị<input name="fieldLabel" maxlength="255" required value="<%=esc(edit==null?"":edit.getFieldLabel())%>"></label>
<label>Kiểu trường<select name="fieldType">
<%for(String t:new String[]{"TEXT","NUMBER","DATE","DROPDOWN"}){%><option value="<%=t%>" <%=edit!=null&&t.equals(edit.getFieldType())?"selected":""%>><%=esc(t)%></option><%}%>
</select></label>
<label>Thứ tự hiển thị<input name="sortOrder" type="number" min="0" max="100000" required value="<%=edit==null?"0":edit.getSortOrder()%>"></label>
<label style="grid-column:1/-1">Tùy chọn danh sách (mỗi dòng một giá trị, cần ít nhất hai cho kiểu DROPDOWN)
<textarea name="options" rows="4"><%=esc(options(edit))%></textarea></label>
</div><div class="cf-checks">
<label><input type="checkbox" name="required" <%=check(edit!=null&&edit.isRequired())%>> Bắt buộc</label>
<label><input type="checkbox" name="active" <%=check(edit==null||edit.isActive())%>> Hoạt động</label>
<label><input type="checkbox" name="inForm" <%=check(edit==null||edit.isInForm())%>> Trên biểu mẫu</label>
<label><input type="checkbox" name="inFilter" <%=check(edit==null||edit.isInFilter())%>> Trong bộ lọc</label>
<label><input type="checkbox" name="inExport" <%=check(edit==null||edit.isInExport())%>> Trong Excel</label>
</div><div class="cf-actions"><button class="cf-btn cf-primary" type="submit">Lưu</button><a class="cf-btn" href="<%=esc(prefix)%>/customfields/page?entity=<%=esc(entity)%>">Làm mới / Hủy sửa</a></div>
</form></section>
</main></div></body></html>
