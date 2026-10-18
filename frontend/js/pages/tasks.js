/**
 * ===================================================================
 * TASKS & TO-DO MANAGEMENT - FRONTEND CONTROLLER
 * Corporate CRM System
 * ===================================================================
 */

"use strict";

const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

let activities = [];
let quickFilter = "all";

const modal = document.getElementById("taskModal");
const overlay = document.getElementById("taskOverlay");

// INITIALIZATION
document.addEventListener("DOMContentLoaded", () => {
    initEvents();
    loadTasks();
});

function initEvents() {
    // Open & Close Modal
    document.getElementById("newTask")?.addEventListener("click", () => openModal());
    document.getElementById("closeTaskModal")?.addEventListener("click", closeModal);
    document.getElementById("cancelTask")?.addEventListener("click", closeModal);
    overlay?.addEventListener("click", closeModal);

    // Escape Key to close modal
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && modal?.classList.contains("open")) {
            closeModal();
        }
    });

    // Refresh Button
    document.getElementById("btnRefreshTasks")?.addEventListener("click", async () => {
        await loadTasks();
        showToast("Đã làm mới danh sách công việc!", "info");
    });

    // Search and Filters
    const searchInput = document.getElementById("taskSearch");
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

    document.getElementById("taskState")?.addEventListener("change", () => {
        quickFilter = "custom";
        document.querySelectorAll(".quick-tab-btn").forEach(b => b.classList.remove("active"));
        render();
    });

    document.getElementById("taskPriorityFilter")?.addEventListener("change", () => {
        quickFilter = "custom";
        document.querySelectorAll(".quick-tab-btn").forEach(b => b.classList.remove("active"));
        render();
    });

    // Quick Filter Tabs
    document.querySelectorAll(".quick-tab-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".quick-tab-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            quickFilter = btn.dataset.quickFilter || "all";

            // Reset dropdowns to match
            const stateSelect = document.getElementById("taskState");
            const prioritySelect = document.getElementById("taskPriorityFilter");

            if (quickFilter === "open" && stateSelect) stateSelect.value = "open";
            else if (quickFilter === "done" && stateSelect) stateSelect.value = "done";
            else if (quickFilter === "late" && stateSelect) stateSelect.value = "late";
            else if (stateSelect) stateSelect.value = "";

            if (quickFilter === "high" && prioritySelect) prioritySelect.value = "high";
            else if (prioritySelect && quickFilter !== "custom") prioritySelect.value = "";

            render();
        });
    });

    // Priority Tiles Selector inside Modal
    document.querySelectorAll(".priority-tile").forEach(tile => {
        tile.addEventListener("click", () => {
            document.querySelectorAll(".priority-tile").forEach(t => t.classList.remove("active"));
            tile.classList.add("active");
            const p = tile.dataset.priority || "medium";
            const hiddenInput = document.getElementById("taskPriority");
            if (hiddenInput) hiddenInput.value = p;
        });
    });

    // Form Submit
    document.getElementById("taskForm")?.addEventListener("submit", event => {
        event.preventDefault();
        saveTask();
    });

    // Delegated actions (Toggle done, Edit, Delete)
    document.addEventListener("click", async event => {
        // Toggle Task
        const toggleBtn = event.target.closest("[data-toggle-task]");
        if (toggleBtn) {
            const taskId = toggleBtn.dataset.toggleTask;
            const task = activities.find(item => String(item.id) === String(taskId));
            if (task) {
                const newStatus = (task.status === "done") ? "open" : "done";
                task.status = newStatus;
                saveActivities();
                render();

                showToast(
                    newStatus === "done"
                        ? `Đã hoàn thành công việc "${task.title}"!`
                        : `Đã mở lại công việc "${task.title}"!`,
                    "success"
                );

                if (!String(task.id).startsWith("act_")) {
                    fetch(`${API_BASE}/api/activities/${task.id}`, {
                        method: "PUT",
                        headers: { "Content-Type": "application/json" },
                        credentials: "include",
                        body: JSON.stringify({
                            subject: task.title,
                            type: "TASK",
                            description: task.description || "",
                            status: newStatus === "done" ? "COMPLETED" : "OPEN",
                            dueDate: task.date ? (task.date + " 00:00:00") : null
                        })
                    }).catch(() => {});
                }
            }
            return;
        }

        // Edit Task
        const editBtn = event.target.closest("[data-edit-task]");
        if (editBtn) {
            openModal(editBtn.dataset.editTask);
            return;
        }

        // Delete Task
        const deleteBtn = event.target.closest("[data-delete-task]");
        if (deleteBtn) {
            const id = deleteBtn.dataset.deleteTask;
            const task = activities.find(item => String(item.id) === String(id));
            const title = task ? task.title : "công việc này";

            if (confirm(`Bạn có chắc chắn muốn xóa "${title}" không?`)) {
                activities = activities.filter(item => String(item.id) !== String(id));
                saveActivities();
                render();
                showToast(`Đã xóa "${title}"!`, "info");

                if (!String(id).startsWith("act_")) {
                    fetch(`${API_BASE}/api/activities/${id}`, {
                        method: "DELETE",
                        credentials: "include"
                    }).catch(() => {});
                }
            }
        }
    });
}

// LOAD TASKS (FROM LOCALSTORAGE & BACKEND)
async function loadTasks() {
    activities = getCachedActivities();
    render();

    try {
        const res = await fetch(`${API_BASE}/api/activities`, { credentials: "include" });
        const json = await res.json();
        if (json?.success && Array.isArray(json.data)) {
            activities = json.data.map(item => {
                let d = "";
                let t = "";
                if (item.dueDate) {
                    const dt = new Date(item.dueDate);
                    if (!isNaN(dt.getTime())) {
                        d = dt.toISOString().split("T")[0];
                        t = dt.toTimeString().slice(0, 5);
                    }
                }
                return {
                    id: item.id,
                    type: (item.type || "task").toLowerCase(),
                    title: item.subject || item.title || "Công việc",
                    relation: item.customerName || item.opportunityName || item.relation || "",
                    date: d,
                    time: t,
                    owner: item.ownerName || "Tôi",
                    description: item.description || "",
                    priority: (item.priority || "medium").toLowerCase(),
                    status: (item.status || "OPEN").toUpperCase() === "COMPLETED" ? "done" : "open"
                };
            });
            saveActivities();
            render();
        }
    } catch (_) {
        // Fallback to cache already rendered
    }
}

function getCachedActivities() {
    try {
        return JSON.parse(localStorage.getItem("crm_ui_activities")) || [];
    } catch {
        return [];
    }
}

function saveActivities() {
    localStorage.setItem("crm_ui_activities", JSON.stringify(activities));
}

// SAVE / EDIT TASK
function saveTask() {
    const title = value("taskTitle");
    const error = document.getElementById("taskError");
    if (error) {
        error.textContent = "";
        error.style.display = "none";
    }

    if (!title) {
        if (error) {
            error.textContent = "Vui lòng nhập tiêu đề công việc.";
            error.style.display = "flex";
        }
        return;
    }

    const editingId = value("editingTaskId");
    const old = activities.find(item => String(item.id) === String(editingId));

    const record = {
        id: editingId || ("act_" + Date.now()),
        type: "task",
        title,
        relation: value("taskRelation"),
        date: value("taskDate"),
        time: "09:00",
        owner: value("taskOwner") || "Tôi",
        description: value("taskDescription"),
        priority: value("taskPriority") || "medium",
        reminder: true,
        status: old ? old.status : "open",
        createdAt: old ? old.createdAt : new Date().toISOString()
    };

    if (editingId) {
        const index = activities.findIndex(item => String(item.id) === String(editingId));
        if (index >= 0) {
            activities[index] = { ...activities[index], ...record };
        }
        showToast(`Cập nhật công việc "${title}" thành công!`, "success");
    } else {
        activities.unshift(record);
        showToast(`Tạo công việc "${title}" thành công!`, "success");
    }

    saveActivities();
    closeModal();
    render();

    // Async sync to backend
    (async () => {
        try {
            const dueDate = record.date ? (record.date + " 09:00:00") : null;
            if (editingId && !String(editingId).startsWith("act_")) {
                await fetch(`${API_BASE}/api/activities/${editingId}`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({
                        subject: record.title,
                        type: "TASK",
                        description: record.description || "",
                        status: record.status === "done" ? "COMPLETED" : "OPEN",
                        dueDate: dueDate
                    })
                });
            } else {
                const res = await fetch(`${API_BASE}/api/activities`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    credentials: "include",
                    body: JSON.stringify({
                        subject: record.title,
                        type: "TASK",
                        description: record.description || "",
                        status: record.status === "done" ? "COMPLETED" : "OPEN",
                        dueDate: dueDate
                    })
                });
                const json = await res.json();
                if (json?.success && json.data?.id) {
                    record.id = json.data.id;
                    saveActivities();
                    render();
                }
            }
        } catch (_) {}
    })();
}

// RENDER TASKS & UPDATE KPIS
function render() {
    const query = value("taskSearch").toLowerCase();
    const state = value("taskState");
    const priority = value("taskPriorityFilter");
    const now = todayString();

    const tasksOnly = activities.filter(item => (item.type || "task").toLowerCase() === "task");

    const filtered = tasksOnly.filter(item => {
        const text = `${item.title} ${item.relation} ${item.owner} ${item.description}`.toLowerCase();
        const searchOk = !query || text.includes(query);

        let stateMatch = true;
        if (state === "open") {
            stateMatch = item.status === "open" && (!item.date || item.date >= now);
        } else if (state === "done") {
            stateMatch = item.status === "done";
        } else if (state === "late") {
            stateMatch = isLate(item);
        }

        let priorityMatch = true;
        if (priority) {
            priorityMatch = (item.priority || "medium") === priority;
        }

        // Quick tabs override
        if (quickFilter === "open") {
            if (item.status !== "open" || isLate(item)) return false;
        } else if (quickFilter === "done") {
            if (item.status !== "done") return false;
        } else if (quickFilter === "late") {
            if (!isLate(item)) return false;
        } else if (quickFilter === "high") {
            if ((item.priority || "medium") !== "high") return false;
        }

        return searchOk && stateMatch && priorityMatch;
    });

    const list = document.getElementById("tasksList");
    const empty = document.getElementById("tasksEmpty");
    if (!list) return;

    list.innerHTML = "";
    if (empty) {
        empty.style.display = (filtered.length === 0) ? "flex" : "none";
    }

    filtered.forEach(task => {
        const priorityVal = task.priority || "medium";
        const isDone = (task.status === "done");
        const late = isLate(task);

        const row = document.createElement("article");
        row.className = `task-row ${isDone ? "done" : ""}`;

        row.innerHTML = `
            <button
                class="task-check"
                type="button"
                data-toggle-task="${task.id}"
                title="${isDone ? 'Bấm để mở lại công việc' : 'Bấm để đánh dấu hoàn thành'}"
            >
                <i class="fa-solid fa-check"></i>
            </button>

            <div class="task-main">
                <strong>${escapeHtml(task.title)}</strong>
                <p>${escapeHtml(task.description || "Chưa có mô tả chi tiết.")}</p>
            </div>

            <div>
                <span class="task-relation" title="Khách hàng / Liên kết">
                    <i class="fa-solid fa-building"></i> ${escapeHtml(task.relation || "Không liên kết")}
                </span>
            </div>

            <div>
                <span class="task-date ${late ? 'late' : ''}">
                    <i class="fa-regular fa-calendar"></i> ${formatDueDate(task.date)}
                </span>
            </div>

            <div>
                <span class="priority ${priorityVal}">
                    ${priorityIcon(priorityVal)} ${priorityLabel(priorityVal)}
                </span>
            </div>

            <div>
                <span class="task-owner" title="Người phụ trách">
                    <i class="fa-solid fa-user"></i> ${escapeHtml(task.owner || "Tôi")}
                </span>
            </div>

            <div class="task-actions">
                <button
                    type="button"
                    class="btn-item-action"
                    data-edit-task="${task.id}"
                    title="Chỉnh sửa công việc"
                >
                    <i class="fa-solid fa-pen-to-square"></i> Sửa
                </button>
                <button
                    type="button"
                    class="btn-item-action delete"
                    data-delete-task="${task.id}"
                    title="Xóa công việc"
                >
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            </div>
        `;
        list.appendChild(row);
    });

    updateSummary(tasksOnly);
}

function updateSummary(tasks) {
    const now = todayString();
    const open = tasks.filter(item => item.status === "open" && (!item.date || item.date >= now));
    const done = tasks.filter(item => item.status === "done");
    const late = tasks.filter(item => isLate(item));

    text("kpiTotalTasks", tasks.length);
    text("kpiOpenTasks", open.length);
    text("kpiDoneTasks", done.length);
    text("kpiLateTasks", late.length);
}

// FLOATING MODAL WINDOW
function openModal(id = null) {
    document.getElementById("taskForm")?.reset();
    setValue("editingTaskId", "");
    setValue("taskPriority", "medium");

    const error = document.getElementById("taskError");
    if (error) {
        error.textContent = "";
        error.style.display = "none";
    }

    if (id) {
        const task = activities.find(item => String(item.id) === String(id));
        if (task) {
            setValue("editingTaskId", task.id);
            setValue("taskTitle", task.title);
            setValue("taskRelation", task.relation);
            setValue("taskDate", task.date);
            setValue("taskOwner", task.owner);
            setValue("taskDescription", task.description);
            const p = task.priority || "medium";
            setValue("taskPriority", p);

            document.querySelectorAll(".priority-tile").forEach(tile => {
                tile.classList.toggle("active", tile.dataset.priority === p);
            });

            text("taskModalTitle", "Chỉnh sửa công việc");
        }
    } else {
        text("taskModalTitle", "Tạo công việc mới");
        setValue("taskDate", todayString());
        setValue("taskOwner", "Nông Quang Tiệp (Tôi)");

        document.querySelectorAll(".priority-tile").forEach(tile => {
            tile.classList.toggle("active", tile.dataset.priority === "medium");
        });
    }

    modal?.classList.add("open");
    overlay?.classList.add("open");
    document.getElementById("taskTitle")?.focus();
}

function closeModal() {
    modal?.classList.remove("open");
    overlay?.classList.remove("open");
}

// FLOATING TOAST
let toastTimer = null;
function showToast(message, type = "success") {
    const toast = document.getElementById("taskToast");
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

// HELPERS
function isLate(task) {
    return (
        task.status !== "done" &&
        task.date &&
        task.date < todayString()
    );
}

function priorityLabel(value) {
    switch (value) {
        case "high": return "Ưu tiên Cao";
        case "low": return "Thấp";
        default: return "Trung bình";
    }
}

function priorityIcon(value) {
    switch (value) {
        case "high": return '<i class="fa-solid fa-fire"></i>';
        case "low": return '<i class="fa-solid fa-leaf"></i>';
        default: return '<i class="fa-solid fa-bolt"></i>';
    }
}

function formatDueDate(dateStr) {
    if (!dateStr) return "Chưa có hạn";
    const parts = dateStr.split("-");
    return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : dateStr;
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