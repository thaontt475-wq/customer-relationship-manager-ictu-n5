<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.net.URLEncoder,java.nio.charset.StandardCharsets,com.crm.model.Category,com.crm.model.CategoryType,com.crm.controller.ServerForms" %>
<%!
private String esc(Object value) {
    if (value == null) return "";
    return value.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
private String url(Object value) { return URLEncoder.encode(value == null ? "" : value.toString(),StandardCharsets.UTF_8); }
%>
<%
if (request.getAttribute("categories") == null) {response.sendRedirect(request.getContextPath()+"/configuration/page");return;}
List<Category> categories = (List<Category>)request.getAttribute("categories");
CategoryType type = (CategoryType)request.getAttribute("currentType");
Category edit = (Category)request.getAttribute("editCategory");
String q = (String)request.getAttribute("keyword");
String status = (String)request.getAttribute("statusFilter");
boolean manage = Boolean.TRUE.equals(request.getAttribute("canManage"));
String prefix = request.getContextPath();
%>
<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Danh mục dùng chung - CRM</title>
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/common.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/layout.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/header.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/sidebar.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/components.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/configuration/configuration.css">
<style>
.category-shell{width:100%;max-width:1180px;margin:0 auto;padding:20px}.category-box{border:1px solid #bbb;padding:18px;border-radius:8px;margin:18px 0;background:var(--card-bg,#fff);color:var(--text-color,#222)}
.category-flex{display:flex;flex-wrap:wrap;gap:12px;align-items:center}.category-fields{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:14px}.category-fields label{display:flex;flex-direction:column;gap:5px}
.category-fields input,.category-fields select,.category-fields textarea{padding:8px;border:1px solid #999;border-radius:5px;background:inherit;color:inherit;max-width:100%}.category-btn{border:1px solid #888;padding:8px 14px;border-radius:5px;background:inherit;color:inherit;cursor:pointer}
.category-btn.primary{background:#164c78;color:#fff}.category-scroll{overflow-x:auto}.category-table{width:100%;border-collapse:collapse}.category-table th,.category-table td{padding:10px;border-bottom:1px solid #ccc;text-align:left}
.category-notice{padding:12px;border-left:4px solid green;background:#e8f5e9;color:#234d2b}.category-delete{display:inline-flex;gap:6px;align-items:center;flex-wrap:wrap}
</style></head><body class="crm-body"><jsp:include page="/jsp/shared/header.jsp"/><div class="crm-main-layout"><jsp:include page="/jsp/shared/sidebar.jsp"/>
<main class="crm-page category-shell"><a href="<%=esc(prefix)%>/dashboard">Trang chủ</a> / Danh mục dùng chung
<h1>Danh mục dùng chung</h1><p>Quản lý danh mục bằng biểu mẫu HTML, không sử dụng JavaScript.</p>
<%if(request.getAttribute("notice")!=null){%><p role="status" class="category-notice"><%=esc(request.getAttribute("notice"))%></p><%}%>
<nav class="category-flex" aria-label="Nhóm danh mục">
<%for(CategoryType item: CategoryType.values()) {%>
<a class="category-btn <%=item == type ? "primary" : ""%>" href="<%=esc(prefix)%>/configuration/page?type=<%=item.name()%>"><%=esc(item.getDisplayName())%></a>
<%}%></nav>
<section class="category-box"><h2><%=esc(type.getDisplayName())%></h2>
<form method="get" action="<%=esc(prefix)%>/configuration/page" class="category-fields">
<input type="hidden" name="type" value="<%=type.name()%>"><label>Tìm mã, tên hoặc mô tả<input name="q" type="search" maxlength="150" value="<%=esc(q)%>"></label>
<label>Trạng thái<select name="active"><option value="">Tất cả</option><option value="true" <%= "true".equals(status) ? "selected" : "" %>>Đang sử dụng</option><option value="false" <%= "false".equals(status) ? "selected" : "" %>>Ngừng sử dụng</option></select></label>
<div class="category-flex"><button type="submit" class="category-btn primary">Lọc</button><a href="<%=esc(prefix)%>/configuration/page?type=<%=type.name()%>">Xóa lọc</a></div></form>
<div class="category-scroll"><table class="category-table"><thead><tr><th>Thứ tự</th><th>Mã</th><th>Tên</th><th>Mô tả</th><th>Trạng thái</th><%if(manage){%><th>Thao tác</th><%}%></tr></thead><tbody>
<%for(Category row:categories){%><tr><td><%=row.getDisplayOrder()%></td><td><%=esc(row.getCode())%></td><td><%=esc(row.getName())%></td><td><%=esc(row.getDescription())%></td><td><%=row.isActive()?"Đang sử dụng":"Ngừng sử dụng"%></td>
<%if(manage){%><td><a href="<%=esc(prefix)%>/configuration/page?type=<%=type.name()%>&edit=<%=row.getId()%>&q=<%=esc(url(q))%>&active=<%=esc(url(status))%>">Sửa</a>
<form class="category-delete" method="post" action="<%=esc(prefix)%>/configuration/page">
<input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>"><input type="hidden" name="action" value="delete"><input type="hidden" name="type" value="<%=type.name()%>"><input type="hidden" name="id" value="<%=row.getId()%>">
<label><input type="checkbox" name="confirm" value="yes" required>Xác nhận xóa</label><button type="submit" class="category-btn">Xóa</button></form></td><%}%></tr><%}%>
<%if(categories.isEmpty()){%><tr><td colspan="6">Chưa có danh mục phù hợp.</td></tr><%}%>
</tbody></table></div></section>
<%if(manage){%><section class="category-box"><h2><%=edit == null ? "Thêm danh mục" : "Sửa danh mục"%></h2>
<form method="post" action="<%=esc(prefix)%>/configuration/page" class="category-fields">
<input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>"><input type="hidden" name="action" value="<%=edit == null ? "create" : "update"%>"><input type="hidden" name="type" value="<%=type.name()%>">
<%if(edit!=null){%><input type="hidden" name="id" value="<%=edit.getId()%>"><%}%>
<label>Mã danh mục<input name="code" required maxlength="50" value="<%=edit==null?"":esc(edit.getCode())%>"></label>
<label>Tên danh mục<input name="name" required maxlength="255" value="<%=edit==null?"":esc(edit.getName())%>"></label>
<label>Thứ tự hiển thị<input name="displayOrder" type="number" min="0" max="2147483647" required value="<%=edit==null?0:edit.getDisplayOrder()%>"></label>
<label>Trạng thái<select name="active"><option value="true" <%=edit==null||edit.isActive()?"selected":""%>>Đang sử dụng</option><option value="false" <%=edit!=null&&!edit.isActive()?"selected":""%>>Ngừng sử dụng</option></select></label>
<label>Mô tả<textarea name="description" maxlength="500" rows="3"><%=edit==null?"":esc(edit.getDescription())%></textarea></label>
<div class="category-flex"><button type="submit" class="category-btn primary"><%=edit==null?"Thêm":"Lưu thay đổi"%></button><%if(edit!=null){%><a href="<%=esc(prefix)%>/configuration/page?type=<%=type.name()%>">Hủy sửa</a><%}%></div>
</form></section><%}%>
</main></div></body></html>
