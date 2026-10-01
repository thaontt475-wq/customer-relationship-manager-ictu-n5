<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,java.net.URLEncoder,java.nio.charset.StandardCharsets,com.crm.model.Organization,com.crm.controller.ServerForms" %>
<%!
private String esc(Object value) {
    if (value == null) return "";
    return value.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
private String encoded(Object value) { return URLEncoder.encode(value == null ? "" : value.toString(), StandardCharsets.UTF_8); }
private String regionName(String value) {
    if (value == null) return "Chưa xác định";
    return switch(value) { case "NORTH" -> "Miền Bắc"; case "CENTRAL" -> "Miền Trung";
        case "SOUTH" -> "Miền Nam"; case "NATIONAL" -> "Toàn quốc";
        case "OVERSEAS" -> "Quốc tế"; default -> value; };
}
%>
<%
if (request.getAttribute("units") == null) {
    response.sendRedirect(request.getContextPath()+"/organization/page"); return;
}
List<Organization> units = (List<Organization>)request.getAttribute("units");
List<Organization> filtered = (List<Organization>)request.getAttribute("filtered");
Organization edit = (Organization)request.getAttribute("editUnit");
boolean manage = Boolean.TRUE.equals(request.getAttribute("canManage"));
String prefix = request.getContextPath();
String q = (String)request.getAttribute("keyword");
String region = (String)request.getAttribute("regionFilter");
%>
<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Cơ cấu tổ chức - CRM</title>
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/common.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/layout.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/header.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/sidebar.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/components.css">
<style>
.org-shell{max-width:1200px;padding:24px;margin:auto}.org-card{border:1px solid #bbb;border-radius:8px;padding:18px;margin:16px 0;background:var(--card-bg,#fff);color:var(--text-color,#222)}
.org-fields{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:14px}.org-fields label{display:flex;flex-direction:column;gap:6px}.org-fields input,.org-fields select{padding:9px;border:1px solid #999;border-radius:5px;background:inherit;color:inherit}
.org-actions{display:flex;flex-wrap:wrap;gap:12px;align-items:center;margin:14px 0}.org-btn{padding:8px 14px;border:1px solid #666;border-radius:5px;background:inherit;color:inherit;cursor:pointer}.org-btn.primary{background:#164c78;color:#fff}
.org-table{border-collapse:collapse;width:100%}.org-table td,.org-table th{padding:10px;text-align:left;border-bottom:1px solid #ddd}.org-scroll{overflow-x:auto}.org-notice{border-left:4px solid green;padding:12px;background:#e7f4e9;color:#214422}
</style></head><body class="crm-body"><jsp:include page="/jsp/shared/header.jsp"/><div class="crm-main-layout"><jsp:include page="/jsp/shared/sidebar.jsp"/>
<main class="crm-page org-shell"><nav aria-label="Breadcrumb"><a href="<%=esc(prefix)%>/dashboard">CRM</a> / Cơ cấu tổ chức</nav>
<h1>Cơ cấu tổ chức</h1><p>Các đơn vị và trưởng nhóm được lấy trực tiếp từ máy chủ.</p>
<%if(request.getAttribute("notice")!=null){%><p class="org-notice" role="status"><%=esc(request.getAttribute("notice"))%></p><%}%>
<section class="org-card"><h2>Tìm kiếm và lọc</h2><form class="org-fields" method="get" action="<%=esc(prefix)%>/organization/page">
<label>Tên đơn vị hoặc trưởng nhóm<input type="search" name="q" value="<%=esc(q)%>" maxlength="150"></label>
<label>Khu vực<select name="region"><option value="">Tất cả khu vực</option>
<% for(String r:new String[]{"NORTH","CENTRAL","SOUTH","NATIONAL","OVERSEAS"}) { %><option value="<%=r%>" <%=r.equals(region)?"selected":""%>><%=esc(regionName(r))%></option><%}%>
</select></label><div class="org-actions"><button class="org-btn primary" type="submit">Lọc</button><a href="<%=esc(prefix)%>/organization/page">Làm mới</a></div></form></section>
<section class="org-card"><h2>Danh sách đơn vị (<%=filtered.size()%>)</h2><div class="org-scroll"><table class="org-table"><thead><tr><th>Đơn vị</th><th>Cấp trên</th><th>Trưởng đơn vị</th><th>Khu vực</th><th>Thành viên</th><th>Trạng thái</th><%if(manage){%><th>Thao tác</th><%}%></tr></thead><tbody>
<%for(Organization unit:filtered){ String parent="Đơn vị gốc"; for(Organization candidate:units) if(unit.getParentId()!=null && candidate.getId()==unit.getParentId()) {parent=candidate.getName();break;} %>
<tr><td><strong><%=esc(unit.getName())%></strong></td><td><%=esc(parent)%></td><td><%=esc(unit.getManagerName())%></td><td><%=esc(regionName(unit.getRegion()))%></td><td><%=unit.getMemberCount()%></td><td><%=unit.isActive()?"Hoạt động":"Ngừng hoạt động"%></td>
<%if(manage){%><td><a href="<%=esc(prefix)%>/organization/page?edit=<%=unit.getId()%>&q=<%=esc(encoded(q))%>&region=<%=esc(encoded(region))%>">Sửa</a></td><%}%></tr>
<%}%><%if(filtered.isEmpty()){%><tr><td colspan="7">Không tìm thấy đơn vị.</td></tr><%}%>
</tbody></table></div></section>
<%if(manage){%><section class="org-card"><h2><%=edit==null?"Thêm đơn vị":"Sửa đơn vị"%></h2>
<p>Chỉ chọn trưởng đơn vị đã có tài khoản và vai trò hợp lệ. Hệ thống kiểm tra phân quyền và ràng buộc khi lưu.</p>
<form method="post" action="<%=esc(prefix)%>/organization/page">
<input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>">
<input type="hidden" name="action" value="<%=edit==null?"create":"update"%>">
<%if(edit!=null){%><input type="hidden" name="id" value="<%=edit.getId()%>"><%}%>
<div class="org-fields"><label>Tên đơn vị *<input name="name" maxlength="150" required value="<%=esc(edit==null?"":edit.getName())%>"></label>
<label>Đơn vị cấp trên<select name="parentId"><option value="">Không có (đơn vị gốc)</option>
<%for(Organization candidate:units){if(edit!=null && candidate.getId()==edit.getId())continue;%>
<option value="<%=candidate.getId()%>" <%=edit!=null&&edit.getParentId()!=null&&edit.getParentId()==candidate.getId()?"selected":""%>><%=esc(candidate.getName())%></option><%}%>
</select></label>
<label>ID trưởng đơn vị *<input type="number" name="managerId" min="1" required value="<%=esc(edit==null?"":edit.getManagerId())%>"></label>
<label>Khu vực *<select name="region" required><%for(String r:new String[]{"NORTH","CENTRAL","SOUTH","NATIONAL","OVERSEAS"}){%>
<option value="<%=r%>" <%=edit!=null&&r.equals(edit.getRegion())?"selected":""%>><%=esc(regionName(r))%></option><%}%></select></label>
<label>Trạng thái<select name="active"><option value="true" <%=edit==null||edit.isActive()?"selected":""%>>Hoạt động</option><option value="false" <%=edit!=null&&!edit.isActive()?"selected":""%>>Ngừng hoạt động</option></select></label></div>
<div class="org-actions"><button type="submit" class="org-btn primary">Lưu đơn vị</button><a href="<%=esc(prefix)%>/organization/page">Hủy</a></div></form></section><%}%>
<p>Biểu mẫu và bộ lọc chạy trên máy chủ. Không sử dụng JavaScript.</p>
</main></div></body></html>
