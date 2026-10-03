<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List,com.crm.model.PipelineStage,com.crm.controller.ServerForms" %>
<%!
private String esc(Object value) {
    if (value == null) return "";
    return value.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
}
private String probClass(int p) {
    if (p >= 70) return "prob-badge--high";
    if (p >= 30) return "prob-badge--mid";
    return "prob-badge--low";
}
%>
<%
List<PipelineStage> stages = (List<PipelineStage>) request.getAttribute("stages");
if (stages == null) stages = java.util.Collections.emptyList();
PipelineStage edit = (PipelineStage) request.getAttribute("editStage");
long pipelineId = request.getAttribute("pipelineId") instanceof Number
        ? ((Number) request.getAttribute("pipelineId")).longValue() : 1L;
String activeFilter = (String) request.getAttribute("activeFilter");
if (activeFilter == null) activeFilter = "";
boolean manage = Boolean.TRUE.equals(request.getAttribute("canManage"));
String prefix = request.getContextPath();
String csrfToken = ServerForms.csrf(request);
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cấu hình Pipeline - CRM</title>
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/common.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/layout.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/header.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/sidebar.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/shared/components.css">
    <link rel="stylesheet" href="<%= esc(prefix) %>/css/pipeline/pipeline.css">
</head>
<body class="crm-body">
<jsp:include page="/jsp/shared/header.jsp"/>

<div class="crm-main-layout">
    <jsp:include page="/jsp/shared/sidebar.jsp"/>

    <main class="pipeline-page">
        <h1>Cấu hình giai đoạn bán hàng</h1>
        <p class="page-subtitle">Quản lý xác suất thắng và điều kiện chuyển bước trong quy trình bán hàng.</p>

        <%-- === Success Notice === --%>
        <% if (request.getAttribute("notice") != null) { %>
            <div class="pipeline-notice" role="status">
                <%= esc(request.getAttribute("notice")) %>
            </div>
        <% } %>

        <%-- === Stage List Section === --%>
        <section class="pipeline-card">
            <h2>Danh sách giai đoạn</h2>

            <%-- Filter Bar --%>
            <form method="get" action="<%= esc(prefix) %>/pipeline/page" class="pipeline-filter">
                <input type="hidden" name="pipelineId" value="<%= pipelineId %>">
                <label>
                    Trạng thái
                    <select name="active">
                        <option value="">Tất cả</option>
                        <option value="true" <%= "true".equals(activeFilter) ? "selected" : "" %>>Đang dùng</option>
                        <option value="false" <%= "false".equals(activeFilter) ? "selected" : "" %>>Ngừng dùng</option>
                    </select>
                </label>
                <button type="submit" class="pipeline-btn pipeline-btn--primary">Lọc</button>
                <a class="pipeline-btn" href="<%= esc(prefix) %>/pipeline/page?pipelineId=<%= pipelineId %>">Xóa lọc</a>
            </form>

            <%-- Stage Table --%>
            <div class="pipeline-scroll">
                <table class="pipeline-table">
                    <thead>
                        <tr>
                            <th class="col-order">Thứ tự</th>
                            <th>Mã</th>
                            <th>Tên giai đoạn</th>
                            <th>Xác suất thắng</th>
                            <th>Điều kiện rời bước</th>
                            <th>Loại</th>
                            <th>Trạng thái</th>
                            <th>Cơ hội</th>
                            <% if (manage) { %><th>Thao tác</th><% } %>
                        </tr>
                    </thead>
                    <tbody>
                    <% if (stages.isEmpty()) { %>
                        <tr>
                            <td colspan="<%= manage ? 9 : 8 %>">
                                <div class="pipeline-empty">
                                    <span class="pipeline-empty-icon">&#8709;</span>
                                    Chưa có giai đoạn nào phù hợp với bộ lọc.
                                </div>
                            </td>
                        </tr>
                    <% } else {
                        for (PipelineStage stage : stages) { %>
                        <tr>
                            <td class="col-order"><%= stage.getStageOrder() %></td>
                            <td><%= esc(stage.getCode()) %></td>
                            <td><strong><%= esc(stage.getName()) %></strong></td>
                            <td>
                                <span class="prob-badge <%= probClass(stage.getWinProbability()) %>">
                                    <%= stage.getWinProbability() %>%
                                </span>
                            </td>
                            <td>
                                <% if (stage.getRequirements() != null && !stage.getRequirements().isEmpty()) { %>
                                    <span class="req-text" title="<%= esc(stage.getRequirements()) %>"><%= esc(stage.getRequirements()) %></span>
                                <% } else { %>
                                    <span style="color:#9ca3af">—</span>
                                <% } %>
                            </td>
                            <td>
                                <% if (stage.isWon()) { %>
                                    <span class="type-badge type-badge--won">Thắng</span>
                                <% } else if (stage.isLost()) { %>
                                    <span class="type-badge type-badge--lost">Thua</span>
                                <% } else { %>
                                    <span class="type-badge type-badge--normal">Thông thường</span>
                                <% } %>
                            </td>
                            <td>
                                <% if (stage.isActive()) { %>
                                    <span class="status-badge status-badge--active">Đang dùng</span>
                                <% } else { %>
                                    <span class="status-badge status-badge--inactive">Ngừng dùng</span>
                                <% } %>
                            </td>
                            <td>
                                <span class="opp-count <%= stage.getOpportunityCount() > 0 ? "opp-count--nonzero" : "" %>">
                                    <%= stage.getOpportunityCount() %>
                                </span>
                            </td>
                            <% if (manage) { %>
                            <td>
                                <div class="pipeline-actions">
                                    <a href="<%= esc(prefix) %>/pipeline/page?pipelineId=<%= pipelineId %>&edit=<%= stage.getId() %>"
                                       class="pipeline-btn pipeline-btn--sm">Sửa</a>
                                </div>
                                <%-- Delete form --%>
                                <form method="post" action="<%= esc(prefix) %>/pipeline/page" class="pipeline-delete-form">
                                    <input type="hidden" name="csrfToken" value="<%= esc(csrfToken) %>">
                                    <input type="hidden" name="pipelineId" value="<%= pipelineId %>">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="<%= stage.getId() %>">
                                    <label>
                                        Chuyển cơ hội:
                                        <select name="targetStageId">
                                            <option value="">Không chuyển</option>
                                            <% for (PipelineStage candidate : stages) {
                                                if (!candidate.getId().equals(stage.getId())) { %>
                                                <option value="<%= candidate.getId() %>"><%= esc(candidate.getName()) %></option>
                                            <% }
                                            } %>
                                        </select>
                                    </label>
                                    <label>
                                        <input type="checkbox" name="confirm" value="yes" required>
                                        Xác nhận xóa
                                    </label>
                                    <button type="submit" class="pipeline-btn pipeline-btn--sm pipeline-btn--danger">Xóa</button>
                                </form>
                            </td>
                            <% } %>
                        </tr>
                    <%  }
                    } %>
                    </tbody>
                </table>
            </div>

            <%-- === Reorder Section (Admin only, via form POST) === --%>
            <% if (manage && stages.size() > 1) { %>
            <details class="reorder-section">
                <summary class="pipeline-btn" style="margin-bottom:10px;cursor:pointer">
                    &#8693; Sắp xếp lại thứ tự
                </summary>
                <form method="post" action="<%= esc(prefix) %>/pipeline/page">
                    <input type="hidden" name="csrfToken" value="<%= esc(csrfToken) %>">
                    <input type="hidden" name="pipelineId" value="<%= pipelineId %>">
                    <input type="hidden" name="action" value="reorder">
                    <ul class="reorder-list">
                        <% for (PipelineStage stage : stages) { %>
                        <li class="reorder-item">
                            <label>
                                Thứ tự:
                                <input type="number" name="order_<%= stage.getId() %>" min="1"
                                       value="<%= stage.getStageOrder() %>" required>
                            </label>
                            <span class="reorder-name"><%= esc(stage.getName()) %></span>
                            <span class="prob-badge <%= probClass(stage.getWinProbability()) %>">
                                <%= stage.getWinProbability() %>%
                            </span>
                        </li>
                        <% } %>
                    </ul>
                    <button type="submit" class="pipeline-btn pipeline-btn--primary">Lưu thứ tự</button>
                </form>
            </details>
            <% } %>
        </section>

        <%-- === Create / Edit Form (Admin Only) === --%>
        <% if (manage) { %>
        <section class="pipeline-card">
            <h2><%= edit == null ? "Thêm giai đoạn mới" : "Chỉnh sửa giai đoạn" %></h2>

            <form method="post" action="<%= esc(prefix) %>/pipeline/page" class="pipeline-form">
                <input type="hidden" name="csrfToken" value="<%= esc(csrfToken) %>">
                <input type="hidden" name="pipelineId" value="<%= pipelineId %>">
                <input type="hidden" name="action" value="<%= edit == null ? "create" : "update" %>">
                <% if (edit != null) { %>
                    <input type="hidden" name="id" value="<%= edit.getId() %>">
                <% } %>

                <label>
                    Mã giai đoạn <span style="color:#b91c1c">*</span>
                    <input type="text" name="code" maxlength="50" required
                           pattern="[A-Za-z0-9_\-]{1,50}"
                           title="Chỉ cho phép chữ, số, gạch ngang và gạch dưới (tối đa 50 ký tự)"
                           value="<%= edit == null ? "" : esc(edit.getCode()) %>">
                </label>

                <label>
                    Tên giai đoạn <span style="color:#b91c1c">*</span>
                    <input type="text" name="name" maxlength="150" required
                           value="<%= edit == null ? "" : esc(edit.getName()) %>">
                </label>

                <label>
                    Thứ tự <span style="color:#b91c1c">*</span>
                    <input type="number" name="stageOrder" min="1" max="99" required
                           value="<%= edit == null ? (stages.size() + 1) : edit.getStageOrder() %>">
                </label>

                <label>
                    Xác suất thắng (%) <span style="color:#b91c1c">*</span>
                    <input type="number" name="winProbability" min="0" max="100" required
                           value="<%= edit == null ? 50 : edit.getWinProbability() %>">
                </label>

                <label>
                    Loại giai đoạn
                    <select name="outcome">
                        <option value="normal" <%= (edit != null && !edit.isWon() && !edit.isLost()) || edit == null ? "selected" : "" %>>Thông thường</option>
                        <option value="won" <%= edit != null && edit.isWon() ? "selected" : "" %>>Thắng (Won)</option>
                        <option value="lost" <%= edit != null && edit.isLost() ? "selected" : "" %>>Thua (Lost)</option>
                    </select>
                </label>

                <label>
                    Trạng thái
                    <select name="active">
                        <option value="true" <%= edit == null || edit.isActive() ? "selected" : "" %>>Đang dùng</option>
                        <option value="false" <%= edit != null && !edit.isActive() ? "selected" : "" %>>Ngừng dùng</option>
                    </select>
                </label>

                <label class="full-width">
                    Điều kiện rời giai đoạn (Exit Criteria)
                    <textarea rows="3" name="requirements" maxlength="2000"
                              placeholder="Mô tả điều kiện cần hoàn thành trước khi chuyển sang bước tiếp theo..."><%= edit == null ? "" : esc(edit.getRequirements()) %></textarea>
                </label>

                <div class="pipeline-form-actions">
                    <button type="submit" class="pipeline-btn pipeline-btn--primary">
                        <%= edit == null ? "Thêm giai đoạn" : "Lưu thay đổi" %>
                    </button>
                    <% if (edit != null) { %>
                        <a href="<%= esc(prefix) %>/pipeline/page?pipelineId=<%= pipelineId %>"
                           class="pipeline-btn">Hủy sửa</a>
                    <% } %>
                </div>
            </form>
        </section>
        <% } %>
    </main>
</div>
</body>
</html>
