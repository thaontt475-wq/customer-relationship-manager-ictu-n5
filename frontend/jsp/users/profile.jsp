<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.model.User, com.crm.model.Role, com.crm.util.Html, com.crm.controller.ServerForms" %>
<%@ page import="java.util.List" %>
<%
  User person = (User) request.getAttribute("profileUser");
  if (person == null) { response.sendError(500, "Thiáº¿u dá»¯ liá»‡u há»“ sÆ¡"); return; }
  String err = (String) request.getAttribute("error");
  String signature = (String) request.getAttribute("signature");
  if (signature == null) signature = person.getSignature();

  List<Role> roles = person.getRoles();
  String rolesStr = "";
  if (roles != null && !roles.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Role r : roles) {
          if (sb.length() > 0) sb.append(", ");
          sb.append(r.getName());
      }
      rolesStr = sb.toString();
  }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Há»“ sÆ¡ cÃ¡ nhÃ¢n | CRM</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/components.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/users/profile.css">
</head>
<body class="crm-body">
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <main class="profile-page crm-page" role="main">
            <div class="crm-page-header">
                <h1 class="crm-page-title">Há»“ sÆ¡ cÃ¡ nhÃ¢n</h1>
                <p class="crm-page-description">Cáº­p nháº­t thÃ´ng tin cÃ¡ nhÃ¢n vÃ  chá»¯ kÃ½ email cá»§a báº¡n.</p>
            </div>

            <section class="crm-card">
                <% if (err != null && !err.isBlank()) { %>
                    <div class="crm-alert crm-alert-error" role="alert">
                        <%= Html.escape(err) %>
                    </div>
                <% } %>
                <% if ("1".equals(request.getParameter("updated"))) { %>
                    <div class="crm-alert crm-alert-success" role="status">
                        Cáº­p nháº­t há»“ sÆ¡ thÃ nh cÃ´ng.
                    </div>
                <% } %>

                <form method="post" action="${pageContext.request.contextPath}/profile" class="crm-form">
                    <input type="hidden" name="csrfToken" value="<%= Html.escape(ServerForms.csrf(request)) %>">

                    <div class="profile-header-group">
                        <div class="profile-avatar-section">
                            <div class="avatar-preview">
                                <img src="${pageContext.request.contextPath}/profile/avatar/thumbnail" alt="áº¢nh Ä‘áº¡i diá»‡n" class="avatar-img" onerror="this.src='${pageContext.request.contextPath}/images/default-avatar.png'; this.onerror=null;">
                            </div>
                            <div class="avatar-actions">
                                <a href="${pageContext.request.contextPath}/profile/avatar" class="crm-btn crm-btn-secondary">Äá»•i áº£nh Ä‘áº¡i diá»‡n</a>
                            </div>
                        </div>
                    </div>

                    <div class="crm-form-grid">
                        <div class="crm-form-group">
                            <label class="crm-form-label" for="fullName">Há» vÃ  tÃªn <span class="crm-required">*</span></label>
                            <input type="text" id="fullName" name="fullName" class="crm-form-control" value="<%= Html.escape(person.getFullName()) %>" required maxlength="150" placeholder="Nháº­p há» vÃ  tÃªn">
                        </div>

                        <div class="crm-form-group">
                            <label class="crm-form-label" for="phone">Sá»‘ Ä‘iá»‡n thoáº¡i <span class="crm-required">*</span></label>
                            <input type="tel" id="phone" name="phone" class="crm-form-control" value="<%= Html.escape(person.getPhone()) %>" required autocomplete="tel" pattern="^0[35789][0-9]{8}$" title="Sá»‘ Ä‘iá»‡n thoáº¡i Viá»‡t Nam há»£p lá»‡, vÃ­ dá»¥: 0912345678" placeholder="09xxxxxxxx">
                        </div>
                    </div>

                    <div class="crm-form-group">
                        <label class="crm-form-label" for="signature">Chá»¯ kÃ½ email</label>
                        <textarea id="signature" name="signature" class="crm-form-control" rows="5" placeholder="Nháº­p chá»¯ kÃ½ hiá»ƒn thá»‹ dÆ°á»›i cuá»‘i email..."><%= Html.escape(signature) %></textarea>
                    </div>

                    <h3 class="section-title">ThÃ´ng tin tá»• chá»©c</h3>
                    <div class="crm-form-grid readonly-grid">
                        <div class="crm-form-group">
                            <label class="crm-form-label" for="email">Email cÃ´ng ty</label>
                            <input type="email" id="email" class="crm-form-control crm-readonly" value="<%= Html.escape(person.getEmail()) %>" readonly>
                        </div>

                        <div class="crm-form-group">
                            <label class="crm-form-label" for="team">NhÃ³m / PhÃ²ng ban</label>
                            <input type="text" id="team" class="crm-form-control crm-readonly" value="<%= Html.escape(person.getTeamName() != null ? person.getTeamName() : "ChÆ°a phÃ¢n nhÃ³m") %>" readonly>
                        </div>

                        <div class="crm-form-group">
                            <label class="crm-form-label" for="role">Vai trÃ² há»‡ thá»‘ng</label>
                            <input type="text" id="role" class="crm-form-control crm-readonly" value="<%= Html.escape(rolesStr.isEmpty() ? "ChÆ°a cáº¥p quyá»n" : rolesStr) %>" readonly>
                        </div>
                    </div>

                    <div class="crm-form-help">
                        Ghi chÃº: ThÃ´ng tin tá»• chá»©c (Email, NhÃ³m, Vai trÃ²) Ä‘Æ°á»£c quáº£n lÃ½ vÃ  khÃ³a bá»Ÿi Quáº£n trá»‹ viÃªn.
                    </div>

                    <div class="crm-form-actions">
                        <button type="submit" class="crm-btn crm-btn-primary">LÆ°u thay Ä‘á»•i</button>
                        <a href="${pageContext.request.contextPath}/dashboard" class="crm-btn crm-btn-secondary">Há»§y bá»</a>
                    </div>
                </form>
            </section>
        </main>
    </div>
</body>
</html>
