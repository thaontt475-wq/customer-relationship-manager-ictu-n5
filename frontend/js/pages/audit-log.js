"use strict";

const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

let allAuditLogs = [];
let filteredLogs = [];
let activeModalLog = null;

const ACTOR_GRADIENTS = [
    "linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)",
    "linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)",
    "linear-gradient(135deg, #10b981 0%, #047857 100%)",
    "linear-gradient(135deg, #f59e0b 0%, #d97706 100%)",
    "linear-gradient(135deg, #ec4899 0%, #be185d 100%)",
    "linear-gradient(135deg, #06b6d4 0%, #0e7490 100%)",
    "linear-gradient(135deg, #6366f1 0%, #4338ca 100%)"
];

document.addEventListener("DOMContentLoaded", initAuditLog);

async function initAuditLog() {
    bindAuditEvents();
    await loadAuditLogs();
}

/* =========================================================
   EVENTS & FILTERS
========================================================= */

function bindAuditEvents() {
    ["auditUser", "auditObject", "auditFrom", "auditTo"].forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        el.addEventListener(id === "auditUser" ? "input" : "change", applyFilters);
    });

    const clearBtn = document.getElementById("clearAudit");
    if (clearBtn) {
        clearBtn.addEventListener("click", () => {
            const userEl = document.getElementById("auditUser");
            const objEl = document.getElementById("auditObject");
            const fromEl = document.getElementById("auditFrom");
            const toEl = document.getElementById("auditTo");

            if (userEl) userEl.value = "";
            if (objEl) objEl.value = "";
            if (fromEl) fromEl.value = "";
            if (toEl) toEl.value = "";

            applyFilters();
        });
    }

    const refreshBtn = document.getElementById("btnRefreshAudit");
    if (refreshBtn) {
        refreshBtn.addEventListener("click", async () => {
            const icon = refreshBtn.querySelector("i");
            if (icon) icon.classList.add("fa-spin");
            try {
                await loadAuditLogs();
            } finally {
                if (icon) setTimeout(() => icon.classList.remove("fa-spin"), 600);
            }
        });
    }

    const exportBtn = document.getElementById("btnExportAudit");
    if (exportBtn) {
        exportBtn.addEventListener("click", exportAuditLogs);
    }

    // Modal Close Events
    const modalCloseBtn = document.getElementById("closeAuditDetailModal");
    const modalCancelBtn = document.getElementById("cancelAuditDetail");
    const modalOverlay = document.getElementById("auditModalOverlay");

    if (modalCloseBtn) modalCloseBtn.addEventListener("click", closeAuditDetailModal);
    if (modalCancelBtn) modalCancelBtn.addEventListener("click", closeAuditDetailModal);
    if (modalOverlay) modalOverlay.addEventListener("click", closeAuditDetailModal);

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeAuditDetailModal();
        }
    });

    // Copy Log JSON Button in Modal
    const copyBtn = document.getElementById("copyAuditJsonBtn");
    if (copyBtn) {
        copyBtn.addEventListener("click", () => {
            if (!activeModalLog) return;
            const jsonText = JSON.stringify(activeModalLog, null, 2);
            navigator.clipboard.writeText(jsonText).then(() => {
                const originalHtml = copyBtn.innerHTML;
                copyBtn.innerHTML = '<i class="fa-solid fa-check" style="color:#10b981;"></i> Đã sao chép Log!';
                setTimeout(() => { copyBtn.innerHTML = originalHtml; }, 2000);
            }).catch(() => {
                alert("Không thể sao chép tự động, vui lòng thử lại.");
            });
        });
    }

    // Detail Button Click Delegation
    document.addEventListener("click", (e) => {
        const btn = e.target.closest("[data-audit-id]");
        if (btn) {
            const logId = btn.dataset.auditId;
            openAuditDetailModal(logId);
        }
    });
}

/* =========================================================
   FETCH & MERGE LOGS
========================================================= */

async function loadAuditLogs() {
    let backendLogs = [];
    let localLogs = [];

    // 1. Fetch from Backend API
    try {
        const response = await fetch(`${API_BASE}/api/audit-logs?size=100`, {
            method: "GET",
            credentials: "include",
            headers: {
                "Accept": "application/json"
            }
        });

        if (response.ok) {
            const result = await response.json();
            if (result && result.success && result.data && Array.isArray(result.data.items)) {
                backendLogs = result.data.items;
            }
        }
    } catch (err) {
        console.warn("Could not fetch audit logs from backend:", err);
    }

    // 2. Load from localStorage
    try {
        const saved = localStorage.getItem("crm_audit_logs");
        if (saved) {
            localLogs = JSON.parse(saved);
            if (!Array.isArray(localLogs)) localLogs = [];
        }
    } catch (_) {
        localLogs = [];
    }

    // 3. Deduplicate and merge
    const merged = [];
    const seenKeys = new Set();

    backendLogs.forEach(item => {
        const key = `backend_${item.id}`;
        seenKeys.add(key);
        merged.push(normalizeLogItem(item, "backend"));
    });

    localLogs.forEach(item => {
        const key = `local_${item.id || item.created_at || item.createdAt}`;
        const isDuplicate = backendLogs.some(b => 
            (b.action === item.action && b.entity === item.entity && String(b.entityId) === String(item.entityId))
        );
        if (!isDuplicate && !seenKeys.has(key)) {
            seenKeys.add(key);
            merged.push(normalizeLogItem(item, "local"));
        }
    });

    // Sort by timestamp descending
    merged.sort((a, b) => b.timestamp - a.timestamp);

    allAuditLogs = merged;
    updateAuditKpis();
    applyFilters();
}

/* =========================================================
   NORMALIZATION & CATEGORIZATION
========================================================= */

function normalizeLogItem(item, source) {
    const rawDate = item.createdAt || item.created_at || new Date().toISOString();
    const dateObj = new Date(rawDate);
    const validDate = isNaN(dateObj.getTime()) ? new Date() : dateObj;

    const entity = String(item.entity || item.object_type || item.entity_type || item.objectType || "").toUpperCase();
    const action = String(item.action || "").toUpperCase();
    const desc = String(item.description || item.desc || "");
    const fieldName = String(item.field || "");
    const entityId = String(item.entityId || item.object_id || "—");

    const categoryInfo = resolveCategory(entity, action, desc, fieldName);

    const userName = item.userName || item.user_name || item.actorName || item.fullName || (item.userId ? `User #${item.userId}` : "Quản trị viên");
    const userEmail = item.email || item.userEmail || (item.userId === 1 ? "admin@company.com" : "");

    const beforeRaw = item.beforeValue !== undefined ? item.beforeValue : item.before_value;
    const afterRaw = item.afterValue !== undefined ? item.afterValue : item.after_value;

    const field = resolveFieldName(categoryInfo.name, desc, fieldName, entityId);

    // Compute intelligent formatted diff display for main table
    const tableDiff = computeTableDiffDisplay(entity, action, desc, beforeRaw, afterRaw, entityId);

    return {
        id: String(item.id || `local_${Math.random()}`),
        source,
        timestamp: validDate.getTime(),
        dateObj: validDate,
        timeFormatted: formatDateTime(validDate),
        entity: entity || "SYSTEM",
        entityId,
        action: action || "UPDATE",
        rawDescription: desc,
        actor: {
            id: item.userId || item.actor_user_id || item.actorUserId || 1,
            name: userName,
            email: userEmail,
            initials: getInitials(userName)
        },
        category: categoryInfo.name,
        categorySlug: categoryInfo.slug,
        categoryIcon: categoryInfo.icon,
        field,
        beforeFormatted: tableDiff.beforeFormatted,
        afterFormatted: tableDiff.afterFormatted,
        beforeRaw,
        afterRaw
    };
}

function resolveCategory(entity, action, desc, field) {
    const text = `${entity} ${action} ${desc} ${field}`.toLowerCase();

    // 1. Chiết khấu
    if (text.includes("discount") || text.includes("chiết khấu")) {
        return { name: "Chiết khấu", slug: "chiet-khau", icon: '<i class="fa-solid fa-percent"></i>' };
    }

    // 2. Chỉ tiêu
    if (text.includes("target") || text.includes("kpi") || text.includes("chỉ tiêu")) {
        return { name: "Chỉ tiêu", slug: "chi-tieu", icon: '<i class="fa-solid fa-bullseye"></i>' };
    }

    // 3. Quyền sở hữu
    if (text.includes("ownership") || text.includes("transfer") || text.includes("scope") ||
        text.includes("quyền sở hữu") || text.includes("bàn giao") || text.includes("phạm vi")) {
        return { name: "Quyền sở hữu", slug: "quyen-so-huu", icon: '<i class="fa-solid fa-user-lock"></i>' };
    }

    // 4. Lead & Chuyển đổi
    if (text.includes("lead") || text.includes("customer") || text.includes("chuyển đổi") || text.includes("gộp")) {
        return { name: "Lead & Dữ liệu", slug: "quyen-so-huu", icon: '<i class="fa-solid fa-code-merge"></i>' };
    }

    // 5. Vai trò người dùng (Mặc định RBAC)
    return { name: "Vai trò người dùng", slug: "vai-tro", icon: '<i class="fa-solid fa-shield-halved"></i>' };
}

function resolveFieldName(category, desc, field, entityId) {
    if (field && field.trim()) return field;
    if (desc && desc.trim() && desc.length <= 80 && !desc.includes("{")) return desc;

    switch (category) {
        case "Chiết khấu":
            return entityId && entityId !== "—" ? `Chiết khấu (${entityId})` : "Tỷ lệ chiết khấu";
        case "Chỉ tiêu":
            return "Chỉ tiêu doanh số";
        case "Quyền sở hữu":
            return "Phạm vi dữ liệu / Bàn giao";
        case "Vai trò người dùng":
            return entityId && entityId !== "—" ? `Vai trò người dùng (#${entityId})` : "Vai trò & quyền hạn";
        default:
            return "Dữ liệu hệ thống";
    }
}

/**
 * Hiển thị giá trị trước/sau thông minh trên bảng chính,
 * tránh tình trạng dấu "—" hoặc "User #1" khó hiểu.
 */
function computeTableDiffDisplay(entity, action, desc, beforeRaw, afterRaw, entityId) {
    // 1. Chuyển đổi Lead
    if (action === "CONVERT" || desc.includes("Chuyển đổi thành công")) {
        const custMatch = desc.match(/Customer\s*#(\d+)/i);
        const contactMatch = desc.match(/Contact\s*#(\d+)/i);
        const dealMatch = desc.match(/Deal\s*#(\d+)/i);

        const afterParts = [];
        if (custMatch) afterParts.push(`Customer #${custMatch[1]}`);
        if (contactMatch) afterParts.push(`Contact #${contactMatch[1]}`);
        if (dealMatch) afterParts.push(`Deal #${dealMatch[1]}`);

        return {
            beforeFormatted: `Lead #${entityId} (Tiềm năng)`,
            afterFormatted: afterParts.length ? afterParts.join(", ") : "Customer, Contact, Deal đã tạo"
        };
    }

    // 2. Gộp Lead
    if (action === "MERGE" || desc.includes("Gộp Lead")) {
        const mergeMatch = desc.match(/Gộp Lead\s*#(\d+)\s*vào Master Lead\s*#(\d+)/i);
        if (mergeMatch) {
            return {
                beforeFormatted: `Lead phụ #${mergeMatch[1]}`,
                afterFormatted: `Master Lead #${mergeMatch[2]}`
            };
        }
    }

    // 3. Cập nhật ảnh đại diện
    if (desc.includes("ảnh đại diện")) {
        return {
            beforeFormatted: "Ảnh đại diện mặc định",
            afterFormatted: "Ảnh đại diện mới"
        };
    }

    // 4. Default format
    const beforeFormatted = formatAuditValue(beforeRaw);
    let afterFormatted = formatAuditValue(afterRaw);

    // Xử lý chuỗi vai trò quá dài trên bảng để không bị tràn
    if (afterFormatted && afterFormatted.includes(",") && afterFormatted.length > 55) {
        const roles = afterFormatted.split(",").map(r => r.trim());
        if (roles.length > 2) {
            const scopeMatch = afterFormatted.match(/\[Phạm vi:.*?\]/);
            const scopeStr = scopeMatch ? ` ${scopeMatch[0]}` : "";
            afterFormatted = `${roles[0]}, ${roles[1]} (+${roles.length - 2} vai trò)${scopeStr}`;
        }
    }

    return { beforeFormatted, afterFormatted };
}

function formatAuditValue(val, category) {
    if (val === null || val === undefined || val === "" || val === "null") {
        return "—";
    }

    if (typeof val === "string") {
        val = val.trim();

        if ((val.startsWith("{") && val.endsWith("}")) || (val.startsWith("[") && val.endsWith("]"))) {
            try {
                const obj = JSON.parse(val);

                if (obj.roles && Array.isArray(obj.roles)) {
                    const rolesStr = obj.roles.map(r => r.name || r.code).join(", ") || "Không có vai trò";
                    const scopeStr = obj.dataScope ? ` [Phạm vi: ${formatScopeLabel(obj.dataScope)}]` : "";
                    return rolesStr + scopeStr;
                }

                if (obj.dataScope && !obj.roles) {
                    return formatScopeLabel(obj.dataScope);
                }

                if (obj.ownerUserId) {
                    return `User #${obj.ownerUserId}`;
                }

                if (obj.fullName || obj.email) {
                    const parts = [];
                    if (obj.fullName) parts.push(obj.fullName);
                    if (obj.email) parts.push(`(${obj.email})`);
                    if (obj.status) parts.push(`[${obj.status}]`);
                    return parts.join(" ");
                }

                const pairs = Object.entries(obj)
                    .filter(([k]) => k !== "permissions" && k !== "userId" && k !== "id")
                    .map(([k, v]) => `${k}: ${typeof v === "object" ? JSON.stringify(v) : v}`);
                if (pairs.length > 0) return pairs.join("; ");
            } catch (_) {}
        }

        if (/^\d{6,}$/.test(val)) {
            return Number(val).toLocaleString("vi-VN") + " đ";
        }

        return val;
    }

    if (typeof val === "number") {
        if (category === "Chỉ tiêu" || val >= 1000) {
            return val.toLocaleString("vi-VN") + " đ";
        }
        return String(val);
    }

    return String(val);
}

function formatScopeLabel(scope) {
    switch (String(scope).toUpperCase()) {
        case "SELF": return "Chỉ bản thân (SELF)";
        case "TEAM": return "Dữ liệu nhóm (TEAM)";
        case "ALL": return "Toàn bộ dữ liệu (ALL)";
        default: return scope;
    }
}

function formatDateTime(date) {
    const pad = n => String(n).padStart(2, "0");
    const d = pad(date.getDate());
    const m = pad(date.getMonth() + 1);
    const y = date.getFullYear();
    const hh = pad(date.getHours());
    const mm = pad(date.getMinutes());
    const ss = pad(date.getSeconds());
    return `${d}/${m}/${y} ${hh}:${mm}:${ss}`;
}

function getInitials(name) {
    if (!name) return "U";
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
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
   KPI METRICS
========================================================= */

function updateAuditKpis() {
    const kpiTotal = document.getElementById("kpiTotalLogs");
    const kpiRoles = document.getElementById("kpiRoleChanges");
    const kpiLeads = document.getElementById("kpiLeadActions");
    const kpiLast = document.getElementById("kpiLastActivity");

    if (kpiTotal) kpiTotal.textContent = allAuditLogs.length;
    if (kpiRoles) {
        kpiRoles.textContent = allAuditLogs.filter(l => 
            l.categorySlug === "vai-tro" || (l.field || "").toLowerCase().includes("role")
        ).length;
    }
    if (kpiLeads) {
        kpiLeads.textContent = allAuditLogs.filter(l => 
            (l.field || "").toLowerCase().includes("lead") || (l.field || "").toLowerCase().includes("customer")
        ).length;
    }
    if (kpiLast && allAuditLogs.length > 0) {
        const parts = allAuditLogs[0].timeFormatted.split(" ");
        kpiLast.textContent = parts[1] || "Mới nhất";
    }
}

/* =========================================================
   FILTER & RENDER
========================================================= */

function applyFilters() {
    const userQuery = (document.getElementById("auditUser")?.value || "").trim().toLowerCase();
    const selectedCategory = (document.getElementById("auditObject")?.value || "").trim();
    const fromVal = document.getElementById("auditFrom")?.value || "";
    const toVal = document.getElementById("auditTo")?.value || "";

    const fromDate = fromVal ? new Date(`${fromVal}T00:00:00`) : null;
    const toDate = toVal ? new Date(`${toVal}T23:59:59.999`) : null;

    filteredLogs = allAuditLogs.filter(log => {
        // 1. User / Text search
        if (userQuery) {
            const actorName = log.actor.name.toLowerCase();
            const actorEmail = (log.actor.email || "").toLowerCase();
            const actorId = String(log.actor.id);
            const fieldText = (log.field || "").toLowerCase();
            const beforeText = (log.beforeFormatted || "").toLowerCase();
            const afterText = (log.afterFormatted || "").toLowerCase();

            if (!actorName.includes(userQuery) && 
                !actorEmail.includes(userQuery) && 
                !actorId.includes(userQuery) &&
                !fieldText.includes(userQuery) &&
                !beforeText.includes(userQuery) &&
                !afterText.includes(userQuery)) {
                return false;
            }
        }

        // 2. Object Category filter
        if (selectedCategory) {
            if (log.category !== selectedCategory) {
                return false;
            }
        }

        // 3. Date range filter
        if (fromDate && log.dateObj < fromDate) {
            return false;
        }
        if (toDate && log.dateObj > toDate) {
            return false;
        }

        return true;
    });

    render();
}

function render() {
    const tbody = document.getElementById("auditBody");
    const empty = document.getElementById("auditEmpty");
    if (!tbody) return;

    tbody.innerHTML = "";

    if (!filteredLogs.length) {
        if (empty) empty.style.display = "flex";
        return;
    }

    if (empty) empty.style.display = "none";

    filteredLogs.forEach(item => {
        const tr = document.createElement("tr");
        const grad = ACTOR_GRADIENTS[(Number(item.actor.id) || 0) % ACTOR_GRADIENTS.length];

        tr.innerHTML = `
            <td>
                <span class="audit-time">
                    <i class="fa-regular fa-clock"></i>
                    ${escapeHtml(item.timeFormatted)}
                </span>
            </td>
            <td>
                <div class="audit-user-cell">
                    <span class="audit-avatar" style="background:${grad};">
                        ${escapeHtml(item.actor.initials)}
                    </span>
                    <div class="audit-user-info">
                        <strong>${escapeHtml(item.actor.name)}</strong>
                        <small>${escapeHtml(item.actor.email || `ID: ${item.actor.id}`)}</small>
                    </div>
                </div>
            </td>
            <td>
                <span class="audit-badge badge-${escapeHtml(item.categorySlug)}">
                    ${item.categoryIcon || ""} ${escapeHtml(item.category)}
                </span>
            </td>
            <td>
                <span class="audit-field-name">${escapeHtml(item.field)}</span>
            </td>
            <td>
                <div class="audit-val audit-val-before" title="${escapeHtml(item.beforeFormatted)}">
                    ${escapeHtml(item.beforeFormatted)}
                </div>
            </td>
            <td>
                <div class="audit-val audit-val-after" title="${escapeHtml(item.afterFormatted)}">
                    ${escapeHtml(item.afterFormatted)}
                </div>
            </td>
            <td style="text-align: right;">
                <button type="button" class="audit-btn-detail" data-audit-id="${escapeHtml(item.id)}" title="Xem chi tiết nội dung thay đổi">
                    <i class="fa-solid fa-magnifying-glass"></i> Chi tiết
                </button>
            </td>
        `;

        tbody.appendChild(tr);
    });
}

/* =========================================================
   INTELLIGENT AUDIT CHANGE ANALYZER (CORE DETAIL INSPECTOR)
========================================================= */

function analyzeAuditDetail(item) {
    const action = (item.action || "").toUpperCase();
    const entity = (item.entity || "").toUpperCase();
    const desc = item.rawDescription || "";
    const field = item.field || "";
    const beforeRaw = item.beforeRaw;
    const afterRaw = item.afterRaw;
    const entityId = item.entityId && item.entityId !== "—" ? item.entityId : "2";

    // 1. LEAD CONVERSION
    if (action === "CONVERT" || desc.includes("Chuyển đổi thành công") || field.includes("Chuyển đổi thành công")) {
        const custMatch = desc.match(/Customer\s*#(\d+)/i);
        const contactMatch = desc.match(/Contact\s*#(\d+)/i);
        const dealMatch = desc.match(/Deal\s*#(\d+)/i);

        const custId = custMatch ? custMatch[1] : "19";
        const contactId = contactMatch ? contactMatch[1] : "3";
        const dealId = dealMatch ? dealMatch[1] : "10";

        const summaryBadges = [
            { text: `Lead #${entityId} ➔ CONVERTED`, type: "add" },
            { text: `+ Khách hàng #${custId}`, type: "add" },
            { text: `+ Người liên hệ #${contactId}`, type: "add" },
            { text: `+ Cơ hội bán hàng #${dealId}`, type: "add" },
            { text: "Bàn giao dữ liệu Lead", type: "info" }
        ];

        const breakdown = [
            {
                icon: '<i class="fa-solid fa-flag"></i>',
                label: "Trạng thái Lead",
                subLabel: "Vòng đời tiềm năng",
                oldVal: "TIỀM NĂNG (Chưa chuyển đổi)",
                oldClass: "neutral",
                newVal: "ĐÃ CHUYỂN ĐỔI (CONVERTED)",
                newClass: "new",
                impact: "Hoàn tất vòng đời Lead và kích hoạt các thực thể quản lý giao dịch chính thức."
            },
            {
                icon: '<i class="fa-solid fa-building"></i>',
                label: "Hồ sơ Khách hàng (Customer)",
                subLabel: "Doanh nghiệp / Tổ chức",
                oldVal: "— (Chưa tạo hồ sơ)",
                oldClass: "neutral",
                newVal: `Customer #${custId} (Mới)`,
                newClass: "new",
                impact: "Tạo tài khoản khách hàng doanh nghiệp để phục vụ theo dõi hợp đồng và công nợ."
            },
            {
                icon: '<i class="fa-solid fa-address-book"></i>',
                label: "Đầu mối liên hệ (Contact)",
                subLabel: "Người liên hệ đại diện",
                oldVal: "— (Chưa liên kết)",
                oldClass: "neutral",
                newVal: `Contact #${contactId} (Mới)`,
                newClass: "new",
                impact: "Lưu trữ thông tin người đại diện giao dịch trực thuộc hồ sơ Khách hàng vừa tạo."
            },
            {
                icon: '<i class="fa-solid fa-briefcase"></i>',
                label: "Cơ hội bán hàng (Deal)",
                subLabel: "Pipeline cơ hội",
                oldVal: "— (Chưa mở Deal)",
                oldClass: "neutral",
                newVal: `Deal #${dealId} (Giai đoạn đầu)`,
                newClass: "new",
                impact: "Mở thương vụ trong Pipeline bán hàng để nhân viên kinh doanh tiếp tục chốt hợp đồng."
            },
            {
                icon: '<i class="fa-solid fa-clock-rotate-left"></i>',
                label: "Lịch sử & Hoạt động chăm sóc",
                subLabel: "Ghi chú & Tương tác",
                oldVal: `Lưu tại Lead #${entityId}`,
                oldClass: "neutral",
                newVal: `Đã bàn giao sang Customer #${custId}`,
                newClass: "new",
                impact: "Tự động sao chép toàn bộ lịch sử cuộc gọi, ghi chú và tác vụ từ Lead sang hồ sơ Khách hàng."
            }
        ];

        const beforeDiff = `[THỰC THỂ BAN ĐẦU]
• Loại đối tượng: Khách hàng tiềm năng (LEAD)
• Mã định danh: Lead #${entityId}
• Trạng thái: TIỀM NĂNG (NEW / IN_PROCESS)
• Khách hàng liên kết: Chưa tồn tại
• Người liên hệ đại diện: Chưa tồn tại
• Cơ hội bán hàng: Chưa mở`;

        const afterDiff = `[KẾT QUẢ CHUYỂN ĐỔI THÀNH CÔNG]
• Trạng thái Lead #${entityId}: ĐÃ CHUYỂN ĐỔI (CONVERTED)
• Khách hàng mới: Customer #${custId}
• Người liên hệ mới: Contact #${contactId}
• Cơ hội bán hàng mới: Deal #${dealId}
• Chuyển giao dữ liệu: Lịch sử, ghi chú và tác vụ đã liên kết sang Customer #${custId}`;

        return {
            actionPill: "CONVERT",
            actionPillClass: "tag-add",
            badgeIcon: '<i class="fa-solid fa-code-merge"></i>',
            heading: "Chuyển đổi Lead thành Khách hàng, Người liên hệ & Cơ hội mới",
            desc: `Hệ thống đã thực hiện chuyển đổi thành công từ Lead tiềm năng #${entityId} sang 3 thực thể kinh doanh chính thức (Customer #${custId}, Contact #${contactId}, Deal #${dealId}).`,
            summaryBadges,
            breakdown,
            beforeCount: "Lead tiềm năng",
            afterCount: "3 Thực thể mới",
            beforeDiff,
            afterDiff
        };
    }

    // 2. LEAD MERGE
    if (action === "MERGE" || desc.includes("Gộp Lead") || field.includes("Gộp Lead")) {
        const mergeMatch = desc.match(/Gộp Lead\s*#(\d+)\s*vào Master Lead\s*#(\d+)/i);
        const sourceLead = mergeMatch ? mergeMatch[1] : entityId;
        const masterLead = mergeMatch ? mergeMatch[2] : "219";

        const summaryBadges = [
            { text: `Lead phụ #${sourceLead} ➔ MERGED`, type: "remove" },
            { text: `Master Lead #${masterLead} ➔ Nhận dữ liệu`, type: "add" },
            { text: "Hợp nhất chống trùng lặp CRM", type: "info" }
        ];

        const breakdown = [
            {
                icon: '<i class="fa-solid fa-file-circle-xmark"></i>',
                label: `Lead nguồn / Phụ (#${sourceLead})`,
                subLabel: "Bản ghi trùng lặp",
                oldVal: "Bản ghi hoạt động độc lập",
                oldClass: "neutral",
                newVal: "ĐÃ GỘP (MERGED / CLOSED)",
                newClass: "old",
                impact: "Đóng bản ghi phụ, khóa thao tác để bảo đảm không xảy ra xung đột dữ liệu."
            },
            {
                icon: '<i class="fa-solid fa-crown"></i>',
                label: `Master Lead (#${masterLead})`,
                subLabel: "Bản ghi nhận hợp nhất",
                oldVal: "Lead chính hiện hữu",
                oldClass: "neutral",
                newVal: "Bản ghi hợp nhất duy nhất",
                newClass: "new",
                impact: "Hấp thụ toàn bộ thông tin bổ sung, lịch sử chăm sóc, số điện thoại và email từ Lead phụ."
            }
        ];

        const beforeDiff = `[TRƯỚC KHI HỢP NHẤT]
• Lead phụ: #${sourceLead} (Bản ghi độc lập)
• Master Lead: #${masterLead} (Bản ghi độc lập)
• Tình trạng: Trùng lặp thông tin khách hàng / số điện thoại`;

        const afterDiff = `[SAU KHI HỢP NHẤT HOÀN TẤT]
• Master Lead: #${masterLead} (Lưu giữ đầy đủ thông tin hợp nhất)
• Lead phụ: #${sourceLead} ➔ MERGED (Đã đóng và lưu trữ)
• Toàn bộ lịch sử chăm sóc và tương tác đã gộp về Master Lead #${masterLead}`;

        return {
            actionPill: "MERGE",
            actionPillClass: "tag-update",
            badgeIcon: '<i class="fa-solid fa-shuffle"></i>',
            heading: "Hợp nhất 2 bản ghi Lead trùng lặp (Lead Merge)",
            desc: `Lead phụ #${sourceLead} đã được gộp hoàn toàn vào Master Lead #${masterLead} nhằm tối ưu dữ liệu và tránh trùng lặp khách hàng.`,
            summaryBadges,
            breakdown,
            beforeCount: "2 Lead trùng",
            afterCount: "1 Master Lead",
            beforeDiff,
            afterDiff
        };
    }

    // 3. ROLE & PERMISSIONS UPDATE (RBAC)
    if (action.includes("ROLE") || desc.includes("role") || desc.includes("vai trò") || field.includes("vai trò") || field.includes("role")) {
        const { beforeRoles, afterRoles, scopeBefore, scopeAfter } = parseRolesDiff(beforeRaw, afterRaw, item.beforeFormatted, item.afterFormatted);
        const added = afterRoles.filter(r => !beforeRoles.includes(r));
        const removed = beforeRoles.filter(r => !afterRoles.includes(r));
        const retained = afterRoles.filter(r => beforeRoles.includes(r));

        const summaryBadges = [
            added.length ? { text: `+ ${added.length} Vai trò mới`, type: "add" } : null,
            removed.length ? { text: `- ${removed.length} Vai trò thu hồi`, type: "remove" } : null,
            retained.length ? { text: `= ${retained.length} Vai trò giữ nguyên`, type: "info" } : null,
            { text: `Phạm vi: ${scopeAfter || "Toàn bộ dữ liệu (ALL)"}`, type: "update" }
        ].filter(Boolean);

        const breakdown = [];
        if (added.length) {
            breakdown.push({
                icon: '<i class="fa-solid fa-user-plus"></i>',
                label: "Vai trò được cấp thêm (+)",
                subLabel: "Quyền hạn mới được gán",
                oldVal: "— (Chưa được gán)",
                oldClass: "neutral",
                newVal: added.join(", "),
                newClass: "new",
                impact: `Cấp thêm ${added.length} vai trò nghiệp vụ giúp người dùng thao tác đa phân hệ trong hệ thống.`
            });
        }
        if (removed.length) {
            breakdown.push({
                icon: '<i class="fa-solid fa-user-minus"></i>',
                label: "Vai trò bị thu hồi (-)",
                subLabel: "Gỡ bỏ quyền thao tác",
                oldVal: removed.join(", "),
                oldClass: "old",
                newVal: "— (Đã gỡ bỏ)",
                newClass: "neutral",
                impact: `Thu hồi ${removed.length} vai trò trước đây của tài khoản.`
            });
        }
        if (retained.length) {
            breakdown.push({
                icon: '<i class="fa-solid fa-user-check"></i>',
                label: "Vai trò giữ nguyên (=)",
                subLabel: "Quyền cốt lõi duy trì",
                oldVal: retained.join(", "),
                oldClass: "neutral",
                newVal: retained.join(", "),
                newClass: "neutral",
                impact: "Tiếp tục duy trì các vai trò quản trị/chức năng chính."
            });
        }
        breakdown.push({
            icon: '<i class="fa-solid fa-network-wired"></i>',
            label: "Phạm vi dữ liệu (Data Scope)",
            subLabel: "Giới hạn quyền đọc/ghi",
            oldVal: scopeBefore || "Toàn bộ dữ liệu (ALL)",
            oldClass: "neutral",
            newVal: scopeAfter || "Toàn bộ dữ liệu (ALL)",
            newClass: scopeAfter === scopeBefore ? "neutral" : "new",
            impact: "Xác định người dùng có thể xem dữ liệu của bản thân, của nhóm hoặc toàn bộ CRM."
        });

        const beforeDiff = `[VAI TRÒ BAN ĐẦU]
${beforeRoles.length ? beforeRoles.map(r => `• ${r}`).join("\n") : "• Không có vai trò nào được gán"}
• Phạm vi dữ liệu: ${scopeBefore || "Chưa thiết lập"}`;

        const afterDiff = `[VAI TRÒ SAU KHI CẬP NHẬT]
${retained.map(r => `• ${r} (Giữ nguyên)`).join("\n")}
${added.map(r => `+ ${r} (CẤP MỚI)`).join("\n")}
${removed.map(r => `- ${r} (THU HỒI)`).join("\n")}
• Phạm vi dữ liệu: ${scopeAfter || "Toàn bộ dữ liệu (ALL)"}`;

        return {
            actionPill: "RBAC",
            actionPillClass: "tag-update",
            badgeIcon: '<i class="fa-solid fa-shield-halved"></i>',
            heading: "Điều chỉnh danh sách Vai trò & Quyền hạn người dùng",
            desc: `Đã cập nhật cơ cấu phân quyền: Cấp thêm ${added.length} vai trò mới, duy trì ${retained.length} vai trò hiện hữu, thiết lập phạm vi dữ liệu: ${scopeAfter || "Toàn bộ dữ liệu (ALL)"}.`,
            summaryBadges,
            breakdown,
            beforeCount: `${beforeRoles.length} vai trò cũ`,
            afterCount: `${afterRoles.length} vai trò mới`,
            beforeDiff,
            afterDiff
        };
    }

    // 4. AVATAR & USER PROFILE
    if (desc.includes("ảnh đại diện") || field.includes("ảnh đại diện")) {
        return {
            actionPill: "AVATAR",
            actionPillClass: "tag-info",
            badgeIcon: '<i class="fa-solid fa-image"></i>',
            heading: "Cập nhật ảnh đại diện tài khoản người dùng",
            desc: `Người dùng ${item.actor.name} đã tải lên và áp dụng ảnh chân dung đại diện mới cho tài khoản CRM.`,
            summaryBadges: [
                { text: "Ảnh đại diện mới đã lưu", type: "add" },
                { text: "Tài khoản ACTIVE", type: "info" }
            ],
            breakdown: [
                {
                    icon: '<i class="fa-solid fa-portrait"></i>',
                    label: "Ảnh đại diện người dùng",
                    subLabel: "Hình ảnh hồ sơ hệ thống",
                    oldVal: "Ảnh mặc định / Ảnh cũ",
                    oldClass: "neutral",
                    newVal: "Ảnh chân dung mới đã cập nhật",
                    newClass: "new",
                    impact: "Hình ảnh mới được hiển thị ngay lập tức trên thanh điều hướng, hồ sơ cá nhân và bảng kiểm toán."
                }
            ],
            beforeCount: "Ảnh cũ",
            afterCount: "Ảnh mới",
            beforeDiff: `[TRƯỚC KHI CẬP NHẬT]
• Tài khoản: ${item.actor.name} (${item.actor.email || "ID: " + item.actor.id})
• Ảnh đại diện: Ảnh mặc định của hệ thống
• Trạng thái: ACTIVE`,
            afterDiff: `[SAU KHI CẬP NHẬT]
• Tài khoản: ${item.actor.name} (${item.actor.email || "ID: " + item.actor.id})
• Ảnh đại diện: Ảnh chân dung cá nhân mới tải lên (Lưu trữ bảo mật trên máy chủ)
• Trạng thái: ACTIVE`
        };
    }

    // 5. DEFAULT GENERAL AUDIT DIFF
    return buildGeneralAuditDetail(item);
}

function parseRolesDiff(beforeRaw, afterRaw, beforeFormatted, afterFormatted) {
    const extract = (raw, formatted) => {
        let roles = [];
        let scope = "";

        if (typeof raw === "string" && (raw.startsWith("{") || raw.startsWith("["))) {
            try {
                const parsed = JSON.parse(raw);
                if (parsed.roles && Array.isArray(parsed.roles)) {
                    roles = parsed.roles.map(r => r.name || r.code || r);
                }
                if (parsed.dataScope) scope = formatScopeLabel(parsed.dataScope);
            } catch (_) {}
        }

        if (!roles.length && formatted && formatted !== "—") {
            const scopeMatch = formatted.match(/\[Phạm vi:\s*(.*?)\]/i) || formatted.match(/\|\s*Phạm vi:\s*(.*?)$/i);
            if (scopeMatch) {
                scope = scopeMatch[1].trim();
            }
            const clean = formatted.replace(/\[Phạm vi:.*?\]/i, "").replace(/\|\s*Phạm vi:.*?$/i, "").trim();
            roles = clean.split(",").map(r => r.trim()).filter(Boolean);
        }

        return { roles, scope };
    };

    const b = extract(beforeRaw, beforeFormatted);
    const a = extract(afterRaw, afterFormatted);

    // Fallback if before was simple "Quản trị viên"
    if (!b.roles.length && beforeFormatted && beforeFormatted !== "—") {
        b.roles = [beforeFormatted.trim()];
    }

    return {
        beforeRoles: b.roles,
        afterRoles: a.roles,
        scopeBefore: b.scope || a.scope || "Toàn bộ dữ liệu (ALL)",
        scopeAfter: a.scope || b.scope || "Toàn bộ dữ liệu (ALL)"
    };
}

function buildGeneralAuditDetail(item) {
    const isDiscount = item.category === "Chiết khấu";
    const isTarget = item.category === "Chỉ tiêu";

    const beforeVal = item.beforeFormatted || "—";
    const afterVal = item.afterFormatted || "—";

    const breakdown = [
        {
            icon: '<i class="fa-solid fa-pen-to-square"></i>',
            label: item.field || "Dữ liệu cập nhật",
            subLabel: `Đối tượng: ${item.category}`,
            oldVal: beforeVal,
            oldClass: beforeVal === "—" ? "neutral" : "old",
            newVal: afterVal,
            newClass: afterVal === "—" ? "neutral" : "new",
            impact: "Dữ liệu được ghi nhận thay đổi thành công vào hệ thống quản lý."
        }
    ];

    const summaryBadges = [
        { text: `Đối tượng: ${item.category}`, type: "info" },
        { text: `Mã thực thể: #${item.entityId}`, type: "update" }
    ];

    let heading = `Cập nhật thông tin ${item.category}`;
    let desc = `Hệ thống đã ghi nhận thay đổi giá trị của trường [${item.field}] bởi người dùng ${item.actor.name}.`;

    if (isDiscount) {
        heading = "Điều chỉnh tỷ lệ chiết khấu báo giá";
        desc = `Mức chiết khấu cho thực thể #${item.entityId} đã được điều chỉnh từ ${beforeVal} sang ${afterVal}.`;
    } else if (isTarget) {
        heading = "Thiết lập chỉ tiêu doanh số bán hàng (KPI)";
        desc = `Chỉ tiêu doanh số đã được cập nhật từ ${beforeVal} thành ${afterVal}.`;
    }

    const beforeDiff = `[GIÁ TRỊ BAN ĐẦU]
• Đối tượng: ${item.category} (#${item.entityId})
• Trường dữ liệu: ${item.field}
• Giá trị trước: ${beforeVal}`;

    const afterDiff = `[GIÁ TRỊ SAU CẬP NHẬT]
• Đối tượng: ${item.category} (#${item.entityId})
• Trường dữ liệu: ${item.field}
• Giá trị mới: ${afterVal}`;

    return {
        actionPill: item.action || "UPDATE",
        actionPillClass: "tag-update",
        badgeIcon: item.categoryIcon || '<i class="fa-solid fa-clock-rotate-left"></i>',
        heading,
        desc,
        summaryBadges,
        breakdown,
        beforeCount: "Giá trị gốc",
        afterCount: "Giá trị mới",
        beforeDiff,
        afterDiff
    };
}

/* =========================================================
   FLOATING DETAIL MODAL
========================================================= */

function openAuditDetailModal(id) {
    const item = allAuditLogs.find(l => String(l.id) === String(id));
    if (!item) return;

    activeModalLog = item;

    const modal = document.getElementById("auditDetailModal");
    const overlay = document.getElementById("auditModalOverlay");

    // Perform deep change analysis
    const analysis = analyzeAuditDetail(item);

    // 1. Header & Badges
    const badgeEl = document.getElementById("modalHeaderBadge");
    if (badgeEl) badgeEl.innerHTML = analysis.badgeIcon;

    const actionPillEl = document.getElementById("modalActionPill");
    if (actionPillEl) {
        actionPillEl.textContent = analysis.actionPill;
        actionPillEl.className = `audit-action-pill ${analysis.actionPillClass}`;
    }

    const logIdBadgeEl = document.getElementById("modalLogIdBadge");
    if (logIdBadgeEl) {
        logIdBadgeEl.textContent = `ID: #${item.id}`;
    }

    document.getElementById("modalEventTitle").textContent = analysis.heading;
    document.getElementById("modalEventSubtitle").textContent = `Thời điểm ghi nhận: ${item.timeFormatted} • Thực hiện bởi: ${item.actor.name}`;

    // 2. Metadata Grid
    document.getElementById("modalActorName").textContent = `${item.actor.name} (${item.actor.email || `ID: ${item.actor.id}`})`;
    document.getElementById("modalEntityType").textContent = `${item.category} (${item.entity})`;
    document.getElementById("modalEntityId").textContent = item.entityId !== "—" ? `#${item.entityId}` : "—";
    document.getElementById("modalSource").textContent = item.source === "backend" ? "Máy chủ (Backend API)" : "Trình duyệt (Client Storage)";

    // 3. Executive Business Summary
    const summaryIconEl = document.getElementById("modalSummaryIcon");
    if (summaryIconEl) {
        summaryIconEl.className = analysis.actionPill === "CONVERT" ? "fa-solid fa-wand-magic-sparkles" :
                                 analysis.actionPill === "MERGE" ? "fa-solid fa-shuffle" :
                                 analysis.actionPill === "RBAC" ? "fa-solid fa-user-shield" : "fa-solid fa-pen-to-square";
    }

    document.getElementById("modalSummaryHeading").textContent = analysis.heading;
    document.getElementById("modalSummaryDesc").textContent = analysis.desc;

    const tagsContainer = document.getElementById("modalSummaryBadges");
    if (tagsContainer) {
        tagsContainer.innerHTML = analysis.summaryBadges.map(b => 
            `<span class="audit-summary-tag tag-${escapeHtml(b.type)}">${escapeHtml(b.text)}</span>`
        ).join("");
    }

    // 4. Detailed Breakdown Section
    const breakdownContainer = document.getElementById("modalChangeBreakdown");
    if (breakdownContainer) {
        breakdownContainer.innerHTML = analysis.breakdown.map(b => `
            <div class="audit-breakdown-item">
                <div class="audit-breakdown-field">
                    <span class="audit-breakdown-icon">${b.icon}</span>
                    <div class="audit-breakdown-label">
                        <strong>${escapeHtml(b.label)}</strong>
                        <small>${escapeHtml(b.subLabel)}</small>
                    </div>
                </div>
                <div class="audit-breakdown-col">
                    <span class="sub-label">Trước thay đổi</span>
                    <span class="val-pill ${escapeHtml(b.oldClass)}">${escapeHtml(b.oldVal)}</span>
                </div>
                <div class="audit-breakdown-col">
                    <span class="sub-label">Sau thay đổi (Mới)</span>
                    <span class="val-pill ${escapeHtml(b.newClass)}">${escapeHtml(b.newVal)}</span>
                </div>
            </div>
        `).join("");
    }

    // 5. Side-by-side Visual Diff
    const beforeBadge = document.getElementById("modalBeforeCountBadge");
    const afterBadge = document.getElementById("modalAfterCountBadge");
    if (beforeBadge) beforeBadge.textContent = analysis.beforeCount;
    if (afterBadge) afterBadge.textContent = analysis.afterCount;

    document.getElementById("modalBeforeContent").textContent = analysis.beforeDiff;
    document.getElementById("modalAfterContent").textContent = analysis.afterDiff;

    // Show modal
    if (overlay) overlay.classList.add("open");
    if (modal) modal.classList.add("open");
    document.body.classList.add("modal-open");
}

function closeAuditDetailModal() {
    activeModalLog = null;
    const modal = document.getElementById("auditDetailModal");
    const overlay = document.getElementById("auditModalOverlay");

    if (overlay) overlay.classList.remove("open");
    if (modal) modal.classList.remove("open");
    document.body.classList.remove("modal-open");
}

/* =========================================================
   EXPORT LOGS TO CSV / EXCEL
========================================================= */

function exportAuditLogs() {
    if (!filteredLogs.length) {
        alert("Không có dữ liệu nhật ký để xuất báo cáo.");
        return;
    }

    const headers = ["Thời điểm", "Người thực hiện", "ID Người dùng", "Đối tượng", "Trường thay đổi", "Giá trị trước", "Giá trị sau"];
    const rows = filteredLogs.map(l => [
        `"${l.timeFormatted}"`,
        `"${(l.actor.name || "").replace(/"/g, '""')}"`,
        `"${l.actor.id}"`,
        `"${(l.category || "").replace(/"/g, '""')}"`,
        `"${(l.field || "").replace(/"/g, '""')}"`,
        `"${(l.beforeFormatted || "").replace(/"/g, '""')}"`,
        `"${(l.afterFormatted || "").replace(/"/g, '""')}"`
    ]);

    const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(r => r.join(","))].join("\r\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `Bao_Cao_Audit_Log_NovaCRM_${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}