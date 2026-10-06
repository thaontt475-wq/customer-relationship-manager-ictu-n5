"use strict";

(function () {

    const API_BASE =
        "http://localhost:8080/crm";


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
                `<svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>`,

            customer:
                `<svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-7 8-7s8 3 8 7"/></svg>`,

            opportunity:
                `<svg viewBox="0 0 24 24"><path d="m12 3 7 7-7 7-7-7 7-7Z"/><path d="M12 17v4"/></svg>`,

            activity:
                `<svg viewBox="0 0 24 24"><path d="m4 12 4 4L20 4"/><path d="M4 20h16"/></svg>`,

            task:
                `<svg viewBox="0 0 24 24"><path d="m5 12 4 4L19 6"/></svg>`,

            calendar:
                `<svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></svg>`,

            quote:
                `<svg viewBox="0 0 24 24"><path d="M5 3h14v18H5z"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>`,

            product:
                `<svg viewBox="0 0 24 24"><path d="m12 3 8 4-8 4-8-4 8-4Z"/><path d="m4 7 8 4 8-4v10l-8 4-8-4V7Z"/></svg>`,

            users:
                `<svg viewBox="0 0 24 24"><circle cx="9" cy="8" r="3"/><circle cx="17" cy="9" r="2"/><path d="M3 20c0-4 2.7-7 6-7s6 3 6 7"/><path d="M15 14c3 0 5 2 5 5"/></svg>`,

            organization:
                `<svg viewBox="0 0 24 24"><rect x="9" y="3" width="6" height="5"/><rect x="3" y="16" width="6" height="5"/><rect x="15" y="16" width="6" height="5"/><path d="M12 8v4M6 16v-4h12v4"/></svg>`,

            import:
                `<svg viewBox="0 0 24 24"><path d="M12 3v12m-4-4 4 4 4-4M4 17v4h16v-4"/><path d="M4 7V3h16v4"/></svg>`,

            audit:
                `<svg viewBox="0 0 24 24"><rect x="5" y="3" width="14" height="18" rx="2"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>`,

            master:
                `<svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="6" rx="1"/><rect x="3" y="14" width="18" height="6" rx="1"/><path d="M7 7h1M7 17h1"/></svg>`,

            custom:
                `<svg viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M12 7v10M7 12h10"/></svg>`,

            winLoss:
                `<svg viewBox="0 0 24 24"><path d="M4 4h16v5c0 4-3 7-8 7S4 13 4 9V4ZM8 20h8M12 16v4M4 7H2v2c0 2 1 3 3 3m15-5h2v2c0 2-1 3-3 3"/></svg>`,

            settings:
                `<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19 12a7 7 0 0 0-.1-1l2-1.5-2-3.4-2.5 1A7 7 0 0 0 15 6l-.4-2.7h-4L10 6a7 7 0 0 0-1.4.8l-2.5-1-2 3.4L6 11a7 7 0 0 0 0 2l-2 1.5 2 3.4 2.5-1A7 7 0 0 0 10 18l.5 2.7h4L15 18a7 7 0 0 0 1.4-.8l2.5 1 2-3.4L19 13a7 7 0 0 0 0-1Z"/></svg>`,

            bell:
                `<svg viewBox="0 0 24 24"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></svg>`,

            moon:
                `<svg viewBox="0 0 24 24"><path d="M20 15a8 8 0 1 1-11-11 7 7 0 0 0 11 11Z"/></svg>`,

            sun:
                `<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>`
        };

        return icons[name] || "";
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
                    C
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

                <svg viewBox="0 0 24 24">
                    <circle cx="11" cy="11" r="7"/>
                    <path d="m20 20-4-4"/>
                </svg>

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
                    title="Đổi giao diện"
                >
                    ${icon("moon")}
                </button>


                <button
                    id="notificationButton"
                    class="crm-shell-icon-button crm-notification-button"
                    type="button"
                    title="Thông báo"
                >
                    ${icon("bell")}

                    <span class="crm-notification-dot"></span>
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
                            ⌄
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
                            Hồ sơ cá nhân
                        </a>

                        <a href="change-password.html">
                            Đổi mật khẩu
                        </a>

                        <button
                            id="logoutButton"
                            type="button"
                        >
                            Đăng xuất
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


        return roles
            .map(
                role =>
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


        document
            .querySelectorAll(
                ".crm-avatar"
            )
            .forEach(
                element => {

                    if (
                        !element.style.backgroundImage
                    ) {

                        element.textContent =
                            userInitial();
                    }
                }
            );
    }


    async function hydrateShell() {

        try {

            const [
                sessionResult,
                menuResult
            ] =
                await Promise.all([
                    api(
                        "/api/auth/session"
                    ),
                    api(
                        "/api/navigation/menu"
                    )
                ]);


            currentSession =
                sessionResult.data;


            currentMenu =
                Array.isArray(
                    menuResult.data?.menuItems
                )
                ?
                menuResult.data.menuItems
                :
                [];


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

        document
            .documentElement
            .setAttribute(
                "data-crm-theme",
                theme
            );


        localStorage.setItem(
            themeKey,
            theme
        );


        const button =
            document.getElementById(
                "themeToggle"
            );


        if (button) {

            button.innerHTML =
                theme === "dark"
                ?
                icon("sun")
                :
                icon("moon");
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

    function updateAvatar() {

        const avatar =
            localStorage.getItem(
                "crm_ui_profile_avatar"
            );


        if (!avatar) {

            renderUser();

            return;
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
                        `url("${avatar}")`;

                    element.style.backgroundSize =
                        "cover";

                    element.style.backgroundPosition =
                        "center";
                }
            );
    }


    window.crmUpdateGlobalAvatar =
        updateAvatar;


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


        /*
         * Hiện project chưa có API notification.
         * Không tạo notification giả.
         */
        panel.innerHTML = `
            <div class="crm-shell-panel-header">
                <strong>Thông báo</strong>
            </div>

            <div class="crm-shell-panel-empty">
                Chưa có thông báo.
            </div>
        `;


        document.body
            .appendChild(
                panel
            );


        return panel;
    }


    function toggleNotifications() {

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
                `<svg viewBox="0 0 24 24">
                    <rect x="3" y="3" width="7" height="7"/>
                    <rect x="14" y="3" width="7" height="7"/>
                    <rect x="3" y="14" width="7" height="7"/>
                    <rect x="14" y="14" width="7" height="7"/>
                </svg>`,

            customer:
                `<svg viewBox="0 0 24 24">
                    <circle cx="12" cy="8" r="4"/>
                    <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7"/>
                </svg>`,

            opportunity:
                `<svg viewBox="0 0 24 24">
                    <path d="m12 3 8 9-8 9-8-9 8-9Z"/>
                </svg>`,

            calendar:
                `<svg viewBox="0 0 24 24">
                    <rect x="3" y="5" width="18" height="16" rx="2"/>
                    <path d="M8 3v4m8-4v4M3 10h18"/>
                </svg>`
        };

        return icons[type] || "";
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
                                ${
                                    page === item.path
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