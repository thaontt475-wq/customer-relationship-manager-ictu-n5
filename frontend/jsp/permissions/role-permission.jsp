<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,com.crm.model.User,com.crm.model.Team,com.crm.model.Role,com.crm.controller.ServerForms" %>
<%!
private String esc(Object value) {
    if (value == null) return "";
    return String.valueOf(value).replace("&","&amp;").replace("<","&lt;")
        .replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
%>
<%
List<User> users = (List<User>) request.getAttribute("users");
List<Team> teams = (List<Team>) request.getAttribute("teams");
List<Role> roles = (List<Role>) request.getAttribute("roles");
List<Long> assigned = (List<Long>) request.getAttribute("userRoles");
User selected = (User) request.getAttribute("selectedUser");
String scope = (String) request.getAttribute("dataScope");
String error = (String) request.getAttribute("error");
String message = (String) request.getAttribute("message");
if ("1".equals(request.getParameter("teamSaved"))) message = "Đã cập nhật nhóm kinh doanh.";
long selectedId = selected == null ? -1L : selected.getId();
if (assigned == null) assigned = Collections.emptyList();
if (scope == null || scope.isBlank()) scope = "SELF";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Phân quyền và nhóm kinh doanh - CRM</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/permissions/permissions.css">
  <style>
    .nojs-container{max-width:1000px;margin:auto;padding:24px}.nojs-card{padding:20px;border:1px solid #d5dce5;border-radius:12px;margin:14px 0;background:var(--crm-card-bg,#fff)}
    .nojs-label{display:block;margin:12px 0 6px;font-weight:600}.nojs-input{width:100%;max-width:520px;padding:10px;border:1px solid #a5b1c0;border-radius:7px}
    .nojs-role{display:block;padding:8px 0}.nojs-button{padding:10px 16px;margin:14px 0;border:0;border-radius:7px;background:#2557a7;color:white;cursor:pointer}
    .nojs-alert{padding:12px;border:1px solid #c4d5bd;border-radius:8px;margin:12px 0}.nojs-error{border-color:#ba7777}
  </style>
</head>
<body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp" />
<div class="crm-main-layout">
<jsp:include page="/jsp/shared/sidebar.jsp" />
<main class="crm-page nojs-container">
  <nav aria-label="Đường dẫn"><a href="${pageContext.request.contextPath}/dashboard">Trang chủ</a> / Phân quyền</nav>
  <h1>Phân quyền và nhóm kinh doanh</h1>
  <p>Chọn tài khoản, sau đó cập nhật nhóm, vai trò và phạm vi dữ liệu.</p>
  <% if (error != null && !error.isBlank()) { %><p class="nojs-alert nojs-error" role="alert"><%=esc(error)%></p><% } %>
  <% if (message != null && !message.isBlank()) { %><p class="nojs-alert" role="status"><%=esc(message)%></p><% } %>
  <section class="nojs-card">
    <h2>1. Chọn tài khoản</h2>
    <form action="${pageContext.request.contextPath}/permissions" method="get">
      <label for="viewUserId" class="nojs-label">Tài khoản</label>
      <select id="viewUserId" name="viewUserId" class="nojs-input" required>
        <option value="">-- Chọn tài khoản --</option>
        <% if (users != null) for (User u : users) { %>
          <option value="<%=u.getId()%>" <%=u.getId()==selectedId?"selected":""%>><%=esc(u.getFullName())%> (<%=esc(u.getEmail())%>)</option>
        <% } %>
      </select>
      <button class="nojs-button" type="submit">Xem phân quyền</button>
    </form>
  </section>
  <% if (selected != null) { %>
  <section class="nojs-card">
    <h2><%=esc(selected.getFullName())%></h2>
    <p>Email: <%=esc(selected.getEmail())%></p>
    <p>Nhóm hiện tại: <%=esc(selected.getTeamName()==null?"Chưa gán nhóm":selected.getTeamName())%></p>
  </section>
  <section class="nojs-card">
    <h2>2. Gán nhóm kinh doanh</h2>
    <form action="${pageContext.request.contextPath}/permissions/team" method="post">
      <input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>">
      <input type="hidden" name="userId" value="<%=selectedId%>">
      <label class="nojs-label" for="teamId">Chọn nhóm</label>
      <select id="teamId" name="teamId" class="nojs-input" required>
        <option value="">-- Chọn nhóm --</option>
        <% if (teams != null) for (Team t : teams) { %>
          <option value="<%=t.getId()%>" <%=selected.getTeamId()!=null&&selected.getTeamId()==t.getId()?"selected":""%>><%=esc(t.getName())%></option>
        <% } %>
      </select>
      <button type="submit" class="nojs-button">Lưu nhóm</button>
    </form>
  </section>
  <section class="nojs-card">
    <h2>3. Vai trò và phạm vi dữ liệu</h2>
    <form action="${pageContext.request.contextPath}/permissions/assign" method="post">
      <input type="hidden" name="csrfToken" value="<%=esc(ServerForms.csrf(request))%>">
      <input type="hidden" name="userId" value="<%=selectedId%>">
      <fieldset><legend>Vai trò hệ thống (có thể chọn nhiều)</legend>
      <% if (roles != null) for (Role role : roles) { %>
        <label class="nojs-role"><input type="checkbox" name="roleIds" value="<%=role.getId()%>" <%=assigned.contains(role.getId())?"checked":""%>> <%=esc(role.getName())%></label>
      <% } %>
      </fieldset>
      <fieldset><legend>Phạm vi dữ liệu</legend>
        <label class="nojs-role"><input type="radio" name="dataScope" value="SELF" <%=scope.equalsIgnoreCase("SELF")?"checked":""%> required> Của tôi</label>
        <label class="nojs-role"><input type="radio" name="dataScope" value="TEAM" <%=scope.equalsIgnoreCase("TEAM")?"checked":""%>> Của nhóm</label>
        <label class="nojs-role"><input type="radio" name="dataScope" value="ALL" <%=scope.equalsIgnoreCase("ALL")?"checked":""%>> Tất cả</label>
      </fieldset>
      <p>Lưu ý: Team Lead cần có nhóm kinh doanh. Không thể tự gỡ quyền Admin.</p>
      <button class="nojs-button" type="submit">Lưu phân quyền</button>
    </form>
  </section>
  <% } %>
</main>
</div>
<jsp:include page="/jsp/shared/footer.jsp" />
</body></html>
