/**
 * ===================================================================
 * ENTERPRISE MULTI-CRITERIA FILTER & SAVED VIEWS ENGINE (CRM-67)
 * (Synchronized for SPA Page Transition)
 * ===================================================================
 */

"use strict";

(function () {

    // Danh mục tỉnh thành / khu vực địa lý Việt Nam
    const REGIONS = [
        { code: "MB", name: "Miền Bắc (Hà Nội, Hải Phòng, Quảng Ninh, Thái Nguyên...)" },
        { code: "MT", name: "Miền Trung (Đà Nẵng, Huế, Khánh Hòa, Nghệ An...)" },
        { code: "MN", name: "Miền Nam (TP.HCM, Bình Dương, Đồng Nai, Cần Thơ...)" }
    ];

    // Cấu hình View mặc định của hệ thống
    const DEFAULT_PRESET_VIEWS = [{id:"view_all",name:"Tất cả khách hàng",isPreset:true,filter:{}}];

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

    async function initFilterEngine() {
        cacheElements();
        try { await loadSavedViews(); } catch(error) { alert(error.message); }
        populateFilterDropdowns();
        bindFilterEvents();
        renderViewTabs();
        renderActiveFilterChips();
        syncWithGlobalState();
        try { const data=await CrmApi.request("/api/customer-lookups"); window.CustomerFilterManager.populateCatalogs(data);window.CustomerFilterManager.populateOwners(data.owners); } catch(error) { showToast(error.message,"error"); }
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

    async function loadSavedViews() {
        const views=[];
        for (let page=1;;page++) {
            const data=await CrmApi.request(`/api/saved-filters?entity=CUSTOMER&page=${page}&size=100`);
            views.push(...data);
            if (data.length<100) break;
        }
        savedViews=[...DEFAULT_PRESET_VIEWS,...views.map(v=>({id:String(v.id),name:v.name,isPreset:false,filter:{keyword:v.criteria.keyword,status:v.criteria.status,industryId:v.criteria.industry,companySizeId:v.criteria.size,region:v.criteria.region,ownerUserId:v.criteria.ownerId}}))];
        if (!savedViews.some(v=>v.id===activeViewId)) activeViewId="view_all";
    }

    function saveUserViews() {
        // Saved filters are persisted only by POST/DELETE /api/saved-filters.
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

    async function deleteSavedView(viewId) {
        if (!confirm("Xóa bộ lọc đã lưu?")) return;
        try {
            await CrmApi.request(`/api/saved-filters/${viewId}`,{method:"DELETE"});
            await loadSavedViews();
            applySavedView("view_all");
            renderViewTabs();
        } catch(error) { alert(error.message); }
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

    async function handleSaveCurrentView() {
        const name=elements.saveViewNameInput?.value.trim();
        if (!name) { alert("Vui lòng nhập tên bộ lọc."); return; }
        const criteria={keyword:filterCriteria.keyword,status:filterCriteria.status,industry:filterCriteria.industryId,size:filterCriteria.companySizeId,region:filterCriteria.region,ownerId:filterCriteria.ownerUserId};
        for (const key of Object.keys(criteria)) if (!criteria[key]) delete criteria[key];
        elements.btnConfirmSaveView.disabled=true;
        try {
            const saved=await CrmApi.request("/api/saved-filters",{method:"POST",body:JSON.stringify({entity:"CUSTOMER",name,criteria})});
            await loadSavedViews();
            activeViewId=String(saved.id);
            renderViewTabs(); closeSaveViewModal();
        } catch(error) { alert(error.message); }
        finally { elements.btnConfirmSaveView.disabled=false; }
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
            populateCatalogs: data => {
                if (elements.companySizeFilter) elements.companySizeFilter.innerHTML='<option value="">Tất cả quy mô</option>'+data.companySizes.map(item=>`<option value="${item.id}">${escapeHtml(item.name)}</option>`).join("");
            },
            populateOwners: (users) => {
                if (!elements.ownerFilter) return;
                elements.ownerFilter.innerHTML = '<option value="">Tất cả người phụ trách (Sale)</option>';
                users.forEach(u => {
                    const opt = document.createElement("option");
                    opt.value = String(u.id);
                    opt.textContent = u.fullName || u.email || `Người dùng #${u.id}`;
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
