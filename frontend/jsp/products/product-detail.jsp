<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  ==========================================================================
  FEATURE S2-05: Màn hình Chi tiết sản phẩm / dịch vụ (Standalone View)
  Phụ trách Frontend/View: Tiệp
  BE CONTRACT:
    - GET ${pageContext.request.contextPath}/products/detail?id={id}
  ==========================================================================
--%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chi tiết sản phẩm - CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống CRM -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/header.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/shared/sidebar.css">

    <!-- CSS riêng biệt của module Sản phẩm & Bảng giá (S2-05) -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/products/products.css">
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp" />

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp" />

        <!-- Khu vực nội dung chính -->
        <main class="prod-page" role="main">
            <div class="prod-container" style="max-width: 900px;">

                <!-- Breadcrumb -->
                <nav class="prod-breadcrumb" aria-label="Breadcrumb">
                    <a href="${pageContext.request.contextPath}/">CRM</a>
                    <span class="separator">/</span>
                    <a href="${pageContext.request.contextPath}/products">Sản phẩm & Bảng giá</a>
                    <span class="separator">/</span>
                    <span class="active">Chi tiết sản phẩm</span>
                </nav>

                <!-- Header -->
                <header class="prod-header">
                    <div class="prod-header-info">
                        <h1>Chi tiết sản phẩm & Chính sách giá</h1>
                        <p>Xem thông tin chi tiết, đơn vị tính, ngưỡng giá sàn và bảng giá áp dụng cho sản phẩm.</p>
                    </div>
                    <div class="prod-header-badges">
                        <a href="${pageContext.request.contextPath}/products/edit?id=${product != null ? product.id : param.id}" class="btn-prod btn-prod-primary">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                            </svg>
                            <span>Chỉnh sửa sản phẩm</span>
                        </a>
                    </div>
                </header>

                <!-- Detail Card -->
                <section class="prod-card" style="padding: 24px;">
                    <div class="prod-detail-grid">
                        <span class="prod-detail-label">Mã sản phẩm:</span>
                        <span class="prod-detail-val" style="font-weight: 700; color: var(--prod-primary); font-size: 1.1rem;">
                            ${product != null ? product.code : 'SP-001'}
                        </span>

                        <span class="prod-detail-label">Tên sản phẩm:</span>
                        <span class="prod-detail-val" style="font-weight: 600; font-size: 1.05rem;">
                            ${product != null ? product.name : 'Gói CRM Cloud Enterprise (1 Năm)'}
                        </span>

                        <span class="prod-detail-label">Loại sản phẩm:</span>
                        <span class="prod-detail-val">
                            <span class="prod-badge prod-badge-type-subscription">🔄 Dịch vụ định kỳ (Subscription)</span>
                        </span>

                        <span class="prod-detail-label">Đơn vị tính:</span>
                        <span class="prod-detail-val">${product != null ? product.unit : 'Gói / Năm'}</span>

                        <span class="prod-detail-label">Trạng thái:</span>
                        <span class="prod-detail-val">
                            <span class="prod-badge prod-badge-active">● Đang kinh doanh</span>
                        </span>

                        <span class="prod-detail-label">Giá niêm yết:</span>
                        <span class="prod-detail-val" style="font-weight: 700; font-size: 1.2rem; color: var(--prod-text-main);">
                            ${product != null ? product.listPrice : '24.000.000 ₫'}
                        </span>

                        <span class="prod-detail-label">Giá sàn (Duyệt):</span>
                        <span class="prod-detail-val" style="font-weight: 700; font-size: 1.1rem; color: #b45309;">
                            ${product != null ? product.floorPrice : '19.200.000 ₫'}
                            <span class="prod-floor-badge">⚠️ Báo giá dưới mức này cần duyệt chiết khấu</span>
                        </span>

                        <span class="prod-detail-label">Giá vốn:</span>
                        <span class="prod-detail-val">
                            <% if (request.getAttribute("isDirector") != null && (Boolean)request.getAttribute("isDirector")) { %>
                                <span class="prod-cost-val">${product != null ? product.costPrice : '9.600.000 ₫'}</span>
                            <% } else { %>
                                <span class="prod-cost-masked" title="Chỉ Giám đốc kinh doanh có quyền xem giá vốn">•••••• (Chỉ Giám đốc kinh doanh)</span>
                            <% } %>
                        </span>

                        <span class="prod-detail-label">Mô tả chi tiết:</span>
                        <span class="prod-detail-val" style="line-height: 1.6; color: var(--prod-text-sub);">
                            ${product != null ? product.description : 'Hệ thống CRM toàn diện trên nền tảng điện toán đám mây. Không giới hạn số lượng tài khoản người dùng, tích hợp tự động hóa quy trình chăm sóc khách hàng và báo cáo doanh số đa chiều.'}
                        </span>
                    </div>

                    <!-- Bảng giá liên kết -->
                    <div style="margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--prod-border);">
                        <h3 style="font-size: 1rem; color: var(--prod-text-main); margin-bottom: 12px;">Bảng giá đang áp dụng sản phẩm này</h3>
                        <table class="prod-table" style="background: #f8fafc; border-radius: 8px;">
                            <thead>
                                <tr>
                                    <th>Bảng giá</th>
                                    <th>Đối tượng áp dụng</th>
                                    <th>Đơn giá áp dụng</th>
                                    <th>Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td><strong>Bảng giá chuẩn 2026</strong></td>
                                    <td>Khách hàng toàn quốc</td>
                                    <td style="font-weight: 600;">24.000.000 ₫</td>
                                    <td><span class="prod-badge prod-badge-active">Hiệu lực</span></td>
                                </tr>
                                <tr>
                                    <td><strong>Bảng giá Doanh nghiệp VIP</strong></td>
                                    <td>Hợp đồng > 100 triệu</td>
                                    <td style="font-weight: 600; color: #16a34a;">21.600.000 ₫ (-10%)</td>
                                    <td><span class="prod-badge prod-badge-active">Hiệu lực</span></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <div style="display: flex; justify-content: flex-end; margin-top: 20px;">
                        <a href="${pageContext.request.contextPath}/products" class="btn-prod btn-prod-secondary">Quay lại danh mục</a>
                    </div>
                </section>

            </div>
        </main>
    </div>
</body>
</html>