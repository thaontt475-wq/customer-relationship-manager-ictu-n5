/**
 * ===================================================================
 * CRM-113 / S4-01: THU THẬP LEAD TỪ BIỂU MẪU NHÚNG TRÊN WEBSITE
 * Phụ trách: Senior Frontend Developer (Nông Quang Tiệp)
 * 
 * - Trình tạo mã nhúng (Embed Code Generator UI) cho Marketing
 * - Xem trước trực tiếp theo thời gian thực (Live Preview)
 * - Quản lý tập trung Lead, Bộ lọc 1 hàng chuẩn Enterprise
 * - Fallback RESTful API an toàn sang LocalStorage (crm_captured_leads)
 * - Tích hợp chuyển đổi Lead thành Khách hàng Doanh nghiệp
 * ===================================================================
 */

"use strict";

(function () {
    const STORAGE_KEY_LEADS = "crm_captured_leads";
    const STORAGE_KEY_CUSTOMERS = "crm_ui_customers";
    const API_ENDPOINT = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`) + "/api/leads";

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
       1. INITIALIZATION & MOCK SEEDING
       =================================================================== */

    function init() {
        seedInitialLeadsIfEmpty();
        bindNavigationTabs();
        bindGeneratorEvents();
        bindFilterEvents();
        bindModalEvents();
        bindWindowListeners();

        // Initial Load
        loadLeadsData();
        updateEmbedCodeAndPreview();
    }

    /**
     * Dữ liệu mẫu chuẩn Enterprise B2B phục vụ Demo & Test
     */
    function seedInitialLeadsIfEmpty() {
        try {
            const stored = localStorage.getItem(STORAGE_KEY_LEADS);
            if (!stored || JSON.parse(stored).length === 0) {
                const sampleLeads = [
                    {
                        id: "LEAD-101",
                        fullName: "Nguyễn Minh Quang",
                        phone: "0987123456",
                        email: "quang.nm@digitech.vn",
                        companyName: "Công ty Cổ phần Công nghệ DigiTech VN",
                        notes: "Cần báo giá và tư vấn triển khai CRM cho đội ngũ 35 nhân viên Sales B2B.",
                        source: "Website Form",
                        status: "NEW", // NEW, CONTACTED, CONVERTED, REJECTED
                        createdAt: new Date(Date.now() - 45 * 60 * 1000).toISOString() // 45 phút trước
                    },
                    {
                        id: "LEAD-102",
                        fullName: "Trần Thu Hà",
                        phone: "0912345678",
                        email: "ha.tran@hoalinhpharma.com",
                        companyName: "Tập đoàn Dược phẩm Hoa Linh",
                        notes: "Quan tâm đến giải pháp quản trị hợp đồng điện tử và phân quyền chi nhánh.",
                        source: "Website Form",
                        status: "CONTACTED",
                        createdAt: new Date(Date.now() - 3 * 3600 * 1000).toISOString() // 3 giờ trước
                    },
                    {
                        id: "LEAD-103",
                        fullName: "Lê Hoàng Nam",
                        phone: "0905678910",
                        email: "nam.lh@datxanhgroup.vn",
                        companyName: "Bất động sản Đất Xanh Miền Bắc",
                        notes: "Đang tìm hiểu phân hệ tích hợp Lead Form tự động từ 5 Landing Page dự án.",
                        source: "Landing Page",
                        status: "CONVERTED",
                        createdAt: new Date(Date.now() - 26 * 3600 * 1000).toISOString() // hôm qua
                    },
                    {
                        id: "LEAD-104",
                        fullName: "Phạm Thị Mai",
                        phone: "0933221100",
                        email: "mai.pt@bachhoasach.vn",
                        companyName: "Chuỗi Siêu thị Bách Hóa Sạch",
                        notes: "Cần đặt lịch hẹn demo trực tiếp vào thứ 6 tuần này qua Google Meet.",
                        source: "Website Form",
                        status: "NEW",
                        createdAt: new Date(Date.now() - 20 * 60 * 1000).toISOString() // 20 phút trước
                    }
                ];
                localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(sampleLeads));
            }
        } catch (e) {
            console.error("Lỗi khi khởi tạo seed data Lead:", e);
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
            renderLeadsTable();
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

        // Generate Demo Lead button
        if (dom.btnGenerateDemoLead) {
            dom.btnGenerateDemoLead.addEventListener("click", handleGenerateRandomLead);
        }
        if (dom.btnEmptyCreateLead) {
            dom.btnEmptyCreateLead.addEventListener("click", handleGenerateRandomLead);
        }
    }

    /**
     * Đồng bộ URL vào Iframe và tạo thẻ nhúng HTML
     */
    function updateEmbedCodeAndPreview() {
        const origin = window.location.origin;
        // Đường dẫn tương đối hoặc tuyệt đối tới lead-capture-form.html
        let formUrl = "lead-capture-form.html";

        const params = new URLSearchParams();
        if (generatorState.title) params.append("title", generatorState.title);
        if (generatorState.subtitle) params.append("subtitle", generatorState.subtitle);
        if (generatorState.btnText) params.append("btn", generatorState.btnText);
        if (generatorState.accentColor) params.append("color", generatorState.accentColor);
        if (generatorState.theme !== "auto") params.append("theme", generatorState.theme);
        if (!generatorState.showCompany) params.append("company", "0");
        if (!generatorState.showNotes) params.append("notes", "0");

        const queryString = params.toString();
        const fullPreviewSrc = `${formUrl}?${queryString}`;

        // Cập nhật Live Preview
        if (dom.livePreviewIframe) {
            dom.livePreviewIframe.src = fullPreviewSrc;
        }

        // Sinh mã nhúng
        const absoluteUrl = `${origin}/crm/${formUrl}?${queryString}`;
        const embedSnippet = `<iframe src="${absoluteUrl}" width="100%" height="560" style="border:none; border-radius:14px; box-shadow:0 15px 35px rgba(0,0,0,0.1); max-width:540px; margin:0 auto; display:block;" title="Biểu mẫu thu thập Lead Website" loading="lazy"></iframe>`;

        if (dom.txtEmbedCode) {
            dom.txtEmbedCode.value = embedSnippet;
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
        document.execCommand("copy");
        showToast("Đã sao chép mã nhúng vào Clipboard!", "success");
    }

    /* ===================================================================
       4. LEAD DATA MANAGEMENT & REST API FALLBACK
       =================================================================== */

    /**
     * Tải danh sách Lead từ BE (POST /api/leads) hoặc fallback localStorage
     */
    async function loadLeadsData() {
        try {
            // Thử gọi REST API BE của Tiến
            const response = await fetch(API_ENDPOINT, {
                method: "GET",
                headers: { "Accept": "application/json" }
            });

            if (response.ok) {
                const data = await response.json();
                if (Array.isArray(data)) {
                    allLeads = data;
                    localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(data));
                }
            } else {
                fallbackReadLeads();
            }
        } catch (err) {
            // BE chưa chạy hoặc endpoint chưa có -> đọc an toàn từ localStorage
            fallbackReadLeads();
        }

        updateKpiCounters();
        renderLeadsTable();
    }

    function fallbackReadLeads() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_LEADS);
            allLeads = raw ? JSON.parse(raw) : [];
        } catch (e) {
            allLeads = [];
        }
    }

    function saveLeadsLocally() {
        try {
            localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(allLeads));
        } catch (e) {
            console.error("Lỗi lưu localStorage:", e);
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
                const lead = allLeads.find(l => l.id === id);
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
        if (!lead) return;

        if (lead.status === "CONVERTED") {
            showToast("Lead này đã được chuyển thành Khách hàng trước đó!", "warning");
            return;
        }

        const confirmMsg = `Bạn có chắc chắn muốn chuyển đổi Lead "${lead.fullName}" (${lead.companyName || "Cá nhân"}) thành Khách hàng chính thức trong CRM?`;
        if (!window.confirm(confirmMsg)) return;

        // 1. Cập nhật trạng thái Lead
        lead.status = "CONVERTED";
        lead.convertedAt = new Date().toISOString();
        saveLeadsLocally();

        // 2. Thêm vào kho khách hàng UI (LocalStorage crm_ui_customers)
        try {
            let customers = [];
            const rawCustomers = localStorage.getItem(STORAGE_KEY_CUSTOMERS);
            if (rawCustomers) {
                customers = JSON.parse(rawCustomers);
            }

            const newCustomerId = `CUST-${Date.now().toString().slice(-4)}`;
            const newCustomer = {
                id: newCustomerId,
                code: `KH-${Date.now().toString().slice(-4)}`,
                name: lead.companyName || lead.fullName,
                contactPerson: lead.fullName,
                phone: lead.phone,
                email: lead.email,
                address: "Chưa cập nhật",
                taxCode: "",
                industry: "Công nghệ / Dịch vụ",
                source: "Website Form (CRM-113)",
                status: "ACTIVE",
                notes: `Chuyển đổi từ Lead Web Form ngày ${new Date().toLocaleDateString("vi-VN")}. Nhu cầu ban đầu: ${lead.notes || "Không"}`,
                createdAt: new Date().toISOString()
            };

            customers.unshift(newCustomer);
            localStorage.setItem(STORAGE_KEY_CUSTOMERS, JSON.stringify(customers));
        } catch (e) {
            console.warn("Không thể lưu trực tiếp vào crm_ui_customers:", e);
        }

        // 3. Phản hồi giao diện
        updateKpiCounters();
        renderLeadsTable();
        if (dom.leadDetailModal && dom.leadDetailModal.classList.contains("active")) {
            closeLeadDetailModal();
        }

        showToast(`Đã chuyển đổi thành công Khách hàng "${lead.companyName || lead.fullName}"!`, "success");
    }

    /**
     * Xóa Lead khỏi danh sách
     */
    function handleDeleteLead(lead) {
        if (!lead) return;
        if (!window.confirm(`Xác nhận xóa Lead "${lead.fullName}" khỏi hệ thống?`)) return;

        allLeads = allLeads.filter(l => l.id !== lead.id);
        saveLeadsLocally();
        updateKpiCounters();
        renderLeadsTable();
        showToast("Đã xóa Lead thành công", "info");
    }

    /**
     * Tạo nhanh 1 Lead Test ngẫu nhiên để kiểm thử tương tác
     */
    function handleGenerateRandomLead() {
        const sampleNames = ["Vũ Đức Đạt", "Hoàng Kim Ngân", "Đặng Thanh Tùng", "Bùi Mai Hương", "Phan Quốc Bảo"];
        const sampleCompanies = ["Tập đoàn SunTech Global", "Công ty CP Logistics Á Châu", "Thương mại Điện tử VinaShop", "Nội thất Hiện đại EcoHome", "Tài chính Số FinSmart"];
        const sampleNeeds = [
            "Tư vấn gói CRM Enterprise cho 100 nhân sự",
            "Cần kết nối API từ Website bán hàng sang CRM",
            "Muốn dùng thử bản Demo quản lý công việc và báo giá",
            "Cần tư vấn phân hệ quản lý Người liên hệ CRM-62"
        ];

        const randName = sampleNames[Math.floor(Math.random() * sampleNames.length)];
        const randComp = sampleCompanies[Math.floor(Math.random() * sampleCompanies.length)];
        const randNeed = sampleNeeds[Math.floor(Math.random() * sampleNeeds.length)];
        const randPhone = "09" + Math.floor(10000000 + Math.random() * 90000000);
        const emailSlug = randName.toLowerCase().replace(/\s+/g, ".") + "@" + randComp.toLowerCase().replace(/[^a-z0-9]/g, "") + ".vn";

        const newLead = {
            id: `LEAD-${Date.now().toString().slice(-4)}`,
            fullName: randName,
            phone: randPhone,
            email: emailSlug,
            companyName: randComp,
            notes: randNeed,
            source: "Website Form",
            status: "NEW",
            createdAt: new Date().toISOString()
        };

        allLeads.unshift(newLead);
        saveLeadsLocally();
        updateKpiCounters();
        renderLeadsTable();

        showToast(`Đã tạo Lead mẫu "${randName}" từ Web Form!`, "success");
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
                currentSelectedLead.status = "CONTACTED";
                saveLeadsLocally();
                updateKpiCounters();
                renderLeadsTable();
                closeLeadDetailModal();
                showToast("Đã đánh dấu Lead là ĐÃ LIÊN HỆ TƯ VẤN", "info");
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
       8. WINDOW LISTENERS (POSTMESSAGE & STORAGE SYNC)
       =================================================================== */

    function bindWindowListeners() {
        // Nhận postMessage từ Iframe khi người dùng bấm gửi Form trong Live Preview
        window.addEventListener("message", (event) => {
            if (!event.data) return;
            if (event.data.type === "CRM_LEAD_SUBMITTED" || event.data.type === "crm-form-submitted") {
                const newLead = event.data.lead;
                if (newLead) {
                    showToast(`Nhận được Lead mới: "${newLead.fullName}" từ Web Form!`, "success");
                    loadLeadsData();
                }
            } else if (event.data.type === "crm-form-resize" && event.data.height && dom.livePreviewIframe) {
                dom.livePreviewIframe.style.height = `${Math.max(500, event.data.height + 20)}px`;
            }
        });

        // Lắng nghe thay đổi LocalStorage từ tab khác (khi mở lead-capture-form.html riêng)
        window.addEventListener("storage", (event) => {
            if (event.key === STORAGE_KEY_LEADS) {
                loadLeadsData();
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
            case "CONTACTED":
                return { cls: "status-contacted", label: "Đã liên hệ", icon: "fa-phone-flip" };
            case "CONVERTED":
                return { cls: "status-converted", label: "Đã chuyển đổi", icon: "fa-circle-check" };
            case "REJECTED":
                return { cls: "status-rejected", label: "Không liên lạc được", icon: "fa-circle-xmark" };
            default:
                return { cls: "status-new", label: "Mới", icon: "fa-circle-dot" };
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
