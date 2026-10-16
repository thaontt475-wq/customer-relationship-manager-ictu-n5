"use strict";

(function () {

    const API_BASE =
        (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`);


    const currentPage =
        window.location.pathname
            .split("/")
            .pop()
        ||
        "dashboard.html";


    let currentSession = null;
    let currentMenu = [];


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
            const saved = localStorage.getItem("crm_ui_session");
            if (saved) {
                try {
                    const parsed = JSON.parse(saved);
                    if (parsed && (parsed.authenticated || parsed.email)) {
                        return { success: true, data: parsed };
                    }
                } catch (_) {}
            }

            window.location.href =
                "login.html";

            throw new Error(
                "Chưa đăng nhập"
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
    ===================================================== */

    function renderNavigation(
        target
    ) {

        if (!target) {
            return;
        }


        const workspace =
            currentMenu.filter(
                item =>
                    item.section ===
                    "workspace"
            );


        const admin =
            currentMenu.filter(
                item =>
                    item.section ===
                    "admin"
            );


        let html = "";


        if (workspace.length) {

            html += `
                <div class="crm-nav-section">
                    KHÔNG GIAN LÀM VIỆC
                </div>
            `;


            html +=
                workspace
                    .map(
                        navigationItemHtml
                    )
                    .join("");
        }


        if (admin.length) {

            html += `
                <div class="crm-nav-section">
                    QUẢN TRỊ
                </div>
            `;


            html +=
                admin
                    .map(
                        navigationItemHtml
                    )
                    .join("");
        }


        target.innerHTML =
            html;
    }


    function navigationItemHtml(item) {

        const active =
            currentPage === item.path
                ?
                " active"
                :
                "";


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
            { name: "PIPELINE", label: "Cơ hội & Pipeline", path: "pipeline.html", icon: "opportunity", section: "workspace" },
            { name: "CAMPAIGNS", label: "Chiến dịch & Lead", path: "campaigns.html", icon: "campaign", section: "workspace" },
            { name: "LEAD_SLA_RESPONSE", label: "Tiếp nhận Lead SLA", path: "lead-sla-response.html", icon: "sla", section: "workspace" },
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
            { name: "AUDIT_LOG", label: "Nhật ký hệ thống", path: "audit-log.html", icon: "audit", section: "admin" }
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

            // CRM-113 (S4-01): Ensure Lead Capture from Website Form is in workspace menu
            if (!currentMenu.some(m => m.path === "lead-capture-manager.html" || m.name === "LEAD_CAPTURE")) {
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
                        "login.html";
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


        document.body
            .appendChild(
                panel
            );


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
