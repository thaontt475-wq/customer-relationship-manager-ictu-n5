/**
 * ===================================================================
 * ENTERPRISE MULTI-CRITERIA FILTER & SAVED VIEWS ENGINE (CRM-67)
 * (Synchronized for SPA Page Transition)
 * ===================================================================
 */

"use strict";

(function () {
    const STORAGE_KEY_SAVED_VIEWS = "crm_saved_customer_views";
    const STORAGE_KEY_ACTIVE_VIEW = "crm_active_customer_view_id";

    // Danh mục tỉnh thành / khu vực địa lý Việt Nam
    const REGIONS = [
        { code: "MB", name: "Miền Bắc (Hà Nội, Hải Phòng, Quảng Ninh, Thái Nguyên...)" },
        { code: "MT", name: "Miền Trung (Đà Nẵng, Huế, Khánh Hòa, Nghệ An...)" },
        { code: "MN", name: "Miền Nam (TP.HCM, Bình Dương, Đồng Nai, Cần Thơ...)" }
    ];

    // Cấu hình View mặc định của hệ thống
    const DEFAULT_PRESET_VIEWS = [
        {
            id: "view_all",
            name: "Tất cả khách hàng",
            isPreset: true,
            filter: {
                keyword: "",
                status: "",
                industryId: "",
                companySizeId: "",
                region: "",
                ownerUserId: "",
                scopeFilter: "ALL"
            }
        },
        {
            id: "view_calls_this_week",
            name: "Khách cần gọi trong tuần",
            isPreset: true,
            badge: "Gợi ý",
            filter: {
                keyword: "",
                status: "TIEM_NANG",
                industryId: "",
                companySizeId: "",
                region: "",
                ownerUserId: "",
                scopeFilter: "SELF"
            }
        },
        {
            id: "view_negotiating_deals",
            name: "Đang đàm phán hợp đồng",
            isPreset: true,
            filter: {
                keyword: "",
                status: "DANG_GIAO_DICH",
                industryId: "",
                companySizeId: "",
                region: "",
                ownerUserId: "",
                scopeFilter: "ALL"
            }
        },
        {
            id: "view_north_vip",
            name: "Khách VIP Miền Bắc",
            isPreset: true,
            filter: {
                keyword: "",
                status: "CHINH_THUC",
                industryId: "",
                companySizeId: "13", // Enterprise
                region: "MB",
                ownerUserId: "",
                scopeFilter: "ALL"
            }
        },
        {
            id: "view_duplicates",
            name: "Trùng lặp tiềm ẩn",
            badge: "CRM-64",
            isPreset: true,
            filter: {
                keyword: "",
                status: "",
                industryId: "",
                companySizeId: "",
                region: "",
                ownerUserId: "",
                scopeFilter: "ALL",
                isDuplicateOnly: true
            }
        }
    ];

    // Trạng thái bộ lọc hiện hành
    let filterCriteria = {
        keyword: "",
        status: "",
        industryId: "",
        companySizeId: "",
        region: "",
        ownerUserId: "",
        scopeFilter: "ALL",
        isDuplicateOnly: false
    };

    let savedViews = [];
    let activeViewId = "view_all";
    let debounceTimer = null;

    // Cache elements
    let elements = {};

    function initFilterEngine() {
        cacheElements();
        loadSavedViews();
        populateFilterDropdowns();
        bindFilterEvents();
        renderViewTabs();
        renderActiveFilterChips();
        syncWithGlobalState();
    }

    function cacheElements() {
        elements = {
            search: document.getElementById("customerSearch"),
            statusFilter: document.getElementById("statusFilter"),
            industryFilter: document.getElementById("industryFilter"),
            companySizeFilter: document.getElementById("filterCompanySize"),
            regionFilter: document.getElementById("filterRegion"),
            ownerFilter: document.getElementById("filterOwner"),
            scopeFilter: document.getElementById("scopeFilter"),
            btnResetFilters: document.getElementById("btnResetFilters"),
            btnToggleAdvFilter: document.getElementById("btnToggleAdvFilter"),
            advFilterDrawer: document.getElementById("advFilterDrawer"),
            advFilterDrawerOverlay: document.getElementById("advFilterDrawerOverlay"),
            btnCloseAdvFilter: document.getElementById("btnCloseAdvFilter"),
            btnApplyAdvFilter: document.getElementById("btnApplyAdvFilter"),
            btnResetAdvFilter: document.getElementById("btnResetAdvFilter"),
            btnOpenSaveViewModal: document.getElementById("btnOpenSaveViewModal"),
            activeChipsContainer: document.getElementById("activeFilterChips"),
            viewTabsContainer: document.getElementById("savedViewTabs"),
            viewDropdown: document.getElementById("savedViewSelect"),
            filterMatchCountBadge: document.getElementById("filterMatchCountBadge"),
            // Save view modal
            saveViewModal: document.getElementById("saveViewModal"),
            saveViewModalOverlay: document.getElementById("saveViewModalOverlay"),
            saveViewNameInput: document.getElementById("saveViewNameInput"),
            btnConfirmSaveView: document.getElementById("btnConfirmSaveView"),
            btnCancelSaveView: document.getElementById("btnCancelSaveView")
        };
    }

    function loadSavedViews() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_SAVED_VIEWS);
            const userViews = raw ? JSON.parse(raw) : [];
            savedViews = [...DEFAULT_PRESET_VIEWS, ...userViews];
        } catch (e) {
            console.warn("Lỗi đọc saved views từ localStorage:", e);
            savedViews = [...DEFAULT_PRESET_VIEWS];
        }

        const savedActiveId = localStorage.getItem(STORAGE_KEY_ACTIVE_VIEW);
        if (savedActiveId && savedViews.some(v => v.id === savedActiveId)) {
            activeViewId = savedActiveId;
        } else {
            activeViewId = "view_all";
        }
    }

    function saveUserViews() {
        try {
            const customViews = savedViews.filter(v => !v.isPreset);
            localStorage.setItem(STORAGE_KEY_SAVED_VIEWS, JSON.stringify(customViews));
            localStorage.setItem(STORAGE_KEY_ACTIVE_VIEW, activeViewId);
        } catch (e) {
            console.error("Lỗi lưu saved views:", e);
        }
    }

    function populateFilterDropdowns() {
        // 1. Populate Region Filter
        if (elements.regionFilter) {
            elements.regionFilter.innerHTML = '<option value="">Tất cả khu vực địa lý</option>';
            REGIONS.forEach(reg => {
                const opt = document.createElement("option");
                opt.value = reg.code;
                opt.textContent = reg.name;
                elements.regionFilter.appendChild(opt);
            });
        }

        // 2. Populate Company Size Filter (10-50, 51-200, Enterprise...)
        if (elements.companySizeFilter) {
            elements.companySizeFilter.innerHTML = '<option value="">Tất cả quy mô nhân sự</option>';
            const sizes = [
                { id: "9", name: "Dưới 10 nhân sự (Siêu nhỏ)" },
                { id: "10", name: "10 - 50 nhân sự (Nhỏ)" },
                { id: "11", name: "51 - 200 nhân sự (Vừa)" },
                { id: "12", name: "201 - 500 nhân sự (Lớn)" },
                { id: "13", name: "Trên 500 nhân sự (Tập đoàn)" }
            ];
            sizes.forEach(s => {
                const opt = document.createElement("option");
                opt.value = s.id;
                opt.textContent = s.name;
                elements.companySizeFilter.appendChild(opt);
            });
        }
    }

    function renderViewTabs() {
        if (!elements.viewTabsContainer) return;
        elements.viewTabsContainer.innerHTML = "";

        savedViews.forEach(view => {
            const tabBtn = document.createElement("button");
            tabBtn.type = "button";
            tabBtn.className = `view-tab-btn ${view.id === activeViewId ? "active" : ""}`;
            tabBtn.dataset.viewId = view.id;

            let badgeHtml = view.badge ? `<span class="view-tab-badge">${escapeHtml(view.badge)}</span>` : "";
            let deleteHtml = !view.isPreset ? `
                <span class="btn-delete-view" title="Xóa bộ lọc đã lưu" data-view-id="${view.id}">
                    <svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </span>` : "";

            tabBtn.innerHTML = `
                <span class="view-tab-name">${escapeHtml(view.name)}</span>
                ${badgeHtml}
                ${deleteHtml}
            `;

            tabBtn.addEventListener("click", (e) => {
                const delBtn = e.target.closest(".btn-delete-view");
                if (delBtn) {
                    e.stopPropagation();
                    deleteSavedView(delBtn.dataset.viewId);
                    return;
                }
                applySavedView(view.id);
            });

            elements.viewTabsContainer.appendChild(tabBtn);
        });

        // Dropdown cho mobile/small screens
        if (elements.viewDropdown) {
            elements.viewDropdown.innerHTML = "";
            savedViews.forEach(v => {
                const opt = document.createElement("option");
                opt.value = v.id;
                opt.textContent = v.name + (v.isPreset ? "" : " (Tùy chỉnh)");
                if (v.id === activeViewId) opt.selected = true;
                elements.viewDropdown.appendChild(opt);
            });
        }
    }

    function applySavedView(viewId) {
        const found = savedViews.find(v => v.id === viewId);
        if (!found) return;

        activeViewId = viewId;
        localStorage.setItem(STORAGE_KEY_ACTIVE_VIEW, activeViewId);
        filterCriteria = { ...found.filter };

        // Cập nhật giá trị vào các controls
        if (elements.search) elements.search.value = filterCriteria.keyword || "";
        if (elements.statusFilter) elements.statusFilter.value = filterCriteria.status || "";
        if (elements.industryFilter) elements.industryFilter.value = filterCriteria.industryId || "";
        if (elements.companySizeFilter) elements.companySizeFilter.value = filterCriteria.companySizeId || "";
        if (elements.regionFilter) elements.regionFilter.value = filterCriteria.region || "";
        if (elements.ownerFilter) elements.ownerFilter.value = filterCriteria.ownerUserId || "";
        if (elements.scopeFilter) elements.scopeFilter.value = filterCriteria.scopeFilter || "ALL";

        renderViewTabs();
        renderActiveFilterChips();
        dispatchFilterChange();
    }

    function deleteSavedView(viewId) {
        if (!confirm("Bạn có chắc chắn muốn xóa bộ lọc đã lưu này?")) return;
        savedViews = savedViews.filter(v => v.id !== viewId);
        if (activeViewId === viewId) {
            activeViewId = "view_all";
            applySavedView("view_all");
        } else {
            saveUserViews();
            renderViewTabs();
        }
    }

    function openSaveViewModal() {
        if (elements.saveViewNameInput) {
            elements.saveViewNameInput.value = "";
        }
        elements.saveViewModal?.classList.add("open");
        elements.saveViewModalOverlay?.classList.add("open");
        setTimeout(() => elements.saveViewNameInput?.focus(), 100);
    }

    function closeSaveViewModal() {
        elements.saveViewModal?.classList.remove("open");
        elements.saveViewModalOverlay?.classList.remove("open");
    }

    function handleSaveCurrentView() {
        const name = elements.saveViewNameInput?.value?.trim();
        if (!name) {
            alert("Vui lòng nhập tên cho bộ lọc cần lưu.");
            return;
        }

        const newView = {
            id: "view_custom_" + Date.now(),
            name: name,
            isPreset: false,
            filter: { ...filterCriteria }
        };

        savedViews.push(newView);
        activeViewId = newView.id;
        saveUserViews();
        renderViewTabs();
        closeSaveViewModal();

        if (window.showToast) {
            window.showToast(`Đã lưu bộ lọc "${name}" thành công!`, "success");
        } else {
            alert(`Đã lưu bộ lọc "${name}" thành công!`);
        }
    }

    function renderActiveFilterChips() {
        if (!elements.activeChipsContainer) return;
        elements.activeChipsContainer.innerHTML = "";

        const chips = [];

        if (filterCriteria.keyword) {
            chips.push({ key: "keyword", label: `Từ khóa: "${filterCriteria.keyword}"` });
        }
        if (filterCriteria.status) {
            const statusNames = {
                "TIEM_NANG": "Tiềm năng",
                "DANG_GIAO_DICH": "Đang giao dịch",
                "CHINH_THUC": "Khách hàng",
                "NGUNG_HOP_TAC": "Ngừng hợp tác"
            };
            chips.push({ key: "status", label: `Trạng thái: ${statusNames[filterCriteria.status] || filterCriteria.status}` });
        }
        if (filterCriteria.industryId) {
            const opt = elements.industryFilter?.querySelector(`option[value="${filterCriteria.industryId}"]`);
            chips.push({ key: "industryId", label: `Ngành: ${opt ? opt.textContent : filterCriteria.industryId}` });
        }
        if (filterCriteria.companySizeId) {
            const opt = elements.companySizeFilter?.querySelector(`option[value="${filterCriteria.companySizeId}"]`);
            chips.push({ key: "companySizeId", label: `Quy mô: ${opt ? opt.textContent : filterCriteria.companySizeId}` });
        }
        if (filterCriteria.region) {
            const opt = elements.regionFilter?.querySelector(`option[value="${filterCriteria.region}"]`);
            chips.push({ key: "region", label: `Khu vực: ${opt ? opt.textContent.split(" (")[0] : filterCriteria.region}` });
        }
        if (filterCriteria.ownerUserId) {
            const opt = elements.ownerFilter?.querySelector(`option[value="${filterCriteria.ownerUserId}"]`);
            chips.push({ key: "ownerUserId", label: `Phụ trách: ${opt ? opt.textContent.split(" (")[0] : filterCriteria.ownerUserId}` });
        }
        if (filterCriteria.scopeFilter && filterCriteria.scopeFilter !== "ALL") {
            chips.push({ key: "scopeFilter", label: `Phạm vi: ${filterCriteria.scopeFilter === "SELF" ? "Cá nhân (SELF)" : "Nhóm (TEAM)"}` });
        }
        if (filterCriteria.isDuplicateOnly) {
            chips.push({ key: "isDuplicateOnly", label: "⚠️ Chỉ khách trùng lặp" });
        }

        if (chips.length === 0) {
            elements.activeChipsContainer.style.display = "none";
            return;
        }

        elements.activeChipsContainer.style.display = "flex";

        const labelPrefix = document.createElement("span");
        labelPrefix.className = "chips-title";
        labelPrefix.innerHTML = `<svg viewBox="0 0 24 24"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg> Đang lọc theo:`;
        elements.activeChipsContainer.appendChild(labelPrefix);

        chips.forEach(chip => {
            const chipEl = document.createElement("div");
            chipEl.className = "filter-chip";
            chipEl.innerHTML = `
                <span>${escapeHtml(chip.label)}</span>
                <button type="button" class="chip-remove-btn" title="Xóa điều kiện này" data-key="${chip.key}">
                    <svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
            `;

            chipEl.querySelector(".chip-remove-btn")?.addEventListener("click", () => {
                removeFilterKey(chip.key);
            });

            elements.activeChipsContainer.appendChild(chipEl);
        });

        // Nút xóa tất cả chip
        const clearAllBtn = document.createElement("button");
        clearAllBtn.type = "button";
        clearAllBtn.className = "btn-clear-all-chips";
        clearAllBtn.innerHTML = `
            <svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/></svg>
            Xóa tất cả bộ lọc
        `;
        clearAllBtn.addEventListener("click", resetAllFilters);
        elements.activeChipsContainer.appendChild(clearAllBtn);
    }

    function removeFilterKey(key) {
        if (key === "scopeFilter") {
            filterCriteria[key] = "ALL";
            if (elements.scopeFilter) elements.scopeFilter.value = "ALL";
        } else if (key === "isDuplicateOnly") {
            filterCriteria.isDuplicateOnly = false;
        } else {
            filterCriteria[key] = "";
            if (key === "keyword" && elements.search) elements.search.value = "";
            if (key === "status" && elements.statusFilter) elements.statusFilter.value = "";
            if (key === "industryId" && elements.industryFilter) elements.industryFilter.value = "";
            if (key === "companySizeId" && elements.companySizeFilter) elements.companySizeFilter.value = "";
            if (key === "region" && elements.regionFilter) elements.regionFilter.value = "";
            if (key === "ownerUserId" && elements.ownerFilter) elements.ownerFilter.value = "";
        }

        markCustomViewActive();
        renderActiveFilterChips();
        dispatchFilterChange();
    }

    function resetAllFilters() {
        filterCriteria = {
            keyword: "",
            status: "",
            industryId: "",
            companySizeId: "",
            region: "",
            ownerUserId: "",
            scopeFilter: "ALL"
        };

        if (elements.search) elements.search.value = "";
        if (elements.statusFilter) elements.statusFilter.value = "";
        if (elements.industryFilter) elements.industryFilter.value = "";
        if (elements.companySizeFilter) elements.companySizeFilter.value = "";
        if (elements.regionFilter) elements.regionFilter.value = "";
        if (elements.ownerFilter) elements.ownerFilter.value = "";
        if (elements.scopeFilter) elements.scopeFilter.value = "ALL";

        activeViewId = "view_all";
        localStorage.setItem(STORAGE_KEY_ACTIVE_VIEW, activeViewId);

        renderViewTabs();
        renderActiveFilterChips();
        dispatchFilterChange();
    }

    function markCustomViewActive() {
        // Kiểm tra xem tổ hợp hiện tại có khớp view nào không
        const matched = savedViews.find(v =>
            (v.filter.keyword || "") === (filterCriteria.keyword || "") &&
            (v.filter.status || "") === (filterCriteria.status || "") &&
            (v.filter.industryId || "") === (filterCriteria.industryId || "") &&
            (v.filter.companySizeId || "") === (filterCriteria.companySizeId || "") &&
            (v.filter.region || "") === (filterCriteria.region || "") &&
            (v.filter.ownerUserId || "") === (filterCriteria.ownerUserId || "") &&
            (v.filter.scopeFilter || "ALL") === (filterCriteria.scopeFilter || "ALL")
        );

        if (matched) {
            activeViewId = matched.id;
        } else {
            activeViewId = ""; // Custom / Unsaved state
        }
        localStorage.setItem(STORAGE_KEY_ACTIVE_VIEW, activeViewId);
        renderViewTabs();
    }

    function toggleAdvFilterDrawer(show) {
        if (!elements.advFilterDrawer) return;
        const willOpen = show !== undefined ? show : !elements.advFilterDrawer.classList.contains("open");
        if (willOpen) {
            elements.advFilterDrawer.classList.add("open");
            elements.advFilterDrawerOverlay?.classList.add("open");
        } else {
            elements.advFilterDrawer.classList.remove("open");
            elements.advFilterDrawerOverlay?.classList.remove("open");
        }
    }

    function bindFilterEvents() {
        // Smart Search Debounce
        elements.search?.addEventListener("input", () => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                filterCriteria.keyword = elements.search.value.trim();
                markCustomViewActive();
                renderActiveFilterChips();
                dispatchFilterChange();
            }, 300);
        });

        // Quick status
        elements.statusFilter?.addEventListener("change", () => {
            filterCriteria.status = elements.statusFilter.value;
            markCustomViewActive();
            renderActiveFilterChips();
            dispatchFilterChange();
        });

        // Quick industry
        elements.industryFilter?.addEventListener("change", () => {
            filterCriteria.industryId = elements.industryFilter.value;
            markCustomViewActive();
            renderActiveFilterChips();
            dispatchFilterChange();
        });

        // Quick scope
        elements.scopeFilter?.addEventListener("change", () => {
            filterCriteria.scopeFilter = elements.scopeFilter.value;
            markCustomViewActive();
            renderActiveFilterChips();
            dispatchFilterChange();
        });

        // Advanced filter drawer trigger
        elements.btnToggleAdvFilter?.addEventListener("click", () => toggleAdvFilterDrawer(true));
        elements.btnCloseAdvFilter?.addEventListener("click", () => toggleAdvFilterDrawer(false));
        elements.advFilterDrawerOverlay?.addEventListener("click", () => toggleAdvFilterDrawer(false));

        // Advanced Drawer Apply
        elements.btnApplyAdvFilter?.addEventListener("click", () => {
            if (elements.companySizeFilter) filterCriteria.companySizeId = elements.companySizeFilter.value;
            if (elements.regionFilter) filterCriteria.region = elements.regionFilter.value;
            if (elements.ownerFilter) filterCriteria.ownerUserId = elements.ownerFilter.value;

            markCustomViewActive();
            renderActiveFilterChips();
            toggleAdvFilterDrawer(false);
            dispatchFilterChange();
        });

        // Advanced Drawer Reset
        elements.btnResetAdvFilter?.addEventListener("click", () => {
            if (elements.companySizeFilter) elements.companySizeFilter.value = "";
            if (elements.regionFilter) elements.regionFilter.value = "";
            if (elements.ownerFilter) elements.ownerFilter.value = "";
            filterCriteria.companySizeId = "";
            filterCriteria.region = "";
            filterCriteria.ownerUserId = "";
        });

        // Reset button on toolbar
        elements.btnResetFilters?.addEventListener("click", resetAllFilters);

        // Save view modal
        elements.btnOpenSaveViewModal?.addEventListener("click", openSaveViewModal);
        elements.btnCancelSaveView?.addEventListener("click", closeSaveViewModal);
        elements.saveViewModalOverlay?.addEventListener("click", closeSaveViewModal);
        elements.btnConfirmSaveView?.addEventListener("click", handleSaveCurrentView);

        // Mobile dropdown select view
        elements.viewDropdown?.addEventListener("change", () => {
            applySavedView(elements.viewDropdown.value);
        });
    }

    function dispatchFilterChange() {
        const event = new CustomEvent("customerFilterChange", {
            detail: { ...filterCriteria }
        });
        window.dispatchEvent(event);
    }

    function syncWithGlobalState() {
        // Expose API cho customers.js
        window.CustomerFilterManager = {
            getCriteria: () => ({ ...filterCriteria }),
            setCriteria: (newCriteria) => {
                filterCriteria = { ...filterCriteria, ...newCriteria };
                markCustomViewActive();
                renderActiveFilterChips();
                dispatchFilterChange();
            },
            reset: resetAllFilters,
            updateMatchCount: (count, total) => {
                if (elements.filterMatchCountBadge) {
                    elements.filterMatchCountBadge.innerHTML = `
                        <svg viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                        Tìm thấy <strong>${count}</strong> / ${total} khách hàng phù hợp
                    `;
                    elements.filterMatchCountBadge.style.display = "inline-flex";
                }
            },
            populateOwners: (users) => {
                if (!elements.ownerFilter) return;
                elements.ownerFilter.innerHTML = '<option value="">Tất cả người phụ trách (Sale)</option>';
                users.forEach(u => {
                    const opt = document.createElement("option");
                    opt.value = String(u.id);
                    opt.textContent = `${u.fullName || u.email} (${u.teamName || "Chưa phân nhóm"})`;
                    elements.ownerFilter.appendChild(opt);
                });
            }
        };
    }

    function escapeHtml(str) {
        if (!str) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    // Tự động khởi tạo khi DOM sẵn sàng
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initFilterEngine);
    } else {
        initFilterEngine();
    }
})();
