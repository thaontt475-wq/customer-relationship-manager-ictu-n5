/**
 * ===================================================================
 * ACTIVITIES & INTERACTIONS - FRONTEND CONTROLLER
 * Corporate CRM System
 * ===================================================================
 */

"use strict";

const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

let activities = [];
let quickTypeFilter = "";

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
async function loadActivities(isSilent = false) {
    try {
        const data = await api("/api/activities");
        if (Array.isArray(data)) {
            activities = data.map(item => normalizeActivityFromBackend(item));
            saveActivitiesCache();
            render();
            if (!isSilent) {
                // Initial load
            }
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
        reminder: Boolean(item.reminder),
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
document.addEventListener("DOMContentLoaded", () => {
    initEvents();
    loadActivities();
});

function initEvents() {
    // Open & Close Modal
    document.getElementById("createActivity")?.addEventListener("click", () => openDrawer());
    document.getElementById("closeActivityDrawer")?.addEventListener("click", closeDrawer);
    document.getElementById("cancelActivity")?.addEventListener("click", closeDrawer);
    overlay?.addEventListener("click", closeDrawer);

    // Escape key to close modal
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && drawer?.classList.contains("open")) {
            closeDrawer();
        }
    });

    // Refresh Button
    document.getElementById("btnRefreshActivities")?.addEventListener("click", async () => {
        await loadActivities();
        showToast("Đã làm mới danh sách hoạt động!", "info");
    });

    // Search and Filters
    const searchInput = document.getElementById("activitySearch");
    const clearSearchBtn = document.getElementById("clearSearchBtn");

    searchInput?.addEventListener("input", event => {
        const query = event.target.value.trim();
        if (clearSearchBtn) {
            clearSearchBtn.style.display = query ? "block" : "none";
        }
        render();
    });

    clearSearchBtn?.addEventListener("click", () => {
        if (searchInput) {
            searchInput.value = "";
            clearSearchBtn.style.display = "none";
            render();
            searchInput.focus();
        }
    });

    document.getElementById("activityTypeFilter")?.addEventListener("change", event => {
        quickTypeFilter = event.target.value;
        // Sync quick tabs
        document.querySelectorAll(".quick-tab-btn").forEach(btn => {
            btn.classList.toggle("active", btn.dataset.quickType === quickTypeFilter);
        });
        render();
    });

    document.getElementById("activityStatusFilter")?.addEventListener("change", render);

    // Quick Type Tabs
    document.querySelectorAll(".quick-tab-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".quick-tab-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            quickTypeFilter = btn.dataset.quickType || "";
            const typeFilterSelect = document.getElementById("activityTypeFilter");
            if (typeFilterSelect) {
                typeFilterSelect.value = quickTypeFilter;
            }
            render();
        });
    });

    // Visual Type Selector Tiles in Modal
    document.querySelectorAll(".type-tile").forEach(tile => {
        tile.addEventListener("click", () => {
            document.querySelectorAll(".type-tile").forEach(t => t.classList.remove("active"));
            tile.classList.add("active");
            const typeVal = tile.dataset.type || "task";
            const hiddenType = document.getElementById("activityType");
            if (hiddenType) hiddenType.value = typeVal;
        });
    });

    // Form Submit
    activityForm?.addEventListener("submit", event => {
        event.preventDefault();
        saveActivity();
    });

    // Table delegated actions
    document.addEventListener("click", async event => {
        // Edit button
        const editBtn = event.target.closest("[data-edit-activity]");
        if (editBtn) {
            const id = editBtn.dataset.editActivity;
            const item = activities.find(row => String(row.id) === String(id));
            if (item) openDrawer(item);
            return;
        }

        // Toggle / Complete button
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
                                type: item.type.toUpperCase(),
                                description: item.description,
                                status: newStatus,
                                dueDate: item.date ? `${item.date} ${item.time || "09:00"}:00` : null
                            })
                        });
                    }
                    item.status = (newStatus === "COMPLETED") ? "done" : "open";
                    saveActivitiesCache();
                    render();
                    showToast(
                        newStatus === "COMPLETED"
                            ? `Đã hoàn thành: "${item.title}"!`
                            : `Đã mở lại hoạt động: "${item.title}"!`,
                        "success"
                    );
                } catch (err) {
                    showToast("Lỗi khi cập nhật trạng thái: " + err.message, "error");
                }
            }
            return;
        }

        // Delete button
        const deleteBtn = event.target.closest("[data-delete-activity]");
        if (deleteBtn) {
            const id = deleteBtn.dataset.deleteActivity;
            const item = activities.find(row => String(row.id) === String(id));
            const title = item ? item.title : "hoạt động này";

            if (confirm(`Bạn có chắc chắn muốn xóa "${title}" không?`)) {
                try {
                    if (typeof Number(id) === "number" && !isNaN(Number(id))) {
                        await api(`/api/activities/${id}`, { method: "DELETE" });
                    }
                    activities = activities.filter(a => String(a.id) !== String(id));
                    saveActivitiesCache();
                    render();
                    showToast(`Đã xóa "${title}" thành công!`, "info");
                } catch (err) {
                    showToast("Lỗi khi xóa hoạt động: " + err.message, "error");
                }
            }
        }
    });
}

/* =========================================================
   SAVE / EDIT ACTIVITY
========================================================= */
async function saveActivity() {
    const title = value("activityTitle");
    const error = document.getElementById("activityError");
    if (error) {
        error.textContent = "";
        error.style.display = "none";
    }

    if (!title) {
        if (error) {
            error.textContent = "Vui lòng nhập tiêu đề hoạt động.";
            error.style.display = "flex";
        }
        return;
    }

    const editingId = value("editingActivityId");
    const type = value("activityType") || "task";
    const relation = value("activityRelation");
    const date = value("activityDate");
    const time = value("activityTime");
    const owner = value("activityOwner");
    const description = value("activityDescription");
    const reminder = Boolean(document.getElementById("activityReminder")?.checked);

    const dueDateStr = date ? `${date} ${time || "09:00"}:00` : null;

    try {
        if (editingId && !isNaN(Number(editingId))) {
            await api(`/api/activities/${editingId}`, {
                method: "PUT",
                body: JSON.stringify({
                    subject: title,
                    type: type.toUpperCase(),
                    description: description,
                    status: "OPEN",
                    dueDate: dueDateStr
                })
            });
            showToast(`Cập nhật hoạt động "${title}" thành công!`, "success");
        } else {
            await api("/api/activities", {
                method: "POST",
                body: JSON.stringify({
                    subject: title,
                    type: type.toUpperCase(),
                    description: description,
                    status: "OPEN",
                    dueDate: dueDateStr
                })
            });
            showToast(`Ghi nhận hoạt động "${title}" thành công!`, "success");
        }

        closeDrawer();
        await loadActivities(true);
    } catch (err) {
        console.error("Save activity error:", err);
        if (error) {
            error.textContent = "Lỗi: " + err.message;
            error.style.display = "flex";
        }
    }
}

/* =========================================================
   RENDER
========================================================= */
function render() {
    const query = value("activitySearch").toLowerCase();
    const typeFilter = quickTypeFilter || value("activityTypeFilter");
    const status = value("activityStatusFilter");
    const now = todayString();

    const filtered = activities.filter(item => {
        const text = `${item.title} ${item.relation} ${item.description} ${item.owner}`.toLowerCase();
        const searchOk = !query || text.includes(query);
        const typeOk = !typeFilter || item.type === typeFilter;

        let statusOk = true;
        if (status === "open") {
            statusOk = item.status === "open" && (!item.date || item.date >= now);
        } else if (status === "done") {
            statusOk = item.status === "done";
        } else if (status === "late") {
            statusOk = item.status === "open" && item.date && item.date < now;
        }

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

        const stClass = statusClass(item);
        const stLabel = statusLabel(item);

        article.innerHTML = `
            <div class="activity-icon ${item.type}">
                ${typeIcon(item.type)}
            </div>

            <div class="activity-main">
                <strong>${escapeHtml(item.title)}</strong>
                <p>${escapeHtml(item.description || "Chưa có nội dung ghi chú chi tiết.")}</p>
            </div>

            <div class="activity-relation">
                <span class="relation-chip" title="Khách hàng / Liên kết">
                    <i class="fa-solid fa-building"></i> ${escapeHtml(item.relation || "Không liên kết")}
                </span>
                <span class="owner-chip" title="Người phụ trách">
                    <i class="fa-solid fa-user"></i> ${escapeHtml(item.owner || "Tôi")}
                </span>
            </div>

            <div class="activity-date">
                <span class="date-text">
                    <i class="fa-regular fa-calendar"></i> ${formatSchedule(item)}
                </span>
                <span class="time-text">
                    ${item.reminder ? '<i class="fa-solid fa-bell" style="color:#fbbf24;"></i> Nhắc việc bật' : ''}
                </span>
            </div>

            <div>
                <span class="activity-status ${stClass}">
                    ${stLabel}
                </span>
            </div>

            <div class="activity-actions">
                <button
                    type="button"
                    class="btn-item-action complete"
                    data-complete-activity="${item.id}"
                    title="${item.status === 'done' ? 'Mở lại hoạt động này' : 'Đánh dấu hoàn thành'}"
                >
                    <i class="fa-solid ${item.status === 'done' ? 'fa-rotate-left' : 'fa-check'}"></i>
                    <span>${item.status === "done" ? "Mở lại" : "Xong"}</span>
                </button>

                <button
                    type="button"
                    class="btn-item-action"
                    data-edit-activity="${item.id}"
                    title="Chỉnh sửa thông tin"
                >
                    <i class="fa-solid fa-pen-to-square"></i>
                </button>

                <button
                    type="button"
                    class="btn-item-action delete"
                    data-delete-activity="${item.id}"
                    title="Xóa hoạt động này"
                >
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            </div>
        `;
        list.appendChild(article);
    });

    updateSummary();
}

function updateSummary() {
    const now = todayString();
    const open = activities.filter(item => item.status === "open" && (!item.date || item.date >= now));
    const done = activities.filter(item => item.status === "done");
    const late = activities.filter(item => item.status === "open" && item.date && item.date < now);

    // Synchronize exactly with activities.html IDs
    text("kpiTotal", activities.length);
    text("kpiOpen", open.length);
    text("kpiDone", done.length);
    text("kpiLate", late.length);
}

/* =========================================================
   FLOATING MODAL MANAGEMENT
========================================================= */
function openDrawer(item = null) {
    const err = document.getElementById("activityError");
    if (err) {
        err.textContent = "";
        err.style.display = "none";
    }

    if (item) {
        text("activityDrawerTitle", "Chỉnh sửa hoạt động");
        setValue("editingActivityId", item.id);
        setValue("activityType", item.type);
        setValue("activityTitle", item.title);
        setValue("activityRelation", item.relation);
        setValue("activityDate", item.date);
        setValue("activityTime", item.time || "09:00");
        setValue("activityOwner", item.owner);
        setValue("activityDescription", item.description);

        const rem = document.getElementById("activityReminder");
        if (rem) rem.checked = Boolean(item.reminder);

        // Activate corresponding visual tile
        document.querySelectorAll(".type-tile").forEach(t => {
            t.classList.toggle("active", t.dataset.type === item.type);
        });
    } else {
        text("activityDrawerTitle", "Tạo hoạt động mới");
        activityForm?.reset();
        setValue("editingActivityId", "");
        setValue("activityType", "task");
        setValue("activityDate", todayString());
        setValue("activityTime", "09:00");
        setValue("activityOwner", "Nông Quang Tiệp (Tôi)");

        const rem = document.getElementById("activityReminder");
        if (rem) rem.checked = false;

        document.querySelectorAll(".type-tile").forEach(t => {
            t.classList.toggle("active", t.dataset.type === "task");
        });
    }

    drawer?.classList.add("open");
    overlay?.classList.add("open");
    document.getElementById("activityTitle")?.focus();
}

function closeDrawer() {
    drawer?.classList.remove("open");
    overlay?.classList.remove("open");
}

/* =========================================================
   FLOATING TOAST
========================================================= */
let toastTimer = null;
function showToast(message, type = "success") {
    const toast = document.getElementById("activityToast");
    const msgEl = document.getElementById("toastMessage");
    if (!toast || !msgEl) return;

    msgEl.textContent = message;
    const icon = toast.querySelector("i");
    if (icon) {
        if (type === "error") {
            icon.className = "fa-solid fa-circle-exclamation";
            icon.style.color = "#f87171";
        } else if (type === "info") {
            icon.className = "fa-solid fa-circle-info";
            icon.style.color = "#38bdf8";
        } else {
            icon.className = "fa-solid fa-circle-check";
            icon.style.color = "#34d399";
        }
    }

    toast.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        toast.classList.remove("show");
    }, 3200);
}

/* =========================================================
   UI HELPERS
========================================================= */
function typeIcon(type) {
    switch (type) {
        case "call":
            return '<i class="fa-solid fa-phone"></i>';
        case "email":
            return '<i class="fa-solid fa-envelope"></i>';
        case "meeting":
            return '<i class="fa-solid fa-calendar-days"></i>';
        case "note":
            return '<i class="fa-solid fa-file-signature"></i>';
        default:
            return '<i class="fa-solid fa-circle-check"></i>';
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
    if (!item.date) return "Chưa đặt ngày";
    const parts = item.date.split("-");
    const dateFormatted = parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : item.date;
    return dateFormatted + (item.time ? " · " + item.time : "");
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