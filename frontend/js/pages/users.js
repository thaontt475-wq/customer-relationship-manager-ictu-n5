"use strict";

const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;
const PAGE_SIZE = 20;

let currentPage = 1;
let totalPages = 1;
let totalItems = 0;

let users = [];
let roles = [];
let teams = [];
let sessionUser = null;

const tableBody = document.getElementById("usersTableBody");
const empty = document.getElementById("usersEmpty");

const searchInput = document.getElementById("userSearch");
const roleFilter = document.getElementById("roleFilter");
const statusFilter = document.getElementById("statusFilter");

const drawer = document.getElementById("userDrawer");
const drawerOverlay = document.getElementById("userDrawerOverlay");

const form = document.getElementById("userForm");

const lockModal = document.getElementById("lockModal");
const lockOverlay = document.getElementById("lockOverlay");

let searchTimer = null;

const ROLE_NAMES = {
    ADMIN: "Quản trị viên",
    DIRECTOR: "Giám đốc kinh doanh",
    TEAM_LEAD: "Trưởng nhóm kinh doanh",
    SALES_REP: "Nhân viên kinh doanh",
    MARKETING: "Nhân viên Marketing",
    CUSTOMER_SUCCESS: "Chăm sóc khách hàng",
    ACCOUNTANT: "Kế toán",
    SALE: "Nhân viên kinh doanh",
    MANAGER: "Trưởng nhóm kinh doanh"
};

const TEAM_NAMES = {
    1: "Kinh doanh miền Bắc",
    2: "Kinh doanh miền Trung",
    3: "Kinh doanh miền Nam"
};

function fixMojibake(str) {
    if (!str || typeof str !== "string") return str;
    return str
        .replace(/miß╗ün Bß│«c/g, "miền Bắc")
        .replace(/miß╗ün Trung/g, "miền Trung")
        .replace(/miß╗ün Nam/g, "miền Nam")
        .replace(/Quß│ún trß╗ï vi├¬n/g, "Quản trị viên")
        .replace(/Quß│ún trß╗ï hß╗ç thß╗æng/g, "Quản trị hệ thống")
        .replace(/Kß║┐ to├ín/g, "Kế toán")
        .replace(/Ch─âm s├│c kh├ích h├íng/g, "Chăm sóc khách hàng")
        .replace(/Gi├ím ─æß╗æc kinh doanh/g, "Giám đốc kinh doanh")
        .replace(/Nh├ón vi├¬n Marketing/g, "Nhân viên Marketing")
        .replace(/Nh├ón vi├¬n kinh doanh/g, "Nhân viên kinh doanh")
        .replace(/Tr╞░ß╗ƒng nh├│m kinh doanh/g, "Trưởng nhóm kinh doanh");
}

function normalizeRoleName(role) {
    if (!role) return "";
    if (typeof role === "string") {
        return ROLE_NAMES[role] || fixMojibake(role);
    }
    if (role.code && ROLE_NAMES[role.code]) {
        return ROLE_NAMES[role.code];
    }
    return fixMojibake(role.name || "");
}

function normalizeTeamName(team) {
    if (!team) return "";
    if (typeof team === "string") {
        return fixMojibake(team);
    }
    const id = Number(team.id);
    let name = fixMojibake(team.name || (id && TEAM_NAMES[id] ? TEAM_NAMES[id] : ""));
    const meta = [];
    if (team.regionName) meta.push(team.regionName);
    if (team.leaderName) meta.push(`Trưởng nhóm: ${team.leaderName}`);
    if (meta.length > 0) {
        return `${name} [${meta.join(" · ")}]`;
    }
    return name;
}


/* =========================================================
   API
========================================================= */

async function api(path, options = {}) {

    const config = {
        credentials: "include",
        headers: {
            "Accept": "application/json",
            ...(options.body
                ? {"Content-Type": "application/json"}
                : {}),
            ...(options.headers || {})
        },
        ...options
    };

    const response = await fetch(
        API_BASE + path,
        config
    );

    let result = null;

    try {
        result = await response.json();
    } catch (_) {
        result = null;
    }

    if (response.status === 401) {

        localStorage.removeItem("crm_ui_session");

        window.location.href =
            "login.html";

        throw new Error("Phiên đăng nhập đã hết hạn.");
    }

    if (!response.ok || !result?.success) {

        const error =
            new Error(
                result?.message ||
                `HTTP ${response.status}`
            );

        error.status =
            response.status;

        error.data =
            result?.data;

        throw error;
    }

    return result.data;
}


/* =========================================================
   INITIALIZE
========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    init
);

async function init() {

    try {

        prepareFormControls();

        sessionUser =
            await api(
                "/api/auth/session"
            );

        await Promise.all([
            loadRoles(),
            loadTeams()
        ]);

        await loadUsers();

        bindEvents();

    } catch (error) {

        console.error(
            "Users initialization error:",
            error
        );

        showPageError(
            error.message ||
            "Không thể tải dữ liệu người dùng."
        );
    }
}


/* =========================================================
   PREPARE EXISTING UI
========================================================= */

function prepareFormControls() {
    const selfCheck =
        document.querySelector(
            ".self-admin-check"
        );

    if (selfCheck) {
        selfCheck.hidden = true;
        selfCheck.style.display = "none";
    }
}


/* =========================================================
   LOAD ROLES
========================================================= */

async function loadRoles() {

    const data =
        await api(
            "/api/roles"
        );

    roles =
        Array.isArray(data?.roles)
            ? data.roles
            : [];

    renderRoleOptions();
}


function renderRoleOptions() {

    if (roleFilter) {

        roleFilter.innerHTML = `
            <option value="">
                Tất cả vai trò
            </option>
        `;

        roles.forEach(role => {

            const option =
                document.createElement(
                    "option"
                );

            option.value =
                String(role.id);

            option.textContent =
                normalizeRoleName(role);

            roleFilter.appendChild(
                option
            );
        });
    }


    const container =
        document.getElementById("rolesList") ||
        document.querySelector(".roles-fieldset");

    if (!container) {
        return;
    }

    container.innerHTML = "";

    roles.forEach(role => {

        const label =
            document.createElement(
                "label"
            );

        label.className =
            "role-check";

        const cleanName =
            normalizeRoleName(role);

        label.innerHTML = `
            <input
                type="checkbox"
                value="${role.id}"
                name="roles"
                data-role-code="${escapeHtml(role.code || "")}"
            >
            ${escapeHtml(cleanName)}
        `;

        container.appendChild(
            label
        );
    });
}


/* =========================================================
   LOAD TEAMS
========================================================= */

async function loadTeams() {

    const data =
        await api(
            "/api/teams?active=true"
        );

    teams =
        Array.isArray(data?.teams)
            ? data.teams
            : [];

    renderTeamOptions();
}


function renderTeamOptions() {

    const select =
        document.getElementById(
            "userTeam"
        );

    if (!select) {
        return;
    }

    select.innerHTML = `
        <option value="">
            Chưa gán nhóm
        </option>
    `;

    teams.forEach(team => {

        const option =
            document.createElement(
                "option"
            );

        option.value =
            String(team.id);

        option.textContent =
            normalizeTeamName(team);

        select.appendChild(
            option
        );
    });
}


/* =========================================================
   USERS
========================================================= */

async function loadUsers() {

    setTableLoading(true);

    try {

        const params =
            new URLSearchParams();

        params.set(
            "page",
            String(currentPage)
        );

        params.set(
            "size",
            String(PAGE_SIZE)
        );

        const keyword =
            searchInput?.value.trim();

        if (keyword) {

            params.set(
                "keyword",
                keyword
            );
        }

        const status =
            statusFilter?.value;

        if (status) {

            params.set(
                "status",
                status.toUpperCase()
            );
        }

        const data =
            await api(
                `/api/users?${params.toString()}`
            );

        const items =
            Array.isArray(data?.items)
                ? data.items
                : [];

        totalItems =
            Number(data?.totalItems || 0);

        totalPages =
            Math.max(
                1,
                Number(data?.totalPages || 1)
            );


        /*
         * User list chưa chứa roles,
         * lấy role/dataScope qua Permission API.
         */

        users =
            await Promise.all(
                items.map(
                    enrichUser
                )
            );

        render();

    } finally {

        setTableLoading(false);
    }
}


async function enrichUser(user) {

    try {

        const permissionData =
            await api(
                `/api/permissions/users/${user.id}`
            );

        return {
            ...user,

            roles:
                Array.isArray(
                    permissionData?.roles
                )
                    ? permissionData.roles
                    : [],

            dataScope:
                permissionData?.dataScope ||
                user.dataScope ||
                "SELF"
        };

    } catch (error) {

        if (error.status !== 403) {

            console.warn(
                `Không lấy được role user ${user.id}`,
                error
            );
        }

        return {
            ...user,
            roles: [],
            dataScope:
                user.dataScope ||
                "SELF"
        };
    }
}


/* =========================================================
   FILTER + RENDER
========================================================= */

function filteredUsers() {

    const selectedRole =
        roleFilter?.value || "";

    if (!selectedRole) {
        return users;
    }

    return users.filter(
        user =>
            user.roles.some(
                role =>
                    String(role.id) ===
                    selectedRole
            )
    );
}


const AVATAR_GRADIENTS = [
    "linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)",
    "linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)",
    "linear-gradient(135deg, #10b981 0%, #047857 100%)",
    "linear-gradient(135deg, #f59e0b 0%, #d97706 100%)",
    "linear-gradient(135deg, #ec4899 0%, #be185d 100%)",
    "linear-gradient(135deg, #06b6d4 0%, #0e7490 100%)",
    "linear-gradient(135deg, #6366f1 0%, #4338ca 100%)"
];

function getRoleBadgeClass(roleName) {
    const lower = (roleName || "").toLowerCase();
    if (lower.includes("quản trị") || lower.includes("admin")) return "role-admin";
    if (lower.includes("giám đốc") || lower.includes("trưởng nhóm") || lower.includes("lead")) return "role-leader";
    if (lower.includes("kinh doanh") || lower.includes("sale")) return "role-sales";
    if (lower.includes("marketing")) return "role-mkt";
    if (lower.includes("kế toán")) return "role-acc";
    if (lower.includes("chăm sóc") || lower.includes("cskh")) return "role-cskh";
    return "role-default";
}

function updateKpiCards() {
    const kpiTotal = document.getElementById("kpiTotalUsers");
    const kpiActive = document.getElementById("kpiActiveUsers");
    const kpiLocked = document.getElementById("kpiLockedUsers");
    const kpiTeams = document.getElementById("kpiTeamsCount");

    if (kpiTotal) kpiTotal.textContent = totalItems || users.length || 0;
    if (kpiActive) kpiActive.textContent = users.filter(u => String(u.status || "").toUpperCase() === "ACTIVE").length || 0;
    if (kpiLocked) kpiLocked.textContent = users.filter(u => String(u.status || "").toUpperCase() === "LOCKED").length || 0;
    if (kpiTeams) kpiTeams.textContent = teams.length || 0;
}

function render() {

    const filtered =
        filteredUsers();

    tableBody.innerHTML =
        "";

    empty.hidden =
        filtered.length > 0;

    filtered.forEach(user => {

        const tr =
            document.createElement(
                "tr"
            );

        const roleNames =
            user.roles.length
                ? user.roles
                    .map(role => normalizeRoleName(role))
                : [];

        const status =
            String(
                user.status || ""
            ).toUpperCase();

        const current =
            Number(sessionUser?.id) ===
            Number(user.id);

        const grad = AVATAR_GRADIENTS[(Number(user.id) || 0) % AVATAR_GRADIENTS.length];

        tr.innerHTML = `

            <td>
                <div class="user-cell">
                    <span class="user-avatar" style="background:${grad};">
                        ${initials(user.fullName)}
                    </span>
                    <div>
                        <strong>
                            ${escapeHtml(user.fullName)}
                        </strong>
                        ${
                            current
                            ?
                            `<small>Bạn</small>`
                            :
                            ""
                        }
                    </div>
                </div>
            </td>

            <td>
                <div class="user-email-cell">
                    <i class="fa-regular fa-envelope"></i>
                    <span>${escapeHtml(user.email)}</span>
                </div>
            </td>

            <td>
                <div class="role-list">
                    ${
                        roleNames.length
                        ?
                        roleNames
                            .map(
                                role =>
                                `<span class="role-pill ${getRoleBadgeClass(role)}">
                                    ${escapeHtml(role)}
                                </span>`
                            )
                            .join("")
                        :
                        `<span style="color:#64748b;">—</span>`
                    }
                </div>
            </td>

            <td>
                <div class="team-cell">
                    <i class="fa-solid fa-users-rectangle"></i>
                    <span>${escapeHtml(normalizeTeamName(user.teamName) || "—")}</span>
                </div>
            </td>

            <td>
                <span class="user-status ${status.toLowerCase()}">
                    ${
                        status === "ACTIVE"
                        ? '<span class="status-pulse-dot"></span> Đang hoạt động'
                        :
                        status === "LOCKED"
                        ? '<i class="fa-solid fa-lock" style="font-size:0.75rem;"></i> Đã khóa'
                        :
                        status
                    }
                </span>
            </td>

            <td>
                <div class="user-actions">
                    <button
                        class="user-action edit"
                        data-edit-user="${user.id}"
                        type="button"
                        title="Chỉnh sửa thông tin và vai trò"
                    >
                        <i class="fa-solid fa-pen-to-square"></i> Sửa
                    </button>

                    ${
                        !current &&
                        status === "ACTIVE"
                        ?
                        `
                        <button
                            class="user-action lock"
                            data-lock-user="${user.id}"
                            type="button"
                            title="Khóa tài khoản và bàn giao dữ liệu"
                        >
                            <i class="fa-solid fa-lock"></i> Khóa
                        </button>
                        `
                        :
                        ""
                    }

                    ${
                        !current &&
                        status === "LOCKED"
                        ?
                        `
                        <button
                            class="user-action unlock"
                            data-unlock-user="${user.id}"
                            type="button"
                            title="Mở khóa tài khoản"
                        >
                            <i class="fa-solid fa-lock-open"></i> Mở khóa
                        </button>
                        `
                        :
                        ""
                    }

                    ${
                        !current
                        ?
                        `
                        <button
                            class="user-action delete"
                            data-delete-user="${user.id}"
                            type="button"
                            title="Xóa vĩnh viễn tài khoản"
                        >
                            <i class="fa-solid fa-trash-can"></i> Xóa
                        </button>
                        `
                        :
                        ""
                    }
                </div>
            </td>
        `;

        tableBody.appendChild(
            tr
        );
    });

    updateKpiCards();


    const paginationInfo =
        document.getElementById(
            "paginationInfo"
        );

    if (paginationInfo) {

        paginationInfo.textContent =
            `${totalItems} bản ghi · tối đa ${PAGE_SIZE} dòng/trang`;
    }


    const pageNumber =
        document.getElementById(
            "pageNumber"
        );

    if (pageNumber) {

        pageNumber.textContent =
            `${currentPage}/${totalPages}`;
    }


    const prev =
        document.getElementById(
            "prevPage"
        );

    const next =
        document.getElementById(
            "nextPage"
        );

    if (prev) {

        prev.disabled =
            currentPage <= 1;
    }

    if (next) {

        next.disabled =
            currentPage >= totalPages;
    }
}


/* =========================================================
   EVENTS
========================================================= */

function bindEvents() {

    document
        .getElementById(
            "createUserButton"
        )
        ?.addEventListener(
            "click",
            () => openUserDrawer()
        );


    document
        .getElementById(
            "closeUserDrawer"
        )
        ?.addEventListener(
            "click",
            closeUserDrawer
        );


    document
        .getElementById(
            "cancelUserForm"
        )
        ?.addEventListener(
            "click",
            closeUserDrawer
        );


    drawerOverlay
        ?.addEventListener(
            "click",
            closeUserDrawer
        );


    searchInput
        ?.addEventListener(
            "input",
            () => {

                clearTimeout(
                    searchTimer
                );

                searchTimer =
                    setTimeout(
                        async () => {

                            currentPage = 1;

                            await safeReload();

                        },
                        350
                    );
            }
        );


    statusFilter
        ?.addEventListener(
            "change",
            async () => {

                currentPage = 1;

                await safeReload();
            }
        );


    roleFilter
        ?.addEventListener(
            "change",
            render
        );


    document
        .getElementById(
            "prevPage"
        )
        ?.addEventListener(
            "click",
            async () => {

                if (
                    currentPage <= 1
                ) {
                    return;
                }

                currentPage--;

                await safeReload();
            }
        );


    document
        .getElementById(
            "nextPage"
        )
        ?.addEventListener(
            "click",
            async () => {

                if (
                    currentPage >= totalPages
                ) {
                    return;
                }

                currentPage++;

                await safeReload();
            }
        );


    form
        ?.addEventListener(
            "submit",
            saveUserForm
        );


    document
        .getElementById(
            "closeLockModal"
        )
        ?.addEventListener(
            "click",
            closeLockModal
        );


    document
        .getElementById(
            "cancelLock"
        )
        ?.addEventListener(
            "click",
            closeLockModal
        );


    lockOverlay
        ?.addEventListener(
            "click",
            closeLockModal
        );


    document
        .getElementById(
            "lockForm"
        )
        ?.addEventListener(
            "submit",
            submitLock
        );


    document.addEventListener(
        "click",
        handleActionClick
    );

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeUserDrawer();
            closeLockModal();
        }
    });
}


/* =========================================================
   ACTION CLICK
========================================================= */

async function handleActionClick(
    event
) {

    const edit =
        event.target.closest(
            "[data-edit-user]"
        );

    if (edit) {

        openUserDrawer(
            Number(
                edit.dataset.editUser
            )
        );

        return;
    }


    const lock =
        event.target.closest(
            "[data-lock-user]"
        );

    if (lock) {

        openLockModal(
            Number(
                lock.dataset.lockUser
            )
        );

        return;
    }


    const unlock =
        event.target.closest(
            "[data-unlock-user]"
        );

    if (unlock) {

        await unlockUser(
            Number(
                unlock.dataset.unlockUser
            )
        );

        return;
    }


    const remove =
        event.target.closest(
            "[data-delete-user]"
        );

    if (remove) {

        await deleteUser(
            Number(
                remove.dataset.deleteUser
            )
        );
    }
}


/* =========================================================
   USER DRAWER
========================================================= */

function openUserDrawer(
    userId = null
) {

    form?.reset();

    clearErrors();

    if (form) {
        form.scrollTop = 0;
    }

    const editing =
        document.getElementById(
            "editingUserIndex"
        );

    if (editing) {

        editing.value =
            userId === null
                ? ""
                : String(userId);
    }


    const title =
        document.getElementById(
            "userDrawerTitle"
        );

    const passwordLabel =
        document.getElementById(
            "userPasswordLabel"
        );

    const password =
        document.getElementById(
            "userPassword"
        );


    if (userId === null) {

        if (title) {
            title.textContent =
                "Tạo người dùng";
        }

        if (passwordLabel) {
            passwordLabel.hidden =
                false;
            passwordLabel.style.display =
                "";
        }

        if (password) {
            password.required =
                true;
            password.value = "";
        }

        setValue(
            "userDataScope",
            "SELF"
        );

    } else {

        const user =
            users.find(
                item =>
                    Number(item.id) ===
                    Number(userId)
            );

        if (!user) {
            return;
        }

        if (title) {
            title.textContent =
                "Cập nhật người dùng";
        }

        setValue(
            "fullName",
            user.fullName
        );

        setValue(
            "userEmail",
            user.email
        );

        setValue(
            "userTeam",
            user.teamId
        );

        setValue(
            "userDataScope",
            user.dataScope ||
            "SELF"
        );


        document
            .querySelectorAll(
                'input[name="roles"]'
            )
            .forEach(input => {

                input.checked =
                    user.roles.some(
                        role =>
                            String(role.id) ===
                            String(input.value)
                    );
            });


        if (passwordLabel) {
            passwordLabel.hidden =
                true;
            passwordLabel.style.display =
                "none";
        }

        if (password) {

            password.required =
                false;

            password.value =
                "";
        }
    }


    drawer?.classList.add(
        "open"
    );

    drawerOverlay
        ?.classList.add(
            "open"
        );
}


function closeUserDrawer() {

    drawer?.classList.remove(
        "open"
    );

    drawerOverlay
        ?.classList.remove(
            "open"
        );
}


/* =========================================================
   SAVE USER
========================================================= */

async function saveUserForm(
    event
) {

    event.preventDefault();

    clearErrors();

    const fullName =
        value("fullName");

    const email =
        value("userEmail");

    const password =
        document.getElementById(
            "userPassword"
        )?.value || "";

    const teamValue =
        value("userTeam");

    const dataScope =
        value("userDataScope") ||
        "SELF";

    const roleIds =
        Array
            .from(
                document.querySelectorAll(
                    'input[name="roles"]:checked'
                )
            )
            .map(
                input =>
                    Number(input.value)
            );


    const editValue =
        value(
            "editingUserIndex"
        );

    const userId =
        editValue
            ? Number(editValue)
            : null;

    const existing =
        userId !== null
            ? users.find(
                item =>
                    Number(item.id) ===
                    userId
            )
            : null;

    let hasValidationError = false;

    if (!fullName) {
        setError(
            "nameError",
            "Vui lòng nhập họ và tên."
        );
        hasValidationError = true;
    }

    if (!email) {
        setError(
            "emailError",
            "Vui lòng nhập email công ty."
        );
        hasValidationError = true;
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        setError(
            "emailError",
            "Email không đúng định dạng."
        );
        hasValidationError = true;
    }

    if (userId === null) {
        if (!password) {
            setError(
                "passwordUserError",
                "Vui lòng nhập mật khẩu ban đầu."
            );
            hasValidationError = true;
        } else if (password.length < 8 || !/[a-zA-Z]/.test(password) || !/\d/.test(password)) {
            setError(
                "passwordUserError",
                "Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số."
            );
            hasValidationError = true;
        }
    }

    if (!roleIds.length) {
        setError(
            "rolesError",
            "Vui lòng chọn ít nhất một vai trò."
        );
        hasValidationError = true;
    }

    const isTeamLead = roles.some(role => role.code === "TEAM_LEAD" && roleIds.includes(Number(role.id)));
    if (isTeamLead && !teamValue) {
        setError(
            "teamError",
            "Trưởng nhóm kinh doanh phải được gán vào một nhóm kinh doanh."
        );
        hasValidationError = true;
    }

    if (hasValidationError) {
        if (form) {
            form.scrollTop = 0;
        }
        return;
    }


    const submit =
        form.querySelector(
            'button[type="submit"]'
        );

    const original =
        submit?.textContent || "";

    if (submit) {

        submit.disabled = true;
        submit.textContent =
            "Đang lưu...";
    }


    try {

        let savedUser;


        if (userId === null) {

            savedUser =
                await api(
                    "/api/users",
                    {
                        method:
                            "POST",

                        body:
                            JSON.stringify({
                                fullName,
                                email,
                                password,
                                status:
                                    "ACTIVE"
                            })
                    }
                );

        } else {

            savedUser =
                await api(
                    `/api/users/${userId}`,
                    {
                        method:
                            "PUT",

                        body:
                            JSON.stringify({
                                fullName,
                                email,
                                password:
                                    null,
                                status:
                                    existing
                                        ?.status ||
                                    "ACTIVE"
                            })
                    }
                );
        }


        const savedId =
            Number(
                savedUser?.id ||
                userId
            );


        /* Team must be assigned before the backend accepts TEAM_LEAD. */
        await api(
            `/api/users/${savedId}/team`,
            {
                method:
                    "POST",

                body:
                    JSON.stringify({
                        teamId:
                            teamValue
                                ? Number(
                                    teamValue
                                )
                                : null
                    })
            }
        );


        /* Assign role and legacy dataScope after the team. */
        await api(
            "/api/permissions/assign",
            {
                method:
                    "POST",

                body:
                    JSON.stringify({
                        userId:
                            savedId,

                        roleIds,

                        dataScope
                    })
            }
        );

        try {
            if (existing) {
                const oldRoles = (existing.roles || []).map(r => normalizeRoleName(r)).sort().join(", ");
                const newRoleNames = roles.filter(r => roleIds.includes(Number(r.id))).map(r => normalizeRoleName(r)).sort().join(", ");
                if (oldRoles !== newRoleNames) {
                    await recordAuditLog({
                        entity: "USER_ROLE",
                        entityId: String(savedId),
                        action: "UPDATE_ROLE",
                        description: `Cập nhật vai trò người dùng (${fullName})`,
                        beforeValue: oldRoles || "Chưa có vai trò",
                        afterValue: newRoleNames || "Chưa có vai trò"
                    });
                }

                const scopeMap = { SELF: "Chỉ bản thân (SELF)", TEAM: "Dữ liệu nhóm (TEAM)", ALL: "Toàn bộ dữ liệu (ALL)" };
                if (existing.dataScope !== dataScope) {
                    await recordAuditLog({
                        entity: "DATA_OWNERSHIP",
                        entityId: String(savedId),
                        action: "CHANGE_SCOPE",
                        description: `Thay đổi phạm vi quyền sở hữu dữ liệu (${fullName})`,
                        beforeValue: scopeMap[existing.dataScope] || existing.dataScope || "Chỉ bản thân (SELF)",
                        afterValue: scopeMap[dataScope] || dataScope || "Chỉ bản thân (SELF)"
                    });
                }
            } else {
                const newRoleNames = roles.filter(r => roleIds.includes(Number(r.id))).map(r => normalizeRoleName(r)).sort().join(", ");
                const scopeMap = { SELF: "Chỉ bản thân (SELF)", TEAM: "Dữ liệu nhóm (TEAM)", ALL: "Toàn bộ dữ liệu (ALL)" };
                await recordAuditLog({
                    entity: "USER_ROLE",
                    entityId: String(savedId),
                    action: "CREATE_USER",
                    description: `Tạo người dùng mới (${fullName})`,
                    beforeValue: "Chưa có tài khoản",
                    afterValue: `Vai trò: ${newRoleNames || "Chưa có"} | Phạm vi: ${scopeMap[dataScope] || dataScope}`
                });
            }
        } catch (auditErr) {
            console.warn("Audit logging warning:", auditErr);
        }


        closeUserDrawer();

        await loadUsers();

    } catch (error) {

        console.error(
            "Save user error:",
            error
        );

        if (
            String(
                error.message
            )
                .toLowerCase()
                .includes("email")
        ) {

            setError(
                "emailError",
                error.message
            );

        } else {

            setError(
                "userFormError",
                error.message ||
                "Không thể lưu người dùng."
            );
        }

        if (form) {
            form.scrollTop = 0;
        }

    } finally {

        if (submit) {

            submit.disabled =
                false;

            submit.textContent =
                original;
        }
    }
}


/* =========================================================
   LOCK + TRANSFER
========================================================= */

function openLockModal(
    userId
) {

    const user =
        users.find(
            item =>
                Number(item.id) ===
                Number(userId)
        );

    if (!user) {
        return;
    }


    setValue(
        "lockUserIndex",
        userId
    );

    const name =
        document.getElementById(
            "lockUserName"
        );

    if (name) {
        name.textContent =
            user.fullName;
    }


    const select =
        document.getElementById(
            "transferUser"
        );

    if (select) {

        select.innerHTML = `
            <option value="">
                Chọn người nhận
            </option>
        `;


        users
            .filter(
                item =>
                    Number(item.id) !==
                    Number(userId)
                    &&
                    String(
                        item.status
                    ).toUpperCase() ===
                    "ACTIVE"
            )
            .forEach(item => {

                const option =
                    document.createElement(
                        "option"
                    );

                option.value =
                    String(item.id);

                option.textContent =
                    `${item.fullName} (${item.email})`;

                select.appendChild(
                    option
                );
            });
    }


    setValue(
        "transferReason",
        ""
    );

    setError(
        "lockError",
        ""
    );


    lockModal
        ?.classList.add(
            "open"
        );

    lockOverlay
        ?.classList.add(
            "open"
        );
}


function closeLockModal() {

    lockModal
        ?.classList.remove(
            "open"
        );

    lockOverlay
        ?.classList.remove(
            "open"
        );
}


async function submitLock(
    event
) {

    event.preventDefault();

    const fromUserId =
        Number(
            value(
                "lockUserIndex"
            )
        );

    const toUserId =
        Number(
            value(
                "transferUser"
            )
        );

    const reason =
        value(
            "transferReason"
        );


    if (!toUserId) {

        setError(
            "lockError",
            "Vui lòng chọn người nhận bàn giao."
        );

        return;
    }


    const button =
        event.currentTarget
            .querySelector(
                'button[type="submit"]'
            );

    const original =
        button?.textContent || "";

    if (button) {

        button.disabled =
            true;

        button.textContent =
            "Đang xử lý...";
    }


    try {

        /*
         * Bàn giao dữ liệu trước.
         */

        await api(
            `/api/users/${fromUserId}/transfer-data`,
            {
                method:
                    "POST",

                body:
                    JSON.stringify({
                        toUserId
                    })
            }
        );


        /*
         * Sau đó khóa tài khoản.
         */

        await api(
            `/api/users/${fromUserId}/lock`,
            {
                method:
                    "POST",

                body:
                    JSON.stringify({
                        reason:
                            reason ||
                            "Khóa tài khoản và bàn giao dữ liệu"
                    })
            }
        );

        const fromUser =
            users.find(
                u =>
                    Number(u.id) ===
                    fromUserId
            );

        const toUser =
            users.find(
                u =>
                    Number(u.id) ===
                    toUserId
            );

        await recordAuditLog({
            entity:
                "DATA_OWNERSHIP",
            entityId:
                String(fromUserId),
            action:
                "TRANSFER_DATA",
            description:
                `Bàn giao quyền sở hữu dữ liệu từ ${fromUser?.fullName || fromUserId} sang ${toUser?.fullName || toUserId}`,
            beforeValue:
                fromUser
                    ? `Chủ sở hữu: ${fromUser.fullName} (${fromUser.email})`
                    : `User #${fromUserId}`,
            afterValue:
                toUser
                    ? `Chủ sở hữu mới: ${toUser.fullName} (${toUser.email}) - Lý do: ${reason || "Bàn giao khi khóa tài khoản"}`
                    : `User #${toUserId}`
        });


        closeLockModal();

        await loadUsers();

    } catch (error) {

        console.error(
            "Lock user error:",
            error
        );

        setError(
            "lockError",
            error.message ||
            "Không thể khóa tài khoản."
        );

    } finally {

        if (button) {

            button.disabled =
                false;

            button.textContent =
                original;
        }
    }
}


/* =========================================================
   UNLOCK
========================================================= */

async function unlockUser(
    userId
) {

    try {

        await api(
            `/api/users/${userId}/unlock`,
            {
                method:
                    "POST"
            }
        );

        const target = users.find(u => Number(u.id) === Number(userId));
        await recordAuditLog({
            entity: "DATA_OWNERSHIP",
            entityId: String(userId),
            action: "UNLOCK",
            description: `Mở khóa và khôi phục quyền truy cập dữ liệu (${target?.fullName || userId})`,
            beforeValue: "Trạng thái: Đã khóa (LOCKED)",
            afterValue: "Trạng thái: Đang hoạt động (ACTIVE)"
        });

        await loadUsers();

    } catch (error) {

        console.error(
            "Unlock user error:",
            error
        );

        alert(
            error.message ||
            "Không thể mở khóa tài khoản."
        );
    }
}


/* =========================================================
   DELETE
========================================================= */

async function deleteUser(
    userId
) {

    const user =
        users.find(
            item =>
                Number(item.id) ===
                Number(userId)
        );

    if (!user) {
        return;
    }


    const accepted =
        window.confirm(
            `Xóa người dùng "${user.fullName}"?`
        );

    if (!accepted) {
        return;
    }


    try {

        await api(
            `/api/users/${userId}`,
            {
                method:
                    "DELETE"
            }
        );

        await recordAuditLog({
            entity: "USER_ROLE",
            entityId: String(userId),
            action: "DELETE_USER",
            description: `Xóa tài khoản người dùng (${user.fullName})`,
            beforeValue: `${user.fullName} (${user.email}) - Vai trò: ${(user.roles || []).map(r => r.name).join(", ") || "Không có"}`,
            afterValue: "Đã xóa khỏi hệ thống"
        });

        await loadUsers();
        alert(`Đã xóa người dùng "${user.fullName}" thành công!`);

    } catch (error) {

        console.error(
            "Delete user error:",
            error
        );

        alert(
            error.message ||
            "Không thể xóa người dùng."
        );
    }
}


/* =========================================================
   AUDIT LOG HELPER
========================================================= */

async function recordAuditLog(entry) {

    const timestamp =
        new Date().toISOString();

    const actorName =
        sessionUser?.fullName ||
        "Quản trị viên";

    const actorId =
        sessionUser?.id ||
        1;

    const actorEmail =
        sessionUser?.email ||
        "admin@company.com";

    try {
        await fetch(
            `${API_BASE}/api/audit-logs`,
            {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type":
                        "application/json",
                    "Accept":
                        "application/json"
                },
                body:
                    JSON.stringify({
                        entity:
                            entry.entity,
                        entityId:
                            entry.entityId,
                        action:
                            entry.action,
                        description:
                            entry.description,
                        beforeValue:
                            entry.beforeValue,
                        afterValue:
                            entry.afterValue
                    })
            }
        );
    } catch (err) {
        console.warn(
            "Backend audit log record warning:",
            err
        );
    }

    try {
        const raw =
            localStorage.getItem(
                "crm_audit_logs"
            );

        const list =
            raw
                ? JSON.parse(raw)
                : [];

        list.unshift({
            id:
                `local_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
            createdAt:
                timestamp,
            userId:
                actorId,
            userName:
                actorName,
            userEmail:
                actorEmail,
            entity:
                entry.entity,
            entityId:
                entry.entityId,
            action:
                entry.action,
            description:
                entry.description,
            beforeValue:
                entry.beforeValue,
            afterValue:
                entry.afterValue
        });

        if (list.length > 100) {
            list.length = 100;
        }

        localStorage.setItem(
            "crm_audit_logs",
            JSON.stringify(list)
        );

    } catch (_) {}
}


/* =========================================================
   HELPERS
========================================================= */

async function safeReload() {

    try {

        await loadUsers();

    } catch (error) {

        console.error(
            "Reload users error:",
            error
        );

        showPageError(
            error.message
        );
    }
}


function setTableLoading(
    loading
) {

    if (!tableBody) {
        return;
    }

    if (loading) {

        tableBody.style.opacity =
            "0.55";

        tableBody.style.pointerEvents =
            "none";

    } else {

        tableBody.style.opacity =
            "";

        tableBody.style.pointerEvents =
            "";
    }
}


function showPageError(
    message
) {

    if (!empty) {
        return;
    }

    empty.hidden =
        false;

    empty.innerHTML = `
        <strong>
            Không thể tải dữ liệu
        </strong>

        <span>
            ${escapeHtml(message || "")}
        </span>
    `;
}


function clearErrors() {

    [
        "nameError",
        "emailError",
        "passwordUserError",
        "teamError",
        "rolesError",
        "dataScopeError",
        "userFormError",
        "lockError"
    ]
        .forEach(
            id =>
                setError(
                    id,
                    ""
                )
        );
}


function setError(
    id,
    message
) {

    const element =
        document.getElementById(
            id
        );

    if (element) {
        element.textContent =
            message || "";
    }
}


function value(id) {

    return (
        document
            .getElementById(id)
            ?.value ?? ""
    )
        .toString()
        .trim();
}


function setValue(
    id,
    value
) {

    const element =
        document.getElementById(
            id
        );

    if (element) {

        element.value =
            value ?? "";
    }
}


function initials(value) {

    const parts =
        String(value || "")
            .trim()
            .split(/\s+/)
            .filter(Boolean);

    if (!parts.length) {
        return "U";
    }

    if (parts.length === 1) {

        return parts[0]
            .slice(0,2)
            .toUpperCase();
    }

    return (
        parts[0][0] +
        parts[
            parts.length - 1
        ][0]
    )
        .toUpperCase();
}


function escapeHtml(value) {

    return String(
        value ?? ""
    )
        .replace(/&/g,"&amp;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;")
        .replace(/"/g,"&quot;")
        .replace(/'/g,"&#039;");
}
