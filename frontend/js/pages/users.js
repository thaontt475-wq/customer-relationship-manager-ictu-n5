"use strict";

const API_BASE = "http://localhost:8080/crm";
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

    /*
     * userTeam cũ đang là input text.
     * Backend cần teamId nên đổi thành select
     * nhưng vẫn giữ nguyên ID + class.
     */

    const currentTeam =
        document.getElementById(
            "userTeam"
        );

    if (
        currentTeam &&
        currentTeam.tagName !== "SELECT"
    ) {

        const select =
            document.createElement(
                "select"
            );

        select.id =
            "userTeam";

        select.className =
            currentTeam.className;

        currentTeam.replaceWith(
            select
        );
    }


    /*
     * Backend yêu cầu password khi tạo user.
     */

    if (
        !document.getElementById(
            "userPassword"
        )
    ) {

        const team =
            document.getElementById(
                "userTeam"
            );

        const teamLabel =
            team?.closest("label");

        if (teamLabel) {

            const passwordLabel =
                document.createElement(
                    "label"
                );

            passwordLabel.id =
                "userPasswordLabel";

            passwordLabel.innerHTML = `
                Mật khẩu ban đầu
                <em>*</em>

                <input
                    id="userPassword"
                    class="crm-input"
                    type="password"
                    autocomplete="new-password"
                    placeholder="Ít nhất 8 ký tự"
                >

                <small id="passwordUserError"></small>
            `;

            teamLabel.parentNode.insertBefore(
                passwordLabel,
                teamLabel
            );
        }
    }


    /*
     * Permission API yêu cầu dataScope.
     */

    if (
        !document.getElementById(
            "userDataScope"
        )
    ) {

        const rolesFieldset =
            document.querySelector(
                ".roles-fieldset"
            );

        if (rolesFieldset) {

            const wrapper =
                document.createElement(
                    "label"
                );

            wrapper.innerHTML = `
                Phạm vi dữ liệu
                <em>*</em>

                <select
                    id="userDataScope"
                    class="crm-input"
                >
                    <option value="SELF">
                        Chỉ dữ liệu của bản thân
                    </option>

                    <option value="TEAM">
                        Dữ liệu của nhóm
                    </option>

                    <option value="ALL">
                        Toàn bộ dữ liệu
                    </option>
                </select>
            `;

            rolesFieldset.insertAdjacentElement(
                "afterend",
                wrapper
            );
        }
    }


    /*
     * Không dùng checkbox FE giả để xác định
     * tài khoản hiện tại nữa.
     */

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
                role.name;

            roleFilter.appendChild(
                option
            );
        });
    }


    const fieldset =
        document.querySelector(
            ".roles-fieldset"
        );

    if (!fieldset) {
        return;
    }

    fieldset.innerHTML = `
        <legend>Vai trò</legend>
    `;

    roles.forEach(role => {

        const label =
            document.createElement(
                "label"
            );

        label.className =
            "role-check";

        label.innerHTML = `
            <input
                type="checkbox"
                value="${role.id}"
                name="roles"
            >
            ${escapeHtml(role.name)}
        `;

        fieldset.appendChild(
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
            team.name;

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
                    .map(role => role.name)
                : [];

        const status =
            String(
                user.status || ""
            ).toUpperCase();

        const current =
            Number(sessionUser?.id) ===
            Number(user.id);

        tr.innerHTML = `

            <td>

                <div class="user-cell">

                    <span class="user-avatar">
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
                ${escapeHtml(user.email)}
            </td>


            <td>

                <div class="role-list">

                    ${
                        roleNames.length
                        ?
                        roleNames
                            .map(
                                role =>
                                `<span class="role-pill">
                                    ${escapeHtml(role)}
                                </span>`
                            )
                            .join("")
                        :
                        "—"
                    }

                </div>

            </td>


            <td>
                ${escapeHtml(user.teamName || "—")}
            </td>


            <td>

                <span class="user-status ${status.toLowerCase()}">

                    ${
                        status === "ACTIVE"
                        ? "Đang hoạt động"
                        :
                        status === "LOCKED"
                        ? "Đã khóa"
                        :
                        status
                    }

                </span>

            </td>


            <td>

                <div class="user-actions">

                    <button
                        class="user-action"
                        data-edit-user="${user.id}"
                        type="button"
                    >
                        Sửa
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
                        >
                            Khóa
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
                        >
                            Mở khóa
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
                            class="user-action"
                            data-delete-user="${user.id}"
                            type="button"
                        >
                            Xóa
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


    if (
        !fullName ||
        !email
    ) {

        setError(
            "userFormError",
            "Vui lòng nhập họ tên và email."
        );

        return;
    }


    if (!roleIds.length) {

        setError(
            "userFormError",
            "Người dùng phải có ít nhất một vai trò."
        );

        return;
    }


    if (roles.some(role => role.code === "TEAM_LEAD" && roleIds.includes(Number(role.id))) && !teamValue) {
        setError("userFormError", "Trưởng nhóm kinh doanh phải được gán vào một nhóm.");
        return;
    }

    if (
        userId === null &&
        !password
    ) {

        setError(
            "passwordUserError",
            "Vui lòng nhập mật khẩu ban đầu."
        );

        return;
    }


    if (
        userId === null &&
        (
            password.length < 8 ||
            !/[A-Z]/.test(password) ||
            !/[a-z]/.test(password) ||
            !/\d/.test(password)
        )
    ) {

        setError(
            "passwordUserError",
            "Mật khẩu phải có ít nhất 8 ký tự, chữ hoa, chữ thường và số."
        );

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
                const oldRoles = (existing.roles || []).map(r => r.name).sort().join(", ");
                const newRoleNames = roles.filter(r => roleIds.includes(Number(r.id))).map(r => r.name).sort().join(", ");
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
                const newRoleNames = roles.filter(r => roleIds.includes(Number(r.id))).map(r => r.name).sort().join(", ");
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
        "emailError",
        "teamError",
        "userFormError",
        "passwordUserError",
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
