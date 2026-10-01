<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.model.Product,com.crm.service.products.ProductService.ProductSearchResult,com.crm.controller.ServerForms,java.net.URLEncoder,java.nio.charset.StandardCharsets" %>
<%!
private String esc(Object v) {
    if(v==null)return "";
    return v.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
private String url(Object v) {return URLEncoder.encode(v==null?"":v.toString(), StandardCharsets.UTF_8);}
%>
<%
ProductSearchResult result=(ProductSearchResult)request.getAttribute("products");
Product edit=(Product)request.getAttribute("editProduct");
boolean canManage=Boolean.TRUE.equals(request.getAttribute("canManage"));
String q=String.valueOf(request.getAttribute("q"));
String category=String.valueOf(request.getAttribute("category"));
String active=String.valueOf(request.getAttribute("active"));
String prefix=request.getContextPath();
String filters="q="+url(q)+"&category="+url(category)+"&active="+url(active);
%>
<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Sản phẩm &amp; bảng giá - CRM</title>
<link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/common.css">
<link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/layout.css">
<link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/header.css">
<link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/sidebar.css">
<link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/components.css">
<style>
.catalog-shell{padding:24px;max-width:1180px;margin:auto}.catalog-card{background:var(--card-bg,#fff);color:var(--text-color,#222);border:1px solid #ddd;border-radius:8px;padding:18px;margin:16px 0}.catalog-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:12px}.catalog-grid label{display:flex;flex-direction:column;gap:5px}.catalog-grid input,.catalog-grid select,.catalog-grid textarea{padding:9px;border:1px solid #aaa;border-radius:5px;background:inherit;color:inherit}.catalog-table{border-collapse:collapse;width:100%}.catalog-table th,.catalog-table td{text-align:left;border-bottom:1px solid #ddd;padding:9px}.catalog-scroll{overflow-x:auto}.catalog-actions{display:flex;align-items:center;flex-wrap:wrap;gap:10px;margin-top:12px}.catalog-btn{border:1px solid #666;border-radius:5px;padding:8px 13px;background:inherit;color:inherit;cursor:pointer}.catalog-btn.primary{background:#164c78;color:white}.catalog-notice{border-left:4px solid #28794a;padding:10px;background:#e6f3e8;color:#153c25} .catalog-muted{opacity:.75}
</style></head><body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout"><jsp:include page="/jsp/shared/sidebar.jsp"/>
<main class="crm-page catalog-shell"><h1>Sản phẩm &amp; bảng giá niêm yết</h1>
<p>Danh sách lấy trực tiếp từ máy chủ. Giá vốn chỉ hiển thị cho người có quyền.</p>
<% if(request.getAttribute("notice")!=null) { %><p class="catalog-notice" role="status"><%= esc(request.getAttribute("notice")) %></p><% } %>
<section class="catalog-card"><h2>Tìm kiếm và bộ lọc</h2>
<form method="get" action="<%= esc(prefix) %>/products/page" class="catalog-grid">
<label>Tên hoặc mã sản phẩm<input type="search" name="q" value="<%= esc(q) %>" maxlength="255"></label>
<label>Loại sản phẩm<select name="category"><option value="">Tất cả</option><option value="ONE_TIME" <%= "ONE_TIME".equals(category)?"selected":"" %>>Một lần</option><option value="SUBSCRIPTION" <%= "SUBSCRIPTION".equals(category)?"selected":"" %>>Thuê bao</option></select></label>
<label>Trạng thái<select name="active"><option value="">Tất cả</option><option value="true" <%= "true".equals(active)?"selected":"" %>>Đang kinh doanh</option><option value="false" <%= "false".equals(active)?"selected":"" %>>Ngừng kinh doanh</option></select></label>
<div class="catalog-actions"><button class="catalog-btn primary" type="submit">Tìm kiếm</button><a href="<%= esc(prefix) %>/products/page">Xóa bộ lọc</a></div>
</form></section>
<section class="catalog-card"><h2>Danh sách sản phẩm (<%= result.total() %>)</h2><div class="catalog-scroll"><table class="catalog-table"><thead><tr><th>Mã</th><th>Tên</th><th>Loại</th><th>Đơn vị</th><th>Giá niêm yết</th><th>Giá sàn</th><%if(canManage){%><th>Giá vốn</th><%}%><th>Trạng thái</th><%if(canManage){%><th>Thao tác</th><%}%></tr></thead><tbody>
<%for(Product p:result.items()){%><tr><td><%=esc(p.getCode())%></td><td><%=esc(p.getName())%></td><td><%=esc(p.getCategory())%></td><td><%=esc(p.getUnit())%></td><td><%=esc(p.getListPrice())%></td><td><%=esc(p.getFloorPrice())%></td><%if(canManage){%><td><%=esc(p.getCostPrice())%></td><%}%><td><%=p.isActive()?"Đang kinh doanh":"Ngừng kinh doanh"%></td><%if(canManage){%><td><a href="<%=esc(prefix)%>/products/page?edit=<%=p.getId()%>">Sửa</a></td><%}%></tr><%}%>
<%if(result.items().isEmpty()){%><tr><td colspan="9">Không tìm thấy sản phẩm.</td></tr><%}%>
</tbody></table></div><nav class="catalog-actions" aria-label="Phân trang"><%if(result.page()>1){%><a href="<%=esc(prefix)%>/products/page?<%=esc(filters)%>&page=<%=result.page()-1%>">Trang trước</a><%}%><span>Trang <%=result.page()%> / <%=result.totalPages()%></span><%if(result.page()<result.totalPages()){%><a href="<%=esc(prefix)%>/products/page?<%=esc(filters)%>&page=<%=result.page()+1%>">Trang sau</a><%}%></nav></section>
<%if(canManage){%>
<section class="catalog-card"><h2><%=edit==null?"Thêm sản phẩm":"Chỉnh sửa sản phẩm"%></h2>
<form method="post" action="<%=esc(prefix)%>/products/page">
<input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>">
<input type="hidden" name="operation" value="<%=edit==null?"create":"update"%>">
<%if(edit!=null){%><input type="hidden" name="id" value="<%=edit.getId()%>"><%}%>
<div class="catalog-grid">
<label>Mã sản phẩm *<input required maxlength="50" name="code" value="<%=esc(edit==null?"":edit.getCode())%>"></label>
<label>Tên sản phẩm *<input required maxlength="255" name="name" value="<%=esc(edit==null?"":edit.getName())%>"></label>
<label>Loại sản phẩm<select name="category"><option value="ONE_TIME" <%=edit!=null&&"ONE_TIME".equals(edit.getCategory())?"selected":""%>>Một lần</option><option value="SUBSCRIPTION" <%=edit!=null&&"SUBSCRIPTION".equals(edit.getCategory())?"selected":""%>>Thuê bao</option></select></label>
<label>Đơn vị tính<input name="unit" maxlength="80" value="<%=esc(edit==null?"":edit.getUnit())%>"></label>
<label>Giá niêm yết *<input type="number" step="0.01" min="0" required name="listPrice" value="<%=esc(edit==null?"0":edit.getListPrice())%>"></label>
<label>Giá sàn *<input type="number" step="0.01" min="0" required name="floorPrice" value="<%=esc(edit==null?"0":edit.getFloorPrice())%>"></label>
<label>Giá vốn (quản trị/giám đốc)<input type="number" step="0.01" min="0" name="costPrice" value="<%=esc(edit==null?"":edit.getCostPrice())%>"></label>
<label>Mô tả<textarea name="description" rows="2"><%=esc(edit==null?"":edit.getDescription())%></textarea></label>
<label>Trạng thái<select name="active"><option value="true" <%=edit==null||edit.isActive()?"selected":""%>>Đang kinh doanh</option><option value="false" <%=edit!=null&&!edit.isActive()?"selected":""%>>Ngừng kinh doanh</option></select></label>
</div><div class="catalog-actions"><button type="submit" class="catalog-btn primary">Lưu sản phẩm</button><a href="<%=esc(prefix)%>/products/page">Bỏ qua</a></div>
</form>
<%if(edit!=null&&edit.isActive()){%><hr><form method="post" action="<%=esc(prefix)%>/products/page"><input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>"><input type="hidden" name="operation" value="disable"><input type="hidden" name="id" value="<%=edit.getId()%>"><label><input type="checkbox" name="confirm" value="yes" required> Tôi xác nhận ngừng kinh doanh sản phẩm này.</label> <button type="submit" class="catalog-btn">Ngừng kinh doanh</button></form><%}%>
</section><%}%>
<p class="catalog-muted">Các thao tác chạy bằng biểu mẫu HTML và Servlet; không cần JavaScript.</p>
</main></div></body></html>
