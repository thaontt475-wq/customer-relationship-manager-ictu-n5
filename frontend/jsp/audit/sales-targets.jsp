<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.crm.dao.audit.BusinessChangeDAO.Target,com.crm.model.User,com.crm.util.Html,com.crm.controller.ServerForms" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Chỉ tiêu kinh doanh</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
</head>
<body>
<jsp:include page="/jsp/shared/header.jsp"/>
<main class="crm-page">
    <h1>Chỉ tiêu kinh doanh theo tháng</h1>
    <% if ("1".equals(request.getParameter("saved"))) { %>
        <p role="status">Đã lưu chỉ tiêu và ghi nhật ký.</p>
    <% } %>
    <form method="post" action="${pageContext.request.contextPath}/kpi">
        <input type="hidden" name="csrfToken" value="<%=ServerForms.csrf(request)%>">
        <label>Nhân viên
            <select name="userId" required>
                <%
                    List<User> userList = (List<User>) request.getAttribute("users");
                    if (userList != null) {
                        for (User u : userList) {
                %>
                    <option value="<%=u.getId()%>"><%=Html.escape(u.getFullName())%></option>
                <%
                        }
                    }
                %>
            </select>
        </label>
        <label>Tháng <input type="month" name="month" required></label>
        <label>Chỉ tiêu doanh số <input type="number" name="amount" min="0" step="0.01" required></label>
        <button type="submit">Lưu chỉ tiêu</button>
    </form>
    <ul>
        <%
            List<Target> targetList = (List<Target>) request.getAttribute("targets");
            if (targetList != null) {
                for (Target t : targetList) {
        %>
            <li><%=Html.escape(t.name())%> — <%=t.month()%>: <%=t.amount()%></li>
        <%
                }
            }
        %>
    </ul>
    <a href="${pageContext.request.contextPath}/audit?objectType=SALES_TARGET">Xem nhật ký chỉ tiêu</a>
</main>
</body>
</html>
