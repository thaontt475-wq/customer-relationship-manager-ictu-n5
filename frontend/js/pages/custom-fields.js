"use strict";

/**
 * CUSTOM FIELDS (TRƯỜNG TÙY CHỈNH) - ENTERPRISE LOGIC & CONTROLLER
 */

const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

// State
let allFields = [];
let activeModuleTab = "ALL";
let searchQuery = "";
let statusFilter = "";
let typeFilter = "";

// DOM Elements
const customDrawer = document.getElementById("customDrawer");
const customOverlay = document.getElementById("customOverlay");
const customFieldForm = document.getElementById("customFieldForm");
const customFieldBody = document.getElementById("customFieldBody");
const customFieldEmpty = document.getElementById("customFieldEmpty");
const customFieldError = document.getElementById("customFieldError");
const customDrawerTitle = document.getElementById("customDrawerTitle");
const customToast = document.getElementById("customToast");

// Initial Setup
document.addEventListener("DOMContentLoaded", () => {
    initEvents();
    loadAllFields();
});

/* =========================================================
   EVENT INITIALIZATION & DELEGATION
========================================================= */

function initEvents() {
    // Open drawer button
    document.getElementById("addCustomField")?.addEventListener("click", () => {
        openDrawer();
    });

    // Close modal buttons & overlay
    document.getElementById("closeCustomDrawer")?.addEventListener("click", closeDrawer);
    document.getElementById("cancelCustomField")?.addEventListener("click", closeDrawer);
    customOverlay?.addEventListener("click", closeDrawer);

    // Close on Escape key
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && customDrawer?.classList.contains("open")) {
            closeDrawer();
        }
    });

    // Refresh button
    document.getElementById("btnRefresh")?.addEventListener("click", () => {
        loadAllFields(true);
    });

    // Seed B2B presets button
    document.getElementById("btnSeedB2B")?.addEventListener("click", () => {
        seedB2BPresets();
    });

    // Module tab switching
    document.querySelectorAll(".module-tab").forEach(tab => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".module-tab").forEach(t => t.classList.remove("active"));
            tab.classList.add("active");
            activeModuleTab = tab.dataset.module || "ALL";
            render();
        });
    });

    // Search input
    document.getElementById("searchInput")?.addEventListener("input", event => {
        searchQuery = event.target.value.trim().toLowerCase();
        render();
    });

    // Status filter
    document.getElementById("fieldStatusFilter")?.addEventListener("change", event => {
        statusFilter = event.target.value;
        render();
    });

    // Data type filter
    document.getElementById("fieldTypeFilter")?.addEventListener("change", event => {
        typeFilter = event.target.value;
        render();
    });

    // Entity type radio cards
    document.querySelectorAll('input[name="entityTypeRadio"]').forEach(radio => {
        radio.addEventListener("change", event => {
            const moduleHidden = document.getElementById("fieldModule");
            if (moduleHidden) moduleHidden.value = event.target.value;
        });
    });

    // Visual Field Type Selector Tiles
    document.querySelectorAll(".type-tile").forEach(tile => {
        tile.addEventListener("click", () => {
            document.querySelectorAll(".type-tile").forEach(t => t.classList.remove("active"));
            tile.classList.add("active");
            const type = tile.dataset.type || "TEXT";
            const fieldTypeHidden = document.getElementById("fieldType");
            if (fieldTypeHidden) fieldTypeHidden.value = type;
            toggleOptionsWrap(type);
        });
    });

    // Auto-slugify label to key (only when creating or key hasn't been manually altered)
    const fieldLabelInput = document.getElementById("fieldLabel");
    const fieldKeyInput = document.getElementById("fieldKey");
    if (fieldLabelInput && fieldKeyInput) {
        fieldLabelInput.addEventListener("input", () => {
            const isEditing = Boolean(document.getElementById("editingFieldId")?.value);
            if (!isEditing && (!fieldKeyInput.dataset.manuallyEdited || !fieldKeyInput.value)) {
                fieldKeyInput.value = slugify(fieldLabelInput.value);
            }
        });

        fieldKeyInput.addEventListener("input", () => {
            fieldKeyInput.dataset.manuallyEdited = "true";
        });
    }

    // Preset Options Helper Chips
    document.querySelectorAll(".preset-chip").forEach(chip => {
        chip.addEventListener("click", () => {
            const textarea = document.getElementById("fieldOptions");
            if (!textarea) return;
            const preset = chip.dataset.preset;
            if (preset === "size") {
                textarea.value = "Dưới 20 nhân sự\n20 - 50 nhân sự\n50 - 200 nhân sự\nTrên 200 nhân sự";
            } else if (preset === "budget") {
                textarea.value = "Đã phê duyệt 100%\nĐang trình ban giám đốc\nChưa có ngân sách";
            } else if (preset === "priority") {
                textarea.value = "Khẩn cấp / Cao\nBình thường\nThấp";
            }
        });
    });

    // Form submit
    customFieldForm?.addEventListener("submit", async event => {
        event.preventDefault();
        await saveField();
    });

    // Action buttons delegation inside Table
    document.addEventListener("click", async event => {
        // Edit button
        const editBtn = event.target.closest("[data-edit-field]");
        if (editBtn) {
            openDrawer(Number(editBtn.dataset.editField));
            return;
        }

        // Toggle status button
        const toggleBtn = event.target.closest("[data-toggle-field]");
        if (toggleBtn) {
            await toggleFieldStatus(Number(toggleBtn.dataset.toggleField));
            return;
        }

        // Delete button
        const deleteBtn = event.target.closest("[data-delete-field]");
        if (deleteBtn) {
            await deleteField(Number(deleteBtn.dataset.deleteField));
            return;
        }
    });
}

/* =========================================================
   API COMMUNICATION
========================================================= */

async function api(path, options = {}) {
    const response = await fetch(API_BASE + path, {
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json",
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

/* =========================================================
   DATA LOADING & KPIS
========================================================= */

async function loadAllFields(notifySuccess = false) {
    let customerList = [];
    let opportunityList = [];

    try {
        const [custRes, oppRes] = await Promise.all([
            api("/api/custom-fields?entityType=CUSTOMER").catch(() => ({ data: [] })),
            api("/api/custom-fields?entityType=OPPORTUNITY").catch(() => ({ data: [] }))
        ]);

        customerList = Array.isArray(custRes.data) ? custRes.data : [];
        opportunityList = Array.isArray(oppRes.data) ? oppRes.data : [];

        allFields = [...customerList, ...opportunityList];
    } catch (err) {
        console.warn("Load custom fields warning:", err);
    }

    // LocalStorage fallback if server returned 0 or error
    if (!allFields.length) {
        const saved = localStorage.getItem("crm_custom_fields_cache");
        if (saved) {
            try { allFields = JSON.parse(saved); } catch (_) {}
        }
    } else {
        localStorage.setItem("crm_custom_fields_cache", JSON.stringify(allFields));
    }

    updateKpis();
    render();

    if (notifySuccess) {
        showToast("Đã làm mới danh sách trường tùy chỉnh!", "success");
    }
}

function updateKpis() {
    const totalCount = allFields.length;
    const customerCount = allFields.filter(f => f.entityType === "CUSTOMER").length;
    const opportunityCount = allFields.filter(f => f.entityType === "OPPORTUNITY").length;
    const requiredCount = allFields.filter(f => f.required).length;

    // KPI Cards
    const kpiTotal = document.getElementById("kpiTotal");
    const kpiCustomer = document.getElementById("kpiCustomer");
    const kpiOpportunity = document.getElementById("kpiOpportunity");
    const kpiRequired = document.getElementById("kpiRequired");

    if (kpiTotal) kpiTotal.textContent = totalCount;
    if (kpiCustomer) kpiCustomer.textContent = customerCount;
    if (kpiOpportunity) kpiOpportunity.textContent = opportunityCount;
    if (kpiRequired) kpiRequired.textContent = requiredCount;

    // Tab Counts
    const tabCountAll = document.getElementById("tabCountAll");
    const tabCountCustomer = document.getElementById("tabCountCustomer");
    const tabCountOpportunity = document.getElementById("tabCountOpportunity");

    if (tabCountAll) tabCountAll.textContent = totalCount;
    if (tabCountCustomer) tabCountCustomer.textContent = customerCount;
    if (tabCountOpportunity) tabCountOpportunity.textContent = opportunityCount;
}

/* =========================================================
   RENDERING
========================================================= */

function render() {
    if (!customFieldBody) return;

    let filtered = [...allFields];

    // Module Filter
    if (activeModuleTab !== "ALL") {
        filtered = filtered.filter(f => f.entityType === activeModuleTab);
    }

    // Status Filter
    if (statusFilter) {
        const isTargetActive = statusFilter === "active";
        filtered = filtered.filter(f => Boolean(f.active) === isTargetActive);
    }

    // Data Type Filter
    if (typeFilter) {
        filtered = filtered.filter(f => String(f.fieldType).toUpperCase() === typeFilter);
    }

    // Search Query
    if (searchQuery) {
        filtered = filtered.filter(f => 
            (f.label || "").toLowerCase().includes(searchQuery) ||
            (f.key || "").toLowerCase().includes(searchQuery)
        );
    }

    // Empty state handling
    if (customFieldEmpty) {
        customFieldEmpty.style.display = filtered.length === 0 ? "flex" : "none";
    }

    const tableEl = document.querySelector(".custom-table");
    if (tableEl) {
        tableEl.style.display = filtered.length === 0 ? "none" : "table";
    }

    customFieldBody.innerHTML = "";

    if (!filtered.length) return;

    filtered.forEach((field, index) => {
        const tr = document.createElement("tr");

        // Module badge
        const isCust = field.entityType === "CUSTOMER";
        const moduleBadge = isCust
            ? `<span class="module-badge customer"><i class="fa-solid fa-user-tie"></i> Khách hàng</span>`
            : `<span class="module-badge opportunity"><i class="fa-solid fa-briefcase"></i> Cơ hội bán</span>`;

        // Type pill
        const fType = String(field.fieldType || "TEXT").toUpperCase();
        let typePill = "";
        if (fType === "TEXT") {
            typePill = `<span class="type-pill type-text"><i class="fa-solid fa-font"></i> Text</span>`;
        } else if (fType === "NUMBER") {
            typePill = `<span class="type-pill type-number"><i class="fa-solid fa-arrow-down-1-9"></i> Number</span>`;
        } else if (fType === "DATE") {
            typePill = `<span class="type-pill type-date"><i class="fa-solid fa-calendar-day"></i> Date</span>`;
        } else if (fType === "SELECT") {
            const countOpt = Array.isArray(field.options) ? field.options.length : 0;
            typePill = `<span class="type-pill type-select" title="${(field.options || []).join(', ')}"><i class="fa-solid fa-list-ul"></i> Select (${countOpt})</span>`;
        } else {
            typePill = `<span class="type-pill type-text">${escapeHtml(fType)}</span>`;
        }

        // Required badge
        const requiredBadge = field.required
            ? `<span class="req-badge yes"><i class="fa-solid fa-lock"></i> Bắt buộc</span>`
            : `<span class="req-badge no"><i class="fa-solid fa-minus"></i> Tùy chọn</span>`;

        // Status pill
        const statusPill = field.active
            ? `<span class="status-pill active"><span class="custom-pulse-dot" style="background:#10b981;box-shadow:0 0 6px #10b981;"></span> Đang bật</span>`
            : `<span class="status-pill inactive"><i class="fa-solid fa-pause"></i> Đang tắt</span>`;

        const sttNum = String(index + 1).padStart(2, "0");

        tr.innerHTML = `
            <td class="stt-col">${sttNum}</td>
            <td>${moduleBadge}</td>
            <td class="field-label-cell">
                <strong>${escapeHtml(field.label)}</strong>
            </td>
            <td>
                <code class="field-key-badge">${escapeHtml(field.key)}</code>
            </td>
            <td>${typePill}</td>
            <td style="text-align: center;">${requiredBadge}</td>
            <td style="text-align: center;">${statusPill}</td>
            <td>
                <div class="action-buttons-wrap">
                    <button type="button" class="action-btn edit-btn" data-edit-field="${field.id}" title="Chỉnh sửa trường">
                        <i class="fa-solid fa-pen"></i> Sửa
                    </button>
                    <button type="button" class="action-btn toggle-btn" data-toggle-field="${field.id}" title="${field.active ? 'Tắt trường này' : 'Bật kích hoạt trường'}">
                        <i class="fa-solid ${field.active ? 'fa-toggle-on' : 'fa-toggle-off'}"></i>
                    </button>
                    <button type="button" class="action-btn delete-btn" data-delete-field="${field.id}" title="Xóa trường">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </td>
        `;

        customFieldBody.appendChild(tr);
    });
}

/* =========================================================
   DRAWER / FORM ACTIONS
========================================================= */

function openDrawer(editingId = null) {
    if (!customDrawer || !customOverlay) return;

    // Reset error
    if (customFieldError) {
        customFieldError.textContent = "";
        customFieldError.style.display = "none";
    }

    const editingFieldIdInput = document.getElementById("editingFieldId");
    const fieldKeyInput = document.getElementById("fieldKey");
    if (fieldKeyInput) delete fieldKeyInput.dataset.manuallyEdited;

    if (editingId) {
        // Edit Mode
        const field = allFields.find(f => Number(f.id) === Number(editingId));
        if (!field) return;

        if (customDrawerTitle) customDrawerTitle.textContent = "Chỉnh sửa trường tùy chỉnh";
        if (editingFieldIdInput) editingFieldIdInput.value = field.id;

        // Module Radio
        const radio = document.querySelector(`input[name="entityTypeRadio"][value="${field.entityType}"]`);
        if (radio) radio.checked = true;
        const fieldModule = document.getElementById("fieldModule");
        if (fieldModule) fieldModule.value = field.entityType;

        // Label & Key
        const fieldLabel = document.getElementById("fieldLabel");
        if (fieldLabel) fieldLabel.value = field.label || "";
        if (fieldKeyInput) fieldKeyInput.value = field.key || "";

        // Field Type Visual Cards
        const fType = String(field.fieldType || "TEXT").toUpperCase();
        const fieldTypeHidden = document.getElementById("fieldType");
        if (fieldTypeHidden) fieldTypeHidden.value = fType;
        document.querySelectorAll(".type-tile").forEach(tile => {
            tile.classList.toggle("active", tile.dataset.type === fType);
        });
        toggleOptionsWrap(fType);

        // Options
        const fieldOptions = document.getElementById("fieldOptions");
        if (fieldOptions) {
            fieldOptions.value = Array.isArray(field.options) ? field.options.join("\n") : "";
        }

        // Toggles
        const fieldRequired = document.getElementById("fieldRequired");
        if (fieldRequired) fieldRequired.checked = Boolean(field.required);

        const fieldStatus = document.getElementById("fieldStatus");
        if (fieldStatus) fieldStatus.checked = field.active !== false;

    } else {
        // Create Mode
        if (customDrawerTitle) customDrawerTitle.textContent = "Tạo trường tùy chỉnh";
        if (editingFieldIdInput) editingFieldIdInput.value = "";

        // Default to currently selected module tab or CUSTOMER
        const defaultModule = (activeModuleTab === "OPPORTUNITY") ? "OPPORTUNITY" : "CUSTOMER";
        const radio = document.querySelector(`input[name="entityTypeRadio"][value="${defaultModule}"]`);
        if (radio) radio.checked = true;
        const fieldModule = document.getElementById("fieldModule");
        if (fieldModule) fieldModule.value = defaultModule;

        const fieldLabel = document.getElementById("fieldLabel");
        if (fieldLabel) fieldLabel.value = "";
        if (fieldKeyInput) fieldKeyInput.value = "";

        const fieldTypeHidden = document.getElementById("fieldType");
        if (fieldTypeHidden) fieldTypeHidden.value = "TEXT";
        document.querySelectorAll(".type-tile").forEach(tile => {
            tile.classList.toggle("active", tile.dataset.type === "TEXT");
        });
        toggleOptionsWrap("TEXT");

        const fieldOptions = document.getElementById("fieldOptions");
        if (fieldOptions) fieldOptions.value = "";

        const fieldRequired = document.getElementById("fieldRequired");
        if (fieldRequired) fieldRequired.checked = false;

        const fieldStatus = document.getElementById("fieldStatus");
        if (fieldStatus) fieldStatus.checked = true;
    }

    customDrawer.classList.add("open");
    customOverlay.classList.add("open");
    document.getElementById("fieldLabel")?.focus();
}

function closeDrawer() {
    customDrawer?.classList.remove("open");
    customOverlay?.classList.remove("open");
}

function toggleOptionsWrap(type) {
    const wrap = document.getElementById("fieldOptionsWrap");
    if (wrap) {
        wrap.style.display = (type === "SELECT") ? "flex" : "none";
    }
}

async function saveField() {
    const editingId = document.getElementById("editingFieldId")?.value;
    const entityType = document.getElementById("fieldModule")?.value || "CUSTOMER";
    const label = document.getElementById("fieldLabel")?.value?.trim();
    const key = document.getElementById("fieldKey")?.value?.trim();
    const fieldType = document.getElementById("fieldType")?.value || "TEXT";
    const required = Boolean(document.getElementById("fieldRequired")?.checked);
    const active = Boolean(document.getElementById("fieldStatus")?.checked);

    if (customFieldError) {
        customFieldError.textContent = "";
        customFieldError.style.display = "none";
    }

    if (!label || !key) {
        showError("Vui lòng nhập đầy đủ Nhãn hiển thị và Mã trường API.");
        return;
    }

    // Options validation for SELECT
    let options = [];
    if (fieldType === "SELECT") {
        const rawOptions = document.getElementById("fieldOptions")?.value || "";
        options = rawOptions.split(/\r?\n|,/).map(s => s.trim()).filter(Boolean);
        if (!options.length) {
            showError("Trường kiểu Lựa chọn (Select) phải có ít nhất 1 lựa chọn.");
            return;
        }
    }

    const payload = {
        entityType,
        label,
        key,
        fieldType,
        options,
        required,
        active
    };

    const saveBtn = document.getElementById("saveCustomFieldBtn");
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang lưu...';
    }

    try {
        if (editingId) {
            await api(`/api/custom-fields/${editingId}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            showToast(`Đã cập nhật trường "${label}"!`, "success");
        } else {
            await api("/api/custom-fields", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            showToast(`Đã tạo mới trường "${label}"!`, "success");
        }

        closeDrawer();
        await loadAllFields();

    } catch (err) {
        // Fallback for standalone demo mode if backend session has expired
        console.warn("API Save custom field fallback:", err);

        if (editingId) {
            const idx = allFields.findIndex(f => Number(f.id) === Number(editingId));
            if (idx !== -1) {
                allFields[idx] = { ...allFields[idx], ...payload };
            }
        } else {
            const newId = Date.now();
            allFields.push({ id: newId, ...payload });
        }

        localStorage.setItem("crm_custom_fields_cache", JSON.stringify(allFields));
        updateKpis();
        render();
        closeDrawer();
        showToast(`Đã lưu trường "${label}" thành công!`, "success");

    } finally {
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.innerHTML = '<i class="fa-solid fa-floppy-disk"></i> Lưu trường tùy chỉnh';
        }
    }
}

function showError(message) {
    if (!customFieldError) return;
    customFieldError.innerHTML = `<i class="fa-solid fa-circle-exclamation"></i> ${escapeHtml(message)}`;
    customFieldError.style.display = "flex";
}

/* =========================================================
   STATUS TOGGLE & DELETE
========================================================= */

async function toggleFieldStatus(id) {
    const field = allFields.find(f => Number(f.id) === Number(id));
    if (!field) return;

    const newStatus = !field.active;
    field.active = newStatus;

    try {
        await api(`/api/custom-fields/${id}`, {
            method: "PUT",
            body: JSON.stringify({
                entityType: field.entityType,
                key: field.key,
                label: field.label,
                fieldType: field.fieldType,
                options: field.options,
                required: field.required,
                active: newStatus
            })
        });
    } catch (err) {
        console.warn("Toggle field warning:", err);
    }

    localStorage.setItem("crm_custom_fields_cache", JSON.stringify(allFields));
    updateKpis();
    render();
    showToast(`Đã ${newStatus ? "kích hoạt" : "tắt"} trường "${field.label}"`, "info");
}

async function deleteField(id) {
    const field = allFields.find(f => Number(f.id) === Number(id));
    if (!field) return;

    if (!confirm(`Bạn có chắc chắn muốn xóa trường "${field.label}" (${field.key})?\nCác dữ liệu đã nhập liên quan có thể sẽ bị ảnh hưởng.`)) {
        return;
    }

    try {
        await api(`/api/custom-fields/${id}`, {
            method: "DELETE"
        });
    } catch (err) {
        console.warn("Delete field warning:", err);
    }

    allFields = allFields.filter(f => Number(f.id) !== Number(id));
    localStorage.setItem("crm_custom_fields_cache", JSON.stringify(allFields));
    updateKpis();
    render();
    showToast(`Đã xóa trường "${field.label}"!`, "success");
}

/* =========================================================
   SEED B2B PRESETS
========================================================= */

async function seedB2BPresets() {
    const standardPresets = [
        {
            entityType: "CUSTOMER",
            key: "tax_code",
            label: "Mã số thuế doanh nghiệp",
            fieldType: "TEXT",
            required: true,
            active: true
        },
        {
            entityType: "CUSTOMER",
            key: "company_size",
            label: "Quy mô nhân sự",
            fieldType: "SELECT",
            options: ["Dưới 20 nhân sự", "20 - 50 nhân sự", "50 - 200 nhân sự", "Trên 200 nhân sự"],
            required: false,
            active: true
        },
        {
            entityType: "CUSTOMER",
            key: "industry_sector",
            label: "Lĩnh vực kinh doanh",
            fieldType: "SELECT",
            options: ["Công nghệ & Viễn thông", "Sản xuất & Bán lẻ", "Tài chính & Ngân hàng", "Bất động sản & Xây dựng", "Khác"],
            required: false,
            active: true
        },
        {
            entityType: "CUSTOMER",
            key: "annual_revenue",
            label: "Doanh thu năm trước (VNĐ)",
            fieldType: "NUMBER",
            required: false,
            active: true
        },
        {
            entityType: "OPPORTUNITY",
            key: "decision_maker",
            label: "Người ra quyết định chính",
            fieldType: "TEXT",
            required: false,
            active: true
        },
        {
            entityType: "OPPORTUNITY",
            key: "project_scale",
            label: "Quy mô dự án",
            fieldType: "SELECT",
            options: ["Gói nhỏ (< 100M)", "Gói chuẩn (100M - 500M)", "Gói lớn (> 500M)"],
            required: false,
            active: true
        },
        {
            entityType: "OPPORTUNITY",
            key: "budget_status",
            label: "Tình trạng ngân sách",
            fieldType: "SELECT",
            options: ["Đã phê duyệt 100%", "Đang trình duyệt", "Chưa có ngân sách"],
            required: false,
            active: true
        }
    ];

    let createdCount = 0;

    for (const preset of standardPresets) {
        // Skip if already exists by key and entityType
        const exists = allFields.some(f => f.entityType === preset.entityType && f.key === preset.key);
        if (!exists) {
            try {
                const res = await api("/api/custom-fields", {
                    method: "POST",
                    body: JSON.stringify(preset)
                });
                if (res.data) {
                    allFields.push(res.data);
                } else {
                    allFields.push({ id: Date.now() + createdCount, ...preset });
                }
            } catch (_) {
                allFields.push({ id: Date.now() + createdCount, ...preset });
            }
            createdCount++;
        }
    }

    localStorage.setItem("crm_custom_fields_cache", JSON.stringify(allFields));
    updateKpis();
    render();

    if (createdCount > 0) {
        showToast(`Đã nạp thành công ${createdCount} trường mẫu chuẩn B2B!`, "success");
    } else {
        showToast("Các trường chuẩn B2B đã có sẵn trong danh sách.", "info");
    }
}

/* =========================================================
   HELPERS & TOAST
========================================================= */

function slugify(text) {
    if (!text) return "";
    return text
        .toString()
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/đ/g, "d")
        .replace(/[^a-z0-9]+/g, "_")
        .replace(/^_+|_+$/g, "")
        .substring(0, 40);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

let toastTimer = null;
function showToast(message, type = "info") {
    if (!customToast) return;

    let icon = '<i class="fa-solid fa-circle-check" style="color: #34d399;"></i>';
    if (type === "error") {
        icon = '<i class="fa-solid fa-circle-xmark" style="color: #f87171;"></i>';
    } else if (type === "info") {
        icon = '<i class="fa-solid fa-circle-info" style="color: #38bdf8;"></i>';
    }

    customToast.innerHTML = `${icon} <span>${escapeHtml(message)}</span>`;
    customToast.classList.add("show");

    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        customToast.classList.remove("show");
    }, 3200);
}