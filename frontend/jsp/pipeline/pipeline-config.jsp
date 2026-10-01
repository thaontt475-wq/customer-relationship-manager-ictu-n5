<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.crm.model.PipelineStage,com.crm.controller.ServerForms" %>
<%!
private String esc(Object value) {
    if (value == null) return "";
    return value.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
%>
<%
List<PipelineStage> stages=(List<PipelineStage>)request.getAttribute("stages");
if(stages==null) stages=java.util.Collections.emptyList();
PipelineStage edit=(PipelineStage)request.getAttribute("editStage");
long pipelineId=request.getAttribute("pipelineId") instanceof Number ? ((Number)request.getAttribute("pipelineId")).longValue() : 1L;
String activeFilter=(String)request.getAttribute("activeFilter");
if(activeFilter==null)activeFilter="";
boolean manage=Boolean.TRUE.equals(request.getAttribute("canManage"));
String prefix=request.getContextPath();
%>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Cấu hình pipeline - CRM</title>
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/common.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/layout.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/header.css">
<link rel="stylesheet" href="<%=esc(prefix)%>/css/shared/sidebar.css">
<style>
.pipeline-page{padding:24px;min-width:0;flex:1}.pipe-box{background:var(--surface,#fff);border:1px solid #ddd;border-radius:10px;padding:20px;margin:14px 0}
.pipe-form{display:grid;gap:12px;grid-template-columns:repeat(auto-fit,minmax(210px,1fr))}.pipe-form label{display:grid;gap:5px}
.pipe-form input,.pipe-form textarea,.pipe-form select{width:100%;padding:9px;border:1px solid #aaa;border-radius:6px}
.pipe-scroll{overflow-x:auto}.pipe-scroll table{width:100%;border-collapse:collapse}.pipe-scroll th,.pipe-scroll td{border-bottom:1px solid #ddd;padding:10px;text-align:left}
.pipe-btn{display:inline-block;padding:9px 13px;border:1px solid #888;border-radius:6px;background:#f7f7f7;color:#222;text-decoration:none;cursor:pointer}
.pipe-btn.primary{background:#194a83;color:white}.pipe-row{display:flex;gap:12px;flex-wrap:wrap;align-items:center}.pipe-warning{padding:12px;border:1px solid #a46b22;border-radius:5px}
@media(max-width:600px){.pipeline-page{padding:10px}.pipe-box{padding:12px}}
</style></head><body class="crm-body"><jsp:include page="/jsp/shared/header.jsp"/>
<div class="crm-main-layout"><jsp:include page="/jsp/shared/sidebar.jsp"/>
<main class="pipeline-page"><h1>Cấu hình giai đoạn bán hàng</h1><p>Quản lý xác suất thắng và điều kiện chuyển bước. Không cần JavaScript.</p>
<%if(request.getAttribute("notice")!=null){%><p role="status" class="pipe-warning"><%=esc(request.getAttribute("notice"))%></p><%}%>
<section class="pipe-box"><h2>Danh sách giai đoạn</h2>
<form method="get" action="<%=esc(prefix)%>/pipeline/page" class="pipe-row">
<label>ID pipeline <input type="number" min="1" name="pipelineId" value="<%=pipelineId%>" required></label>
<label>Trạng thái <select name="active"><option value="">Tất cả</option><option value="true" <%= "true".equals(activeFilter)?"selected":"" %>>Đang dùng</option><option value="false" <%= "false".equals(activeFilter)?"selected":"" %>>Ngừng dùng</option></select></label>
<button type="submit" class="pipe-btn primary">Lọc</button><a class="pipe-btn" href="<%=esc(prefix)%>/pipeline/page">Xóa lọc</a></form>
<div class="pipe-scroll"><table><thead><tr><th>Thứ tự</th><th>Mã</th><th>Tên</th><th>Tỷ lệ thắng</th><th>Điều kiện chuyển</th><th>Loại</th><th>Trạng thái</th><%if(manage){%><th>Thao tác</th><%}%></tr></thead><tbody>
<%for(PipelineStage stage:stages){%><tr>
<td><%=stage.getStageOrder()%></td><td><%=esc(stage.getCode())%></td><td><%=esc(stage.getName())%></td>
<td><%=stage.getWinProbability()%>%</td><td><%=esc(stage.getRequirements())%></td><td><%=stage.isWon()?"Thắng":stage.isLost()?"Thua":"Thông thường"%></td><td><%=stage.isActive()?"Đang dùng":"Ngừng dùng"%></td>
<%if(manage){%><td><a href="<%=esc(prefix)%>/pipeline/page?pipelineId=<%=pipelineId%>&edit=<%=stage.getId()%>">Sửa</a>
<form method="post" action="<%=esc(prefix)%>/pipeline/page">
<input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>"><input type="hidden" name="pipelineId" value="<%=pipelineId%>">
<input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="<%=stage.getId()%>">
<label>Chuyển cơ hội tới: <select name="targetStageId"><option value="">Không chuyển</option>
<%for(PipelineStage candidate:stages){if(!candidate.getId().equals(stage.getId())){%><option value="<%=candidate.getId()%>"><%=esc(candidate.getName())%></option><%}}%>
</select></label><label><input type="checkbox" name="confirm" value="yes" required>Xác nhận xóa</label><button type="submit" class="pipe-btn">Xóa</button>
</form></td><%}%></tr><%}%>
<%if(stages.isEmpty()){%><tr><td colspan="8">Chưa có giai đoạn phù hợp.</td></tr><%}%>
</tbody></table></div></section>
<%if(manage){%><section class="pipe-box"><h2><%=edit==null?"Thêm giai đoạn":"Chỉnh sửa giai đoạn"%></h2>
<form method="post" action="<%=esc(prefix)%>/pipeline/page" class="pipe-form">
<input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>"><input type="hidden" name="pipelineId" value="<%=pipelineId%>">
<input type="hidden" name="action" value="<%=edit==null?"create":"update"%>"><%if(edit!=null){%><input type="hidden" name="id" value="<%=edit.getId()%>"><%}%>
<label>Mã giai đoạn<input type="text" name="code" maxlength="50" required value="<%=edit==null?"":esc(edit.getCode())%>"></label>
<label>Tên giai đoạn<input type="text" name="name" maxlength="150" required value="<%=edit==null?"":esc(edit.getName())%>"></label>
<label>Thứ tự<input type="number" name="stageOrder" min="1" required value="<%=edit==null?1:edit.getStageOrder()%>"></label>
<label>Xác suất thắng (%)<input type="number" name="winProbability" min="0" max="100" required value="<%=edit==null?50:edit.getWinProbability()%>"></label>
<label>Loại<select name="outcome"><option value="normal">Thông thường</option><option value="won" <%=edit!=null&&edit.isWon()?"selected":""%>>Thắng</option><option value="lost" <%=edit!=null&&edit.isLost()?"selected":""%>>Thua</option></select></label>
<label>Trạng thái<select name="active"><option value="true" <%=edit==null||edit.isActive()?"selected":""%>>Đang dùng</option><option value="false" <%=edit!=null&&!edit.isActive()?"selected":""%>>Ngừng dùng</option></select></label>
<label style="grid-column:1/-1">Điều kiện rời giai đoạn<textarea rows="3" name="requirements" maxlength="2000"><%=edit==null?"":esc(edit.getRequirements())%></textarea></label>
<div class="pipe-row"><button type="submit" class="pipe-btn primary"><%=edit==null?"Thêm giai đoạn":"Lưu"%></button><%if(edit!=null){%><a href="<%=esc(prefix)%>/pipeline/page?pipelineId=<%=pipelineId%>">Hủy sửa</a><%}%></div>
</form></section><%}%>
</main></div></body></html>
