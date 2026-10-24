"use strict";

(function () {

    const API_BASE =
        (window.CRM_BACKEND_BASE || (window.location.pathname.startsWith("/crm/") ? window.location.origin + "/crm" : `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`));


    const currentPage =
        window.location.pathname
            .split("/")
            .pop()
        ||
        "dashboard.html";


    let currentSession = null;
    let currentMenu = [];

    /* =====================================================
       SESSION INACTIVITY & TIMEOUT MANAGER (15 MINUTES)
    ===================================================== */
    const SESSION_TIMEOUT_SECONDS = 15 * 60; // 900 seconds
    const WARNING_THRESHOLD_SECONDS = 14 * 60; // 840 seconds
    const STORAGE_KEY_LAST_REQUEST = "crm_last_request_time";
    const STORAGE_KEY_EXPIRED_MSG = "crm_session_expired_message";
    const STORAGE_KEY_EXPIRED_AT = "crm_session_expired_at";
    const STORAGE_KEY_MANUAL_LOGOUT = "crm_manual_logout";

    let sessionWarningModalEl = null;
    let isExpiring = false;

    function getStoredLastRequestTime() {
        const val = localStorage.getItem(STORAGE_KEY_LAST_REQUEST);
        if (!val) return null;
        const num = parseInt(val, 10);
        return isNaN(num) ? null : num;
    }

    function recordActivityTimestamp() {
        const now = Date.now();
        localStorage.setItem(STORAGE_KEY_LAST_REQUEST, now.toString());
        hideSessionWarningModal();
    }

    function ensureSessionWarningModal() {
        if (sessionWarningModalEl && document.body && document.body.contains(sessionWarningModalEl)) {
            return sessionWarningModalEl;
        }

        let existing = document.getElementById("crmSessionWarningModal");
        if (existing) {
            sessionWarningModalEl = existing;
            return existing;
        }

        if (!document.body) return null;

        const modal = document.createElement("div");
        modal.id = "crmSessionWarningModal";
        modal.setAttribute("role", "dialog");
        modal.setAttribute("aria-modal", "true");
        modal.setAttribute("aria-labelledby", "crmSessionWarningTitle");
        modal.style.cssText = [
            "display: none;",
            "position: fixed;",
            "top: 0;",
            "left: 0;",
            "width: 100vw;",
            "height: 100vh;",
            "background: rgba(15, 23, 42, 0.65);",
            "backdrop-filter: blur(6px);",
            "-webkit-backdrop-filter: blur(6px);",
            "z-index: 999999;",
            "align-items: center;",
            "justify-content: center;",
            "padding: 20px;",
            "box-sizing: border-box;"
        ].join(" ");

        modal.innerHTML = `
            <div style="
                background: var(--crm-card-bg, #ffffff);
                color: var(--crm-text-primary, #0f172a);
                border: 1px solid var(--crm-border-color, rgba(226, 232, 240, 0.8));
                border-radius: 16px;
                box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.35);
                max-width: 440px;
                width: 100%;
                padding: 28px 24px;
                text-align: center;
                box-sizing: border-box;
                font-family: inherit;
            ">
                <div style="
                    width: 56px;
                    height: 56px;
                    border-radius: 50%;
                    background: rgba(245, 158, 11, 0.15);
                    color: #d97706;
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 26px;
                    margin-bottom: 16px;
                ">
                    <i class="fa-solid fa-clock-rotate-left"></i>
                </div>
                <h3 id="crmSessionWarningTitle" style="
                    margin: 0 0 10px 0;
                    font-size: 19px;
                    font-weight: 700;
                    color: var(--crm-text-primary, #0f172a);
                ">Cảnh báo phiên đăng nhập</h3>
                <p style="
                    margin: 0 0 24px 0;
                    font-size: 14.5px;
                    line-height: 1.55;
                    color: var(--crm-text-secondary, #475569);
                ">Phiên đăng nhập của bạn sẽ hết hạn sau 1 phút do không hoạt động.</p>
                <div style="display: flex; gap: 12px; justify-content: center;">
                    <button type="button" id="crmBtnExtendSession" style="
                        background: #2563eb;
                        color: #ffffff;
                        border: none;
                        padding: 11px 22px;
                        border-radius: 10px;
                        font-size: 14.5px;
                        font-weight: 600;
                        cursor: pointer;
                        display: inline-flex;
                        align-items: center;
                        gap: 8px;
                        transition: all 0.15s ease;
                        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
                    ">
                        <i class="fa-solid fa-arrows-rotate"></i>
                        <span>Tiếp tục phiên</span>
                    </button>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        sessionWarningModalEl = modal;

        const btnExtend = modal.querySelector("#crmBtnExtendSession");
        btnExtend?.addEventListener("click", async () => {
            btnExtend.disabled = true;
            btnExtend.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> <span>Đang làm mới...</span>`;
            try {
                await CrmSessionManager.extendSession();
            } finally {
                btnExtend.disabled = false;
                btnExtend.innerHTML = `<i class="fa-solid fa-arrows-rotate"></i> <span>Tiếp tục phiên</span>`;
            }
        });

        return modal;
    }

    function showSessionWarningModal() {
        if (currentPage === "login.html" || isExpiring) return;
        const modal = ensureSessionWarningModal();
        if (modal && modal.style.display !== "flex") {
            modal.style.display = "flex";
        }
    }

    function hideSessionWarningModal() {
        const modal = sessionWarningModalEl || document.getElementById("crmSessionWarningModal");
        if (modal) {
            modal.style.display = "none";
        }
    }

    function handleSessionExpired(customMessage) {
        if (isExpiring) return;
        isExpiring = true;
        hideSessionWarningModal();

        const message = customMessage || "Phiên đăng nhập đã hết hạn do không hoạt động quá 15 phút. Vui lòng đăng nhập lại.";

        try {
            localStorage.setItem(STORAGE_KEY_EXPIRED_AT, Date.now().toString());
            localStorage.setItem(STORAGE_KEY_EXPIRED_MSG, message);
            localStorage.removeItem("crm_ui_session");
            localStorage.removeItem(STORAGE_KEY_LAST_REQUEST);
        } catch (_) {}

        if (currentPage !== "login.html") {
            window.location.href = "login.html?reason=timeout";
        }
    }

    const CrmSessionManager = {
        recordActivity: recordActivityTimestamp,
        isWarningVisible: () => {
            const modal = sessionWarningModalEl || document.getElementById("crmSessionWarningModal");
            return !!(modal && modal.style.display === "flex");
        },
        showWarning: showSessionWarningModal,
        hideWarning: hideSessionWarningModal,
        expire: handleSessionExpired,
        extendSession: async () => {
            try {
                const response = await fetch(API_BASE + "/api/auth/session", {
                    method: "GET",
                    credentials: "include",
                    headers: { "Accept": "application/json" }
                });
                if (response.status === 401 || !response.ok) {
                    handleSessionExpired("Phiên đăng nhập đã hết hạn do không hoạt động quá 15 phút. Vui lòng đăng nhập lại.");
                    return false;
                }
                const result = await response.json().catch(() => null);
                if (result && result.success) {
                    recordActivityTimestamp();
                    return true;
                } else {
                    handleSessionExpired("Phiên đăng nhập đã hết hạn do không hoạt động quá 15 phút. Vui lòng đăng nhập lại.");
                    return false;
                }
            } catch (err) {
                console.warn("Extend session check error:", err);
                return false;
            }
        },
        getRemainingSeconds: () => {
            const last = getStoredLastRequestTime();
            if (!last) return SESSION_TIMEOUT_SECONDS;
            const elapsed = Math.floor((Date.now() - last) / 1000);
            return Math.max(0, SESSION_TIMEOUT_SECONDS - elapsed);
        },
        simulateInactivity: (seconds) => {
            const pastTimestamp = Date.now() - (seconds * 1000);
            localStorage.setItem(STORAGE_KEY_LAST_REQUEST, pastTimestamp.toString());
            tickCheck();
        }
    };

    window.CrmSessionManager = CrmSessionManager;

    // Multi-tab synchronization
    window.addEventListener("storage", (event) => {
        if (event.key === STORAGE_KEY_LAST_REQUEST) {
            hideSessionWarningModal();
        } else if (event.key === STORAGE_KEY_EXPIRED_AT) {
            if (currentPage !== "login.html" && !isExpiring) {
                isExpiring = true;
                window.location.href = "login.html?reason=timeout";
            }
        }
    });

    // Global fetch interceptor for request activity tracking & 401 handling
    const originalFetch = window.fetch;
    window.fetch = async function (...args) {
        const response = await originalFetch.apply(this, args);
        try {
            const rawUrl = (args[0] && (typeof args[0] === "string" ? args[0] : args[0].url)) || "";
            if (rawUrl.includes("/api/")) {
                const isLoginApi = rawUrl.includes("/api/auth/login");
                if (response.ok && !isLoginApi) {
                    recordActivityTimestamp();
                } else if (response.status === 401 && !isLoginApi) {
                    handleSessionExpired("Phiên đăng nhập đã hết hạn do không hoạt động quá 15 phút. Vui lòng đăng nhập lại.");
                }
            }
        } catch (_) {}
        return response;
    };

    function tickCheck() {
        if (currentPage === "login.html" || isExpiring) return;
        const last = getStoredLastRequestTime();
        if (!last) {
            recordActivityTimestamp();
            return;
        }

        const elapsed = Math.floor((Date.now() - last) / 1000);
        if (elapsed >= SESSION_TIMEOUT_SECONDS) {
            handleSessionExpired("Phiên đăng nhập đã hết hạn do không hoạt động quá 15 phút. Vui lòng đăng nhập lại.");
        } else if (elapsed >= WARNING_THRESHOLD_SECONDS) {
            showSessionWarningModal();
        } else {
            hideSessionWarningModal();
        }
    }

    if (currentPage !== "login.html") {
        setInterval(tickCheck, 1000);
        if (!getStoredLastRequestTime()) {
            recordActivityTimestamp();
        }
    }


    /* =====================================================
       ICON
    ===================================================== */

    function icon(name) {

        const icons = {

            dashboard:
                `<i class="fa-solid fa-chart-pie"></i>`,

            customer:
                `<i class="fa-solid fa-users"></i>`,

            contact:
                `<i class="fa-solid fa-address-book"></i>`,

            support:
                `<i class="fa-solid fa-headset"></i>`,

            opportunity:
                `<i class="fa-solid fa-bullseye"></i>`,

            activity:
                `<i class="fa-solid fa-clock-rotate-left"></i>`,

            task:
                `<i class="fa-solid fa-list-check"></i>`,

            calendar:
                `<i class="fa-solid fa-calendar-days"></i>`,

            quote:
                `<i class="fa-solid fa-file-invoice-dollar"></i>`,

            contract:
                `<i class="fa-solid fa-file-signature"></i>`,

            signature:
                `<i class="fa-solid fa-signature"></i>`,

            product:
                `<i class="fa-solid fa-boxes-stacked"></i>`,

            users:
                `<i class="fa-solid fa-user-gear"></i>`,

            organization:
                `<i class="fa-solid fa-sitemap"></i>`,

            import:
                `<i class="fa-solid fa-file-import"></i>`,

            excel:
                `<i class="fa-solid fa-file-excel"></i>`,

            audit:
                `<i class="fa-solid fa-clipboard-list"></i>`,

            master:
                `<i class="fa-solid fa-database"></i>`,

            custom:
                `<i class="fa-solid fa-sliders"></i>`,

            winLoss:
                `<i class="fa-solid fa-trophy"></i>`,

            settings:
                `<i class="fa-solid fa-gear"></i>`,

            lead:
                `<i class="fa-solid fa-bullhorn"></i>`,

            bullhorn:
                `<i class="fa-solid fa-bullhorn"></i>`,

            bell:
                `<i class="fa-solid fa-bell"></i>`,

            campaign:
                `<svg viewBox="0 0 24 24"><path d="m3 11 18-5v12L3 13v-2z"/><path d="M11.6 16.8a3 3 0 1 1-5.8-1.6"/></svg>`,

            moon:
                `<i class="fa-solid fa-moon theme-icon-moon" aria-hidden="true"></i>`,

            sun:
                `<i class="fa-solid fa-sun theme-icon-sun" aria-hidden="true"></i>`,

            lead:
                `<i class="fa-solid fa-users-viewfinder"></i>`,

            filter:
                `<i class="fa-solid fa-filter"></i>`,

            convert:
                `<i class="fa-solid fa-arrows-split-up-and-left"></i>`,

            sla:
                `<i class="fa-solid fa-stopwatch"></i>`,

            assignment:
                `<i class="fa-solid fa-users-gear"></i>`,

            scoring:
                `<i class="fa-solid fa-star-half-stroke"></i>`,

            merge:
                `<i class="fa-solid fa-code-merge"></i>`
        };

        return icons[name] || `<i class="fa-solid fa-circle-notch"></i>`;
    }


    function escapeHtml(value) {

        return String(
            value ?? ""
        )
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }


    /* =====================================================
       STATIC SHELL
    ===================================================== */

    const sidebar =
        document.querySelector(
            ".crm-sidebar"
        );


    if (sidebar) {

        sidebar.innerHTML = `
            <div class="crm-brand">

                <div class="crm-brand-mark">
                    <i class="fa-solid fa-building-columns"></i>
                </div>

                <span class="crm-brand-name">
                    Corporate CRM
                </span>

            </div>

            <nav
                id="desktopNavigation"
                class="crm-nav"
            ></nav>
        `;
    }


    const topbar =
        document.querySelector(
            ".crm-topbar"
        );


    if (topbar) {

        topbar.innerHTML = `
            <div class="crm-search">

                <i class="fa-solid fa-magnifying-glass" style="color:var(--crm-muted); font-size:1rem; margin-right:8px;"></i>

                <input
                    id="globalSearch"
                    type="search"
                    placeholder="Tìm kiếm... (Ctrl + K)"
                >

                <kbd>
                    Ctrl K
                </kbd>

            </div>


            <div class="crm-top-actions">

                <button
                    id="themeToggle"
                    class="crm-shell-icon-button"
                    type="button"
                    title="Chuyển sang giao diện tối"
                    aria-label="Chuyển sang giao diện tối"
                >
                    <i class="fa-solid fa-moon theme-icon-moon" aria-hidden="true"></i>
                </button>


                <button
                    id="notificationButton"
                    class="crm-shell-icon-button crm-notification-button"
                    type="button"
                    title="Thông báo"
                >
                    ${icon("bell")}

                    <span class="crm-notification-dot" hidden></span>
                </button>


                <div class="crm-user">

                    <button
                        id="userButton"
                        class="crm-user-button"
                        type="button"
                    >

                        <div class="crm-avatar">
                            U
                        </div>

                        <div class="crm-user-copy">

                            <span
                                id="shellUserName"
                                class="crm-user-name"
                            >
                                Đang tải...
                            </span>

                            <span
                                id="shellUserRole"
                                class="crm-user-role"
                            >
                                ...
                            </span>

                        </div>

                        <span class="crm-user-arrow">
                            <i class="fa-solid fa-angle-down"></i>
                        </span>

                    </button>


                    <div
                        id="userDropdown"
                        class="crm-user-dropdown"
                    >

                        <div
                            id="shellUserTeam"
                            style="
                                padding: 10px 14px;
                                font-size: 12px;
                                opacity: .7;
                            "
                        >
                            Nhóm: ...
                        </div>

                        <a href="profile.html">
                            <i class="fa-solid fa-id-card" style="margin-right:8px;"></i> Hồ sơ cá nhân
                        </a>

                        <a href="change-password.html">
                            <i class="fa-solid fa-key" style="margin-right:8px;"></i> Đổi mật khẩu
                        </a>

                        <button
                            id="logoutButton"
                            type="button"
                        >
                            <i class="fa-solid fa-right-from-bracket" style="margin-right:8px;"></i> Đăng xuất
                        </button>

                    </div>

                </div>

            </div>
        `;
    }


    const mobileHeader =
        document.querySelector(
            ".crm-mobile-header"
        );


    if (mobileHeader) {

        mobileHeader.innerHTML = `
            <button
                id="mobileMenuButton"
                class="crm-mobile-menu"
                type="button"
                aria-label="Mở menu"
            >
                ☰
            </button>

            <strong>
                Corporate CRM
            </strong>

            <div class="crm-avatar">
                U
            </div>
        `;
    }


    const mobileDrawer =
        document.querySelector(
            ".crm-mobile-drawer"
        );


    if (mobileDrawer) {

        mobileDrawer.innerHTML = `
            <div class="crm-drawer-header">

                <div class="crm-avatar">
                    U
                </div>

                <div class="crm-drawer-user">

                    <strong id="mobileUserName">
                        Đang tải...
                    </strong>

                    <span id="mobileUserRole">
                        ...
                    </span>

                    <span id="mobileUserTeam">
                        ...
                    </span>

                </div>

            </div>

            <nav
                id="mobileNavigation"
                class="crm-nav"
            ></nav>
        `;
    }


    /* =====================================================
       API
    ===================================================== */

    async function api(path) {

        const response =
            await fetch(
                API_BASE + path,
                {
                    credentials:
                        "include",

                    headers: {
                        "Accept":
                            "application/json"
                    }
                }
            );


        const result =
            await response
                .json()
                .catch(
                    () => ({
                        success: false,
                        message:
                            "Phản hồi API không hợp lệ"
                    })
                );


        if (
            response.status === 401
        ) {
            if (window.CrmSessionManager && typeof window.CrmSessionManager.expire === "function") {
                window.CrmSessionManager.expire("Phiên đăng nhập đã hết hạn do không hoạt động quá 15 phút. Vui lòng đăng nhập lại.");
            } else {
                localStorage.removeItem("crm_ui_session");
                localStorage.removeItem("crm_last_request_time");
                localStorage.setItem("crm_session_expired_message", "Phiên đăng nhập đã hết hạn do không hoạt động quá 15 phút. Vui lòng đăng nhập lại.");
                window.location.href = "login.html?reason=timeout";
            }

            throw new Error(
                "Chưa đăng nhập hoặc phiên đã hết hạn"
            );
        }


        if (!response.ok) {

            throw new Error(
                result.message
                ||
                "Không thể tải dữ liệu"
            );
        }


        return result;
    }


    /* =====================================================
       NAVIGATION
    /* =====================================================
       NAVIGATION & COLLAPSIBLE GROUPS
    ===================================================== */

    const WORKSPACE_GROUPS = [
        {
            key: "customers",
            label: "Khách hàng & Liên hệ",
            icon: `<i class="fa-solid fa-users"></i>`,
            patterns: ["customers.html", "customer-360.html", "contacts.html"],
            names: ["CUSTOMERS", "CUSTOMER_360", "CONTACTS"]
        },
        {
            key: "leads",
            label: "Quản lý Lead & Marketing",
            icon: `<i class="fa-solid fa-bullhorn"></i>`,
            patterns: [
                "leads.html",
                "lead-manual-import.html",
                "lead-capture-manager.html",
                "campaigns.html",
                "lead-assignment.html",
                "lead-sla-response.html",
                "lead-scoring.html",
                "lead-duplicates.html",
                "lead-conversion.html"
            ],
            names: [
                "LEADS",
                "LEAD_MANUAL_IMPORT",
                "LEAD_CAPTURE",
                "CAMPAIGNS",
                "LEAD_ASSIGNMENT",
                "LEAD_SLA_RESPONSE",
                "LEAD_SCORING",
                "LEAD_DUPLICATES",
                "LEAD_CONVERSION"
            ]
        },
        {
            key: "sales",
            label: "Bán hàng & Hợp đồng",
            icon: `<i class="fa-solid fa-briefcase"></i>`,
            patterns: [
                "pipeline.html",
                "quotations.html",
                "quote-editor.html",
                "contracts.html",
                "contract-create.html",
                "products.html"
            ],
            names: ["PIPELINE", "QUOTES", "CONTRACTS", "PRODUCTS"]
        },
        {
            key: "support",
            label: "Dịch vụ & Chăm sóc",
            icon: `<i class="fa-solid fa-headset"></i>`,
            patterns: ["support-tickets.html", "periodic-care.html"],
            names: ["SUPPORT_TICKETS", "PERIODIC_CARE"]
        },
        {
            key: "activities",
            label: "Lịch trình & Hoạt động",
            icon: `<i class="fa-solid fa-calendar-days"></i>`,
            patterns: ["activities.html", "calendar.html", "tasks.html"],
            names: ["ACTIVITIES", "CALENDAR", "TASKS"]
        }
    ];

    const ADMIN_GROUPS = [
        {
            key: "admin_users",
            label: "Tổ chức & Người dùng",
            icon: `<i class="fa-solid fa-user-gear"></i>`,
            patterns: ["organization.html", "users.html", "import-users.html", "excel-templates.html"],
            names: ["ORGANIZATION", "USERS", "IMPORT_USERS", "USER_IMPORT", "EXCEL_TEMPLATES"]
        },
        {
            key: "admin_settings",
            label: "Cấu hình & Nhật ký",
            icon: `<i class="fa-solid fa-sliders"></i>`,
            patterns: [
                "master-data.html",
                "custom-fields.html",
                "pipeline-settings.html",
                "win-loss-settings.html",
                "audit-log.html"
            ],
            names: [
                "MASTER_DATA",
                "CUSTOM_FIELDS",
                "PIPELINE_SETTINGS",
                "WIN_LOSS_SETTINGS",
                "AUDIT_LOG"
            ]
        }
    ];

    function isItemInGroup(item, group) {
        if (!item) return false;
        if (group.patterns && group.patterns.includes(item.path)) return true;
        if (item.name && group.names && group.names.includes(item.name)) return true;
        return false;
    }

    function isGroupActive(groupItems) {
        return groupItems.some(item => currentPage === item.path);
    }

    function isGroupExpanded(groupKey, hasActiveChild) {
        if (hasActiveChild) {
            return true; // Tự động mở rộng nhóm chứa trang hiện tại
        }
        try {
            const stored = localStorage.getItem("crm_nav_group_" + groupKey);
            if (stored === "closed") return false;
            if (stored === "open") return true;
        } catch (_) {}
        // Mặc định thu gọn các nhóm khác để thanh điều hướng tinh gọn
        return false;
    }

    function renderNavigation(target) {
        if (!target) return;

        const workspace = currentMenu.filter(item => item.section === "workspace");
        const admin = currentMenu.filter(item => item.section === "admin");

        let html = "";

        if (workspace.length) {
            html += `
                <div class="crm-nav-section">
                    KHÔNG GIAN LÀM VIỆC
                </div>
            `;

            // 1. Mục đứng độc lập (Tổng quan)
            const topStandalone = workspace.filter(item => item.path === "dashboard.html");
            topStandalone.forEach(item => {
                html += navigationItemHtml(item);
            });

            // 2. Các nhóm chức năng có liên quan (Thu gọn / Mở ra)
            WORKSPACE_GROUPS.forEach(group => {
                const groupItems = workspace.filter(
                    item => item.path !== "dashboard.html" && isItemInGroup(item, group)
                );
                if (groupItems.length > 0) {
                    html += navigationGroupHtml(group, groupItems);
                }
            });

            // 3. Các mục workspace chưa gán nhóm (dự phòng)
            const unmatchedWorkspace = workspace.filter(
                item => item.path !== "dashboard.html" && !WORKSPACE_GROUPS.some(g => isItemInGroup(item, g))
            );
            unmatchedWorkspace.forEach(item => {
                html += navigationItemHtml(item);
            });
        }

        if (admin.length) {
            html += `
                <div class="crm-nav-section">
                    QUẢN TRỊ
                </div>
            `;

            ADMIN_GROUPS.forEach(group => {
                const groupItems = admin.filter(item => isItemInGroup(item, group));
                if (groupItems.length > 0) {
                    html += navigationGroupHtml(group, groupItems);
                }
            });

            const unmatchedAdmin = admin.filter(
                item => !ADMIN_GROUPS.some(g => isItemInGroup(item, g))
            );
            unmatchedAdmin.forEach(item => {
                html += navigationItemHtml(item);
            });
        }

        target.innerHTML = html;
        initNavigationGroupEvents(target);
    }

    function navigationGroupHtml(group, groupItems) {
        const hasActive = isGroupActive(groupItems);
        const isExpanded = isGroupExpanded(group.key, hasActive);

        return `
            <div class="crm-nav-group${isExpanded ? " is-expanded" : ""}${hasActive ? " has-active" : ""}" data-group-key="${escapeHtml(group.key)}">
                <button type="button" class="crm-nav-group-header" aria-expanded="${isExpanded}">
                    <span class="crm-nav-icon">
                        ${group.icon}
                    </span>
                    <span class="crm-nav-label">
                        ${escapeHtml(group.label)}
                    </span>
                    ${hasActive ? '<span class="crm-group-active-dot" title="Có mục đang mở"></span>' : ''}
                    <span class="crm-group-count">${groupItems.length}</span>
                    <span class="crm-group-arrow">
                        <i class="fa-solid fa-chevron-right"></i>
                    </span>
                </button>
                <div class="crm-nav-group-items" style="${isExpanded ? "" : "display: none;"}">
                    ${groupItems.map(navigationSubItemHtml).join("")}
                </div>
            </div>
        `;
    }

    function navigationSubItemHtml(item) {
        const active = currentPage === item.path ? " active" : "";
        return `
            <a
                href="${escapeHtml(item.path)}"
                class="crm-nav-item crm-nav-subitem${active}"
                title="${escapeHtml(item.label)}"
            >
                <span class="crm-subitem-bullet" aria-hidden="true"></span>
                <span class="crm-nav-icon crm-subitem-icon">
                    ${icon(item.icon)}
                </span>
                <span class="crm-nav-label">
                    ${escapeHtml(item.label)}
                </span>
            </a>
        `;
    }

    function navigationItemHtml(item) {
        const active = currentPage === item.path ? " active" : "";
        return `
            <a
                href="${escapeHtml(item.path)}"
                class="crm-nav-item${active}"
                title="${escapeHtml(item.label)}"
            >
                <span class="crm-nav-icon">
                    ${icon(item.icon)}
                </span>
                <span class="crm-nav-label">
                    ${escapeHtml(item.label)}
                </span>
            </a>
        `;
    }

    function initNavigationGroupEvents(container) {
        if (!container) return;
        const headers = container.querySelectorAll(".crm-nav-group-header");
        headers.forEach(header => {
            header.addEventListener("click", function (e) {
                e.preventDefault();
                e.stopPropagation();

                const groupEl = this.closest(".crm-nav-group");
                if (!groupEl) return;

                const groupKey = groupEl.dataset.groupKey;
                const itemsEl = groupEl.querySelector(".crm-nav-group-items");
                const isCurrentlyExpanded = groupEl.classList.contains("is-expanded");
                const willExpand = !isCurrentlyExpanded;

                if (willExpand) {
                    groupEl.classList.add("is-expanded");
                    this.setAttribute("aria-expanded", "true");
                    if (itemsEl) {
                        itemsEl.style.display = "";
                    }
                } else {
                    groupEl.classList.remove("is-expanded");
                    this.setAttribute("aria-expanded", "false");
                    if (itemsEl) {
                        itemsEl.style.display = "none";
                    }
                }

                try {
                    localStorage.setItem("crm_nav_group_" + groupKey, willExpand ? "open" : "closed");
                } catch (_) {}
            });
        });
    }


    /* =====================================================
       USER
    ===================================================== */

    function roleText() {

        const roles =
            Array.isArray(
                currentSession?.roles
            )
                ?
                currentSession.roles
                :
                [];


        if (!roles.length) {

            return "Chưa có vai trò";
        }


        const standardRoleLabels = {
            ADMIN: "Quản trị viên",
            DIRECTOR: "Giám đốc",
            TEAM_LEAD: "Trưởng nhóm kinh doanh",
            SALES_REP: "Nhân viên kinh doanh",
            CUSTOMER_SUCCESS: "Chăm sóc khách hàng",
            MARKETING: "Marketing",
            ACCOUNTANT: "Kế toán"
        };

        return roles
            .map(
                role =>
                    standardRoleLabels[role.code]
                    ||
                    role.name
                    ||
                    role.code
            )
            .filter(Boolean)
            .join(", ");
    }


    function userInitial() {

        const name =
            currentSession?.fullName
            ||
            currentSession?.email
            ||
            "U";

        return name
            .trim()
            .charAt(0)
            .toUpperCase()
            ||
            "U";
    }

    function absoluteAvatarUrl(url) {
        if (!url) return "";
        if (/^https?:\/\//i.test(url)) return url;
        const origin = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080`;
        return origin + (url.startsWith("/") ? url : "/" + url);
    }

    function renderUser() {
        if (!currentSession) {
            return;
        }

        const fullName =
            currentSession.fullName
            ||
            currentSession.email
            ||
            "Người dùng";

        const role =
            roleText();

        const team =
            currentSession.teamName
            ||
            "Chưa thuộc nhóm";

        document
            .querySelectorAll(
                "#shellUserName, #mobileUserName"
            )
            .forEach(
                element => {
                    element.textContent =
                        fullName;
                }
            );

        document
            .querySelectorAll(
                "#shellUserRole, #mobileUserRole"
            )
            .forEach(
                element => {
                    element.textContent =
                        role;
                }
            );

        document
            .querySelectorAll(
                "#mobileUserTeam"
            )
            .forEach(
                element => {
                    element.textContent =
                        `Nhóm: ${team}`;
                }
            );

        const teamElement =
            document.getElementById(
                "shellUserTeam"
            );

        if (teamElement) {
            teamElement.textContent =
                `Nhóm: ${team}`;
        }

        const avatarUrl =
            currentSession.avatarThumbnailUrl
            ||
            currentSession.avatarUrl
            ||
            localStorage.getItem(
                "crm_ui_profile_avatar"
            );

        const fullAvatar =
            avatarUrl ? absoluteAvatarUrl(avatarUrl) : "";

        document
            .querySelectorAll(
                ".crm-avatar"
            )
            .forEach(
                element => {
                    if (fullAvatar) {
                        element.textContent = "";
                        element.style.backgroundImage =
                            `url("${fullAvatar}")`;
                        element.style.backgroundSize =
                            "cover";
                        element.style.backgroundPosition =
                            "center";
                        element.style.backgroundRepeat =
                            "no-repeat";
                        element.innerHTML =
                            `<img src="${fullAvatar}" alt="Avatar" style="width:100%;height:100%;border-radius:50%;object-fit:cover;display:block;" onerror="this.remove();">`;
                    } else {
                        element.style.backgroundImage = "";
                        element.innerHTML = "";
                        element.textContent =
                            userInitial();
                    }
                }
            );
    }


    function getDefaultMenu() {
        return [
            { name: "DASHBOARD", label: "Tổng quan", path: "dashboard.html", icon: "dashboard", section: "workspace" },
            { name: "CUSTOMERS", label: "Khách hàng", path: "customers.html", icon: "customer", section: "workspace" },
            { name: "CONTACTS", label: "Người liên hệ", path: "contacts.html", icon: "contact", section: "workspace" },
            { name: "LEADS", label: "Quản lý Lead", path: "leads.html", icon: "lead", section: "workspace" },
            { name: "PIPELINE", label: "Cơ hội & Pipeline", path: "pipeline.html", icon: "opportunity", section: "workspace" },
            { name: "CAMPAIGNS", label: "Chiến dịch & Lead", path: "campaigns.html", icon: "campaign", section: "workspace" },
            { name: "LEAD_SLA_RESPONSE", label: "Tiếp nhận Lead SLA", path: "lead-sla-response.html", icon: "sla", section: "workspace" },
            { name: "LEAD_CONVERSION", label: "Chuyển đổi Lead", path: "lead-conversion.html", icon: "convert", section: "workspace" },
            { name: "SUPPORT_TICKETS", label: "Chăm sóc & Hỗ trợ", path: "support-tickets.html", icon: "support", section: "workspace" },
            { name: "PERIODIC_CARE", label: "Chăm sóc định kỳ", path: "periodic-care.html", icon: "calendar", section: "workspace" },
            { name: "CONTRACTS", label: "Hợp đồng", path: "contracts.html", icon: "quote", section: "workspace" },
            { name: "QUOTES", label: "Báo giá", path: "quotations.html", icon: "quote", section: "workspace" },
            { name: "PRODUCTS", label: "Sản phẩm & Giá", path: "products.html", icon: "product", section: "workspace" },
            { name: "ACTIVITIES", label: "Hoạt động", path: "activities.html", icon: "activity", section: "workspace" },
            { name: "CALENDAR", label: "Lịch làm việc", path: "calendar.html", icon: "calendar", section: "workspace" },
            { name: "ORGANIZATION", label: "Cơ cấu tổ chức", path: "organization.html", icon: "organization", section: "admin" },
            { name: "USERS", label: "Người dùng & Quyền", path: "users.html", icon: "users", section: "admin" },
            { name: "MASTER_DATA", label: "Danh mục Master Data", path: "master-data.html", icon: "master", section: "admin" },
            { name: "CUSTOM_FIELDS", label: "Trường tùy chỉnh", path: "custom-fields.html", icon: "custom", section: "admin" },
            { name: "PIPELINE_SETTINGS", label: "Cấu hình Pipeline", path: "pipeline-settings.html", icon: "settings", section: "admin" },
            { name: "WIN_LOSS_SETTINGS", label: "Cấu hình Win/Loss", path: "win-loss-settings.html", icon: "winLoss", section: "admin" },
            { name: "AUDIT_LOG", label: "Nhật ký hệ thống", path: "audit-log.html", icon: "audit", section: "admin" },
            { name: "EXCEL_TEMPLATES", label: "Mẫu Excel & Báo cáo", path: "excel-templates.html", icon: "excel", section: "admin" }
        ];
    }

    async function hydrateShell() {
        try {
            let sessionResult = null;
            let menuResult = null;

            try {
                [sessionResult, menuResult] = await Promise.all([
                    api("/api/auth/session"),
                    api("/api/navigation/menu")
                ]);
            } catch (err) {
                const saved = localStorage.getItem("crm_ui_session");
                if (saved) {
                    try {
                        sessionResult = { data: JSON.parse(saved) };
                        menuResult = { data: { menuItems: getDefaultMenu() } };
                    } catch (_) {}
                }
            }

            if (!sessionResult?.data) {
                const saved = localStorage.getItem("crm_ui_session");
                if (saved) {
                    try {
                        sessionResult = { data: JSON.parse(saved) };
                    } catch (_) {}
                }
            }

            currentSession = sessionResult?.data || {
                id: 1,
                fullName: "Quản trị viên CRM",
                email: "admin@company.com",
                roleName: "Quản trị viên hệ thống",
                roles: [{ code: "ADMIN", name: "Quản trị viên" }],
                permissions: ["*"],
                teamName: "Ban Giám Đốc",
                authenticated: true
            };

            if (
                currentSession?.avatarThumbnailUrl ||
                currentSession?.avatarUrl
            ) {
                const fullAvt =
                    absoluteAvatarUrl(
                        currentSession.avatarThumbnailUrl ||
                        currentSession.avatarUrl
                    );
                localStorage.setItem(
                    "crm_ui_profile_avatar",
                    fullAvt
                );
            }

            currentMenu =
                Array.isArray(menuResult?.data?.menuItems) && menuResult.data.menuItems.length > 0
                ? menuResult.data.menuItems
                : getDefaultMenu();

            // CRM-62: Ensure Contact Management (Decision Roles) item is in workspace menu
            if (currentSession.permissions.includes("contact.read") && !currentMenu.some(m => m.path === "contacts.html" || m.name === "CONTACTS")) {
                const customerIndex = currentMenu.findIndex(m => m.path === "customers.html");
                const contactItem = {
                    name: "CONTACTS",
                    label: "Người liên hệ (CRM-62)",
                    path: "contacts.html",
                    icon: "contact",
                    section: "workspace"
                };
                if (customerIndex !== -1) {
                    currentMenu.splice(customerIndex + 1, 0, contactItem);
                } else {
                    currentMenu.push(contactItem);
                }
            }

            // CRM-68: Ensure Post-sale Support & Churn Risk item is in workspace menu
            if (currentSession.permissions.includes("support.read") && !currentMenu.some(m => m.path === "support-tickets.html" || m.name === "SUPPORT_TICKETS")) {
                const contactIndex = currentMenu.findIndex(m => m.path === "contacts.html");
                const supportItem = {
                    name: "SUPPORT_TICKETS",
                    label: "Chăm sóc & Hỗ trợ",
                    path: "support-tickets.html",
                    icon: "support",
                    section: "workspace"
                };
                if (contactIndex !== -1) {
                    currentMenu.splice(contactIndex + 1, 0, supportItem);
                } else {
                    currentMenu.push(supportItem);
                }
            }

            // CRM-69: Ensure Periodic Customer Care item is in workspace menu
            if (currentSession.permissions.includes("customer.read") && !currentMenu.some(m => m.path === "periodic-care.html" || m.name === "PERIODIC_CARE")) {
                const supportIndex = currentMenu.findIndex(m => m.path === "support-tickets.html");
                const careItem = {
                    name: "PERIODIC_CARE",
                    label: "Chăm sóc định kỳ",
                    path: "periodic-care.html",
                    icon: "calendar",
                    section: "workspace"
                };
                if (supportIndex !== -1) {
                    currentMenu.splice(supportIndex + 1, 0, careItem);
                } else {
                    currentMenu.push(careItem);
                }
            }

            // CRM-123: Ensure Lead Management & Saved Filters (S4-09) item is in workspace menu
            if (!currentMenu.some(m => m.path === "leads.html" || m.name === "LEADS")) {
                const leadItem = {
                    name: "LEADS",
                    label: "Quản lý Lead (CRM-123)",
                    path: "leads.html",
                    icon: "lead",
                    section: "workspace"
                };
                const customerIndex = currentMenu.findIndex(m => m.path === "customers.html");
                if (customerIndex !== -1) {
                    currentMenu.splice(customerIndex + 1, 0, leadItem);
                } else {
                    currentMenu.push(leadItem);
                }
            }

            // CRM-122: Ensure Lead Conversion (3-in-1: Customer, Contact, Opportunity) item is in workspace menu
            if (!currentMenu.some(m => m.path === "lead-conversion.html" || m.name === "LEAD_CONVERSION")) {
                const leadItem = {
                    name: "LEAD_CONVERSION",
                    label: "Chuyển đổi Lead (CRM-122)",
                    path: "lead-conversion.html",
                    icon: "convert",
                    section: "workspace"
                };
                const customerIndex = currentMenu.findIndex(m => m.path === "customers.html");
                if (customerIndex !== -1) {
                    currentMenu.splice(customerIndex + 1, 0, leadItem);
                } else {
                    currentMenu.push(leadItem);
                }
            }

            // CRM-121: Ensure Lead SLA Response item is in workspace menu
            if (!currentMenu.some(m => m.path === "lead-sla-response.html" || m.name === "LEAD_SLA_RESPONSE")) {
                const customerIndex = currentMenu.findIndex(m => m.path === "customers.html");
                const slaItem = {
                    name: "LEAD_SLA_RESPONSE",
                    label: "Tiếp nhận Lead SLA (CRM-121)",
                    path: "lead-sla-response.html",
                    icon: "sla",
                    section: "workspace"
                };
                if (customerIndex !== -1) {
                    currentMenu.splice(customerIndex + 1, 0, slaItem);
                } else {
                    currentMenu.push(slaItem);
                }
            }

            // CRM-120: Ensure Lead Assignment item is in workspace menu
            if (!currentMenu.some(m => m.path === "lead-assignment.html" || m.name === "LEAD_ASSIGNMENT")) {
                const customerIndex = currentMenu.findIndex(m => m.path === "customers.html");
                const assignmentItem = {
                    name: "LEAD_ASSIGNMENT",
                    label: "Phân bổ Lead (CRM-120)",
                    path: "lead-assignment.html",
                    icon: "assignment",
                    section: "workspace"
                };
                if (customerIndex !== -1) {
                    currentMenu.splice(customerIndex + 1, 0, assignmentItem);
                } else {
                    currentMenu.push(assignmentItem);
                }
            }

            // CRM-119: Ensure Lead Scoring item is in workspace menu
            if (currentSession?.permissions?.includes("lead.read") && !currentMenu.some(m => m.path === "lead-scoring.html" || m.name === "LEAD_SCORING")) {
                const customerIndex = currentMenu.findIndex(m => m.path === "customers.html");
                const scoringItem = {
                    name: "LEAD_SCORING",
                    label: "Chấm điểm Lead (CRM-119)",
                    path: "lead-scoring.html",
                    icon: "scoring",
                    section: "workspace"
                };
                if (customerIndex !== -1) {
                    currentMenu.splice(customerIndex + 1, 0, scoringItem);
                } else {
                    currentMenu.push(scoringItem);
                }
            }

            // CRM-118: Ensure Lead Duplicate Merge item is in workspace menu
            if (!currentMenu.some(m => m.path === "lead-duplicates.html" || m.name === "LEAD_DUPLICATES")) {
                const customerIndex = currentMenu.findIndex(m => m.path === "customers.html");
                const dupItem = {
                    name: "LEAD_DUPLICATES",
                    label: "Phát hiện & Gộp Lead (CRM-118)",
                    path: "lead-duplicates.html",
                    icon: "merge",
                    section: "workspace"
                };
                if (customerIndex !== -1) {
                    currentMenu.splice(customerIndex + 1, 0, dupItem);
                } else {
                    currentMenu.push(dupItem);
                }
            }

            // S4-03: Ensure Campaigns & Leads item is in workspace menu
            const canCampaign = (currentSession && currentSession.permissions && currentSession.permissions.includes("campaign.read")) ||
                                (currentSession && currentSession.role === "ADMIN") ||
                                true; // S4-03 visible in workspace
            if (canCampaign && !currentMenu.some(m => m.path === "campaigns.html" || m.name === "CAMPAIGNS")) {
                const customerIndex = currentMenu.findIndex(m => m.path === "customers.html");
                const campaignItem = {
                    name: "CAMPAIGNS",
                    label: "Chiến dịch & Lead",
                    path: "campaigns.html",
                    icon: "campaign",
                    section: "workspace"
                };
                if (customerIndex !== -1) {
                    currentMenu.splice(customerIndex + 1, 0, campaignItem);
                } else {
                    currentMenu.push(campaignItem);
                }
            }

            // CRM-113 (S4-01): Match the manager's lead.read permission gate.
            const canReadLeadCapture = currentSession?.permissions?.includes("lead.read") === true;
            if (!canReadLeadCapture) {
                currentMenu = currentMenu.filter(m => m.path !== "lead-capture-manager.html" && m.name !== "LEAD_CAPTURE");
            }
            if (canReadLeadCapture && !currentMenu.some(m => m.path === "lead-capture-manager.html" || m.name === "LEAD_CAPTURE")) {
                const pipelineIndex = currentMenu.findIndex(m => m.path === "pipeline.html");
                const leadItem = {
                    name: "LEAD_CAPTURE",
                    label: "Thu thập Lead (CRM-113)",
                    path: "lead-capture-manager.html",
                    icon: "bullhorn",
                    section: "workspace"
                };
                if (pipelineIndex !== -1) {
                    currentMenu.splice(pipelineIndex + 1, 0, leadItem);
                } else {
                    currentMenu.push(leadItem);
                }
            }

            // CRM-116: Ensure Manual Lead Creation & Excel Bulk Import is in workspace menu
            if (!currentMenu.some(m => m.path === "lead-manual-import.html" || m.name === "LEAD_MANUAL_IMPORT")) {
                const customerIndex = currentMenu.findIndex(m => m.path === "customers.html");
                const leadManualItem = {
                    name: "LEAD_MANUAL_IMPORT",
                    label: "Tạo & Nhập Lead (CRM-116)",
                    path: "lead-manual-import.html",
                    icon: "import",
                    section: "workspace"
                };
                if (customerIndex !== -1) {
                    currentMenu.splice(customerIndex + 1, 0, leadManualItem);
                } else {
                    currentMenu.push(leadManualItem);
                }
            }
            renderUser();


            renderNavigation(
                document.getElementById(
                    "desktopNavigation"
                )
            );


            renderNavigation(
                document.getElementById(
                    "mobileNavigation"
                )
            );


            document.dispatchEvent(
                new CustomEvent(
                    "crm:shell-ready",
                    {
                        detail: {
                            session:
                                currentSession,

                            menu:
                                currentMenu
                        }
                    }
                )
            );

        } catch (error) {

            console.error(
                "CRM shell load error:",
                error
            );
        }
    }


    /* =====================================================
       THEME
    ===================================================== */

    const themeKey =
        "crm_ui_theme";


    function applyTheme(theme) {

        const normalized =
            theme === "dark" ? "dark" : "light";

        document
            .documentElement
            .setAttribute(
                "data-theme",
                normalized
            );

        document
            .documentElement
            .setAttribute(
                "data-crm-theme",
                normalized
            );

        localStorage.setItem(
            themeKey,
            normalized
        );


        const button =
            document.getElementById(
                "themeToggle"
            );


        if (button) {

            const isDark =
                theme === "dark";

            button.innerHTML =
                isDark
                    ?
                    icon("sun")
                    :
                    icon("moon");

            const label =
                isDark
                    ? "Chuyển sang giao diện sáng"
                    : "Chuyển sang giao diện tối";

            button.title = label;
            button.setAttribute("aria-label", label);
        }
    }


    applyTheme(
        localStorage.getItem(
            themeKey
        )
        ||
        "light"
    );


    document
        .getElementById(
            "themeToggle"
        )
        ?.addEventListener(
            "click",
            () => {

                const current =
                    document
                        .documentElement
                        .getAttribute(
                            "data-crm-theme"
                        );


                applyTheme(
                    current === "dark"
                        ?
                        "light"
                        :
                        "dark"
                );
            }
        );


    /* =====================================================
       DROPDOWN
    ===================================================== */

    const userButton =
        document.getElementById(
            "userButton"
        );


    const userDropdown =
        document.getElementById(
            "userDropdown"
        );


    userButton
        ?.addEventListener(
            "click",
            event => {

                event.stopPropagation();

                userDropdown
                    ?.classList
                    .toggle(
                        "open"
                    );
            }
        );


    document.addEventListener(
        "click",
        event => {

            if (
                userDropdown
                &&
                !userDropdown.contains(
                    event.target
                )
                &&
                !userButton?.contains(
                    event.target
                )
            ) {

                userDropdown
                    .classList
                    .remove(
                        "open"
                    );
            }
        }
    );


    /* =====================================================
       LOGOUT
    ===================================================== */

    document
        .getElementById(
            "logoutButton"
        )
        ?.addEventListener(
            "click",
            async () => {

                try {
                    localStorage.setItem("crm_manual_logout", "true");
                    localStorage.removeItem("crm_session_expired_message");
                    localStorage.removeItem("crm_ui_session");
                    localStorage.removeItem("crm_last_request_time");
                    localStorage.removeItem("crm_session_expired_at");

                    await fetch(
                        API_BASE +
                        "/api/auth/logout",
                        {
                            method:
                                "POST",

                            credentials:
                                "include"
                        }
                    );

                } catch (error) {

                    console.error(
                        "Logout error:",
                        error
                    );

                } finally {

                    sessionStorage.clear();

                    window.location.href =
                        "login.html?reason=logout";
                }
            }
        );


    /* =====================================================
       MOBILE DRAWER
    ===================================================== */

    const mobileMenuButton =
        document.getElementById(
            "mobileMenuButton"
        );


    const drawerOverlay =
        document.getElementById(
            "drawerOverlay"
        );


    function closeMobileDrawer() {

        mobileDrawer
            ?.classList
            .remove(
                "open"
            );


        drawerOverlay
            ?.classList
            .remove(
                "open"
            );


        document.body
            .classList
            .remove(
                "mobile-menu-open"
            );
    }


    mobileMenuButton
        ?.addEventListener(
            "click",
            event => {

                if (
                    window.innerWidth > 768
                ) {
                    return;
                }


                event.stopPropagation();


                mobileDrawer
                    ?.classList
                    .add(
                        "open"
                    );


                drawerOverlay
                    ?.classList
                    .add(
                        "open"
                    );


                document.body
                    .classList
                    .add(
                        "mobile-menu-open"
                    );
            }
        );


    drawerOverlay
        ?.addEventListener(
            "click",
            closeMobileDrawer
        );


    /* =====================================================
       AVATAR
    ===================================================== */

    function updateAvatar(newUrl) {

        const raw =
            newUrl ||
            localStorage.getItem(
                "crm_ui_profile_avatar"
            ) ||
            currentSession?.avatarThumbnailUrl ||
            currentSession?.avatarUrl;


        if (!raw) {

            renderUser();

            return;
        }

        const full =
            absoluteAvatarUrl(raw);

        localStorage.setItem(
            "crm_ui_profile_avatar",
            full
        );

        if (currentSession) {
            currentSession.avatarThumbnailUrl =
                full;
            currentSession.avatarUrl =
                full;
        }


        document
            .querySelectorAll(
                ".crm-avatar"
            )
            .forEach(
                element => {

                    element.textContent =
                        "";

                    element.style.backgroundImage =
                        `url("${full}")`;

                    element.style.backgroundSize =
                        "cover";

                    element.style.backgroundPosition =
                        "center";

                    element.style.backgroundRepeat =
                        "no-repeat";

                    element.innerHTML =
                        `<img src="${full}" alt="Avatar" style="width:100%;height:100%;border-radius:50%;object-fit:cover;display:block;" onerror="this.remove();">`;
                }
            );
    }


    window.crmUpdateGlobalAvatar =
        updateAvatar;

    window.addEventListener(
        "crm:avatar-updated",
        event => {
            const url =
                event.detail?.avatarThumbnailUrl ||
                event.detail?.avatarUrl ||
                (typeof event.detail === "string" ? event.detail : null);
            if (url) {
                updateAvatar(url);
            }
        }
    );

    window.addEventListener(
        "storage",
        event => {
            if (
                event.key === "crm_ui_profile_avatar" &&
                event.newValue
            ) {
                updateAvatar(event.newValue);
            }
        }
    );


    /* =====================================================
       SEARCH SHORTCUT
    ===================================================== */

    document.addEventListener(
        "keydown",
        event => {

            if (
                (
                    event.ctrlKey
                    ||
                    event.metaKey
                )
                &&
                event.key.toLowerCase()
                ===
                "k"
            ) {

                event.preventDefault();

                document
                    .getElementById(
                        "globalSearch"
                    )
                    ?.focus();
            }
        }
    );



    /* =====================================================
       MOBILE BOTTOM NAV
       Render dùng chung cho mọi trang.
    ===================================================== */

    function ensureMobileBottomNav() {

        let bottomNav =
            document.querySelector(
                ".crm-bottom-nav"
            );


        if (!bottomNav) {

            bottomNav =
                document.createElement(
                    "nav"
                );

            bottomNav.className =
                "crm-bottom-nav";

            document.body.appendChild(
                bottomNav
            );
        }


        const items = [
            {
                label:
                    "Tổng quan",

                path:
                    "dashboard.html",

                icon:
                    "dashboard"
            },

            {
                label:
                    "Khách hàng",

                path:
                    "customers.html",

                icon:
                    "customer"
            },

            {
                label:
                    "Cơ hội",

                path:
                    "pipeline.html",

                icon:
                    "opportunity"
            },

            {
                label:
                    "Lịch",

                path:
                    "calendar.html",

                icon:
                    "calendar"
            }
        ];


        bottomNav.innerHTML =
            items
                .map(
                    item => {

                        const active =
                            currentPage ===
                                item.path
                                ?
                                " active"
                                :
                                "";


                        return `
                            <a
                                href="${item.path}"
                                class="crm-bottom-item${active}"
                            >

                                <span class="crm-bottom-icon">
                                    ${icon(item.icon)}
                                </span>

                                <span>
                                    ${escapeHtml(item.label)}
                                </span>

                            </a>
                        `;
                    }
                )
                .join("");
    }

    /* =====================================================
       START
    ===================================================== */

    ensureMobileBottomNav();

    hydrateShell()
        .then(
            () => {
                updateAvatar();
                ensureMobileBottomNav();
            }
        );

})();
/* =========================================================
   MOBILE MENU CLICK - HARD FIX
========================================================= */

(function () {

    document.addEventListener(
        "click",
        function (event) {

            const button =
                event.target.closest(
                    "#mobileMenuButton"
                );

            if (!button) {
                return;
            }

            event.preventDefault();
            event.stopPropagation();

            const drawer =
                document.getElementById(
                    "mobileDrawer"
                );

            const overlay =
                document.getElementById(
                    "drawerOverlay"
                );


            if (!drawer) {

                console.error(
                    "mobileDrawer not found"
                );

                return;
            }


            drawer
                .classList
                .add(
                    "open"
                );


            overlay
                ?.classList
                .add(
                    "open"
                );


            document.body
                .classList
                .add(
                    "mobile-menu-open"
                );

        },
        true
    );


    document.addEventListener(
        "click",
        function (event) {

            const overlay =
                event.target.closest(
                    "#drawerOverlay"
                );

            if (!overlay) {
                return;
            }

            const drawer =
                document.getElementById(
                    "mobileDrawer"
                );


            drawer
                ?.classList
                .remove(
                    "open"
                );


            overlay
                .classList
                .remove(
                    "open"
                );


            document.body
                .classList
                .remove(
                    "mobile-menu-open"
                );
        },
        true
    );

})();
/* =========================================================
   SHELL INTERACTIONS - STABLE FIX
   Search / Theme / Notification / User dropdown
========================================================= */

(function () {

    function byId(id) {
        return document.getElementById(id);
    }


    function closeShellPanels() {

        byId("userDropdown")
            ?.classList
            .remove("open");

        byId("crmNotificationPanel")
            ?.classList
            .remove("open");

        byId("crmSearchPanel")
            ?.classList
            .remove("open");
    }


    /* =====================================================
       THEME
    ===================================================== */

    function shellToggleTheme() {

        /*
         * Project hiện tại đã có theme.js.
         * Ưu tiên dùng hàm chung của theme.js để không tạo
         * hai chuẩn data-theme / data-crm-theme khác nhau.
         */
        if (
            typeof window.crmToggleTheme
            ===
            "function"
        ) {

            window.crmToggleTheme();

            return;
        }


        const root =
            document.documentElement;

        const current =
            root.getAttribute(
                "data-theme"
            )
            ||
            "light";

        const next =
            current === "dark"
                ?
                "light"
                :
                "dark";


        root.setAttribute(
            "data-theme",
            next
        );

        localStorage.setItem(
            "crm_ui_theme",
            next
        );
    }


    /* =====================================================
       USER DROPDOWN
    ===================================================== */

    function toggleUserDropdown() {

        const dropdown =
            byId(
                "userDropdown"
            );

        if (!dropdown) {
            return;
        }


        byId("crmNotificationPanel")
            ?.classList
            .remove("open");

        byId("crmSearchPanel")
            ?.classList
            .remove("open");


        dropdown
            .classList
            .toggle(
                "open"
            );
    }


    /* =====================================================
       NOTIFICATIONS
    ===================================================== */

    function ensureNotificationPanel() {

        let panel =
            byId(
                "crmNotificationPanel"
            );

        if (panel) {

            return panel;
        }


        panel =
            document.createElement(
                "div"
            );

        panel.id =
            "crmNotificationPanel";

        panel.className =
            "crm-notification-panel";


        panel.innerHTML = '<div class="crm-shell-panel-header"><strong>Thông báo</strong></div><div class="crm-shell-panel-empty">Đang tải...</div>';


        document.body
            .appendChild(
                panel
            );


        return panel;
    }


    async function toggleNotifications() {

        const panel =
            ensureNotificationPanel();


        byId("userDropdown")
            ?.classList
            .remove("open");

        byId("crmSearchPanel")
            ?.classList
            .remove("open");


        panel.classList.toggle(
            "open"
        );
        if (!panel.classList.contains("open")) return;
        try {
            const response = await fetch(API_BASE + "/api/notifications?size=100", { credentials: "include" });
            const body = await response.json();
            if (!response.ok || !body.success) throw new Error(body.message || `HTTP ${response.status}`);
            const data = body.data;
            const dot = document.querySelector(".crm-notification-dot"); if (dot) dot.hidden = !data.unreadCount;
            const content = document.createElement("div");
            for (const item of data.items) {
                const button = document.createElement("button"); button.type = "button"; button.className = "crm-shell-panel-empty";
                button.textContent = `${item.readAt ? "" : "● "}${item.title}: ${item.message}`;
                button.onclick = async () => {
                    try {
                        const r = await fetch(API_BASE + `/api/notifications/${item.id}/read`, { method: "PUT", credentials: "include" });
                        const b = await r.json(); if (!r.ok || !b.success) throw new Error(b.message);
                        button.textContent = `${item.title}: ${item.message}`;
                    } catch (error) { alert(error.message); }
                };
                content.appendChild(button);
            }
            if (!data.items.length) content.textContent = "Chưa có thông báo.";
            panel.replaceChildren(panel.firstElementChild, content);
        } catch (error) {
            const message = document.createElement("div"); message.textContent = error.message;
            panel.replaceChildren(panel.firstElementChild, message);
        }

    }


    /* =====================================================
       GLOBAL SEARCH
       Hiện chưa có API global-search backend.
       Search này tìm chức năng/menu mà account có quyền.
    ===================================================== */

    function positionSearchPanel(panel) {
        if (!panel) return;
        const searchInput = byId("globalSearch");
        const searchWrap = document.querySelector(".crm-search");
        const anchor = searchWrap || searchInput;
        if (!anchor) return;

        const rect = anchor.getBoundingClientRect();
        panel.style.setProperty("position", "fixed", "important");
        panel.style.setProperty("top", `${Math.round(rect.bottom + 6)}px`, "important");
        panel.style.setProperty("left", `${Math.round(rect.left)}px`, "important");
        panel.style.setProperty("width", `${Math.round(rect.width)}px`, "important");
        panel.style.setProperty("min-width", "320px", "important");
        panel.style.setProperty("max-width", "min(500px, calc(100vw - 32px))", "important");
        panel.style.setProperty("z-index", "99999", "important");
    }

    function ensureSearchPanel() {

        let panel =
            byId(
                "crmSearchPanel"
            );

        if (panel) {
            return panel;
        }


        panel =
            document.createElement(
                "div"
            );

        panel.id =
            "crmSearchPanel";

        panel.className =
            "crm-search-panel";

        document.body.appendChild(panel);

        return panel;
    }


    function normalizeText(value) {

        return String(
            value ?? ""
        )
            .normalize("NFD")
            .replace(
                /[\u0300-\u036f]/g,
                ""
            )
            .toLowerCase()
            .trim();
    }


    function getSearchableMenu() {

        /*
         * Dùng menu thật đã render từ backend permission,
         * không hard-code lại quyền.
         */
        return Array
            .from(
                document.querySelectorAll(
                    "#desktopNavigation a.crm-nav-item"
                )
            )
            .map(
                link => ({
                    label:
                        link.textContent
                            .replace(
                                /\s+/g,
                                " "
                            )
                            .trim(),

                    href:
                        link.getAttribute(
                            "href"
                        )
                })
            )
            .filter(
                item =>
                    item.label
                    &&
                    item.href
            );
    }


    function renderSearchResults(
        keyword
    ) {

        const panel =
            ensureSearchPanel();

        const query =
            normalizeText(
                keyword
            );


        if (!query) {

            panel.classList.remove(
                "open"
            );

            panel.innerHTML =
                "";

            return;
        }


        const results =
            getSearchableMenu()
                .filter(
                    item =>
                        normalizeText(
                            item.label
                        )
                            .includes(
                                query
                            )
                )
                .slice(
                    0,
                    8
                );


        if (
            results.length === 0
        ) {

            panel.innerHTML = `
                <div class="crm-shell-panel-empty">
                    Không tìm thấy chức năng phù hợp.
                </div>
            `;

        } else {

            panel.innerHTML =
                results
                    .map(
                        item => `
                            <button
                                type="button"
                                class="crm-search-result"
                                data-search-href="${escapeAttribute(item.href)}"
                            >
                                ${escapeHtmlShell(item.label)}
                            </button>
                        `
                    )
                    .join("");
        }


        positionSearchPanel(panel);

        panel.classList.add(
            "open"
        );
    }


    function escapeHtmlShell(value) {

        return String(
            value ?? ""
        )
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }


    function escapeAttribute(value) {

        return escapeHtmlShell(
            value
        );
    }


    /* =====================================================
       ONE EVENT DELEGATION FOR DYNAMIC SHELL
    ===================================================== */

    document.addEventListener(
        "click",
        function (event) {

            const themeButton =
                event.target.closest(
                    "#themeToggle"
                );

            if (themeButton) {

                event.preventDefault();

                /*
                 * Chặn listener cũ trong shared-shell/ui-core
                 * toggle lần thứ hai.
                 */
                event.stopImmediatePropagation();

                shellToggleTheme();

                return;
            }


            const notificationButton =
                event.target.closest(
                    "#notificationButton"
                );

            if (notificationButton) {

                event.preventDefault();
                event.stopImmediatePropagation();

                toggleNotifications();

                return;
            }


            const userButton =
                event.target.closest(
                    "#userButton"
                );

            if (userButton) {

                event.preventDefault();
                event.stopImmediatePropagation();

                toggleUserDropdown();

                return;
            }


            const searchResult =
                event.target.closest(
                    "[data-search-href]"
                );

            if (searchResult) {

                const href =
                    searchResult.dataset
                        .searchHref;

                if (href) {

                    window.location.href =
                        href;
                }

                return;
            }


            if (
                !event.target.closest(
                    ".crm-user"
                )
                &&
                !event.target.closest(
                    "#crmNotificationPanel"
                )
                &&
                !event.target.closest(
                    ".crm-search"
                )
                &&
                !event.target.closest(
                    "#crmSearchPanel"
                )
            ) {

                closeShellPanels();
            }

        },
        true
    );


    /* =====================================================
       SEARCH INPUT
    ===================================================== */

    document.addEventListener(
        "input",
        function (event) {

            if (
                event.target?.id
                !==
                "globalSearch"
            ) {
                return;
            }


            renderSearchResults(
                event.target.value
            );

        },
        true
    );


    document.addEventListener(
        "focusin",
        function (event) {

            if (
                event.target?.id
                !==
                "globalSearch"
            ) {
                return;
            }

            if (event.target.value?.trim()) {
                renderSearchResults(
                    event.target.value
                );
            }

        },
        true
    );


    window.addEventListener("resize", function () {
        const panel = byId("crmSearchPanel");
        if (panel && panel.classList.contains("open")) {
            positionSearchPanel(panel);
        }
    });

    window.addEventListener("scroll", function () {
        const panel = byId("crmSearchPanel");
        if (panel && panel.classList.contains("open")) {
            positionSearchPanel(panel);
        }
    }, true);


    document.addEventListener(
        "keydown",
        function (event) {

            const search =
                byId(
                    "globalSearch"
                );


            if (
                (
                    event.ctrlKey
                    ||
                    event.metaKey
                )
                &&
                event.key
                    .toLowerCase()
                ===
                "k"
            ) {

                event.preventDefault();

                search?.focus();

                return;
            }


            if (
                event.key
                ===
                "Escape"
            ) {

                closeShellPanels();

                return;
            }


            if (
                event.key
                ===
                "Enter"
                &&
                event.target?.id
                ===
                "globalSearch"
            ) {

                const first =
                    document.querySelector(
                        "#crmSearchPanel [data-search-href]"
                    );

                if (first) {

                    event.preventDefault();

                    window.location.href =
                        first.dataset
                            .searchHref;
                }
            }

        },
        true
    );

})();
/* =========================================================
   MOBILE TOP SHORTCUTS - FORCE TOP
========================================================= */

(function () {

    function currentPageName() {

        return window.location.pathname
            .split("/")
            .pop()
            ||
            "dashboard.html";
    }


    function createIcon(type) {

        const icons = {

            dashboard:
                `<i class="fa-solid fa-chart-pie"></i>`,

            customer:
                `<i class="fa-solid fa-building"></i>`,

            opportunity:
                `<i class="fa-solid fa-chart-line"></i>`,

            calendar:
                `<i class="fa-solid fa-calendar-days"></i>`
        };

        return icons[type] || `<i class="fa-solid fa-layer-group"></i>`;
    }


    function buildTopNavigation() {

        /*
         * Xóa bottom nav cũ nếu JS cũ đã tạo.
         */
        document
            .querySelectorAll(
                ".crm-bottom-nav"
            )
            .forEach(
                element =>
                    element.remove()
            );


        let nav =
            document.querySelector(
                ".crm-mobile-shortcuts"
            );


        if (!nav) {

            nav =
                document.createElement(
                    "nav"
                );

            nav.className =
                "crm-mobile-shortcuts";


            const header =
                document.querySelector(
                    ".crm-mobile-header"
                );


            if (header) {

                header.insertAdjacentElement(
                    "afterend",
                    nav
                );

            } else {

                document.body.prepend(
                    nav
                );
            }
        }


        const page =
            currentPageName();


        const items = [
            {
                path:
                    "dashboard.html",
                label:
                    "Tổng quan",
                icon:
                    "dashboard"
            },

            {
                path:
                    "customers.html",
                label:
                    "Khách hàng",
                icon:
                    "customer"
            },

            {
                path:
                    "pipeline.html",
                label:
                    "Cơ hội",
                icon:
                    "opportunity"
            },

            {
                path:
                    "calendar.html",
                label:
                    "Lịch",
                icon:
                    "calendar"
            }
        ];


        nav.innerHTML =
            items
                .map(
                    item => `
                        <a
                            href="${item.path}"
                            class="
                                crm-mobile-shortcut
                                ${page === item.path
                            ?
                            "active"
                            :
                            ""
                        }
                            "
                        >
                            ${createIcon(item.icon)}

                            <span>
                                ${item.label}
                            </span>
                        </a>
                    `
                )
                .join("");
    }


    if (
        document.readyState ===
        "loading"
    ) {

        document.addEventListener(
            "DOMContentLoaded",
            buildTopNavigation
        );

    } else {

        buildTopNavigation();
    }


    /*
     * Shell có thể render lại header sau đó,
     * chạy lại một lần để đảm bảo vị trí đúng.
     */
    setTimeout(
        buildTopNavigation,
        100
    );

})();
