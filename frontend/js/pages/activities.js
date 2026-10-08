"use strict";

const API_BASE = "http://localhost:8080/crm";

let activities = [];

const drawer = document.getElementById("activityDrawer");
const overlay = document.getElementById("activityOverlay");
const activityForm = document.getElementById("activityForm");

/* =========================================================
   API CLIENT
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

/* =========================================================
   INITIALIZATION & DATA LOADING
========================================================= */
async function loadActivities() {
    try {
        const data = await api("/api/activities");
        if (Array.isArray(data)) {
            activities = data.map(item => normalizeActivityFromBackend(item));
            saveActivitiesCache();
            render();
            return;
        }
    } catch (err) {
        console.warn("Could not load activities from backend, falling back to cache:", err);
    }

    try {
        const cached = JSON.parse(localStorage.getItem("crm_ui_activities"));
        activities = Array.isArray(cached) ? cached : [];
    } catch {
        activities = [];
    }
    render();
}

function normalizeActivityFromBackend(item) {
    let dateStr = "";
    let timeStr = "";
    if (item.dueDate) {
        const dt = new Date(item.dueDate);
        if (!isNaN(dt.getTime())) {
            dateStr = dt.toISOString().split("T")[0];
            timeStr = dt.toTimeString().slice(0, 5);
        }
    }

    const rel = item.customerName || item.opportunityName || item.relation || "";
    const st = (item.status || "OPEN").toUpperCase() === "COMPLETED" ? "done" : "open";

    return {
        id: item.id,
        type: (item.type || "task").toLowerCase(),
        title: item.subject || item.title || "Hoạt động",
        relation: rel,
        date: dateStr,
        time: timeStr,
        owner: item.ownerName || "Tôi",
        description: item.description || "",
        reminder: !!item.reminder,
        status: st,
        customerId: item.customerId,
        opportunityId: item.opportunityId,
        createdAt: item.createdAt || new Date().toISOString()
    };
}

function saveActivitiesCache() {
    try {
        localStorage.setItem("crm_ui_activities", JSON.stringify(activities));
    } catch (_) {}
}

/* =========================================================
   EVENT LISTENERS
========================================================= */
document.getElementById("createActivity")?.addEventListener("click", () => openDrawer());
document.getElementById("closeActivityDrawer")?.addEventListener("click", closeDrawer);
document.getElementById("cancelActivity")?.addEventListener("click", closeDrawer);
overlay?.addEventListener("click", closeDrawer);

["activitySearch", "activityTypeFilter", "activityStatusFilter"].forEach(id => {
    document.getElementById(id)?.addEventListener("input", render);
    document.getElementById(id)?.addEventListener("change", render);
});

activityForm?.addEventListener("submit", event => {
    event.preventDefault();
    saveActivity();
});

document.addEventListener("click", async event => {
    const editBtn = event.target.closest("[data-edit-activity]");
    if (editBtn) {
        const id = editBtn.dataset.editActivity;
        const item = activities.find(row => String(row.id) === String(id));
        if (item) openDrawer(item);
        return;
    }

    const completeBtn = event.target.closest("[data-complete-activity]");
    if (completeBtn) {
        const id = completeBtn.dataset.completeActivity;
        const item = activities.find(row => String(row.id) === String(id));
        if (item) {
            const newStatus = item.status === "done" ? "OPEN" : "COMPLETED";
            try {
                if (typeof item.id === "number") {
                    await api(`/api/activities/${item.id}`, {
                        method: "PUT",
                        body: JSON.stringify({
                            subject: item.title,
                            type: item.type,
                            description: item.description,
                            status: newStatus,
                            dueDate: item.date ? `${item.date} ${item.time || "09:00"}:00` : null
                        })
                    });
                }
                item.status = newStatus === "COMPLETED" ? "done" : "open";
                saveActivitiesCache();
                render();
            } catch (err) {
                alert("Lỗi khi cập nhật trạng thái hoạt động: " + err.message);
            }
        }
        return;
    }

    const deleteBtn = event.target.closest("[data-delete-activity]");
    if (deleteBtn) {
        const id = deleteBtn.dataset.deleteActivity;
        if (confirm("Bạn có chắc chắn muốn xóa hoạt động này không?")) {
            try {
                if (typeof Number(id) === "number" && !isNaN(Number(id))) {
                    await api(`/api/activities/${id}`, { method: "DELETE" });
                }
                activities = activities.filter(a => String(a.id) !== String(id));
                saveActivitiesCache();
                render();
            } catch (err) {
                alert("Lỗi khi xóa hoạt động: " + err.message);
            }
        }
    }
});

/* =========================================================
   SAVE / EDIT ACTIVITY
========================================================= */
async function saveActivity() {
    const title = value("activityTitle");
    const error = document.getElementById("activityError");
    if (error) error.textContent = "";

    if (!title) {
        if (error) error.textContent = "Vui lòng nhập tiêu đề hoạt động.";
        return;
    }

    const editingId = value("editingActivityId");
    const type = value("activityType") || "task";
    const relation = value("activityRelation");
    const date = value("activityDate");
    const time = value("activityTime");
    const owner = value("activityOwner");
    const description = value("activityDescription");
    const reminder = document.getElementById("activityReminder")?.checked;

    const dueDateStr = date ? `${date} ${time || "09:00"}:00` : null;

    try {
        if (editingId && !isNaN(Number(editingId))) {
            const updated = await api(`/api/activities/${editingId}`, {
                method: "PUT",
                body: JSON.stringify({
                    subject: title,
                    type: type.toUpperCase(),
                    description: description,
                    status: "OPEN",
                    dueDate: dueDateStr
                })
            });
            await loadActivities();
        } else {
            const created = await api("/api/activities", {
                method: "POST",
                body: JSON.stringify({
                    subject: title,
                    type: type.toUpperCase(),
                    description: description,
                    status: "OPEN",
                    dueDate: dueDateStr
                })
            });
            await loadActivities();
        }

        closeDrawer();
    } catch (err) {
        if (error) error.textContent = "Lỗi: " + err.message;
        else alert("Lỗi khi lưu hoạt động: " + err.message);
    }
}

/* =========================================================
   RENDER
========================================================= */
function render() {
    const query = value("activitySearch").toLowerCase();
    const type = value("activityTypeFilter");
    const status = value("activityStatusFilter");

    const filtered = activities.filter(item => {
        const text = `${item.title} ${item.relation} ${item.description}`.toLowerCase();
        const searchOk = !query || text.includes(query);
        const typeOk = !type || item.type === type;
        const statusOk = !status || item.status === status;
        return searchOk && typeOk && statusOk;
    });

    const list = document.getElementById("activityList");
    if (!list) return;
    list.innerHTML = "";

    const empty = document.getElementById("activityEmpty");
    if (empty) empty.style.display = filtered.length ? "none" : "flex";

    filtered.forEach(item => {
        const article = document.createElement("article");
        article.className = "activity-row";
        article.innerHTML = `
            <div class="activity-icon ${item.type}">
                ${typeIcon(item.type)}
            </div>

            <div class="activity-main">
                <strong>${escapeHtml(item.title)}</strong>
                <p>${escapeHtml(item.description || "Không có ghi chú")}</p>
            </div>

            <div class="activity-relation">
                ${escapeHtml(item.relation || "Không liên kết")}
            </div>

            <div class="activity-date">
                ${formatSchedule(item)}
            </div>

            <div>
                <span class="activity-status ${statusClass(item)}">
                    ${statusLabel(item)}
                </span>

                <div class="activity-actions">
                    <button type="button" data-edit-activity="${item.id}">Sửa</button>
                    <button type="button" data-complete-activity="${item.id}">
                        ${item.status === "done" ? "Mở lại" : "Hoàn thành"}
                    </button>
                    <button type="button" style="color:#ef4444;" data-delete-activity="${item.id}">Xóa</button>
                </div>
            </div>
        `;
        list.appendChild(article);
    });

    updateSummary();
}

function updateSummary() {
    const now = todayString();
    const open = activities.filter(item => item.status === "open");
    const done = activities.filter(item => item.status === "done");
    const late = activities.filter(item => item.status === "open" && item.date && item.date < now);
    const today = activities.filter(item => item.date === now);

    text("summaryTotal", activities.length);
    text("summaryPending", open.length);
    text("summaryDone", done.length);
    text("summaryLate", late.length);
}

/* =========================================================
   DRAWER
========================================================= */
function openDrawer(item = null) {
    const err = document.getElementById("activityError");
    if (err) err.textContent = "";

    if (item) {
        text("activityDrawerTitle", "Sửa hoạt động");
        setValue("editingActivityId", item.id);
        setValue("activityType", item.type);
        setValue("activityTitle", item.title);
        setValue("activityRelation", item.relation);
        setValue("activityDate", item.date);
        setValue("activityTime", item.time);
        setValue("activityOwner", item.owner);
        setValue("activityDescription", item.description);
        const rem = document.getElementById("activityReminder");
        if (rem) rem.checked = !!item.reminder;
    } else {
        text("activityDrawerTitle", "Tạo hoạt động");
        activityForm?.reset();
        setValue("editingActivityId", "");
        setValue("activityType", "task");
        setValue("activityDate", todayString());
        setValue("activityTime", "09:00");
    }

    drawer?.classList.add("open");
    overlay?.classList.add("open");
}

function closeDrawer() {
    drawer?.classList.remove("open");
    overlay?.classList.remove("open");
}

/* =========================================================
   UI HELPERS
========================================================= */
function typeIcon(type) {
    switch (type) {
        case "call":
            return `<svg class="crm-inline-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>`;
        case "email":
            return `<svg class="crm-inline-icon" viewBox="0 0 24 24" aria-hidden="true"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>`;
        case "meeting":
            return `<svg class="crm-inline-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`;
        case "note":
            return `<svg class="crm-inline-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>`;
        default:
            return `<svg class="crm-inline-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>`;
    }
}

function statusClass(item) {
    if (item.status === "done") return "done";
    if (item.date && item.date < todayString()) return "late";
    return "open";
}

function statusLabel(item) {
    if (item.status === "done") return "Hoàn thành";
    if (item.date && item.date < todayString()) return "Quá hạn";
    return "Đang chờ";
}

function formatSchedule(item) {
    return (item.date || "Chưa đặt ngày") + (item.time ? " · " + item.time : "");
}

function todayString() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function value(id) {
    return (document.getElementById(id)?.value || "").trim();
}

function setValue(id, val) {
    const el = document.getElementById(id);
    if (el) el.value = val ?? "";
}

function text(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

function escapeHtml(val) {
    return String(val ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Start
loadActivities();