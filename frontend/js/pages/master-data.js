/**
 * ===================================================================
 * MASTER DATA MANAGEMENT - FRONTEND CONTROLLER
 * Corporate CRM System
 * ===================================================================
 */

"use strict";

const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

let currentCategory = "industry";
let values = [];
let searchQuery = "";
let statusFilter = "ALL";

const categoryMeta = {
    "industry": {
        label: "Ngành nghề",
        icon: "fa-industry",
        desc: "Quản lý các giá trị phân loại ngành nghề phục vụ quản lý hồ sơ khách hàng và cơ hội bán hàng."
    },
    "lead-source": {
        label: "Nguồn Lead",
        icon: "fa-bullseye",
        desc: "Quản lý các kênh tiếp cận và nguồn gốc phát sinh khách hàng tiềm năng trong CRM."
    },
    "activity-type": {
        label: "Loại hoạt động",
        icon: "fa-calendar-check",
        desc: "Quản lý các loại tương tác như cuộc gọi, họp, email và tác vụ chăm sóc khách hàng."
    },
    "company-size": {
        label: "Quy mô doanh nghiệp",
        icon: "fa-users",
        desc: "Quản lý các khung quy mô nhân sự của đối tác và khách hàng doanh nghiệp."
    },
    "region": {
        label: "Khu vực địa lý",
        icon: "fa-map-location-dot",
        desc: "Quản lý các vùng miền, khu vực địa lý phục vụ chia địa bàn và quản lý đội ngũ kinh doanh."
    }
};

const modal = document.getElementById("masterDrawer");
const overlay = document.getElementById("masterOverlay");

// INITIALIZATION
document.addEventListener("DOMContentLoaded", () => {
    initEvents();
    loadValues();
    fetchAllCategoryCounts();
});

function initEvents() {
    // Category Switcher
    document.querySelectorAll(".category-button").forEach(button => {
        button.addEventListener("click", async () => {
            const cat = button.dataset.category;
            if (cat === currentCategory) return;

            document.querySelectorAll(".category-button").forEach(b => b.classList.remove("active"));
            button.classList.add("active");

            currentCategory = cat;
            searchQuery = "";
            statusFilter = "ALL";

            const searchInput = document.getElementById("masterSearchInput");
            if (searchInput) searchInput.value = "";
            const clearBtn = document.getElementById("clearSearchBtn");
            if (clearBtn) clearBtn.style.display = "none";
            const filterSelect = document.getElementById("masterStatusFilter");
            if (filterSelect) filterSelect.value = "ALL";

            updateCategoryHeader();
            await loadValues();
        });
    });

    // Add Item Buttons
    document.getElementById("addMasterItem")?.addEventListener("click", () => openModal());
    document.getElementById("addMasterItemTable")?.addEventListener("click", () => openModal());

    // Refresh Button
    document.getElementById("btnRefreshMaster")?.addEventListener("click", () => {
        loadValues();
        fetchAllCategoryCounts();
        showToast("Đã làm mới dữ liệu danh mục!", "info");
    });

    // Close Modal
    document.getElementById("closeMasterDrawer")?.addEventListener("click", closeModal);
    document.getElementById("cancelMaster")?.addEventListener("click", closeModal);
    overlay?.addEventListener("click", closeModal);

    // Escape Key to close modal
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && modal?.classList.contains("open")) {
            closeModal();
        }
    });

    // Form submit
    document.getElementById("masterForm")?.addEventListener("submit", async event => {
        event.preventDefault();
        await saveForm();
    });

    // Code auto uppercase
    const codeInput = document.getElementById("masterCode");
    if (codeInput) {
        codeInput.addEventListener("input", () => {
            codeInput.value = codeInput.value.toUpperCase().replace(/\s+/g, "_");
        });
    }

    // Search input
    const searchInput = document.getElementById("masterSearchInput");
    const clearSearchBtn = document.getElementById("clearSearchBtn");
    if (searchInput) {
        searchInput.addEventListener("input", event => {
            searchQuery = event.target.value.trim().toLowerCase();
            if (clearSearchBtn) {
                clearSearchBtn.style.display = searchQuery ? "block" : "none";
            }
            render();
        });
    }

    if (clearSearchBtn) {
        clearSearchBtn.addEventListener("click", () => {
            if (searchInput) {
                searchInput.value = "";
                searchQuery = "";
                clearSearchBtn.style.display = "none";
                render();
                searchInput.focus();
            }
        });
    }

    // Status filter
    document.getElementById("masterStatusFilter")?.addEventListener("change", event => {
        statusFilter = event.target.value;
        render();
    });

    // Status switch toggle in modal
    const statusSwitch = document.getElementById("masterStatusSwitch");
    const statusSelect = document.getElementById("masterStatus");
    if (statusSwitch && statusSelect) {
        statusSwitch.addEventListener("change", () => {
            statusSelect.value = statusSwitch.checked ? "active" : "inactive";
        });
    }

    // Table delegated actions (Edit, Up, Down, Toggle)
    document.addEventListener("click", async event => {
        // Edit
        const editBtn = event.target.closest("[data-edit-master]");
        if (editBtn) {
            openModal(Number(editBtn.dataset.editMaster));
            return;
        }

        // Toggle Status
        const toggleBtn = event.target.closest("[data-toggle-master]");
        if (toggleBtn) {
            await toggleItemStatus(Number(toggleBtn.dataset.toggleMaster));
            return;
        }

        // Move Up
        const upBtn = event.target.closest("[data-master-up]");
        if (upBtn) {
            await moveItem(Number(upBtn.dataset.masterUp), -1);
            return;
        }

        // Move Down
        const downBtn = event.target.closest("[data-master-down]");
        if (downBtn) {
            await moveItem(Number(downBtn.dataset.masterDown), 1);
        }
    });
}

function updateCategoryHeader() {
    const meta = categoryMeta[currentCategory] || {
        label: currentCategory,
        icon: "fa-folder",
        desc: "Quản lý dữ liệu danh mục."
    };

    const titleEl = document.getElementById("categoryTitle");
    if (titleEl) titleEl.textContent = meta.label;

    const descEl = document.getElementById("categoryDesc");
    if (descEl) descEl.textContent = meta.desc;

    const badgeIconEl = document.getElementById("categoryIconBadge");
    if (badgeIconEl) badgeIconEl.innerHTML = `<i class="fa-solid ${meta.icon}"></i>`;

    const kpiLabel = document.getElementById("kpiCategoryLabel");
    if (kpiLabel) kpiLabel.textContent = `Giá trị ${meta.label}`;

    const modalBadge = document.getElementById("modalCategoryBadge");
    if (modalBadge) modalBadge.textContent = meta.label;
}

// API CLIENT
async function api(path, options = {}) {
    const response = await fetch(API_BASE + path, {
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    const result = await response.json().catch(() => ({
        success: false,
        message: "Phản hồi API không hợp lệ"
    }));

    if (!response.ok) {
        throw new Error(result.message || "Có lỗi xảy ra");
    }

    return result;
}

// LOAD VALUES
async function loadValues() {
    try {
        const result = await api(`/api/master-data/${encodeURIComponent(currentCategory)}`);
        values = Array.isArray(result.data) ? result.data : [];
        updateKpis();
        render();
        updateCategoryCountBadge(currentCategory, values.length);
    } catch (error) {
        console.error("Load master data error:", error);
        values = [];
        updateKpis();
        render();
        showToast(error.message || "Không thể tải danh mục.", "error");
    }
}

// PRE-FETCH COUNTS FOR ALL CATEGORIES
async function fetchAllCategoryCounts() {
    for (const key of Object.keys(categoryMeta)) {
        try {
            const res = await api(`/api/master-data/${encodeURIComponent(key)}`);
            if (res.data && Array.isArray(res.data)) {
                updateCategoryCountBadge(key, res.data.length);
            }
        } catch (e) {
            // Ignore background count failure
        }
    }
}

function updateCategoryCountBadge(catKey, count) {
    const badge = document.getElementById(`count-${catKey}`);
    if (badge) {
        badge.textContent = count;
    }
}

// UPDATE KPIS
function updateKpis() {
    const totalEl = document.getElementById("kpiCategoryTotal");
    const activeEl = document.getElementById("kpiActiveTotal");
    const inactiveEl = document.getElementById("kpiInactiveTotal");

    const total = values.length;
    const active = values.filter(v => v.active).length;
    const inactive = total - active;

    if (totalEl) totalEl.textContent = total;
    if (activeEl) activeEl.textContent = active;
    if (inactiveEl) inactiveEl.textContent = inactive;
}

// RENDER LIST
function render() {
    const list = document.getElementById("masterList");
    const empty = document.getElementById("masterEmpty");
    if (!list) return;

    list.innerHTML = "";

    // Apply Filters
    let filtered = values.filter(item => {
        if (statusFilter === "active" && !item.active) return false;
        if (statusFilter === "inactive" && item.active) return false;

        if (searchQuery) {
            const code = String(item.code || "").toLowerCase();
            const name = String(item.name || "").toLowerCase();
            return code.includes(searchQuery) || name.includes(searchQuery);
        }
        return true;
    });

    if (empty) {
        empty.style.display = (filtered.length === 0) ? "flex" : "none";
    }

    filtered.forEach((item, index) => {
        const row = document.createElement("article");
        row.className = "master-item";

        const statusClass = item.active ? "active" : "inactive";
        const statusText = item.active ? "Đang dùng" : "Ngừng dùng";
        const toggleIcon = item.active ? "fa-toggle-on" : "fa-toggle-off";
        const toggleTitle = item.active ? "Bấm để ngừng sử dụng" : "Bấm để kích hoạt sử dụng";

        row.innerHTML = `
            <div class="master-order">
                <button
                    type="button"
                    data-master-up="${item.id}"
                    title="Di chuyển lên trên"
                    ${index === 0 ? "disabled" : ""}
                >
                    <i class="fa-solid fa-chevron-up"></i>
                </button>
                <button
                    type="button"
                    data-master-down="${item.id}"
                    title="Di chuyển xuống dưới"
                    ${index === filtered.length - 1 ? "disabled" : ""}
                >
                    <i class="fa-solid fa-chevron-down"></i>
                </button>
            </div>

            <div>
                <span class="master-code">${escapeHtml(item.code)}</span>
            </div>

            <div class="master-name">
                ${escapeHtml(item.name)}
            </div>

            <div>
                <span class="master-status ${statusClass}">
                    ${statusText}
                </span>
            </div>

            <div class="master-actions">
                <button
                    type="button"
                    class="btn-item-action"
                    data-edit-master="${item.id}"
                    title="Chỉnh sửa thông tin"
                >
                    <i class="fa-solid fa-pen-to-square"></i>
                    <span>Sửa</span>
                </button>

                <button
                    type="button"
                    class="btn-item-action btn-item-toggle"
                    data-toggle-master="${item.id}"
                    title="${toggleTitle}"
                >
                    <i class="fa-solid ${toggleIcon}" style="color: ${item.active ? '#34d399' : '#64748b'};"></i>
                </button>
            </div>
        `;

        list.appendChild(row);
    });
}

// TOGGLE ITEM STATUS (1-CLICK)
async function toggleItemStatus(id) {
    const item = values.find(r => Number(r.id) === Number(id));
    if (!item) return;

    const newActive = !item.active;
    try {
        await api(`/api/master-data/${encodeURIComponent(currentCategory)}/${id}`, {
            method: "PUT",
            body: JSON.stringify({
                code: item.code,
                name: item.name,
                active: newActive,
                displayOrder: item.displayOrder
            })
        });

        item.active = newActive;
        updateKpis();
        render();
        showToast(
            newActive
                ? `Đã kích hoạt giá trị "${item.name}"!`
                : `Đã ngừng sử dụng giá trị "${item.name}"!`,
            "success"
        );
    } catch (error) {
        console.error("Toggle master status error:", error);
        showToast(error.message || "Không thể cập nhật trạng thái.", "error");
    }
}

// REORDER ITEMS
async function moveItem(id, direction) {
    const index = values.findIndex(item => Number(item.id) === Number(id));
    const targetIndex = index + direction;

    if (index < 0 || targetIndex < 0 || targetIndex >= values.length) {
        return;
    }

    const current = values[index];
    const target = values[targetIndex];

    const currentOrder = Number(current.displayOrder);
    const targetOrder = Number(target.displayOrder);

    try {
        await api(`/api/master-data/${encodeURIComponent(currentCategory)}/${current.id}`, {
            method: "PUT",
            body: JSON.stringify({
                code: current.code,
                name: current.name,
                active: current.active,
                displayOrder: targetOrder
            })
        });

        await api(`/api/master-data/${encodeURIComponent(currentCategory)}/${target.id}`, {
            method: "PUT",
            body: JSON.stringify({
                code: target.code,
                name: target.name,
                active: target.active,
                displayOrder: currentOrder
            })
        });

        await loadValues();
        showToast("Đã cập nhật thứ tự sắp xếp!", "success");
    } catch (error) {
        console.error("Move master data error:", error);
        showToast(error.message || "Không thể thay đổi thứ tự.", "error");
    }
}

// SAVE FORM
async function saveForm() {
    const code = value("masterCode");
    const name = value("masterName");
    const statusSwitch = document.getElementById("masterStatusSwitch");
    const active = statusSwitch ? statusSwitch.checked : (value("masterStatus") === "active");

    const errorBox = document.getElementById("masterError");
    if (errorBox) {
        errorBox.textContent = "";
        errorBox.style.display = "none";
    }

    if (!code || !name) {
        if (errorBox) {
            errorBox.textContent = "Mã danh mục và tên hiển thị là bắt buộc.";
            errorBox.style.display = "flex";
        }
        return;
    }

    const editingId = value("editingMasterId");
    const existing = editingId
        ? values.find(item => Number(item.id) === Number(editingId))
        : null;

    const body = {
        code,
        name,
        active,
        displayOrder: existing ? Number(existing.displayOrder) : undefined
    };

    try {
        if (editingId) {
            await api(`/api/master-data/${encodeURIComponent(currentCategory)}/${editingId}`, {
                method: "PUT",
                body: JSON.stringify(body)
            });
            showToast(`Cập nhật thành công "${name}"!`, "success");
        } else {
            await api(`/api/master-data/${encodeURIComponent(currentCategory)}`, {
                method: "POST",
                body: JSON.stringify(body)
            });
            showToast(`Thêm mới thành công "${name}"!`, "success");
        }

        closeModal();
        await loadValues();
        fetchAllCategoryCounts();
    } catch (error) {
        console.error("Save master data error:", error);
        if (errorBox) {
            errorBox.textContent = error.message || "Không thể lưu danh mục.";
            errorBox.style.display = "flex";
        }
    }
}

// MODAL WINDOW MANAGEMENT
function openModal(editingId = null) {
    document.getElementById("masterForm")?.reset();
    setValue("editingMasterId", "");

    const errorBox = document.getElementById("masterError");
    if (errorBox) {
        errorBox.textContent = "";
        errorBox.style.display = "none";
    }

    const meta = categoryMeta[currentCategory] || { label: currentCategory };
    const catSubtitle = document.getElementById("masterDrawerCategory");
    if (catSubtitle) catSubtitle.textContent = `Phân loại: ${meta.label}`;

    const modalBadge = document.getElementById("modalCategoryBadge");
    if (modalBadge) modalBadge.textContent = meta.label;

    const statusSwitch = document.getElementById("masterStatusSwitch");

    if (editingId !== null) {
        const item = values.find(record => Number(record.id) === Number(editingId));
        if (item) {
            setValue("editingMasterId", item.id);
            setValue("masterCode", item.code);
            setValue("masterName", item.name);
            setValue("masterStatus", item.active ? "active" : "inactive");
            if (statusSwitch) statusSwitch.checked = Boolean(item.active);

            const title = document.getElementById("masterDrawerTitle");
            if (title) title.textContent = "Chỉnh sửa giá trị";
        }
    } else {
        setValue("masterStatus", "active");
        if (statusSwitch) statusSwitch.checked = true;

        const title = document.getElementById("masterDrawerTitle");
        if (title) title.textContent = "Thêm giá trị danh mục";
    }

    modal?.classList.add("open");
    overlay?.classList.add("open");
    document.getElementById("masterCode")?.focus();
}

function closeModal() {
    modal?.classList.remove("open");
    overlay?.classList.remove("open");
}

// FLOATING TOAST
let toastTimer = null;
function showToast(message, type = "success") {
    const toast = document.getElementById("masterToast");
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
function value(id) {
    return document.getElementById(id)?.value?.trim() ?? "";
}

function setValue(id, newValue) {
    const element = document.getElementById(id);
    if (element) {
        element.value = newValue ?? "";
    }
}

function escapeHtml(val) {
    return String(val ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}