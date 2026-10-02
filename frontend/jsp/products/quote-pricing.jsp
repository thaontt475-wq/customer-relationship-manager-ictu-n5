<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.crm.service.products.QuotePricingService.Pricing,com.crm.util.Html,com.crm.controller.ServerForms,com.crm.model.QuoteItem" %>
<%
    Pricing pricing = (Pricing) request.getAttribute("pricing");
    long id = (Long) request.getAttribute("quoteId");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Sản phẩm trong báo giá</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
</head>
<body>
<jsp:include page="/jsp/shared/header.jsp"/>
<main class="crm-page">
    <h1>Sản phẩm trong báo giá</h1>
    <p>Chiết khấu: <%= pricing.discount() %>%. Tổng: <%= pricing.total() %>. Trạng thái: <%= Html.escape(pricing.status()) %></p>
    <% if (pricing.requiresApproval()) { %>
        <p role="alert" style="color: var(--crm-danger, #ef4444); font-weight: bold;">
            Giá sau chiết khấu thấp hơn giá sàn; cần phê duyệt trước khi phát hành.
        </p>
    <% } %>
    <ul>
        <% for (QuoteItem item : pricing.items()) { %>
            <li>
                <%= Html.escape(item.code()) %> — <%= Html.escape(item.name()) %>,
                <%= item.quantity() %> <%= Html.escape(item.unit()) %>,
                đơn giá <%= item.unitPrice() %>,
                giá sàn tại thời điểm tạo <%= item.floorPrice() %>
            </li>
        <% } %>
    </ul>
    <form method="post" action="${pageContext.request.contextPath}/quotes/items">
        <input type="hidden" name="id" value="<%= id %>">
        <input type="hidden" name="csrfToken" value="<%= ServerForms.csrf(request) %>">
        <label>Mã ID sản phẩm <input type="number" min="1" name="productId" required></label>
        <a href="${pageContext.request.contextPath}/products/page">Tra cứu sản phẩm</a>
        <label>Số lượng <input type="number" min="0.01" step="0.01" name="quantity" required></label>
        <label>Đơn giá <input type="number" min="0" step="0.01" name="unitPrice" required></label>
        <button type="submit">Thêm sản phẩm</button>
    </form>
    <% if (ServerForms.admin(request) && "PENDING_APPROVAL".equals(pricing.status())) { %>
        <form method="post" action="${pageContext.request.contextPath}/quotes/approve">
            <input type="hidden" name="id" value="<%= id %>">
            <input type="hidden" name="csrfToken" value="<%= ServerForms.csrf(request) %>">
            <button type="submit">Phê duyệt chiết khấu</button>
        </form>
    <% } %>
    <a href="${pageContext.request.contextPath}/quotes?id=<%= id %>">Quay lại báo giá</a>
</main>
</body>
</html>
