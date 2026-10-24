"use strict";

const API_BASE = `${window.location.pathname.startsWith("/crm/") ? window.location.origin : window.location.protocol + "//" + (window.location.hostname || "localhost") + ":8080"}/crm`;

let currentMode = "TEAMS"; // "TEAMS" (Nhóm kinh doanh & Khu vực) | "UNITS" (Phòng ban nội bộ)

let teamsList = [];
let selectedTeamId = null;
let teamLookups = { regions: [], users: [] };

let organizations = [];
let selectedOrgId = null;

const tree = document.getElementById("organizationTree");
const empty = document.getElementById("organizationEmpty");

// TAB BUTTONS
const tabTeamsMode = document.getElementById("tabTeamsMode");
const tabUnitsMode = document.getElementById("tabUnitsMode");
const addMainActionBtn = document.getElementById("addMainActionBtn");

// HEADINGS
const pageHeadingTitle = document.getElementById("pageHeadingTitle");
const pageHeadingSub = document.getElementById("pageHeadingSub");
const treeCardTitle = document.getElementById("treeCardTitle");
const treeCardSubtitle = document.getElementById("treeCardSubtitle");

// DETAIL PANEL
const orgDetailEmpty = document.getElementById("orgDetailEmpty");
const orgDetail = document.getElementById("orgDetail");
const detailBadgeType = document.getElementById("detailBadgeType");
const detailName = document.getElementById("detailName");
const detailType = document.getElementById("detailType");
const rowDetailCode = document.getElementById("rowDetailCode");
const detailCode = document.getElementById("detailCode");
const rowDetailRegion = document.getElementById("rowDetailRegion");
const detailRegion = document.getElementById("detailRegion");
const labelDetailParent = document.getElementById("labelDetailParent");
const detailParent = document.getElementById("detailParent");
const labelDetailManager = document.getElementById("labelDetailManager");
const detailManager = document.getElementById("detailManager");
const rowDetailMembersCount = document.getElementById("rowDetailMembersCount");
const detailMembersCount = document.getElementById("detailMembersCount");
const rowDetailDescription = document.getElementById("rowDetailDescription");
const detailDescription = document.getElementById("detailDescription");
const teamMembersContainer = document.getElementById("teamMembersContainer");
const teamMembersTotalBadge = document.getElementById("teamMembersTotalBadge");
const teamMembersList = document.getElementById("teamMembersList");

// ACTION BUTTONS
const addChildBtn = document.getElementById("addChildOrg");
const editBtn = document.getElementById("editOrg");
const deleteBtn = document.getElementById("deleteOrg");

// TEAM DRAWER
const teamOverlay = document.getElementById("teamOverlay");
const teamDrawer = document.getElementById("teamDrawer");
const teamDrawerTitle = document.getElementById("teamDrawerTitle");
const teamForm = document.getElementById("teamForm");
const editingTeamId = document.getElementById("editingTeamId");
const teamNameInput = document.getElementById("teamName");
const teamParentSelect = document.getElementById("teamParent");
const teamLeaderSelect = document.getElementById("teamLeader");
const teamRegionSelect = document.getElementById("teamRegion");
const teamActiveSelect = document.getElementById("teamActive");
const teamFormError = document.getElementById("teamFormError");
const closeTeamDrawerBtn = document.getElementById("closeTeamDrawer");
const cancelTeamBtn = document.getElementById("cancelTeam");

// ORG DRAWER (PHÒNG BAN)
const orgOverlay = document.getElementById("orgOverlay");
const orgDrawer = document.getElementById("orgDrawer");
const orgDrawerTitle = document.getElementById("orgDrawerTitle");
const orgForm = document.getElementById("orgForm");
const editingOrgId = document.getElementById("editingOrgId");
const parentOrgId = document.getElementById("parentOrgId");
const orgCodeInput = document.getElementById("orgCode");
const orgNameInput = document.getElementById("orgName");
const orgTypeInput = document.getElementById("orgType");
const orgManagerInput = document.getElementById("orgManager");
const orgDescriptionInput = document.getElementById("orgDescription");
const orgFormError = document.getElementById("orgFormError");
const closeOrgDrawerBtn = document.getElementById("closeOrgDrawer");
const cancelOrgBtn = document.getElementById("cancelOrg");

/* =========================================================
   API HELPER
========================================================= */

async function api(path, options = {}) {
    const config = {
        credentials: "include",
        headers: {
            "Accept": "application/json",
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...(options.headers || {})
        },
        ...options
    };

    const response = await fetch(API_BASE + path, config);
    let result = null;

    try {
        result = await response.json();
    } catch (_) {
        result = null;
    }

    if (response.status === 401) {
        localStorage.removeItem("crm_ui_session");
        window.location.href = "login.html";
        throw new Error("Phiên đăng nhập đã hết hạn.");
    }

    if (!response.ok || !result?.success) {
        const error = new Error(result?.message || `HTTP ${response.status}`);
        error.status = response.status;
        error.data = result?.data;
        throw error;
    }

    return result.data;
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

/* =========================================================
   INITIALIZE
========================================================= */

document.addEventListener("DOMContentLoaded", init);

async function init() {
    bindTabEvents();
    bindTeamDrawerEvents();
    bindOrgDrawerEvents();
    bindDetailActionEvents();

    await loadInitialData();
}

function bindTabEvents() {
    tabTeamsMode?.addEventListener("click", () => switchMode("TEAMS"));
    tabUnitsMode?.addEventListener("click", () => switchMode("UNITS"));

    addMainActionBtn?.addEventListener("click", () => {
        if (currentMode === "TEAMS") {
            openTeamDrawer(null, null);
        } else {
            openOrgDrawer(null, null);
        }
    });
}

function switchMode(mode) {
    currentMode = mode;
    if (mode === "TEAMS") {
        if (tabTeamsMode) tabTeamsMode.className = "org-tab-btn active";
        if (tabUnitsMode) tabUnitsMode.className = "org-tab-btn";
        if (pageHeadingTitle) pageHeadingTitle.innerHTML = '<i class="fa-solid fa-users-rectangle text-primary"></i> Nhóm kinh doanh &amp; Khu vực địa lý';
        if (pageHeadingSub) pageHeadingSub.textContent = "Quản lý cây nhóm kinh doanh, trưởng nhóm và gán khu vực địa lý cho nhóm.";
        if (addMainActionBtn) addMainActionBtn.innerHTML = '<i class="fa-solid fa-plus"></i> + Thêm nhóm kinh doanh';
        if (treeCardTitle) treeCardTitle.textContent = "Cây nhóm kinh doanh";
        if (treeCardSubtitle) treeCardSubtitle.textContent = "Cấu trúc cây quyết định phạm vi dữ liệu mà Trưởng nhóm nhìn thấy.";
        if (addChildBtn) addChildBtn.innerHTML = '<i class="fa-solid fa-plus"></i> + Thêm nhóm con';
    } else {
        if (tabTeamsMode) tabTeamsMode.className = "org-tab-btn";
        if (tabUnitsMode) tabUnitsMode.className = "org-tab-btn active";
        if (pageHeadingTitle) pageHeadingTitle.innerHTML = '<i class="fa-solid fa-building-user text-primary"></i> Phòng ban nội bộ';
        if (pageHeadingSub) pageHeadingSub.textContent = "Cấu hình phòng ban, đơn vị và phân cấp phòng ban nội bộ.";
        if (addMainActionBtn) addMainActionBtn.innerHTML = '<i class="fa-solid fa-plus"></i> + Thêm đơn vị';
        if (treeCardTitle) treeCardTitle.textContent = "Cây phòng ban";
        if (treeCardSubtitle) treeCardSubtitle.textContent = "Bấm vào một đơn vị để xem hoặc chỉnh sửa.";
        if (addChildBtn) addChildBtn.innerHTML = '<i class="fa-solid fa-plus"></i> + Thêm đơn vị con';
    }

    renderTree();
    renderDetail();
}

async function loadInitialData() {
    try {
        await Promise.all([loadTeams(), loadTeamLookups()]);
    } catch (e) {
        console.error("Load teams error:", e);
    }
}

/* =========================================================
   TEAMS LOGIC
========================================================= */

async function loadTeams() {
    try {
        const res = await api("/api/teams");
        const oldExpanded = new Map(teamsList.map(t => [Number(t.id), t.expanded]));

        teamsList = Array.isArray(res?.teams)
            ? res.teams.map(t => ({
                  ...t,
                  id: Number(t.id),
                  parentId: t.parentId == null ? null : Number(t.parentId),
                  leaderId: t.leaderId == null ? null : Number(t.leaderId),
                  regionId: t.regionId == null ? null : Number(t.regionId),
                  expanded: oldExpanded.get(Number(t.id)) ?? true
              }))
            : [];

        if (selectedTeamId && !teamsList.some(t => Number(t.id) === Number(selectedTeamId))) {
            selectedTeamId = null;
        }

        if (currentMode === "TEAMS") {
            renderTree();
            renderDetail();
        }
    } catch (error) {
        console.error("Load teams failed:", error);
    }
}

async function loadTeamLookups() {
    try {
        const data = await api("/api/teams/lookups");
        teamLookups.regions = Array.isArray(data?.regions) ? data.regions : [];
        teamLookups.users = Array.isArray(data?.users) ? data.users : [];
    } catch (error) {
        console.warn("Lookups load error:", error);
    }
}

async function loadOrganizations() {
    try {
        const res = await api("/api/organization/units");
        const oldExpanded = new Map(organizations.map(o => [Number(o.id), o.expanded]));

        organizations = Array.isArray(res)
            ? res.map(o => ({
                  ...o,
                  id: Number(o.id),
                  parentId: o.parentId == null ? null : Number(o.parentId),
                  managerId: o.managerId == null ? null : Number(o.managerId),
                  expanded: oldExpanded.get(Number(o.id)) ?? true
              }))
            : [];

        if (selectedOrgId && !organizations.some(o => Number(o.id) === Number(selectedOrgId))) {
            selectedOrgId = null;
        }

        if (currentMode === "UNITS") {
            renderTree();
            renderDetail();
        }
    } catch (error) {
        console.error("Load org units failed:", error);
    }
}

/* =========================================================
   RENDER TREE
========================================================= */

function renderTree() {
    if (!tree) return;
    tree.innerHTML = "";

    if (currentMode === "TEAMS") {
        const roots = teamsList.filter(t => t.parentId == null);
        if (empty) empty.style.display = roots.length ? "none" : "flex";

        roots.forEach(root => {
            tree.appendChild(createTeamNode(root));
        });
    } else {
        const roots = organizations.filter(o => o.parentId == null);
        if (empty) empty.style.display = roots.length ? "none" : "flex";

        roots.forEach(root => {
            tree.appendChild(createOrgNode(root));
        });
    }
}

function createTeamNode(item) {
    const wrapper = document.createElement("div");
    wrapper.className = "org-tree-node";

    const children = teamsList.filter(c => Number(c.parentId) === Number(item.id));
    const isSelected = Number(selectedTeamId) === Number(item.id);

    const row = document.createElement("div");
    row.className = "org-node-row" + (isSelected ? " selected" : "");
    row.dataset.selectTeam = item.id;

    const expandChar = children.length ? (item.expanded === false ? '<i class="fa-solid fa-angle-right"></i>' : '<i class="fa-solid fa-angle-down"></i>') : "";
    const regionTag = item.regionName
        ? `<span class="team-badge-region"><i class="fa-solid fa-location-dot"></i> ${escapeHtml(item.regionName)}</span>`
        : `<span class="team-badge-muted"><i class="fa-solid fa-location-dot"></i> Chưa gán KV</span>`;
    const leaderTag = item.leaderName
        ? `<span class="team-badge-leader"><i class="fa-solid fa-user-tie"></i> ${escapeHtml(item.leaderName)}</span>`
        : `<span class="team-badge-no-leader"><i class="fa-solid fa-triangle-exclamation"></i> Chưa có Trưởng nhóm</span>`;
    const memberTag = `<span class="team-badge-members"><i class="fa-solid fa-user-group"></i> ${item.memberCount || 0} nhân sự</span>`;
    const statusTag = item.active === false ? `<span class="team-badge-inactive">(Ngừng HĐ)</span>` : "";

    row.innerHTML = `
        <button class="org-expand" type="button" data-expand-team="${item.id}" ${children.length ? "" : "disabled"}>
            ${expandChar}
        </button>
        <div class="org-node-icon">
            <i class="fa-solid fa-users"></i>
        </div>
        <div class="org-node-copy">
            <strong>${escapeHtml(item.name)} ${statusTag}</strong>
            <div class="team-meta-row">
                ${regionTag}
                ${leaderTag}
                ${memberTag}
            </div>
        </div>
    `;

    wrapper.appendChild(row);

    if (children.length && item.expanded !== false) {
        const childrenBox = document.createElement("div");
        childrenBox.className = "org-children";
        children.forEach(child => {
            childrenBox.appendChild(createTeamNode(child));
        });
        wrapper.appendChild(childrenBox);
    }

    return wrapper;
}

function createOrgNode(item) {
    const wrapper = document.createElement("div");
    wrapper.className = "org-tree-node";

    const children = organizations.filter(c => Number(c.parentId) === Number(item.id));
    const isSelected = Number(selectedOrgId) === Number(item.id);

    const row = document.createElement("div");
    row.className = "org-node-row" + (isSelected ? " selected" : "");
    row.dataset.selectOrg = item.id;

    row.innerHTML = `
        <button class="org-expand" type="button" data-expand-org="${item.id}" ${children.length ? "" : "disabled"}>
            ${children.length ? (item.expanded === false ? '<i class="fa-solid fa-angle-right"></i>' : '<i class="fa-solid fa-angle-down"></i>') : ""}
        </button>
        <div class="org-node-icon org-badge-purple">
            <i class="fa-solid fa-building"></i>
        </div>
        <div class="org-node-copy">
            <strong>${escapeHtml(item.name)}</strong>
            <span>${escapeHtml(item.code || "—")} · ${item.type || "department"} ${item.active === false ? " · Ngừng hoạt động" : ""}</span>
        </div>
    `;

    wrapper.appendChild(row);

    if (children.length && item.expanded !== false) {
        const childrenBox = document.createElement("div");
        childrenBox.className = "org-children";
        children.forEach(child => {
            childrenBox.appendChild(createOrgNode(child));
        });
        wrapper.appendChild(childrenBox);
    }

    return wrapper;
}

/* =========================================================
   TREE EVENT LISTENERS
========================================================= */

tree?.addEventListener("click", async event => {
    // Teams expand toggle
    const expandTeam = event.target.closest("[data-expand-team]");
    if (expandTeam) {
        event.stopPropagation();
        const id = Number(expandTeam.dataset.expandTeam);
        const item = teamsList.find(t => Number(t.id) === id);
        if (item) {
            item.expanded = item.expanded === false;
            renderTree();
        }
        return;
    }

    // Teams select row
    const selectTeam = event.target.closest("[data-select-team]");
    if (selectTeam) {
        selectedTeamId = Number(selectTeam.dataset.selectTeam);
        renderTree();
        renderDetail();
        return;
    }

    // Org expand toggle
    const expandOrg = event.target.closest("[data-expand-org]");
    if (expandOrg) {
        event.stopPropagation();
        const id = Number(expandOrg.dataset.expandOrg);
        const item = organizations.find(o => Number(o.id) === id);
        if (item) {
            item.expanded = item.expanded === false;
            renderTree();
        }
        return;
    }

    // Org select row
    const selectOrg = event.target.closest("[data-select-org]");
    if (selectOrg) {
        selectedOrgId = Number(selectOrg.dataset.selectOrg);
        renderTree();
        renderDetail();
    }
});

/* =========================================================
   RENDER DETAIL PANEL
========================================================= */

async function renderDetail() {
    if (currentMode === "TEAMS") {
        const team = teamsList.find(t => Number(t.id) === Number(selectedTeamId));

        if (!team) {
            orgDetailEmpty.hidden = false;
            orgDetail.hidden = true;
            return;
        }

        orgDetailEmpty.hidden = true;
        orgDetail.hidden = false;

        detailBadgeType.textContent = "Nhóm kinh doanh đang chọn";
        detailName.textContent = team.name;
        detailType.textContent = team.active !== false ? "Đang hoạt động" : "Ngừng hoạt động";
        detailType.style.background = team.active !== false ? "rgba(16, 185, 129, 0.15)" : "rgba(239, 68, 68, 0.15)";
        detailType.style.color = team.active !== false ? "#34d399" : "#f87171";
        detailType.style.border = team.active !== false ? "1px solid rgba(16, 185, 129, 0.25)" : "1px solid rgba(239, 68, 68, 0.25)";

        rowDetailCode.style.display = "none";
        rowDetailDescription.style.display = "none";

        rowDetailRegion.style.display = "block";
        detailRegion.innerHTML = team.regionName
            ? `<span class="team-badge-region"><i class="fa-solid fa-location-dot"></i> ${escapeHtml(team.regionName)} (${escapeHtml(team.regionCode || "")})</span>`
            : "<em>Chưa gán khu vực</em>";

        labelDetailParent.textContent = "Nhóm cấp trên";
        const parent = teamsList.find(t => Number(t.id) === Number(team.parentId));
        detailParent.textContent = parent ? parent.name : "Là nhóm cấp cao nhất (Root)";

        labelDetailManager.textContent = "Trưởng nhóm kinh doanh";
        detailManager.innerHTML = team.leaderName
            ? `<strong>${escapeHtml(team.leaderName)}</strong> ${team.leaderEmail ? `<span style="color:var(--crm-muted); font-size:11.5px;">(${escapeHtml(team.leaderEmail)})</span>` : ""}`
            : "<em>Chưa có trưởng nhóm</em>";

        rowDetailMembersCount.style.display = "block";
        detailMembersCount.textContent = `${team.memberCount || 0} nhân sự`;

        // Render member table
        teamMembersContainer.style.display = "block";
        teamMembersTotalBadge.textContent = String(team.memberCount || 0);
        await loadAndRenderTeamMembers(team.id);

    } else {
        const org = organizations.find(o => Number(o.id) === Number(selectedOrgId));

        if (!org) {
            orgDetailEmpty.hidden = false;
            orgDetail.hidden = true;
            return;
        }

        orgDetailEmpty.hidden = true;
        orgDetail.hidden = false;

        detailBadgeType.textContent = "Đơn vị đang chọn";
        detailName.textContent = org.name;
        detailType.textContent = org.type || "department";
        detailType.style.background = "rgba(59, 130, 246, 0.15)";
        detailType.style.color = "#60a5fa";
        detailType.style.border = "1px solid rgba(59, 130, 246, 0.25)";

        rowDetailCode.style.display = "block";
        detailCode.textContent = org.code || "—";

        rowDetailRegion.style.display = "none";
        rowDetailMembersCount.style.display = "none";
        teamMembersContainer.style.display = "none";

        labelDetailParent.textContent = "Đơn vị cha";
        const parent = organizations.find(o => Number(o.id) === Number(org.parentId));
        detailParent.textContent = parent ? parent.name : "Đơn vị gốc";

        labelDetailManager.textContent = "Người quản lý";
        detailManager.textContent = org.manager || "—";

        rowDetailDescription.style.display = "block";
        detailDescription.textContent = org.description || "—";
    }
}

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

function fixMojibake(str) {
    if (!str || typeof str !== "string") return str;
    return str
        .replace(/miß╗ün Bß│«c/g, "miền Bắc")
        .replace(/miß╗ün Trung/g, "miền Trung")
        .replace(/miß╗ün Nam/g, "miền Nam")
        .replace(/Quß[\|\s\S]*?vi├¬n/g, "Quản trị viên")
        .replace(/Quß│ún trß╗ï vi├¬n/g, "Quản trị viên")
        .replace(/Quß│ún trß╗ï hß╗ç thß╗æng/g, "Quản trị hệ thống")
        .replace(/Kß║┐ to├ín/g, "Kế toán")
        .replace(/Ch─âm s├│c kh├ích h├íng/g, "Chăm sóc khách hàng")
        .replace(/Gi├ím ─æß╗æc kinh doanh/g, "Giám đốc kinh doanh")
        .replace(/Nh├ón vi├¬n Marketing/g, "Nhân viên Marketing")
        .replace(/Nh├ón vi├¬n kinh doanh/g, "Nhân viên kinh doanh")
        .replace(/Nh[\|\s\S]*?doanh/g, "Nhân viên kinh doanh")
        .replace(/Tr[\|\s\S]*?doanh/g, "Trưởng nhóm kinh doanh")
        .replace(/Tr╞░ß╗ƒng nh├│m kinh doanh/g, "Trưởng nhóm kinh doanh");
}

function formatRoleLabel(m) {
    if (m.roleCodes) {
        const codes = String(m.roleCodes).split(",").map(c => c.trim()).filter(Boolean);
        const mapped = codes.map(c => ROLE_NAMES[c] || c);
        if (mapped.length > 0) {
            return mapped.join(", ");
        }
    }
    if (m.roleNames) {
        const cleaned = fixMojibake(m.roleNames);
        if (cleaned.includes("ADMIN") || cleaned.includes("Quản trị")) return "Quản trị viên";
        if (cleaned.includes("TEAM_LEAD") || cleaned.includes("Trưởng nhóm")) return "Trưởng nhóm kinh doanh";
        if (cleaned.includes("SALES_REP") || cleaned.includes("SALE") || cleaned.includes("kinh doanh")) return "Nhân viên kinh doanh";
        return cleaned;
    }
    return "Nhân viên kinh doanh";
}

function getRoleBadgeStyle(roleText) {
    if (roleText.includes("Quản trị")) {
        return "background:#f3e8ff; color:#7e22ce;"; // Tím nhẹ
    }
    if (roleText.includes("Trưởng nhóm")) {
        return "background:#dbeafe; color:#1d4ed8;"; // Lam nhẹ
    }
    if (roleText.includes("Giám đốc")) {
        return "background:#fef3c7; color:#b45309;"; // Hổ phách
    }
    return "background:#f1f5f9; color:#475569;"; // Xám xanh trang nhã
}

async function loadAndRenderTeamMembers(teamId) {
    if (!teamMembersList) return;
    teamMembersList.innerHTML = `<div class="team-members-empty">Đang tải danh sách thành viên...</div>`;

    try {
        const res = await api(`/api/teams/${teamId}/members`);
        const members = Array.isArray(res) ? res : (Array.isArray(res?.members) ? res.members : []);

        if (members.length === 0) {
            teamMembersList.innerHTML = `<div class="team-members-empty">Chưa có nhân viên nào trực thuộc nhóm này.</div>`;
            return;
        }

        let tableHtml = `
            <table class="team-members-table">
                <thead>
                    <tr>
                        <th style="width: 50%;">Thành viên</th>
                        <th style="width: 32%;">Vai trò</th>
                        <th style="width: 18%; text-align: right;">Trạng thái</th>
                    </tr>
                </thead>
                <tbody>
        `;

        members.forEach(m => {
            const roleLabel = formatRoleLabel(m);
            const roleStyle = getRoleBadgeStyle(roleLabel);
            const initial = (m.fullName || "U").trim().charAt(0).toUpperCase();
            const isActive = m.status === "ACTIVE";

            tableHtml += `
                <tr>
                    <td>
                        <div class="team-member-cell">
                            <div class="team-member-avatar">${escapeHtml(initial)}</div>
                            <div class="team-member-meta">
                                <span class="team-member-name">${escapeHtml(m.fullName || "—")}</span>
                                <span class="team-member-email">${escapeHtml(m.email || "—")}</span>
                            </div>
                        </div>
                    </td>
                    <td>
                        <span class="team-role-badge" style="${roleStyle}">${escapeHtml(roleLabel)}</span>
                    </td>
                    <td>
                        <span class="team-status-pill ${isActive ? "active" : "inactive"}">
                            <span class="team-status-dot"></span>
                            ${isActive ? "Hoạt động" : escapeHtml(m.status || "Khóa")}
                        </span>
                    </td>
                </tr>
            `;
        });

        tableHtml += `</tbody></table>`;
        teamMembersList.innerHTML = tableHtml;

    } catch (e) {
        teamMembersList.innerHTML = `<div class="team-members-empty">Không tải được thành viên: ${escapeHtml(e.message)}</div>`;
    }
}

/* =========================================================
   DETAIL ACTIONS (ADD CHILD, EDIT, DELETE)
========================================================= */

function bindDetailActionEvents() {
    addChildBtn?.addEventListener("click", () => {
        if (currentMode === "TEAMS") {
            if (!selectedTeamId) return;
            openTeamDrawer(null, selectedTeamId);
        } else {
            if (!selectedOrgId) return;
            openOrgDrawer(null, selectedOrgId);
        }
    });

    editBtn?.addEventListener("click", () => {
        if (currentMode === "TEAMS") {
            if (!selectedTeamId) return;
            openTeamDrawer(selectedTeamId, null);
        } else {
            if (!selectedOrgId) return;
            openOrgDrawer(selectedOrgId, null);
        }
    });

    deleteBtn?.addEventListener("click", async () => {
        if (currentMode === "TEAMS") {
            if (!selectedTeamId) return;
            const team = teamsList.find(t => Number(t.id) === Number(selectedTeamId));
            if (!team) return;

            const hasChildren = teamsList.some(t => Number(t.parentId) === Number(selectedTeamId));
            if (hasChildren) {
                alert("Không thể xóa nhóm đang có nhóm con. Vui lòng di chuyển hoặc xóa các nhóm con trước.");
                return;
            }

            if (team.memberCount > 0) {
                alert(`Không thể xóa nhóm đang có ${team.memberCount} nhân viên trực thuộc. Vui lòng chuyển nhân viên sang nhóm khác trước.`);
                return;
            }

            if (!confirm(`Bạn có chắc chắn muốn xóa nhóm kinh doanh "${team.name}"?`)) {
                return;
            }

            try {
                await api(`/api/teams/${selectedTeamId}`, { method: "DELETE" });
                selectedTeamId = null;
                await loadTeams();
            } catch (err) {
                alert(err.message || "Không thể xóa nhóm.");
            }

        } else {
            if (!selectedOrgId) return;
            const item = organizations.find(o => Number(o.id) === Number(selectedOrgId));
            if (!item) return;

            const hasChildren = organizations.some(o => Number(o.parentId) === Number(selectedOrgId));
            if (hasChildren) {
                alert("Không thể ngừng hoạt động đơn vị đang có đơn vị con.");
                return;
            }

            if (!confirm("Ngừng hoạt động đơn vị này?")) return;

            try {
                await api(`/api/organization/units/${item.id}`, {
                    method: "PUT",
                    body: JSON.stringify({
                        code: item.code,
                        name: item.name,
                        type: item.type || "department",
                        parentId: item.parentId ?? null,
                        managerId: item.managerId ?? null,
                        description: item.description || "",
                        active: false
                    })
                });
                selectedOrgId = null;
                await loadOrganizations();
            } catch (error) {
                alert(error.message || "Không thể cập nhật đơn vị.");
            }
        }
    });
}

/* =========================================================
   TEAM DRAWER LOGIC
========================================================= */

function bindTeamDrawerEvents() {
    closeTeamDrawerBtn?.addEventListener("click", closeTeamDrawer);
    cancelTeamBtn?.addEventListener("click", closeTeamDrawer);
    teamOverlay?.addEventListener("click", closeTeamDrawer);

    teamForm?.addEventListener("submit", async event => {
        event.preventDefault();
        teamFormError.textContent = "";

        const name = teamNameInput.value.trim();
        if (!name) {
            teamFormError.textContent = "Tên nhóm kinh doanh là bắt buộc.";
            return;
        }

        const editId = editingTeamId.value ? Number(editingTeamId.value) : null;
        const parentId = teamParentSelect.value ? Number(teamParentSelect.value) : null;
        const leaderId = teamLeaderSelect.value ? Number(teamLeaderSelect.value) : null;
        const regionId = teamRegionSelect.value ? Number(teamRegionSelect.value) : null;
        const active = teamActiveSelect.value === "true";

        const payload = {
            name,
            parentId,
            leaderId,
            regionId,
            active
        };

        try {
            let res;
            if (editId) {
                res = await api(`/api/teams/${editId}`, {
                    method: "PUT",
                    body: JSON.stringify(payload)
                });
                selectedTeamId = editId;
            } else {
                res = await api("/api/teams", {
                    method: "POST",
                    body: JSON.stringify(payload)
                });
                selectedTeamId = res?.id ? Number(res.id) : null;
            }

            closeTeamDrawer();
            await loadTeams();
        } catch (err) {
            teamFormError.textContent = err.message || "Không thể lưu nhóm kinh doanh.";
        }
    });
}

function openTeamDrawer(editId = null, parentId = null) {
    teamForm?.reset();
    teamFormError.textContent = "";
    editingTeamId.value = editId ? String(editId) : "";

    // 1. Populate parent options
    teamParentSelect.innerHTML = `<option value="">(Là nhóm gốc / Cấp cao nhất)</option>`;
    teamsList.forEach(t => {
        if (editId && Number(t.id) === Number(editId)) return; // Không cho phép chọn chính nó
        const opt = document.createElement("option");
        opt.value = String(t.id);
        opt.textContent = t.name;
        teamParentSelect.appendChild(opt);
    });

    // 2. Populate leader options
    teamLeaderSelect.innerHTML = `<option value="">Chưa chọn trưởng nhóm</option>`;
    teamLookups.users.forEach(u => {
        const opt = document.createElement("option");
        opt.value = String(u.id);
        opt.textContent = `${u.fullName} (${u.email})`;
        teamLeaderSelect.appendChild(opt);
    });

    // 3. Populate region options
    teamRegionSelect.innerHTML = `<option value="">Chưa gán khu vực</option>`;
    teamLookups.regions.forEach(r => {
        const opt = document.createElement("option");
        opt.value = String(r.id);
        opt.textContent = `${r.name} (${r.code})`;
        teamRegionSelect.appendChild(opt);
    });

    if (editId) {
        teamDrawerTitle.textContent = "Cập nhật nhóm kinh doanh";
        const item = teamsList.find(t => Number(t.id) === Number(editId));
        if (item) {
            teamNameInput.value = item.name || "";
            teamParentSelect.value = item.parentId ? String(item.parentId) : "";
            teamLeaderSelect.value = item.leaderId ? String(item.leaderId) : "";
            teamRegionSelect.value = item.regionId ? String(item.regionId) : "";
            teamActiveSelect.value = item.active !== false ? "true" : "false";
        }
    } else {
        teamDrawerTitle.textContent = parentId ? "Thêm nhóm con" : "Thêm nhóm kinh doanh";
        if (parentId) {
            teamParentSelect.value = String(parentId);
        }
        teamActiveSelect.value = "true";
    }

    teamOverlay?.classList.add("open");
    teamDrawer?.classList.add("open");
}

function closeTeamDrawer() {
    teamOverlay?.classList.remove("open");
    teamDrawer?.classList.remove("open");
}

/* =========================================================
   ORG DRAWER LOGIC (PHÒNG BAN NỘI BỘ)
========================================================= */

function bindOrgDrawerEvents() {
    closeOrgDrawerBtn?.addEventListener("click", closeOrgDrawer);
    cancelOrgBtn?.addEventListener("click", closeOrgDrawer);
    orgOverlay?.addEventListener("click", closeOrgDrawer);

    orgForm?.addEventListener("submit", async event => {
        event.preventDefault();
        orgFormError.textContent = "";

        const code = orgCodeInput.value.trim();
        const name = orgNameInput.value.trim();

        if (!code || !name) {
            orgFormError.textContent = "Mã đơn vị và tên đơn vị là bắt buộc.";
            return;
        }

        const editId = editingOrgId.value ? Number(editingOrgId.value) : null;
        const pId = parentOrgId.value ? Number(parentOrgId.value) : null;
        const type = orgTypeInput.value || "department";
        const managerVal = orgManagerInput.value.trim();
        const desc = orgDescriptionInput.value.trim();

        const body = {
            code,
            name,
            type,
            parentId: pId,
            managerId: managerVal ? Number(managerVal) || null : null,
            description: desc,
            active: true
        };

        try {
            let res;
            if (editId) {
                res = await api(`/api/organization/units/${editId}`, {
                    method: "PUT",
                    body: JSON.stringify(body)
                });
                selectedOrgId = editId;
            } else {
                res = await api("/api/organization/units", {
                    method: "POST",
                    body: JSON.stringify(body)
                });
                selectedOrgId = res?.id ? Number(res.id) : null;
            }

            closeOrgDrawer();
            await loadOrganizations();
        } catch (err) {
            orgFormError.textContent = err.message || "Không thể lưu đơn vị.";
        }
    });
}

function openOrgDrawer(editId = null, parentIdVal = null) {
    orgForm?.reset();
    orgFormError.textContent = "";
    editingOrgId.value = editId ? String(editId) : "";
    parentOrgId.value = parentIdVal ? String(parentIdVal) : "";

    if (editId) {
        orgDrawerTitle.textContent = "Cập nhật đơn vị";
        const item = organizations.find(o => Number(o.id) === Number(editId));
        if (item) {
            orgCodeInput.value = item.code || "";
            orgNameInput.value = item.name || "";
            orgTypeInput.value = item.type || "department";
            orgManagerInput.value = item.managerId ? String(item.managerId) : "";
            orgDescriptionInput.value = item.description || "";
        }
    } else {
        orgDrawerTitle.textContent = parentIdVal ? "Thêm đơn vị con" : "Thêm đơn vị";
    }

    orgOverlay?.classList.add("open");
    orgDrawer?.classList.add("open");
}

function closeOrgDrawer() {
    orgOverlay?.classList.remove("open");
    orgDrawer?.classList.remove("open");
}

// Close floating modals on Escape key
document.addEventListener("keydown", e => {
    if (e.key === "Escape") {
        closeTeamDrawer();
        closeOrgDrawer();
    }
});