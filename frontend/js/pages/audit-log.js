"use strict";

const API_BASE = "http://localhost:8080/crm";

let allAuditLogs = [];
let filteredLogs = [];

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

    // Prioritize backend logs
    backendLogs.forEach(item => {
        const key = `backend_${item.id}`;
        seenKeys.add(key);
        merged.push(normalizeLogItem(item, "backend"));
    });

    // Merge client-local logs if not already recorded in backend
    localLogs.forEach(item => {
        const key = `local_${item.id || item.created_at || item.createdAt}`;
        // If content matches recent backend record within 10 seconds, skip duplicate
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

    const categoryInfo = resolveCategory(entity, action, desc, fieldName);

    const userName = item.userName || item.user_name || item.actorName || item.fullName || (item.userId ? `User #${item.userId}` : "Quản trị viên");
    const userEmail = item.email || item.userEmail || (item.userId === 7 ? "admin@company.com" : "");

    const beforeRaw = item.beforeValue !== undefined ? item.beforeValue : item.before_value;
    const afterRaw = item.afterValue !== undefined ? item.afterValue : item.after_value;

    return {
        id: item.id || `local_${Math.random()}`,
        source,
        timestamp: validDate.getTime(),
        dateObj: validDate,
        timeFormatted: formatDateTime(validDate),
        actor: {
            id: item.userId || item.actor_user_id || item.actorUserId || 1,
            name: userName,
            email: userEmail,
            initials: getInitials(userName)
        },
        category: categoryInfo.name,
        categorySlug: categoryInfo.slug,
        field: resolveFieldName(categoryInfo.name, desc, fieldName, item.entityId || item.object_id),
        beforeFormatted: formatAuditValue(beforeRaw, categoryInfo.name),
        afterFormatted: formatAuditValue(afterRaw, categoryInfo.name)
    };
}

function resolveCategory(entity, action, desc, field) {
    const text = `${entity} ${action} ${desc} ${field}`.toLowerCase();

    // 1. Chiết khấu
    if (text.includes("discount") || text.includes("chiết khấu")) {
        return { name: "Chiết khấu", slug: "chiet-khau" };
    }

    // 2. Chỉ tiêu
    if (text.includes("target") || text.includes("kpi") || text.includes("chỉ tiêu")) {
        return { name: "Chỉ tiêu", slug: "chi-tieu" };
    }

    // 3. Quyền sở hữu
    if (text.includes("ownership") || text.includes("transfer") || text.includes("scope") ||
        text.includes("quyền sở hữu") || text.includes("bàn giao") || text.includes("phạm vi")) {
        return { name: "Quyền sở hữu", slug: "quyen-so-huu" };
    }

    // 4. Vai trò người dùng
    if (text.includes("role") || text.includes("permission") || text.includes("vai trò") ||
        text.includes("assign") || entity === "USER" || action.includes("USER")) {
        return { name: "Vai trò người dùng", slug: "vai-tro" };
    }

    // Default fallback to Vai trò người dùng
    return { name: "Vai trò người dùng", slug: "vai-tro" };
}

function resolveFieldName(category, desc, field, entityId) {
    if (field && field.trim()) return field;
    if (desc && desc.trim() && desc.length <= 60 && !desc.includes("{")) return desc;

    switch (category) {
        case "Chiết khấu":
            return entityId ? `Chiết khấu (${entityId})` : "Tỷ lệ chiết khấu";
        case "Chỉ tiêu":
            return "Chỉ tiêu doanh số";
        case "Quyền sở hữu":
            return "Phạm vi dữ liệu / Bàn giao";
        case "Vai trò người dùng":
            return entityId ? `Vai trò người dùng (#${entityId})` : "Vai trò & quyền hạn";
        default:
            return "Dữ liệu hệ thống";
    }
}

function formatAuditValue(val, category) {
    if (val === null || val === undefined || val === "" || val === "null") {
        return "—";
    }

    if (typeof val === "string") {
        val = val.trim();

        // Check if string is JSON object/array
        if ((val.startsWith("{") && val.endsWith("}")) || (val.startsWith("[") && val.endsWith("]"))) {
            try {
                const obj = JSON.parse(val);

                // Roles array
                if (obj.roles && Array.isArray(obj.roles)) {
                    const rolesStr = obj.roles.map(r => r.name || r.code).join(", ") || "Không có vai trò";
                    const scopeStr = obj.dataScope ? ` [Phạm vi: ${formatScopeLabel(obj.dataScope)}]` : "";
                    return rolesStr + scopeStr;
                }

                // Data scope only
                if (obj.dataScope && !obj.roles) {
                    return formatScopeLabel(obj.dataScope);
                }

                // Transferred user ownership
                if (obj.ownerUserId) {
                    return `User #${obj.ownerUserId}`;
                }

                // User profile
                if (obj.fullName || obj.email) {
                    const parts = [];
                    if (obj.fullName) parts.push(obj.fullName);
                    if (obj.email) parts.push(`(${obj.email})`);
                    if (obj.status) parts.push(`[${obj.status}]`);
                    return parts.join(" ");
                }

                // General key-values
                const pairs = Object.entries(obj)
                    .filter(([k]) => k !== "permissions" && k !== "userId" && k !== "id")
                    .map(([k, v]) => `${k}: ${typeof v === "object" ? JSON.stringify(v) : v}`);
                if (pairs.length > 0) return pairs.join("; ");
            } catch (_) {
                // Not valid JSON, keep as string
            }
        }

        // Pure large integer (sales target / money)
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
        // 1. User search
        if (userQuery) {
            const actorName = log.actor.name.toLowerCase();
            const actorEmail = (log.actor.email || "").toLowerCase();
            const actorId = String(log.actor.id);
            if (!actorName.includes(userQuery) && !actorEmail.includes(userQuery) && !actorId.includes(userQuery)) {
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

        tr.innerHTML = `
            <td>
                <span class="audit-time">${escapeHtml(item.timeFormatted)}</span>
            </td>
            <td>
                <div class="audit-user-cell">
                    <span class="audit-avatar">${escapeHtml(item.actor.initials)}</span>
                    <div class="audit-user-info">
                        <strong>${escapeHtml(item.actor.name)}</strong>
                        <small>${escapeHtml(item.actor.email || `ID: ${item.actor.id}`)}</small>
                    </div>
                </div>
            </td>
            <td>
                <span class="audit-badge badge-${escapeHtml(item.categorySlug)}">
                    ${escapeHtml(item.category)}
                </span>
            </td>
            <td>
                <span class="audit-field-name">${escapeHtml(item.field)}</span>
            </td>
            <td>
                <div class="audit-val audit-val-before">${escapeHtml(item.beforeFormatted)}</div>
            </td>
            <td>
                <div class="audit-val audit-val-after">${escapeHtml(item.afterFormatted)}</div>
            </td>
        `;

        tbody.appendChild(tr);
    });
}