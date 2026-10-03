<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,com.crm.model.PipelineStage,com.crm.controller.ServerForms,com.crm.util.Html" %>
<%!
private String esc(Object value) {
    if (value == null) return "";
    return Html.escape(String.valueOf(value));
}

public static class DemoStep {
    public long id;
    public String code;
    public String name;
    public String title;
    public int prob;
    public String probClass;
    public String ruleCountText;
    public boolean isWon;
    public boolean isLost;
    public String cardClass;

    public DemoStep(long id, String code, String name, String title, int prob, String probClass, String ruleCountText, boolean isWon, boolean isLost, String cardClass) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.title = title;
        this.prob = prob;
        this.probClass = probClass;
        this.ruleCountText = ruleCountText;
        this.isWon = isWon;
        this.isLost = isLost;
        this.cardClass = cardClass;
    }
}
%>
<%
List<PipelineStage> dbStages = (List<PipelineStage>) request.getAttribute("stages");
if (dbStages == null) dbStages = Collections.emptyList();
PipelineStage editStageAttr = (PipelineStage) request.getAttribute("editStage");
long pipelineId = request.getAttribute("pipelineId") instanceof Number
        ? ((Number) request.getAttribute("pipelineId")).longValue() : 1L;
String activeFilter = (String) request.getAttribute("activeFilter");
if (activeFilter == null) activeFilter = "";
boolean manage = Boolean.TRUE.equals(request.getAttribute("canManage"));
String prefix = request.getContextPath();
String csrfToken = ServerForms.csrf(request);

// Determine selected stage ID (defaults to Stage #3)
long selectedStageId = 3L;
String stageParam = request.getParameter("stageId");
if (stageParam != null && !stageParam.isBlank()) {
    try { selectedStageId = Long.parseLong(stageParam.trim()); } catch (Exception ignored) {}
} else if (editStageAttr != null) {
    selectedStageId = editStageAttr.getId();
} else if (request.getParameter("edit") != null) {
    try { selectedStageId = Long.parseLong(request.getParameter("edit").trim()); } catch (Exception ignored) {}
}

// Build standard stepper model
List<DemoStep> stepperList = new ArrayList<>();
stepperList.add(new DemoStep(1L, "STG-PROSPECT", "1. Khảo sát nhu cầu", "Khảo sát nhu cầu", 10, "prob-badge-blue", "1 điều kiện", false, false, ""));
stepperList.add(new DemoStep(2L, "STG-SOLUTION", "2. Demo & Giải pháp", "Demo & Giải pháp", 30, "prob-badge-blue", "2 điều kiện", false, false, ""));
stepperList.add(new DemoStep(3L, "STG-QUOTE", "3. Gửi Báo giá / Đề xuất", "Gửi Báo giá / Đề xuất", 60, "prob-badge-blue", "3 điều kiện bắt buộc", false, false, ""));
stepperList.add(new DemoStep(4L, "STG-NEGOTIATE", "4. Đàm phán & Thương lượng", "Đàm phán & Thương lượng", 80, "prob-badge-blue", "2 điều kiện", false, false, ""));
stepperList.add(new DemoStep(5L, "STG-WON", "5. Chốt Thắng (Won)", "Chốt Thắng (Won)", 100, "prob-badge-green", "1 điều kiện", true, false, "step-won"));
stepperList.add(new DemoStep(6L, "STG-LOST", "6. Đóng Thua (Lost)", "Đóng Thua (Lost)", 0, "prob-badge-red", "1 điều kiện", false, true, "step-lost"));

// If database has stages, enrich name and probability
for (DemoStep ds : stepperList) {
    for (PipelineStage ps : dbStages) {
        if (ps.getId().equals(ds.id)) {
            ds.prob = ps.getWinProbability();
            ds.code = ps.getCode();
            break;
        }
    }
}

// Find currently selected step
DemoStep activeStep = stepperList.get(2); // default step #3
for (DemoStep ds : stepperList) {
    if (ds.id == selectedStageId) {
        activeStep = ds;
        break;
    }
}
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cấu hình Pipeline Bán hàng | CRM ICTU</title>

    <!-- CSS dùng chung của hệ thống -->
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/header.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/components.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/pipeline/pipeline.css">

    <!-- Khối CSS chuẩn doanh nghiệp nhúng trực tiếp tránh mất style -->
    <style>
        .pipeline-page-wrapper {
            flex: 1;
            min-width: 0;
            width: 100%;
            padding: 24px 32px;
            background-color: #f6f8fb;
            min-height: calc(100vh - 64px);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            color: #1e293b;
            box-sizing: border-box;
        }
        .pipeline-container {
            max-width: 1400px;
            margin: 0 auto;
            width: 100%;
            box-sizing: border-box;
        }
        .pipeline-breadcrumb {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
            font-size: 0.8125rem;
            color: #64748b;
            margin-bottom: 16px;
            line-height: 1.4;
        }
        .pipeline-breadcrumb a {
            color: #64748b;
            text-decoration: none;
            transition: color 150ms ease;
        }
        .pipeline-breadcrumb a:hover {
            color: #2563eb;
        }
        .pipeline-breadcrumb .sep {
            color: #cbd5e1;
            font-size: 0.75rem;
        }
        .pipeline-breadcrumb .current {
            color: #1e293b;
            font-weight: 500;
        }
        .pipeline-header {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 16px;
            margin-bottom: 24px;
        }
        .pipeline-header-left {
            flex: 1 1 500px;
        }
        .pipeline-title {
            font-size: 1.5rem;
            font-weight: 700;
            line-height: 1.3;
            color: #0f172a;
            margin: 0 0 6px 0;
            letter-spacing: -0.02em;
        }
        .pipeline-subtitle {
            font-size: 0.875rem;
            color: #64748b;
            margin: 0;
            line-height: 1.5;
        }
        .pipeline-header-actions {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 10px;
        }
        .pipeline-btn-primary {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            background-color: #2563eb;
            color: #ffffff !important;
            font-size: 0.875rem;
            font-weight: 600;
            padding: 8px 16px;
            border-radius: 6px;
            border: 1px solid #2563eb;
            text-decoration: none;
            box-shadow: 0 1px 2px rgba(37, 99, 235, 0.2);
            transition: background-color 150ms ease, box-shadow 150ms ease;
            cursor: pointer;
        }
        .pipeline-btn-primary:hover {
            background-color: #1d4ed8;
            border-color: #1d4ed8;
            box-shadow: 0 2px 4px rgba(37, 99, 235, 0.3);
        }
        .pipeline-btn-outline {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            background-color: #ffffff;
            color: #334155 !important;
            font-size: 0.875rem;
            font-weight: 600;
            padding: 8px 16px;
            border-radius: 6px;
            border: 1px solid #cbd5e1;
            text-decoration: none;
            box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
            transition: all 150ms ease;
            cursor: pointer;
        }
        .pipeline-btn-outline:hover {
            background-color: #f8fafc;
            border-color: #94a3b8;
            color: #0f172a !important;
        }
        .pipeline-stepper-wrap {
            margin-bottom: 24px;
        }
        .pipeline-stepper {
            display: flex;
            gap: 12px;
            overflow-x: auto;
            padding-bottom: 8px;
        }
        .pipeline-step-card {
            flex: 1 1 160px;
            min-width: 160px;
            background-color: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            padding: 12px 14px;
            text-decoration: none;
            transition: all 150ms ease;
            box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
            cursor: pointer;
            box-sizing: border-box;
        }
        .pipeline-step-card:hover {
            border-color: #94a3b8;
            background-color: #f8fafc;
        }
        .pipeline-step-name {
            font-size: 0.8125rem;
            font-weight: 600;
            color: #1e293b;
            margin-bottom: 4px;
            display: block;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .pipeline-step-prob {
            font-size: 0.75rem;
            color: #64748b;
            display: block;
        }
        .pipeline-step-card.active {
            background-color: #eff6ff;
            border: 2px solid #2563eb;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15);
        }
        .pipeline-step-card.active .pipeline-step-name {
            color: #1d4ed8;
            font-weight: 700;
        }
        .pipeline-step-card.active .pipeline-step-prob {
            color: #2563eb;
            font-weight: 600;
        }
        .pipeline-step-card.step-won {
            background-color: #f0fdf4;
            border: 1px solid #86efac;
        }
        .pipeline-step-card.step-won .pipeline-step-name {
            color: #15803d;
        }
        .pipeline-step-card.step-won .pipeline-step-prob {
            color: #16a34a;
        }
        .pipeline-step-card.step-lost {
            background-color: #fef2f2;
            border: 1px solid #fca5a5;
        }
        .pipeline-step-card.step-lost .pipeline-step-name {
            color: #b91c1c;
        }
        .pipeline-step-card.step-lost .pipeline-step-prob {
            color: #dc2626;
        }
        .pipeline-split-layout {
            display: flex;
            gap: 24px;
            align-items: flex-start;
        }
        .pipeline-col-left {
            flex: 0 0 40%;
            min-width: 320px;
            box-sizing: border-box;
        }
        .pipeline-col-right {
            flex: 0 0 calc(60% - 24px);
            min-width: 420px;
            box-sizing: border-box;
        }
        .pipeline-panel-card {
            background-color: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
            box-sizing: border-box;
            overflow: hidden;
        }
        .pipeline-panel-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 16px 20px;
            border-bottom: 1px solid #e2e8f0;
        }
        .pipeline-panel-title {
            font-size: 0.9375rem;
            font-weight: 700;
            color: #0f172a;
            margin: 0;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .pipeline-panel-note {
            font-size: 0.75rem;
            color: #94a3b8;
            font-weight: 500;
        }
        .pipeline-code-badge {
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            font-size: 0.75rem;
            color: #475569;
            background-color: #f1f5f9;
            border: 1px solid #e2e8f0;
            padding: 3px 8px;
            border-radius: 4px;
            font-weight: 600;
        }
        .pipeline-stage-list {
            padding: 12px;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        .pipeline-stage-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 10px 14px;
            background-color: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 6px;
            transition: all 150ms ease;
            text-decoration: none;
            color: inherit;
            box-sizing: border-box;
        }
        .pipeline-stage-row:hover {
            background-color: #f8fafc;
            border-color: #cbd5e1;
        }
        .pipeline-stage-row.active {
            background-color: #eff6ff;
            border: 1.5px solid #2563eb;
            box-shadow: 0 1px 3px rgba(37, 99, 235, 0.1);
        }
        .stage-row-left {
            display: flex;
            align-items: center;
            gap: 10px;
            min-width: 0;
        }
        .drag-handle-icon {
            color: #94a3b8;
            font-size: 1rem;
            letter-spacing: -2px;
            cursor: grab;
            user-select: none;
        }
        .stage-row-title {
            font-size: 0.875rem;
            font-weight: 600;
            color: #1e293b;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .pipeline-stage-row.active .stage-row-title {
            color: #1d4ed8;
        }
        .stage-row-right {
            display: flex;
            align-items: center;
            gap: 8px;
            flex-shrink: 0;
        }
        .stage-prob-badge {
            padding: 2px 7px;
            border-radius: 12px;
            font-size: 0.75rem;
            font-weight: 700;
            line-height: 1.3;
        }
        .prob-badge-blue {
            background-color: #eff6ff;
            color: #1d4ed8;
            border: 1px solid #bfdbfe;
        }
        .prob-badge-green {
            background-color: #ecfdf5;
            color: #047857;
            border: 1px solid #a7f3d0;
        }
        .prob-badge-red {
            background-color: #fef2f2;
            color: #b91c1c;
            border: 1px solid #fecaca;
        }
        .stage-rule-tag {
            font-size: 0.75rem;
            color: #64748b;
            background-color: #f8fafc;
            border: 1px solid #e2e8f0;
            padding: 2px 6px;
            border-radius: 4px;
        }
        .btn-edit-text {
            font-size: 0.8125rem;
            color: #2563eb;
            font-weight: 500;
            text-decoration: none;
            padding: 2px 6px;
        }
        .btn-edit-text:hover {
            text-decoration: underline;
        }
        .pipeline-form-body {
            padding: 24px;
            box-sizing: border-box;
        }
        .pipeline-form-group {
            margin-bottom: 20px;
        }
        .pipeline-label {
            display: block;
            font-size: 0.8125rem;
            font-weight: 600;
            color: #334155;
            margin-bottom: 6px;
        }
        .pipeline-label .required {
            color: #ef4444;
        }
        .pipeline-input-text {
            width: 100%;
            height: 40px;
            padding: 0 12px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            font-size: 0.875rem;
            color: #0f172a;
            background-color: #ffffff;
            box-sizing: border-box;
            transition: border-color 150ms ease, box-shadow 150ms ease;
            outline: none;
        }
        .pipeline-input-text:focus {
            border-color: #2563eb;
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
        }
        .pipeline-field-hint {
            display: block;
            font-size: 0.75rem;
            color: #64748b;
            margin-top: 4px;
        }
        .gate-rules-section {
            margin-top: 24px;
            padding-top: 20px;
            border-top: 1px solid #e2e8f0;
        }
        .gate-rules-title {
            font-size: 0.875rem;
            font-weight: 700;
            color: #0f172a;
            margin: 0 0 14px 0;
        }
        .gate-rules-list {
            display: flex;
            flex-direction: column;
            gap: 10px;
            margin-bottom: 24px;
        }
        .gate-rule-item {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            padding: 12px 14px;
            border: 1px solid #e2e8f0;
            border-radius: 6px;
            background-color: #ffffff;
            transition: border-color 150ms ease;
        }
        .gate-rule-item:hover {
            border-color: #cbd5e1;
            background-color: #fafbfc;
        }
        .gate-rule-left {
            display: flex;
            align-items: flex-start;
            gap: 10px;
            flex: 1;
        }
        .gate-rule-checkbox {
            margin-top: 3px;
            width: 16px;
            height: 16px;
            accent-color: #2563eb;
            cursor: pointer;
        }
        .gate-rule-content {
            flex: 1;
        }
        .gate-rule-name {
            font-size: 0.84375rem;
            font-weight: 600;
            color: #1e293b;
            display: block;
            line-height: 1.4;
        }
        .gate-rule-desc {
            font-size: 0.75rem;
            color: #64748b;
            margin-top: 2px;
            display: block;
            line-height: 1.4;
        }
        .gate-rule-badge-required {
            display: inline-block;
            padding: 3px 8px;
            border-radius: 4px;
            font-size: 0.6875rem;
            font-weight: 700;
            background-color: #ecfdf5;
            color: #047857;
            border: 1px solid #a7f3d0;
            flex-shrink: 0;
        }
        .gate-rule-badge-optional {
            display: inline-block;
            padding: 3px 8px;
            border-radius: 4px;
            font-size: 0.6875rem;
            font-weight: 500;
            background-color: #f1f5f9;
            color: #64748b;
            border: 1px solid #e2e8f0;
            flex-shrink: 0;
        }
        .pipeline-form-actions-footer {
            display: flex;
            align-items: center;
            gap: 10px;
            padding-top: 18px;
            border-top: 1px solid #e2e8f0;
            flex-wrap: wrap;
        }
        .pipeline-btn-delete {
            margin-left: auto;
            background-color: #ffffff;
            color: #dc2626 !important;
            border: 1px solid #fecaca;
            padding: 8px 16px;
            border-radius: 6px;
            font-size: 0.875rem;
            font-weight: 500;
            text-decoration: none;
            transition: all 150ms ease;
            cursor: pointer;
        }
        .pipeline-btn-delete:hover {
            background-color: #fef2f2;
            border-color: #f87171;
        }
        @media (max-width: 1024px) {
            .pipeline-split-layout { flex-direction: column; }
            .pipeline-col-left, .pipeline-col-right { flex: 1 1 100%; width: 100%; }
        }
        @media (max-width: 768px) {
            .pipeline-page-wrapper { padding: 16px; }
            .pipeline-header { flex-direction: column; }
            .pipeline-header-actions { width: 100%; }
            .pipeline-form-actions-footer { flex-direction: column; align-items: stretch; }
            .pipeline-btn-delete { margin-left: 0; text-align: center; }
        }
    </style>
</head>
<body class="crm-body">

    <!-- Header dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/header.jsp"/>

    <div class="crm-main-layout">
        <!-- Sidebar dùng chung của hệ thống -->
        <jsp:include page="/jsp/shared/sidebar.jsp"/>

        <main class="pipeline-page-wrapper" role="main">
            <div class="pipeline-container">

                <!-- 1. BREADCRUMB -->
                <nav class="pipeline-breadcrumb" aria-label="Đường dẫn">
                    <a href="<%= esc(prefix) %>/dashboard">Trang chủ</a>
                    <span class="sep">/</span>
                    <span>Cấu hình Hệ thống</span>
                    <span class="sep">/</span>
                    <span>Quy trình Bán hàng</span>
                    <span class="sep">/</span>
                    <span class="current">Pipeline</span>
                </nav>

                <!-- 2. BỐ CỤC ĐẦU TRANG (HEADER & ACTIONS) -->
                <header class="pipeline-header">
                    <div class="pipeline-header-left">
                        <h1 class="pipeline-title">Thiết lập Chuỗi Giai đoạn Pipeline &amp; Điều kiện Chuyển bước</h1>
                        <p class="pipeline-subtitle">Sprint 2 • S2-09 • Quản lý các bước cơ hội, xác suất thắng trong số (Win Rate %) và cổng kiểm soát điều kiện (Gate Rules)</p>
                    </div>
                    <div class="pipeline-header-actions">
                        <a href="<%= esc(prefix) %>/pipeline/page?pipelineId=<%= pipelineId %>&amp;stageId=1" class="pipeline-btn-primary">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                            + Thêm giai đoạn
                        </a>
                        <form method="post" action="<%= esc(prefix) %>/pipeline/page" style="display:inline-block; margin:0;">
                            <input type="hidden" name="csrfToken" value="<%= esc(csrfToken) %>">
                            <input type="hidden" name="pipelineId" value="<%= pipelineId %>">
                            <input type="hidden" name="action" value="reorder">
                            <button type="submit" class="pipeline-btn-outline">Lưu quy trình</button>
                        </form>
                    </div>
                </header>

                <% if (request.getAttribute("notice") != null) { %>
                    <div class="pipeline-alert" role="status">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                        <%= esc(request.getAttribute("notice")) %>
                    </div>
                <% } %>

                <!-- 3. THANH QUY TRÌNH CHUỖI BÁN HÀNG TRỰC QUAN (PIPELINE STEPPER) -->
                <nav class="pipeline-stepper-wrap" aria-label="Chuỗi bước quy trình bán hàng">
                    <div class="pipeline-stepper">
                        <% for (DemoStep step : stepperList) {
                            boolean isSelected = (step.id == selectedStageId);
                        %>
                        <a href="<%= esc(prefix) %>/pipeline/page?pipelineId=<%= pipelineId %>&amp;stageId=<%= step.id %>"
                           class="pipeline-step-card <%= isSelected ? "active" : "" %> <%= esc(step.cardClass) %>">
                            <span class="pipeline-step-name"><%= esc(step.name) %></span>
                            <span class="pipeline-step-prob">Xác suất: <%= step.prob %>%<%= isSelected ? " (Đang chọn)" : "" %></span>
                        </a>
                        <% } %>
                    </div>
                </nav>

                <!-- 4. BỐ CỤC 2 CỘT ENTERPRISE (SPLIT VIEW) -->
                <div class="pipeline-split-layout">

                    <!-- A. CỘT TRÁI - THỨ TỰ GIAI ĐOẠN TRONG CHUỖI BÁN HÀNG -->
                    <section class="pipeline-col-left" aria-labelledby="left-col-heading">
                        <article class="pipeline-panel-card">
                            <div class="pipeline-panel-header">
                                <h2 class="pipeline-panel-title" id="left-col-heading">Thứ tự Giai đoạn trong Chuỗi bán hàng</h2>
                                <span class="pipeline-panel-note">Kéo thả đổi thứ tự</span>
                            </div>
                            <div class="pipeline-stage-list">
                                <% for (DemoStep step : stepperList) {
                                    boolean isSelected = (step.id == selectedStageId);
                                %>
                                <a href="<%= esc(prefix) %>/pipeline/page?pipelineId=<%= pipelineId %>&amp;stageId=<%= step.id %>"
                                   class="pipeline-stage-row <%= isSelected ? "active" : "" %>">
                                    <div class="stage-row-left">
                                        <span class="drag-handle-icon" aria-hidden="true">⋮⋮</span>
                                        <span class="stage-row-title"><%= esc(step.name) %></span>
                                    </div>
                                    <div class="stage-row-right">
                                        <span class="stage-prob-badge <%= esc(step.probClass) %>"><%= step.prob %>%</span>
                                        <span class="stage-rule-tag"><%= esc(step.ruleCountText) %></span>
                                        <span class="btn-edit-text">Sửa</span>
                                    </div>
                                </a>
                                <% } %>
                            </div>
                        </article>
                    </section>

                    <!-- B. CỘT PHẢI - CHI TIẾT GIAI ĐOẠN ĐANG CHỌN & CẤU HÌNH GATE RULES -->
                    <section class="pipeline-col-right" aria-labelledby="right-col-heading">
                        <article class="pipeline-panel-card">
                            <div class="pipeline-panel-header">
                                <h2 class="pipeline-panel-title" id="right-col-heading">
                                    Chi tiết Giai đoạn: <%= esc(activeStep.title) %> (Stage #<%= activeStep.id %>)
                                </h2>
                                <span class="pipeline-code-badge">MÃ: <%= esc(activeStep.code) %></span>
                            </div>

                            <form method="post" action="<%= esc(prefix) %>/pipeline/page" class="pipeline-form-body">
                                <input type="hidden" name="csrfToken" value="<%= esc(csrfToken) %>">
                                <input type="hidden" name="pipelineId" value="<%= pipelineId %>">
                                <input type="hidden" name="action" value="update">
                                <input type="hidden" name="id" value="<%= activeStep.id %>">
                                <input type="hidden" name="code" value="<%= esc(activeStep.code) %>">
                                <input type="hidden" name="stageOrder" value="<%= activeStep.id %>">
                                <input type="hidden" name="active" value="true">
                                <input type="hidden" name="outcome" value="<%= activeStep.isWon ? "won" : (activeStep.isLost ? "lost" : "normal") %>">

                                <!-- Khối thông tin cơ bản -->
                                <div class="pipeline-form-group">
                                    <label class="pipeline-label">Tên giai đoạn bán hàng <span class="required">*</span></label>
                                    <input type="text" name="name" class="pipeline-input-text" value="<%= esc(activeStep.title) %>" required maxlength="150">
                                </div>

                                <div class="pipeline-form-group">
                                    <label class="pipeline-label">Xác suất thắng dự báo (Win Rate %) <span class="required">*</span></label>
                                    <input type="number" name="winProbability" class="pipeline-input-text" value="<%= activeStep.prob %>" min="0" max="100" required>
                                    <span class="pipeline-field-hint">Trong dự báo doanh thu</span>
                                </div>

                                <!-- Khối cấu hình Gate Rules -->
                                <div class="gate-rules-section">
                                    <h3 class="gate-rules-title">Cấu hình Điều kiện bắt buộc để chuyển sang giai đoạn kế tiếp (Gate Rules):</h3>

                                    <div class="gate-rules-list">
                                        <!-- Điều kiện 1 -->
                                        <div class="gate-rule-item">
                                            <div class="gate-rule-left">
                                                <input type="checkbox" checked class="gate-rule-checkbox" id="rule1" name="rule_quote_approved" value="1">
                                                <div class="gate-rule-content">
                                                    <label for="rule1" class="gate-rule-name">[V] Bắt buộc có ít nhất 1 Báo giá ở trạng thái ĐÃ DUYỆT</label>
                                                    <span class="gate-rule-desc">Chặn nhân viên kéo sang đàm phán nếu chưa có báo giá chính thức</span>
                                                </div>
                                            </div>
                                            <span class="gate-rule-badge-required">Bắt buộc</span>
                                        </div>

                                        <!-- Điều kiện 2 -->
                                        <div class="gate-rule-item">
                                            <div class="gate-rule-left">
                                                <input type="checkbox" checked class="gate-rule-checkbox" id="rule2" name="rule_amount_positive" value="1">
                                                <div class="gate-rule-content">
                                                    <label for="rule2" class="gate-rule-name">[V] Tổng giá trị thương vụ phải &gt; 0 đ (Đã chọn sản phẩm)</label>
                                                </div>
                                            </div>
                                            <span class="gate-rule-badge-required">Bắt buộc</span>
                                        </div>

                                        <!-- Điều kiện 3 -->
                                        <div class="gate-rule-item">
                                            <div class="gate-rule-left">
                                                <input type="checkbox" checked class="gate-rule-checkbox" id="rule3" name="rule_activity_done" value="1">
                                                <div class="gate-rule-content">
                                                    <label for="rule3" class="gate-rule-name">[V] Phải hoàn tất ít nhất 1 hoạt động gặp gỡ hoặc cuộc gọi trao đổi</label>
                                                </div>
                                            </div>
                                            <span class="gate-rule-badge-required">Bắt buộc</span>
                                        </div>

                                        <!-- Điều kiện 4 -->
                                        <div class="gate-rule-item">
                                            <div class="gate-rule-left">
                                                <input type="checkbox" class="gate-rule-checkbox" id="rule4" name="rule_doc_uploaded" value="1">
                                                <div class="gate-rule-content">
                                                    <label for="rule4" class="gate-rule-name">[ ] Bắt buộc tải lên tài liệu RFP / Hồ sơ yêu cầu kỹ thuật</label>
                                                </div>
                                            </div>
                                            <span class="gate-rule-badge-optional">Tùy chọn</span>
                                        </div>
                                    </div>
                                </div>

                                <input type="hidden" name="requirements" value="Bắt buộc có ít nhất 1 Báo giá ở trạng thái ĐÃ DUYỆT; Tổng giá trị thương vụ phải > 0 đ; Phải hoàn tất ít nhất 1 hoạt động gặp gỡ hoặc cuộc gọi trao đổi">

                                <!-- Hàng nút hành động dưới cùng -->
                                <div class="pipeline-form-actions-footer">
                                    <button type="submit" class="pipeline-btn-primary">Lưu giai đoạn này</button>
                                    <a href="#add-rule" class="pipeline-btn-outline">+ Thêm quy tắc</a>
                                    <a href="<%= esc(prefix) %>/pipeline/page?pipelineId=<%= pipelineId %>&amp;delete=<%= activeStep.id %>" class="pipeline-btn-delete">Xóa bước</a>
                                </div>
                            </form>
                        </article>
                    </section>

                </div>

            </div>
        </main>
    </div>

    <!-- Footer dùng chung của hệ thống -->
    <jsp:include page="/jsp/shared/footer.jsp"/>
</body>
</html>
