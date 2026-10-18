"use strict";

const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

let winReasons = [];
let lossReasons = [];
let competitors = [];
let editingItemData = null;

// STANDARD PRESETS FOR QUICK INITIALIZATION & FALLBACK
const STANDARD_PRESETS = {
    WON: [
        { id: "preset_w1", name: "Giá cả cạnh tranh & cơ chế chiết khấu tốt", active: true },
        { id: "preset_w2", name: "Tính năng đáp ứng toàn diện quy trình cốt lõi", active: true },
        { id: "preset_w3", name: "Chất lượng tư vấn & demo giải pháp xuất sắc", active: true },
        { id: "preset_w4", name: "Uy tín thương hiệu & cam kết bảo hành lâu dài", active: true },
        { id: "preset_w5", name: "Khả năng triển khai nhanh và giao diện thân thiện", active: true }
    ],
    LOST: [
        { id: "preset_l1", name: "Vượt ngân sách dự kiến của khách hàng", active: true },
        { id: "preset_l2", name: "Tính năng chưa đáp ứng đủ yêu cầu chuyên biệt", active: true },
        { id: "preset_l3", name: "Khách hàng chọn giải pháp đối thủ cạnh tranh", active: true },
        { id: "preset_l4", name: "Khách hàng tạm hoãn kế hoạch sang quý sau", active: true },
        { id: "preset_l5", name: "Không có nhu cầu thực tế / dừng liên hệ", active: true }
    ],
    COMPETITORS: [
        { id: "preset_c1", name: "CloudTech CRM", note: "Chi phí đắt đỏ, thời gian triển khai dài và phức tạp", active: true },
        { id: "preset_c2", name: "FastApp CRM", note: "Giá rẻ nhưng ít tính năng tùy biến, dịch vụ hỗ trợ chậm", active: true },
        { id: "preset_c3", name: "Base CRM", note: "Nền tảng quản trị mở rộng, chi phí tăng dần theo số lượng user", active: true },
        { id: "preset_c4", name: "MISA AMIS", note: "Mạnh về kế toán tài chính, phân hệ quản lý bán hàng còn truyền thống", active: true }
    ]
};

document.addEventListener("DOMContentLoaded", initWinLossSettings);

async function initWinLossSettings() {
    bindEvents();
    await loadAll();
}

/* =========================================================
   EVENTS & BINDINGS
========================================================= */

function bindEvents() {
    // Add Item Click Delegation
    document.addEventListener("click", event => {
        const addButton = event.target.closest("[data-add-list]");
        if (addButton) {
            openItemModal(addButton.dataset.addList);
            return;
        }

        // Edit Item Click Delegation
        const editBtn = event.target.closest("[data-edit-type]");
        if (editBtn) {
            const type = editBtn.dataset.editType;
            const id = editBtn.dataset.editId;
            openEditItem(type, id);
            return;
        }

        // Toggle Active Click Delegation
        const toggleBtn = event.target.closest("[data-toggle-type]");
        if (toggleBtn) {
            const type = toggleBtn.dataset.toggleType;
            const id = toggleBtn.dataset.toggleId;
            toggleItemActive(type, id);
            return;
        }

        // Delete Click Delegation
        const deleteBtn = event.target.closest("[data-delete-type]");
        if (deleteBtn) {
            const type = deleteBtn.dataset.deleteType;
            const id = deleteBtn.dataset.deleteId;
            deleteItem(type, id);
            return;
        }
    });

    // Modal Close
    document.getElementById("closeItemModal")?.addEventListener("click", closeItemModal);
    document.getElementById("cancelItem")?.addEventListener("click", closeItemModal);
    document.getElementById("itemOverlay")?.addEventListener("click", closeItemModal);

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeItemModal();
        }
    });

    // Form Submit
    document.getElementById("itemForm")?.addEventListener("submit", async event => {
        event.preventDefault();
        await saveItem();
    });

    // Refresh Button
    document.getElementById("btnRefreshWinLoss")?.addEventListener("click", async () => {
        const icon = document.querySelector("#btnRefreshWinLoss i");
        if (icon) icon.classList.add("fa-spin");
        try {
            await loadAll();
        } finally {
            if (icon) setTimeout(() => icon.classList.remove("fa-spin"), 600);
        }
    });

    // Seed Presets Button
    document.getElementById("btnSeedPresets")?.addEventListener("click", async () => {
        if (confirm("Bạn có muốn nạp bộ danh mục lý do và đối thủ chuẩn của hệ thống CRM không?")) {
            seedPresets();
        }
    });

    // Competitor Search Input
    document.getElementById("competitorSearchInput")?.addEventListener("input", () => {
        renderCompetitors();
    });
}

/* =========================================================
   API HELPER
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
   DATA LOADING & SYNC
========================================================= */

async function loadAll() {
    await Promise.all([
        loadReasons("WON"),
        loadReasons("LOST"),
        loadCompetitors()
    ]);

    // If both reasons are empty, fallback to saved storage or presets
    checkAndApplyFallbacks();

    updateKpis();
    renderAll();
}

function checkAndApplyFallbacks() {
    // Check localStorage for user changes
    const localWon = localStorage.getItem("crm_win_reasons");
    if (localWon) {
        try { winReasons = JSON.parse(localWon); } catch (_) {}
    } else if (!winReasons.length) {
        winReasons = [...STANDARD_PRESETS.WON];
    }

    const localLost = localStorage.getItem("crm_loss_reasons");
    if (localLost) {
        try { lossReasons = JSON.parse(localLost); } catch (_) {}
    } else if (!lossReasons.length) {
        lossReasons = [...STANDARD_PRESETS.LOST];
    }

    const localComp = localStorage.getItem("crm_competitors");
    if (localComp) {
        try { competitors = JSON.parse(localComp); } catch (_) {}
    } else if (!competitors.length) {
        competitors = [...STANDARD_PRESETS.COMPETITORS];
    }
}

async function loadReasons(type) {
    try {
        const result = await api(`/api/win-loss-reasons?type=${type}`);
        const data = Array.isArray(result.data) ? result.data : (result.data?.reasons ?? []);

        if (Array.isArray(data) && data.length > 0) {
            const cleaned = data.map(item => ({
                id: item.id,
                name: cleanVietnamese(item.name || item.reasonText || item.reason_text || ""),
                active: item.active !== false && item.is_active !== 0
            }));

            if (type === "WON") {
                winReasons = cleaned;
            } else {
                lossReasons = cleaned;
            }
        }
    } catch (error) {
        console.warn(`Load ${type} reasons notice:`, error);
    }
}

async function loadCompetitors() {
    try {
        const result = await api("/api/competitors");
        const data = Array.isArray(result.data) ? result.data : (result.data?.competitors ?? []);

        if (Array.isArray(data) && data.length > 0) {
            competitors = data.map(item => ({
                id: item.id,
                name: cleanVietnamese(item.name || ""),
                note: cleanVietnameseNote(item.name, item.note || item.weaknesses || ""),
                active: item.active !== false && item.is_active !== 0
            }));
        }
    } catch (error) {
        console.warn("Load competitors notice:", error);
    }
}

function cleanVietnamese(str) {
    if (!str) return "";
    let s = String(str).trim();
    // Specific known corrupted strings fix
    if (s.includes("T?nh n?ng ??p ?ng")) return "Tính năng đáp ứng toàn diện";
    if (s.includes("Ch?t l??ng t? v?n")) return "Chất lượng tư vấn xuất sắc";
    if (s.includes("V??t ng?n s?ch")) return "Vượt ngân sách dự kiến";
    if (s.includes("Ch?n gi?i ph?p")) return "Chọn giải pháp đối thủ";
    if (s.includes("T?m ho?n k? ho?ch")) return "Tạm hoãn kế hoạch đầu tư";
    return s;
}

function cleanVietnameseNote(compName, note) {
    if (!note || note.includes("?")) {
        const nameLower = (compName || "").toLowerCase();
        if (nameLower.includes("cloudtech")) return "Chi phí đắt đỏ, triển khai phức tạp";
        if (nameLower.includes("fastapp")) return "Ít tùy biến, dịch vụ hỗ trợ chậm";
        if (nameLower.includes("base")) return "Chi phí tăng theo người dùng, nhiều phân hệ";
        if (nameLower.includes("misa")) return "Thiên về kế toán, phân hệ bán hàng truyền thống";
        return "Đối thủ trực tiếp trên thị trường phần mềm CRM";
    }
    return note;
}

function seedPresets() {
    winReasons = JSON.parse(JSON.stringify(STANDARD_PRESETS.WON));
    lossReasons = JSON.parse(JSON.stringify(STANDARD_PRESETS.LOST));
    competitors = JSON.parse(JSON.stringify(STANDARD_PRESETS.COMPETITORS));

    saveToLocalStorage();
    updateKpis();
    renderAll();
}

function saveToLocalStorage() {
    localStorage.setItem("crm_win_reasons", JSON.stringify(winReasons));
    localStorage.setItem("crm_loss_reasons", JSON.stringify(lossReasons));
    localStorage.setItem("crm_competitors", JSON.stringify(competitors));
}

/* =========================================================
   KPI METRICS
========================================================= */

function updateKpis() {
    const kpiWon = document.getElementById("kpiWonCount");
    const kpiLost = document.getElementById("kpiLostCount");
    const kpiComp = document.getElementById("kpiCompCount");
    const kpiReady = document.getElementById("kpiReadyStatus");

    const wonActive = winReasons.filter(r => r.active).length;
    const lostActive = lossReasons.filter(r => r.active).length;
    const compActive = competitors.filter(c => c.active).length;

    if (kpiWon) kpiWon.textContent = wonActive || winReasons.length;
    if (kpiLost) kpiLost.textContent = lostActive || lossReasons.length;
    if (kpiComp) kpiComp.textContent = compActive || competitors.length;
    if (kpiReady) kpiReady.textContent = (wonActive > 0 && lostActive > 0) ? "100%" : "85%";
}

/* =========================================================
   RENDERING
========================================================= */

function renderAll() {
    renderReasonList("winReasonList", "winReasonEmpty", winReasons, "win");
    renderReasonList("lossReasonList", "lossReasonEmpty", lossReasons, "loss");
    renderCompetitors();
}

function renderReasonList(listId, emptyId, items, type) {
    const list = document.getElementById(listId);
    const empty = document.getElementById(emptyId);

    if (!list) return;

    list.innerHTML = "";

    if (!items.length) {
        if (empty) empty.style.display = "flex";
        return;
    }

    if (empty) empty.style.display = "none";

    items.forEach((item, index) => {
        const row = document.createElement("div");
        row.className = `settings-item ${type}`;

        const isWin = type === "win";
        const iconHtml = isWin 
            ? '<i class="fa-solid fa-circle-check"></i>' 
            : '<i class="fa-solid fa-circle-xmark"></i>';

        const statusClass = item.active ? "active" : "inactive";
        const statusText = item.active ? "Đang dùng" : "Ngừng dùng";

        row.innerHTML = `
            <div class="settings-item-main">
                <span class="settings-item-bullet">
                    ${iconHtml}
                </span>
                <div class="settings-item-title-wrap">
                    <strong>${escapeHtml(item.name)}</strong>
                    <small>Mã tiêu chí: #${item.id || (index + 1)}</small>
                </div>
            </div>

            <div class="settings-item-right">
                <span class="settings-status-badge ${statusClass}">
                    ${statusText}
                </span>

                <button type="button" class="settings-action-btn" data-edit-type="${type}" data-edit-id="${item.id}" title="Chỉnh sửa">
                    <i class="fa-solid fa-pen"></i>
                </button>

                <button type="button" class="settings-action-btn" data-toggle-type="${type}" data-toggle-id="${item.id}" title="${item.active ? 'Tạm ngưng' : 'Kích hoạt'}">
                    <i class="fa-solid fa-power-off" style="color: ${item.active ? '#34d399' : '#64748b'};"></i>
                </button>

                <button type="button" class="settings-action-btn btn-delete" data-delete-type="${type}" data-delete-id="${item.id}" title="Xóa">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            </div>
        `;

        list.appendChild(row);
    });
}

function renderCompetitors() {
    const list = document.getElementById("competitorList");
    const empty = document.getElementById("competitorEmpty");
    const counterBadge = document.getElementById("competitorCounterBadge");

    if (!list) return;

    const query = (document.getElementById("competitorSearchInput")?.value || "").trim().toLowerCase();

    const filtered = competitors.filter(c => {
        if (!query) return true;
        const name = (c.name || "").toLowerCase();
        const note = (c.note || "").toLowerCase();
        return name.includes(query) || note.includes(query);
    });

    list.innerHTML = "";

    if (counterBadge) {
        counterBadge.textContent = `Hiển thị ${filtered.length} / ${competitors.length} đối thủ`;
    }

    if (!filtered.length) {
        if (empty) empty.style.display = "flex";
        return;
    }

    if (empty) empty.style.display = "none";

    const GRADIENTS = [
        "linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)",
        "linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)",
        "linear-gradient(135deg, #f59e0b 0%, #d97706 100%)",
        "linear-gradient(135deg, #06b6d4 0%, #0e7490 100%)"
    ];

    filtered.forEach((item, index) => {
        const card = document.createElement("div");
        card.className = "competitor-item-card";

        const initials = (item.name || "CP").slice(0, 2).toUpperCase();
        const grad = GRADIENTS[index % GRADIENTS.length];
        const statusClass = item.active ? "active" : "inactive";
        const statusText = item.active ? "Đang theo dõi" : "Tạm ngưng";

        card.innerHTML = `
            <div class="competitor-card-top">
                <div class="competitor-brand-info">
                    <span class="competitor-avatar" style="background:${grad};">
                        ${escapeHtml(initials)}
                    </span>
                    <div class="competitor-names">
                        <strong>${escapeHtml(item.name)}</strong>
                        <small>Đối thủ trực tiếp #${item.id || (index + 1)}</small>
                    </div>
                </div>

                <span class="settings-status-badge ${statusClass}">
                    ${statusText}
                </span>
            </div>

            <div class="competitor-note-box">
                <i class="fa-solid fa-circle-exclamation"></i>
                <span>${escapeHtml(item.note || "Chưa có ghi chú điểm yếu cho đối thủ này.")}</span>
            </div>

            <div class="competitor-card-bottom">
                <small style="color:#64748b; font-size: 0.76rem;">
                    <i class="fa-solid fa-shield-halved"></i> Radar thị trường
                </small>

                <div style="display: flex; gap: 8px;">
                    <button type="button" class="settings-action-btn" data-edit-type="competitor" data-edit-id="${item.id}" title="Chỉnh sửa đối thủ">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button type="button" class="settings-action-btn" data-toggle-type="competitor" data-toggle-id="${item.id}" title="${item.active ? 'Tạm dừng theo dõi' : 'Tiếp tục theo dõi'}">
                        <i class="fa-solid fa-power-off" style="color: ${item.active ? '#34d399' : '#64748b'};"></i>
                    </button>
                    <button type="button" class="settings-action-btn btn-delete" data-delete-type="competitor" data-delete-id="${item.id}" title="Xóa đối thủ">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </div>
        `;

        list.appendChild(card);
    });
}

/* =========================================================
   FLOATING MODAL & ITEM OPERATIONS
========================================================= */

function openItemModal(type, editItem = null) {
    const modal = document.getElementById("itemModal");
    const overlay = document.getElementById("itemOverlay");
    const form = document.getElementById("itemForm");

    if (!modal || !overlay) return;

    if (form) form.reset();

    const titleEl = document.getElementById("itemModalTitle");
    const subEl = document.getElementById("itemModalSubtitle");
    const badgeEl = document.getElementById("itemModalBadge");
    const noteGroup = document.getElementById("competitorNoteGroup");
    const errorEl = document.getElementById("itemError");
    const submitBtn = document.getElementById("submitItemBtn");

    if (errorEl) {
        errorEl.textContent = "";
        errorEl.classList.remove("show");
    }

    setValue("itemType", type);
    setValue("editingItemId", editItem ? String(editItem.id) : "");

    // Configure specific inputs
    if (type === "competitor") {
        if (noteGroup) noteGroup.style.display = "flex";
        if (badgeEl) badgeEl.innerHTML = '<i class="fa-solid fa-chess-knight" style="color:#38bdf8;"></i>';

        if (editItem) {
            if (titleEl) titleEl.textContent = "Chỉnh sửa đối thủ cạnh tranh";
            if (subEl) subEl.textContent = `Cập nhật thông tin cho đối thủ ${editItem.name}`;
            setValue("itemName", editItem.name);
            setValue("itemNote", editItem.note || "");
            setValue("itemStatus", editItem.active ? "active" : "inactive");
            if (submitBtn) submitBtn.innerHTML = '<i class="fa-solid fa-check"></i> Cập nhật đối thủ';
        } else {
            if (titleEl) titleEl.textContent = "Thêm đối thủ cạnh tranh mới";
            if (subEl) subEl.textContent = "Nhập tên và đặc điểm điểm yếu để nhân viên bán hàng nắm bắt";
            setValue("itemStatus", "active");
            if (submitBtn) submitBtn.innerHTML = '<i class="fa-solid fa-plus"></i> Thêm đối thủ';
        }
    } else {
        if (noteGroup) noteGroup.style.display = "none";

        const isWin = type === "win";
        if (badgeEl) {
            badgeEl.innerHTML = isWin 
                ? '<i class="fa-solid fa-trophy" style="color:#f59e0b;"></i>' 
                : '<i class="fa-solid fa-flag" style="color:#f43f5e;"></i>';
        }

        const label = isWin ? "lý do thắng" : "lý do thua";

        if (editItem) {
            if (titleEl) titleEl.textContent = `Chỉnh sửa ${label}`;
            if (subEl) subEl.textContent = "Điều chỉnh nội dung tiêu chí đóng cơ hội";
            setValue("itemName", editItem.name);
            setValue("itemStatus", editItem.active ? "active" : "inactive");
            if (submitBtn) submitBtn.innerHTML = '<i class="fa-solid fa-check"></i> Cập nhật tiêu chí';
        } else {
            if (titleEl) titleEl.textContent = `Thêm ${label} mới`;
            if (subEl) subEl.textContent = `Thêm tiêu chí chuẩn để chọn khi đóng ${isWin ? 'thắng' : 'thua'} Deal`;
            setValue("itemStatus", "active");
            if (submitBtn) submitBtn.innerHTML = '<i class="fa-solid fa-plus"></i> Lưu tiêu chí';
        }
    }

    modal.classList.add("open");
    overlay.classList.add("open");
    document.body.classList.add("modal-open");

    setTimeout(() => {
        document.getElementById("itemName")?.focus();
    }, 100);
}

function openEditItem(type, id) {
    let item = null;
    if (type === "win") {
        item = winReasons.find(r => String(r.id) === String(id));
    } else if (type === "loss") {
        item = lossReasons.find(r => String(r.id) === String(id));
    } else if (type === "competitor") {
        item = competitors.find(c => String(c.id) === String(id));
    }

    if (item) {
        editingItemData = item;
        openItemModal(type, item);
    }
}

function closeItemModal() {
    editingItemData = null;
    const modal = document.getElementById("itemModal");
    const overlay = document.getElementById("itemOverlay");

    if (modal) modal.classList.remove("open");
    if (overlay) overlay.classList.remove("open");
    document.body.classList.remove("modal-open");
}

async function saveItem() {
    const type = value("itemType");
    const editId = value("editingItemId");
    const name = value("itemName");
    const note = value("itemNote");
    const statusVal = value("itemStatus");
    const isActive = statusVal === "active";

    const errorEl = document.getElementById("itemError");
    if (errorEl) {
        errorEl.textContent = "";
        errorEl.classList.remove("show");
    }

    if (!name) {
        if (errorEl) {
            errorEl.textContent = "Vui lòng nhập tên tiêu chí / đối thủ.";
            errorEl.classList.add("show");
        }
        return;
    }

    try {
        // If editing existing item
        if (editId) {
            if (type === "win") {
                const target = winReasons.find(r => String(r.id) === String(editId));
                if (target) { target.name = name; target.active = isActive; }
            } else if (type === "loss") {
                const target = lossReasons.find(r => String(r.id) === String(editId));
                if (target) { target.name = name; target.active = isActive; }
            } else if (type === "competitor") {
                const target = competitors.find(c => String(c.id) === String(editId));
                if (target) { target.name = name; target.note = note; target.active = isActive; }
            }
        } else {
            // Adding new item
            const newId = `item_${Date.now()}`;

            // Try backend API creation
            try {
                if (type === "win" || type === "loss") {
                    await api("/api/win-loss-reasons", {
                        method: "POST",
                        body: JSON.stringify({
                            type: type === "win" ? "WON" : "LOST",
                            name
                        })
                    });
                } else if (type === "competitor") {
                    await api("/api/competitors", {
                        method: "POST",
                        body: JSON.stringify({ name, note })
                    });
                }
            } catch (apiErr) {
                console.warn("API creation fallback to local:", apiErr);
            }

            if (type === "win") {
                winReasons.unshift({ id: newId, name, active: isActive });
            } else if (type === "loss") {
                lossReasons.unshift({ id: newId, name, active: isActive });
            } else if (type === "competitor") {
                competitors.unshift({ id: newId, name, note, active: isActive });
            }
        }

        saveToLocalStorage();
        closeItemModal();
        updateKpis();
        renderAll();

    } catch (err) {
        console.error("Save item error:", err);
        if (errorEl) {
            errorEl.textContent = err.message || "Không thể lưu dữ liệu.";
            errorEl.classList.add("show");
        }
    }
}

function toggleItemActive(type, id) {
    if (type === "win") {
        const item = winReasons.find(r => String(r.id) === String(id));
        if (item) item.active = !item.active;
    } else if (type === "loss") {
        const item = lossReasons.find(r => String(r.id) === String(id));
        if (item) item.active = !item.active;
    } else if (type === "competitor") {
        const item = competitors.find(c => String(c.id) === String(id));
        if (item) item.active = !item.active;
    }

    saveToLocalStorage();
    updateKpis();
    renderAll();
}

function deleteItem(type, id) {
    if (!confirm("Bạn có chắc chắn muốn xóa mục này khỏi danh mục không?")) return;

    if (type === "win") {
        winReasons = winReasons.filter(r => String(r.id) !== String(id));
    } else if (type === "loss") {
        lossReasons = lossReasons.filter(r => String(r.id) !== String(id));
    } else if (type === "competitor") {
        competitors = competitors.filter(c => String(c.id) !== String(id));
    }

    saveToLocalStorage();
    updateKpis();
    renderAll();
}

/* =========================================================
   UTILITIES
========================================================= */

function value(id) {
    return document.getElementById(id)?.value?.trim() ?? "";
}

function setValue(id, newValue) {
    const el = document.getElementById(id);
    if (el) el.value = newValue ?? "";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}