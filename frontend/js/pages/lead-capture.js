/**
 * ===================================================================
 * CRM-113 / S4-01: THU THẬP LEAD TỪ BIỂU MẪU NHÚNG TRÊN WEBSITE
 * Phụ trách: Senior Frontend Developer (Nông Quang Tiệp)
 * 
 * - Trình tạo mã nhúng (Embed Code Generator UI) cho Marketing
 * - Xem trước trực tiếp theo thời gian thực (Live Preview)
 * - Quản lý tập trung Lead, Bộ lọc 1 hàng chuẩn Enterprise
 * - Thu thập và đọc Lead từ API/MySQL
 * - Tích hợp chuyển đổi Lead thành Khách hàng Doanh nghiệp
 * ===================================================================
 */

"use strict";

(function () {
    let registeredFormUrl = null;
    let loadingLeads = false;
    let preparingCapture = false;

    // Trạng thái ứng dụng
    let allLeads = [];
    let currentSelectedLead = null;

    // DOM Elements
    const dom = {
        // Tabs
        tabBtnGenerator: document.getElementById("tabBtnGenerator"),
        tabBtnLeads: document.getElementById("tabBtnLeads"),
        paneGenerator: document.getElementById("paneGenerator"),
        paneLeads: document.getElementById("paneLeads"),
        badgeLeadsCount: document.getElementById("badgeLeadsCount"),

        // Generator Config
        cfgFormTitle: document.getElementById("cfgFormTitle"),
        cfgFormSubtitle: document.getElementById("cfgFormSubtitle"),
        cfgBtnText: document.getElementById("cfgBtnText"),
        colorPresetsContainer: document.getElementById("colorPresetsContainer"),
        themeOptionsGrid: document.getElementById("themeOptionsGrid"),
        cfgShowCompany: document.getElementById("cfgShowCompany"),
        cfgShowNotes: document.getElementById("cfgShowNotes"),
        txtEmbedCode: document.getElementById("txtEmbedCode"),
        btnCopyEmbedCode: document.getElementById("btnCopyEmbedCode"),
        captureStatus: document.getElementById("leadCaptureStatus"),
        captureStatusText: document.getElementById("leadCaptureStatusText"),
        btnRetryCapture: document.getElementById("btnRetryCapture"),
        listStatus: document.getElementById("leadListStatus"),

        // Live Preview
        livePreviewIframe: document.getElementById("livePreviewIframe"),
        deviceSwitcher: document.getElementById("deviceSwitcher"),

        // Quick Actions
        btnGenerateDemoLead: document.getElementById("btnGenerateDemoLead"),
        btnEmptyCreateLead: document.getElementById("btnEmptyCreateLead"),

        // Filter Bar (Single Row)
        filterSearch: document.getElementById("filterSearch"),
        filterStatus: document.getElementById("filterStatus"),
        filterSource: document.getElementById("filterSource"),
        filterSort: document.getElementById("filterSort"),
        btnResetFilters: document.getElementById("btnResetFilters"),

        // KPI
        kpiTotalLeads: document.getElementById("kpiTotalLeads"),
        kpiTodayLeads: document.getElementById("kpiTodayLeads"),
        kpiPhoneValidPercent: document.getElementById("kpiPhoneValidPercent"),
        kpiConvertedLeads: document.getElementById("kpiConvertedLeads"),

        // Table & Empty
        leadsTableBody: document.getElementById("leadsTableBody"),
        leadsEmptyState: document.getElementById("leadsEmptyState"),

        // Modal
        leadDetailModal: document.getElementById("leadDetailModal"),
        leadModalBody: document.getElementById("leadModalBody"),
        btnCloseLeadModal: document.getElementById("btnCloseLeadModal"),
        btnModalCancel: document.getElementById("btnModalCancel"),
        btnModalMarkContacted: document.getElementById("btnModalMarkContacted"),
        btnModalConvertCustomer: document.getElementById("btnModalConvertCustomer"),

        // Toast Container
        toastContainer: document.getElementById("leadToastContainer")
    };

    // Current Generator State
    const generatorState = {
        title: "Đăng ký tư vấn giải pháp CRM",
        subtitle: "Để lại thông tin, chuyên viên tư vấn sẽ liên hệ lại trong vòng 15 phút.",
        btnText: "Nhận tư vấn miễn phí",
        accentColor: "#2563eb",
        theme: "auto",
        showCompany: true,
        showNotes: true,
        device: "desktop"
    };

    /* ===================================================================
       1. INITIALIZATION
       =================================================================== */

    async function init() {
        setCaptureActions(false);
        bindNavigationTabs();
        bindGeneratorEvents();
        bindFilterEvents();
        bindModalEvents();
        bindWindowListeners();
        dom.btnRetryCapture?.addEventListener("click", prepareCapture);
        updateEmbedCodeAndPreview();
        await prepareCapture();
    }

    function setCaptureActions(enabled) {
        [dom.btnGenerateDemoLead, dom.btnEmptyCreateLead, dom.btnCopyEmbedCode]
            .forEach(button => { if (button) button.disabled = !enabled; });
        const openLink = document.getElementById("btnOpenPublicForm");
        openLink?.setAttribute("aria-disabled", String(!enabled));
        if (!enabled) openLink?.removeAttribute("href");
    }

    function captureStatus(message, retry = false) {
        if (dom.captureStatus) dom.captureStatus.hidden = !message;
        if (dom.captureStatusText) dom.captureStatusText.textContent = message;
        if (dom.btnRetryCapture) {
            dom.btnRetryCapture.hidden = !retry;
            dom.btnRetryCapture.disabled = preparingCapture;
        }
    }

    function apiErrorMessage(error, action) {
        const explanations = {
            401: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
            403: "Tài khoản không có quyền thực hiện thao tác này.",
            404: "Không tìm thấy API Lead Capture trên máy chủ đang chạy.",
            500: "Máy chủ chưa thể xử lý biểu mẫu."
        };
        const status = error.status ? ` (HTTP ${error.status})` : "";
        const details = explanations[error.status] || "Kiểm tra kết nối và thử lại.";
        return `${action}${status}: ${details} ${error.message || ""}`.trim();
    }

    async function prepareCapture() {
        if (preparingCapture) return;
        preparingCapture = true;
        setCaptureActions(false);
        captureStatus("Đang chuẩn bị biểu mẫu thu thập Lead…");
        let listRequest;
        try {
            const session = await CrmApi.session();
            if (!session.permissions?.includes("lead.read")) {
                const denied = new Error("Bạn không có quyền xem Lead");
                denied.status = 403;
                throw denied;
            }
            // Listing must remain available if preparing a new public form fails.
            listRequest = loadLeadsData();
            if (session.permissions?.includes("lead.create")) {
                const embed = await CrmApi.request("/api/leads/embed-code?formId=crm-web-lead-form");
                if (!embed?.formUrl || !embed?.formId) throw new Error("Máy chủ trả về biểu mẫu không hợp lệ");
                registeredFormUrl = embed.formUrl;
                updateEmbedCodeAndPreview();
                setCaptureActions(true);
                captureStatus("");
            } else {
                captureStatus("Bạn có quyền xem Lead, nhưng chưa được cấp quyền tạo biểu mẫu.");
            }
        } catch (error) {
            registeredFormUrl = null;
            setCaptureActions(false);
            updateEmbedCodeAndPreview();
            captureStatus(apiErrorMessage(error, "Không thể chuẩn bị mã nhúng"), ![401, 403].includes(error.status));
        } finally {
            preparingCapture = false;
            if (dom.btnRetryCapture) dom.btnRetryCapture.disabled = false;
            if (listRequest) await listRequest;
        }
    }

    /* ===================================================================
       2. TABS SWITCHING
       =================================================================== */

    function bindNavigationTabs() {
        if (dom.tabBtnGenerator && dom.tabBtnLeads) {
            dom.tabBtnGenerator.addEventListener("click", () => switchTab("generator"));
            dom.tabBtnLeads.addEventListener("click", () => switchTab("leads"));
        }
    }

    function switchTab(tabId) {
        if (tabId === "generator") {
            dom.tabBtnGenerator.classList.add("active");
            dom.tabBtnLeads.classList.remove("active");
            dom.paneGenerator.style.display = "block";
            dom.paneLeads.style.display = "none";
        } else {
            dom.tabBtnGenerator.classList.remove("active");
            dom.tabBtnLeads.classList.add("active");
            dom.paneGenerator.style.display = "none";
            dom.paneLeads.style.display = "block";
            // Làm mới bảng dữ liệu khi chuyển tab
            loadLeadsData();
        }
    }

    /* ===================================================================
       3. EMBED CODE GENERATOR & LIVE PREVIEW
       =================================================================== */

    function bindGeneratorEvents() {
        // Inputs text
        const updateText = () => {
            generatorState.title = dom.cfgFormTitle.value.trim() || "Đăng ký tư vấn giải pháp CRM";
            generatorState.subtitle = dom.cfgFormSubtitle.value.trim();
            generatorState.btnText = dom.cfgBtnText.value.trim() || "Gửi yêu cầu";
            updateEmbedCodeAndPreview();
        };

        if (dom.cfgFormTitle) dom.cfgFormTitle.addEventListener("input", debounce(updateText, 300));
        if (dom.cfgFormSubtitle) dom.cfgFormSubtitle.addEventListener("input", debounce(updateText, 300));
        if (dom.cfgBtnText) dom.cfgBtnText.addEventListener("input", debounce(updateText, 300));

        // Color Presets
        if (dom.colorPresetsContainer) {
            dom.colorPresetsContainer.addEventListener("click", (e) => {
                const dot = e.target.closest(".color-dot");
                if (!dot) return;
                dom.colorPresetsContainer.querySelectorAll(".color-dot").forEach(d => d.classList.remove("active"));
                dot.classList.add("active");
                generatorState.accentColor = dot.dataset.color || "#2563eb";
                updateEmbedCodeAndPreview();
            });
        }

        // Theme Options (Auto, Light, Dark)
        if (dom.themeOptionsGrid) {
            dom.themeOptionsGrid.addEventListener("click", (e) => {
                const btn = e.target.closest(".theme-option-btn");
                if (!btn) return;
                dom.themeOptionsGrid.querySelectorAll(".theme-option-btn").forEach(b => b.classList.remove("active"));
                btn.classList.add("active");
                generatorState.theme = btn.dataset.theme || "auto";
                updateEmbedCodeAndPreview();
            });
        }

        // Checkboxes fields
        if (dom.cfgShowCompany) {
            dom.cfgShowCompany.addEventListener("change", (e) => {
                generatorState.showCompany = e.target.checked;
                updateEmbedCodeAndPreview();
            });
        }

        if (dom.cfgShowNotes) {
            dom.cfgShowNotes.addEventListener("change", (e) => {
                generatorState.showNotes = e.target.checked;
                updateEmbedCodeAndPreview();
            });
        }

        // Device Switcher
        if (dom.deviceSwitcher) {
            dom.deviceSwitcher.addEventListener("click", (e) => {
                const btn = e.target.closest(".device-btn");
                if (!btn) return;
                dom.deviceSwitcher.querySelectorAll(".device-btn").forEach(b => b.classList.remove("active"));
                btn.classList.add("active");
                generatorState.device = btn.dataset.device || "desktop";

                // Update iframe container class
                dom.livePreviewIframe.className = `preview-iframe-frame ${generatorState.device}`;
            });
        }

        // Copy Button
        if (dom.btnCopyEmbedCode) {
            dom.btnCopyEmbedCode.addEventListener("click", copyEmbedCodeToClipboard);
        }

        // Open the registered form for real data entry
        if (dom.btnGenerateDemoLead) {
            dom.btnGenerateDemoLead.addEventListener("click", openCaptureForm);
        }
        if (dom.btnEmptyCreateLead) {
            dom.btnEmptyCreateLead.addEventListener("click", openCaptureForm);
        }
    }

    /**
     * Đồng bộ URL vào Iframe và tạo thẻ nhúng HTML
     */
    function updateEmbedCodeAndPreview() {
        const formUrl = new URL(registeredFormUrl || `${CrmApi.base}/lead-capture-form.html`);
        const params = formUrl.searchParams;
        if (!registeredFormUrl) params.set("preview", "true");
        if (generatorState.title) params.set("title", generatorState.title);
        if (generatorState.subtitle) params.set("subtitle", generatorState.subtitle);
        if (generatorState.btnText) params.set("btn", generatorState.btnText);
        if (generatorState.accentColor) params.set("color", generatorState.accentColor);
        params.set("theme", generatorState.theme);
        if (!generatorState.showCompany) params.set("company", "0");
        if (!generatorState.showNotes) params.set("notes", "0");

        const queryString = params.toString();
        const fullPreviewSrc = `${formUrl.origin}${formUrl.pathname}?${queryString}`;
        if (registeredFormUrl) document.getElementById("btnOpenPublicForm").href = fullPreviewSrc;

        // Cập nhật Live Preview
        if (dom.livePreviewIframe) {
            dom.livePreviewIframe.src = fullPreviewSrc;
        }

        // Sinh mã nhúng
        const absoluteUrl = new URL(fullPreviewSrc, window.location.href).href;
        const embedSnippet = `<iframe src="${absoluteUrl}" width="100%" height="560" style="border:none; border-radius:14px; box-shadow:0 15px 35px rgba(0,0,0,0.1); max-width:540px; margin:0 auto; display:block;" title="Biểu mẫu thu thập Lead Website" loading="lazy"></iframe>`;

        if (dom.txtEmbedCode) {
            dom.txtEmbedCode.value = registeredFormUrl ? embedSnippet : "";
        }
    }

    /**
     * Sao chép mã nhúng vào Clipboard
     */
    function copyEmbedCodeToClipboard() {
        if (!dom.txtEmbedCode) return;
        const code = dom.txtEmbedCode.value;
        if (!code) return;

        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(code).then(() => {
                showToast("Đã sao chép mã nhúng vào Clipboard!", "success");
            }).catch(() => {
                fallbackCopy(code);
            });
        } else {
            fallbackCopy(code);
        }
    }

    function fallbackCopy(text) {
        dom.txtEmbedCode.select();
        if (document.execCommand("copy")) {
            showToast("Đã sao chép mã nhúng vào Clipboard!", "success");
        } else {
            showToast("Trình duyệt chưa cho phép sao chép. Hãy chọn mã và nhấn Ctrl+C.", "warning");
        }
    }

    /* ===================================================================
       4. LEAD DATA MANAGEMENT
       =================================================================== */

    /**
     * Tải danh sách Lead thực tế từ Database thông qua REST API
     */
    async function loadLeadsData() {
        if (loadingLeads) return;
        loadingLeads = true;
        if (dom.listStatus) dom.listStatus.hidden = true;
        try {
            const list = [];
            for (let page = 1; ; page++) {
                const data = await CrmApi.request(`/api/leads?page=${page}&size=100`);
                list.push(...data.items);
                if (page >= data.totalPages || !data.items.length) break;
            }
            allLeads = list;
        } catch (error) {
            const message = apiErrorMessage(error, "Không thể tải danh sách Lead");
            if (dom.listStatus) {
                dom.listStatus.textContent = message;
                dom.listStatus.hidden = false;
            }
            showToast(message, "danger");
        } finally {
            loadingLeads = false;
            updateKpiCounters();
            renderLeadsTable();
        }
    }

    /**
     * Cập nhật các chỉ số KPI trên đầu trang
     */
    function updateKpiCounters() {
        const total = allLeads.length;
        const today = new Date().toDateString();
        const todayCount = allLeads.filter(l => new Date(l.createdAt).toDateString() === today).length;
        const phoneValidCount = allLeads.filter(l => isValidPhone(l.phone)).length;
        const convertedCount = allLeads.filter(l => l.status === "CONVERTED").length;

        if (dom.kpiTotalLeads) dom.kpiTotalLeads.textContent = total;
        if (dom.kpiTodayLeads) dom.kpiTodayLeads.textContent = todayCount;
        if (dom.kpiPhoneValidPercent) {
            const pct = total > 0 ? Math.round((phoneValidCount / total) * 100) : 100;
            dom.kpiPhoneValidPercent.textContent = `${pct}%`;
        }
        if (dom.kpiConvertedLeads) dom.kpiConvertedLeads.textContent = convertedCount;
        if (dom.badgeLeadsCount) dom.badgeLeadsCount.textContent = total;
    }

    /* ===================================================================
       5. FILTER TOOLBAR (STRICT SINGLE ROW) & TABLE RENDERING
       =================================================================== */

    function bindFilterEvents() {
        if (dom.filterSearch) dom.filterSearch.addEventListener("input", debounce(renderLeadsTable, 250));
        if (dom.filterStatus) dom.filterStatus.addEventListener("change", renderLeadsTable);
        if (dom.filterSource) dom.filterSource.addEventListener("change", renderLeadsTable);
        if (dom.filterSort) dom.filterSort.addEventListener("change", renderLeadsTable);

        if (dom.btnResetFilters) {
            dom.btnResetFilters.addEventListener("click", () => {
                if (dom.filterSearch) dom.filterSearch.value = "";
                if (dom.filterStatus) dom.filterStatus.value = "ALL";
                if (dom.filterSource) dom.filterSource.value = "ALL";
                if (dom.filterSort) dom.filterSort.value = "newest";
                renderLeadsTable();
                showToast("Đã đặt lại bộ lọc tìm kiếm", "info");
            });
        }
    }

    /**
     * Lọc và sắp xếp danh sách Lead
     */
    function getFilteredLeads() {
        const query = (dom.filterSearch ? dom.filterSearch.value.trim().toLowerCase() : "");
        const status = dom.filterStatus ? dom.filterStatus.value : "ALL";
        const source = dom.filterSource ? dom.filterSource.value : "ALL";
        const sort = dom.filterSort ? dom.filterSort.value : "newest";

        return allLeads.filter(lead => {
            // Search query
            if (query) {
                const matchName = (lead.fullName || "").toLowerCase().includes(query);
                const matchEmail = (lead.email || "").toLowerCase().includes(query);
                const matchPhone = (lead.phone || "").toLowerCase().includes(query);
                const matchCompany = (lead.companyName || "").toLowerCase().includes(query);
                const matchNotes = (lead.notes || "").toLowerCase().includes(query);
                if (!matchName && !matchEmail && !matchPhone && !matchCompany && !matchNotes) {
                    return false;
                }
            }

            // Status filter
            if (status !== "ALL" && lead.status !== status) {
                return false;
            }

            // Source filter
            if (source !== "ALL" && lead.source !== source) {
                return false;
            }

            return true;
        }).sort((a, b) => {
            if (sort === "newest") {
                return new Date(b.createdAt) - new Date(a.createdAt);
            } else if (sort === "oldest") {
                return new Date(a.createdAt) - new Date(b.createdAt);
            } else if (sort === "name_asc") {
                return (a.fullName || "").localeCompare(b.fullName || "");
            } else if (sort === "company_asc") {
                return (a.companyName || "").localeCompare(b.companyName || "");
            }
            return 0;
        });
    }

    /**
     * Render bảng danh sách Lead
     */
    function renderLeadsTable() {
        if (!dom.leadsTableBody) return;

        const filtered = getFilteredLeads();

        if (filtered.length === 0) {
            dom.leadsTableBody.innerHTML = "";
            if (dom.leadsEmptyState) dom.leadsEmptyState.style.display = "block";
            return;
        }

        if (dom.leadsEmptyState) dom.leadsEmptyState.style.display = "none";

        let html = "";
        filtered.forEach((lead, idx) => {
            const initials = getInitials(lead.fullName);
            const statusMeta = getStatusBadge(lead.status);
            const formattedTime = formatTimestamp(lead.createdAt);

            html += `
                <tr data-lead-id="${escapeHtml(lead.id)}">
                    <td style="text-align: center; color: var(--crm-muted); font-weight: 600;">${idx + 1}</td>
                    
                    <!-- Họ tên & Người liên hệ -->
                    <td>
                        <div class="lead-user-cell">
                            <div class="lead-avatar">${escapeHtml(initials)}</div>
                            <div class="lead-user-meta">
                                <span class="lead-user-name">${escapeHtml(lead.fullName)}</span>
                                <span class="lead-user-id">Mã: ${escapeHtml(lead.id)}</span>
                            </div>
                        </div>
                    </td>

                    <!-- Liên hệ (Email / Phone) -->
                    <td>
                        <div class="lead-contact-block">
                            <a href="mailto:${escapeHtml(lead.email)}" class="lead-contact-line">
                                <i class="fa-solid fa-envelope text-primary"></i> ${escapeHtml(lead.email)}
                            </a>
                            <a href="tel:${escapeHtml(lead.phone)}" class="lead-contact-line">
                                <i class="fa-solid fa-phone text-success"></i> ${escapeHtml(lead.phone)}
                            </a>
                        </div>
                    </td>

                    <!-- Doanh nghiệp -->
                    <td>
                        <div class="lead-company-cell">
                            <strong>${escapeHtml(lead.companyName || "Chưa cung cấp")}</strong>
                        </div>
                    </td>

                    <!-- Ghi chú / Nhu cầu -->
                    <td>
                        <div class="lead-notes-cell" title="${escapeHtml(lead.notes || "Không có ghi chú")}">
                            ${escapeHtml(lead.notes ? truncate(lead.notes, 55) : "—")}
                        </div>
                    </td>

                    <!-- Nguồn Lead -->
                    <td>
                        <span class="source-tag">
                            <i class="fa-solid fa-globe"></i> ${escapeHtml(lead.source || "Website Form")}
                        </span>
                    </td>

                    <!-- Trạng thái -->
                    <td>
                        <span class="status-pill ${statusMeta.cls}">
                            <i class="fa-solid ${statusMeta.icon}"></i> ${statusMeta.label}
                        </span>
                    </td>

                    <!-- Thời gian gửi -->
                    <td style="font-size: 12.5px; color: var(--crm-muted); white-space: nowrap;">
                        ${formattedTime}
                    </td>

                    <!-- Thao tác -->
                    <td style="text-align: right;">
                        <div class="lead-action-btns">
                            <button type="button" class="btn-table-action btn-view-lead" data-action="view" data-id="${escapeHtml(lead.id)}" title="Xem chi tiết">
                                <i class="fa-solid fa-eye"></i>
                            </button>
                            ${lead.status !== "CONVERTED" ? `
                                <button type="button" class="btn-table-action convert btn-convert-lead" data-action="convert" data-id="${escapeHtml(lead.id)}" title="Chuyển thành Khách hàng CRM">
                                    <i class="fa-solid fa-user-check"></i>
                                </button>
                            ` : ""}
                            <button type="button" class="btn-table-action delete btn-delete-lead" data-action="delete" data-id="${escapeHtml(lead.id)}" title="Xóa Lead">
                                <i class="fa-solid fa-trash-can"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        });

        dom.leadsTableBody.innerHTML = html;
        bindTableActionButtons();
    }

    /**
     * Bắt sự kiện trên các nút thao tác trong bảng
     */
    function bindTableActionButtons() {
        if (!dom.leadsTableBody) return;

        dom.leadsTableBody.querySelectorAll(".btn-table-action").forEach(btn => {
            btn.addEventListener("click", (e) => {
                const target = e.currentTarget;
                const action = target.dataset.action;
                const id = target.dataset.id;
                const lead = allLeads.find(l => String(l.id) === String(id));
                if (!lead) return;

                if (action === "view") {
                    openLeadDetailModal(lead);
                } else if (action === "convert") {
                    handleConvertLeadToCustomer(lead);
                } else if (action === "delete") {
                    handleDeleteLead(lead);
                }
            });
        });
    }

    /* ===================================================================
       6. CONVERT LEAD TO CUSTOMER (CRM INTEGRATION)
       =================================================================== */

    /**
     * Chuyển đổi Lead thành Khách hàng chính thức trong hệ thống CRM
     */
    function handleConvertLeadToCustomer(lead) {
        showToast("Chức năng chuyển đổi Lead hiện chưa khả dụng.", "warning");
    }

    /**
     * Xóa Lead khỏi danh sách
     */
    function handleDeleteLead(lead) {
        showToast("Chức năng xóa Lead hiện chưa khả dụng. Dữ liệu chưa được thay đổi.", "warning");
    }

    function openCaptureForm() {
        if (!registeredFormUrl) return showToast("Biểu mẫu chưa sẵn sàng hoặc bạn không có quyền tạo Lead", "warning");
        window.open(document.getElementById("btnOpenPublicForm").href, "_blank", "noopener");
    }

    /* ===================================================================
       7. LEAD DETAIL MODAL
       =================================================================== */

    function bindModalEvents() {
        if (dom.btnCloseLeadModal) dom.btnCloseLeadModal.addEventListener("click", closeLeadDetailModal);
        if (dom.btnModalCancel) dom.btnModalCancel.addEventListener("click", closeLeadDetailModal);

        if (dom.leadDetailModal) {
            dom.leadDetailModal.addEventListener("click", (e) => {
                if (e.target === dom.leadDetailModal) closeLeadDetailModal();
            });
        }

        if (dom.btnModalMarkContacted) {
            dom.btnModalMarkContacted.addEventListener("click", () => {
                if (!currentSelectedLead) return;
                showToast("Chức năng cập nhật trạng thái hiện chưa khả dụng. Dữ liệu chưa được thay đổi.", "warning");
            });
        }

        if (dom.btnModalConvertCustomer) {
            dom.btnModalConvertCustomer.addEventListener("click", () => {
                if (!currentSelectedLead) return;
                handleConvertLeadToCustomer(currentSelectedLead);
            });
        }
    }

    function openLeadDetailModal(lead) {
        currentSelectedLead = lead;
        const statusMeta = getStatusBadge(lead.status);

        dom.leadModalBody.innerHTML = `
            <div style="display: flex; gap: 16px; align-items: center; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid var(--crm-border);">
                <div class="lead-avatar" style="width: 56px; height: 56px; font-size: 20px;">
                    ${escapeHtml(getInitials(lead.fullName))}
                </div>
                <div>
                    <h3 style="margin: 0; font-size: 18px; font-weight: 700; color: var(--crm-text);">${escapeHtml(lead.fullName)}</h3>
                    <div style="font-size: 13px; color: var(--crm-muted); margin-top: 4px;">
                        Mã định danh: <strong>${escapeHtml(lead.id)}</strong> &bull; Thu thập: <strong>${formatTimestamp(lead.createdAt)}</strong>
                    </div>
                </div>
                <div style="margin-left: auto;">
                    <span class="status-pill ${statusMeta.cls}">
                        <i class="fa-solid ${statusMeta.icon}"></i> ${statusMeta.label}
                    </span>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;">
                <div class="config-card" style="padding: 14px;">
                    <span style="font-size: 11.5px; font-weight: 700; text-transform: uppercase; color: var(--crm-muted); display: block; margin-bottom: 6px;">Số điện thoại</span>
                    <a href="tel:${escapeHtml(lead.phone)}" style="font-size: 15px; font-weight: 700; color: var(--crm-primary); text-decoration: none;">
                        <i class="fa-solid fa-phone"></i> ${escapeHtml(lead.phone)}
                    </a>
                </div>

                <div class="config-card" style="padding: 14px;">
                    <span style="font-size: 11.5px; font-weight: 700; text-transform: uppercase; color: var(--crm-muted); display: block; margin-bottom: 6px;">Địa chỉ Email</span>
                    <a href="mailto:${escapeHtml(lead.email)}" style="font-size: 15px; font-weight: 700; color: var(--crm-primary); text-decoration: none;">
                        <i class="fa-solid fa-envelope"></i> ${escapeHtml(lead.email)}
                    </a>
                </div>
            </div>

            <div class="config-card" style="padding: 14px; margin-bottom: 16px;">
                <span style="font-size: 11.5px; font-weight: 700; text-transform: uppercase; color: var(--crm-muted); display: block; margin-bottom: 6px;">Doanh nghiệp / Đơn vị công tác</span>
                <div style="font-size: 14.5px; font-weight: 600; color: var(--crm-text);">
                    <i class="fa-solid fa-building text-primary"></i> ${escapeHtml(lead.companyName || "Chưa cung cấp")}
                </div>
            </div>

            <div class="config-card" style="padding: 14px;">
                <span style="font-size: 11.5px; font-weight: 700; text-transform: uppercase; color: var(--crm-muted); display: block; margin-bottom: 6px;">Nội dung nhu cầu & Ghi chú từ Form</span>
                <p style="margin: 0; font-size: 13.5px; color: var(--crm-text-2); line-height: 1.6; white-space: pre-wrap;">${escapeHtml(lead.notes || "Khách hàng không để lại ghi chú cụ thể.")}</p>
            </div>
        `;

        // Điều khiển trạng thái nút trong modal
        if (lead.status === "CONVERTED") {
            dom.btnModalConvertCustomer.style.display = "none";
            dom.btnModalMarkContacted.style.display = "none";
        } else {
            dom.btnModalConvertCustomer.style.display = "inline-flex";
            dom.btnModalMarkContacted.style.display = "inline-flex";
        }

        dom.leadDetailModal.classList.add("active");
    }

    function closeLeadDetailModal() {
        if (dom.leadDetailModal) dom.leadDetailModal.classList.remove("active");
        currentSelectedLead = null;
    }

    /* ===================================================================
       8. WINDOW LISTENERS
       =================================================================== */

    function bindWindowListeners() {
        window.addEventListener("focus", () => { if (registeredFormUrl) loadLeadsData(); });
        // Nhận postMessage từ Iframe khi người dùng bấm gửi Form trong Live Preview
        window.addEventListener("message", (event) => {
            if (!event.data || !registeredFormUrl ||
                event.origin !== new URL(registeredFormUrl).origin ||
                event.source !== dom.livePreviewIframe?.contentWindow) return;
            if (event.data.type === "CRM_LEAD_SUBMITTED" || event.data.type === "crm-form-submitted") {
                const newLead = event.data.lead;
                if (newLead) {
                    showToast(`Nhận được Lead mới: "${newLead.fullName}" từ Web Form!`, "success");
                    loadLeadsData();
                }
            } else if (event.data.type === "crm-form-resize" && event.data.height && dom.livePreviewIframe) {
                const targetH = Math.min(620, Math.max(520, Math.round(event.data.height)));
                dom.livePreviewIframe.style.height = `${targetH}px`;
            }
        });
    }

    /* ===================================================================
       9. UTILITIES & HELPERS
       =================================================================== */

    function getInitials(name) {
        if (!name) return "L";
        const parts = name.trim().split(/\s+/);
        if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
        return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    function getStatusBadge(status) {
        switch (status) {
            case "NEW":
                return { cls: "status-new", label: "Mới (Chưa liên hệ)", icon: "fa-circle-dot" };
            case "PENDING_ASSIGNMENT":
                return { cls: "status-contacted", label: "Chờ phân bổ", icon: "fa-clock" };
            case "CONTACTED":
                return { cls: "status-contacted", label: "Đã liên hệ", icon: "fa-phone-flip" };
            case "CONVERTED":
                return { cls: "status-converted", label: "Đã chuyển đổi", icon: "fa-circle-check" };
            case "REJECTED":
                return { cls: "status-rejected", label: "Không liên lạc được", icon: "fa-circle-xmark" };
            default:
                return { cls: "status-contacted", label: "Trạng thái khác", icon: "fa-circle-question" };
        }
    }

    function isValidPhone(phone) {
        if (!phone) return false;
        return /^(0|\+84)[0-9]{9}$/.test(phone.replace(/\s+/g, ""));
    }

    function formatTimestamp(isoStr) {
        if (!isoStr) return "—";
        try {
            const date = new Date(isoStr);
            const now = new Date();
            const diffMs = now - date;
            const diffMin = Math.floor(diffMs / 60000);
            const diffHour = Math.floor(diffMs / 3600000);

            if (diffMin < 1) return "Vừa xong";
            if (diffMin < 60) return `${diffMin} phút trước`;
            if (diffHour < 24) return `${diffHour} giờ trước`;

            return date.toLocaleDateString("vi-VN", {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit"
            });
        } catch (e) {
            return isoStr;
        }
    }

    function truncate(str, len) {
        if (!str) return "";
        return str.length > len ? str.substring(0, len) + "..." : str;
    }

    function escapeHtml(str) {
        if (!str) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function debounce(fn, delay) {
        let timer = null;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
    }

    function showToast(message, type = "info") {
        if (!dom.toastContainer) return;

        const toast = document.createElement("div");
        toast.className = `crm-lead-toast ${type}`;

        let iconCls = "fa-circle-info";
        if (type === "success") iconCls = "fa-circle-check";
        if (type === "warning") iconCls = "fa-triangle-exclamation";
        if (type === "danger") iconCls = "fa-circle-xmark";

        toast.innerHTML = `
            <i class="fa-solid ${iconCls}"></i>
            <span>${escapeHtml(message)}</span>
        `;

        dom.toastContainer.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = "toastOut 0.3s forwards ease";
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    }

    // Khởi chạy khi DOM sẵn sàng
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
