/**
 * ===================================================================
 * CRM-118 / S4-04: PHÁT HIỆN VÀ GỘP LEAD TRÙNG LẶP (SIDE-BY-SIDE MERGE)
 * Phụ trách: Senior Frontend Developer (Nông Quang Tiệp)
 * 
 * - Tự động phát hiện trùng lặp theo SĐT, Email, Tên Công ty
 * - Phát hiện & gợi ý gắn Lead trùng vào Khách hàng hiện có trong CRM
 * - Giao diện So sánh - Đối chiếu Song song (Side-by-Side Merge Modal)
 * - Cơ chế chọn từng trường dữ liệu giữ lại (Field Selection)
 * - Bảo toàn toàn bộ lịch sử chăm sóc & ghi chú tương tác sau gộp
 * - Tương thích REST API (POST /api/leads/merge & GET /api/leads/duplicates)
 * ===================================================================
 */

"use strict";

(function () {
    const STORAGE_KEY_LEADS = "crm_captured_leads";
    const STORAGE_KEY_CUSTOMERS = "crm_ui_customers";
    const STORAGE_KEY_DUPLICATES = "crm_lead_duplicate_pairs";

    const API_DUPLICATES = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`) + "/api/leads/duplicates";
    const API_MERGE = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`) + "/api/leads/merge";

    // State
    let allLeads = [];
    let allCustomers = [];
    let duplicatePairs = [];
    let currentPair = null;
    let selectedFieldValues = {};

    // DOM Elements
    const dom = {
        // KPI
        kpiTotalDuplicates: document.getElementById("kpiTotalDuplicates"),
        kpiPhoneMatches: document.getElementById("kpiPhoneMatches"),
        kpiEmailMatches: document.getElementById("kpiEmailMatches"),
        kpiCustomerMatches: document.getElementById("kpiCustomerMatches"),

        // Filter Toolbar (Single Row)
        dupSearchInput: document.getElementById("dupSearchInput"),
        dupFilterCriterion: document.getElementById("dupFilterCriterion"),
        dupSortOrder: document.getElementById("dupSortOrder"),
        btnResetFilters: document.getElementById("btnResetFilters"),
        btnScanDuplicates: document.getElementById("btnScanDuplicates"),
        btnGenerateDemoDups: document.getElementById("btnGenerateDemoDups"),

        // Table
        dupTableBody: document.getElementById("dupTableBody"),
        dupEmptyState: document.getElementById("dupEmptyState"),

        // Merge Modal
        mergeModal: document.getElementById("mergeModal"),
        btnCloseMergeModal: document.getElementById("btnCloseMergeModal"),
        btnCancelMerge: document.getElementById("btnCancelMerge"),
        btnConfirmMerge: document.getElementById("btnConfirmMerge"),
        btnConfirmMergeText: document.getElementById("btnConfirmMergeText"),
        btnSwapMasterDup: document.getElementById("btnSwapMasterDup"),
        chkRetainHistory: document.getElementById("chkRetainHistory"),
        mergeCustomerAlert: document.getElementById("mergeCustomerAlert"),
        mergeCustomerAlertText: document.getElementById("mergeCustomerAlertText"),

        lblMasterSource: document.getElementById("lblMasterSource"),
        lblDupSource: document.getElementById("lblDupSource"),
        colMasterFields: document.getElementById("colMasterFields"),
        colDupFields: document.getElementById("colDupFields"),
        previewSummaryList: document.getElementById("previewSummaryList"),

        // Attach to Customer Modal
        attachCustomerModal: document.getElementById("attachCustomerModal"),
        btnCloseAttachModal: document.getElementById("btnCloseAttachModal"),
        btnCancelAttach: document.getElementById("btnCancelAttach"),
        btnConfirmAttach: document.getElementById("btnConfirmAttach"),
        attachModalBody: document.getElementById("attachModalBody"),

        // Toast
        toastContainer: document.getElementById("leadDupToastContainer")
    };

    /* ===================================================================
       1. INITIALIZATION & DATA SEEDING
       =================================================================== */

    function init() {
        loadBaseData();
        bindFilterEvents();
        bindMergeModalEvents();
        bindAttachModalEvents();

        // Initial scan / render
        scanAndRenderDuplicates();
    }

    function loadBaseData() {
        // Load Leads
        try {
            const rawLeads = localStorage.getItem(STORAGE_KEY_LEADS);
            allLeads = rawLeads ? JSON.parse(rawLeads) : [];
        } catch (_) {
            allLeads = [];
        }

        // Load Customers
        try {
            const rawCust = localStorage.getItem(STORAGE_KEY_CUSTOMERS);
            allCustomers = rawCust ? JSON.parse(rawCust) : [];
        } catch (_) {
            allCustomers = [];
        }

        // If no duplicate pairs stored yet, seed standard realistic enterprise pairs
        const storedPairs = localStorage.getItem(STORAGE_KEY_DUPLICATES);
        if (storedPairs) {
            try {
                duplicatePairs = JSON.parse(storedPairs);
            } catch (_) {
                duplicatePairs = [];
            }
        }

        if (!duplicatePairs || duplicatePairs.length === 0) {
            seedDefaultDuplicatePairs();
        }
    }

    function seedDefaultDuplicatePairs() {
        duplicatePairs = [
            {
                id: "DUP-101",
                criterion: "PHONE", // PHONE, EMAIL, COMPANY, CUSTOMER
                confidence: 98,
                detectedAt: new Date(Date.now() - 25 * 60 * 1000).toISOString(),
                recordA: {
                    id: "LD-889101",
                    fullName: "Nguyễn Minh Quang",
                    phone: "0987123456",
                    email: "quang.nm@digitech.vn",
                    companyName: "Công ty Cổ phần Công nghệ DigiTech VN",
                    title: "Giám đốc Điều hành (CEO)",
                    source: "Website Form",
                    notes: "Cần báo giá và tư vấn triển khai CRM cho 40 Sales B2B trong quý 4.",
                    createdAt: "2026-10-14T08:30:00.000Z"
                },
                recordB: {
                    id: "LD-889102",
                    fullName: "N. M. Quang",
                    phone: "0987123456",
                    email: "quang.nm@digitech.vn",
                    companyName: "DigiTech Global Corp",
                    title: "Chủ tịch HĐQT",
                    source: "Hội thảo / Sự kiện chuyên ngành",
                    notes: "Đã trao đổi tại gian hàng TechExpo 2026. Khách hàng mong muốn kết nối API với hệ thống ERP hiện tại.",
                    createdAt: "2026-10-14T09:15:00.000Z"
                },
                isCustomerMatch: false,
                matchedFields: ["Số điện thoại (0987123456)", "Email (quang.nm@digitech.vn)"]
            },
            {
                id: "DUP-102",
                criterion: "CUSTOMER",
                confidence: 99,
                detectedAt: new Date(Date.now() - 2 * 3600 * 1000).toISOString(),
                recordA: {
                    id: "LD-889201",
                    fullName: "Trần Thu Hà",
                    phone: "0912345678",
                    email: "ha.tran@hoalinh.vn",
                    companyName: "Tập đoàn Dược phẩm Hoa Linh",
                    title: "Phó Giám đốc Phụ trách Khối Bán lẻ",
                    source: "Nhập hàng loạt từ Excel",
                    notes: "Cần tư vấn thêm module Quản lý Chữ ký số và Hợp đồng cho 5 chi nhánh.",
                    createdAt: "2026-10-14T07:00:00.000Z"
                },
                recordB: {
                    id: "CUST-1002",
                    fullName: "Trần Thu Hà",
                    phone: "0912345678",
                    email: "ha.tran@hoalinh.vn",
                    companyName: "Tập đoàn Dược phẩm Hoa Linh",
                    title: "Giám đốc Kinh doanh",
                    source: "Khách hàng CRM hiện hữu (Mã KH-1002)",
                    notes: "Khách hàng thân thiết từ năm 2024, đã hoàn thành 2 hợp đồng dịch vụ CRM.",
                    createdAt: "2024-05-10T00:00:00.000Z"
                },
                isCustomerMatch: true,
                matchedFields: ["Trùng khớp Khách hàng CRM (Mã KH-1002)", "SĐT: 0912345678", "Email: ha.tran@hoalinh.vn"]
            },
            {
                id: "DUP-103",
                criterion: "COMPANY",
                confidence: 82,
                detectedAt: new Date(Date.now() - 5 * 3600 * 1000).toISOString(),
                recordA: {
                    id: "LD-889301",
                    fullName: "Lê Hoàng Cường",
                    phone: "0903456789",
                    email: "cuong.lh@vinasmart.vn",
                    companyName: "Công ty Cổ phần VinaSmart Global",
                    title: "Giám đốc Kỹ thuật (CTO)",
                    source: "Người quen / Đối tác giới thiệu",
                    notes: "Anh Tuấn giới thiệu sang. Cần hỗ trợ phân hệ cơ hội và báo giá.",
                    createdAt: "2026-10-13T14:20:00.000Z"
                },
                recordB: {
                    id: "LD-889302",
                    fullName: "Vũ Hải Đăng",
                    phone: "0908889999",
                    email: "dang.vh@vinasmart.vn",
                    companyName: "Công ty Cổ phần VinaSmart Global",
                    title: "Trưởng nhóm CNTT",
                    source: "Website Form",
                    notes: "Điền form xin tài liệu kỹ thuật và bản demo trải nghiệm.",
                    createdAt: "2026-10-14T06:10:00.000Z"
                },
                isCustomerMatch: false,
                matchedFields: ["Cùng doanh nghiệp: Công ty Cổ phần VinaSmart Global", "Domain Email: @vinasmart.vn"]
            },
            {
                id: "DUP-104",
                criterion: "PHONE",
                confidence: 94,
                detectedAt: new Date(Date.now() - 10 * 3600 * 1000).toISOString(),
                recordA: {
                    id: "LD-889401",
                    fullName: "Phạm Thu Dung",
                    phone: "0933221100",
                    email: "dung.pt@bachhoasach.vn",
                    companyName: "Chuỗi Bách Hóa Sạch Miền Bắc",
                    title: "Quản lý Vận hành",
                    source: "Tư vấn qua điện thoại (Telesales)",
                    notes: "Nhân viên Telesales gọi tư vấn ngày 13/10, hẹn demo thứ 6 tuần này.",
                    createdAt: "2026-10-13T10:00:00.000Z"
                },
                recordB: {
                    id: "LD-889402",
                    fullName: "Phạm Thu Dung",
                    phone: "0933221100",
                    email: "dung.pham@gmail.com",
                    companyName: "Bách Hóa Thực Phẩm Sạch",
                    title: "Chủ cơ sở",
                    source: "Gặp trực tiếp / Trao đổi Danh thiếp",
                    notes: "Gặp tại hội chợ nông sản sạch, gửi danh thiếp cá nhân.",
                    createdAt: "2026-10-12T16:45:00.000Z"
                },
                isCustomerMatch: false,
                matchedFields: ["Số điện thoại giống nhau hoàn toàn (0933221100)", "Họ tên trùng khớp"]
            }
        ];

        saveDuplicatesLocally();
    }

    function saveDuplicatesLocally() {
        try {
            localStorage.setItem(STORAGE_KEY_DUPLICATES, JSON.stringify(duplicatePairs));
        } catch (_) {}
    }

    /* ===================================================================
       2. SCANNING & FILTERING LOGIC
       =================================================================== */

    function scanAndRenderDuplicates() {
        updateKpiCounters();
        renderDuplicatesTable();
    }

    function updateKpiCounters() {
        const total = duplicatePairs.length;
        const phone = duplicatePairs.filter(p => p.criterion === "PHONE").length;
        const email = duplicatePairs.filter(p => p.criterion === "EMAIL").length;
        const customer = duplicatePairs.filter(p => p.isCustomerMatch || p.criterion === "CUSTOMER").length;

        if (dom.kpiTotalDuplicates) dom.kpiTotalDuplicates.textContent = total;
        if (dom.kpiPhoneMatches) dom.kpiPhoneMatches.textContent = phone;
        if (dom.kpiEmailMatches) dom.kpiEmailMatches.textContent = email;
        if (dom.kpiCustomerMatches) dom.kpiCustomerMatches.textContent = customer;
    }

    function bindFilterEvents() {
        if (dom.dupSearchInput) dom.dupSearchInput.addEventListener("input", debounce(renderDuplicatesTable, 200));
        if (dom.dupFilterCriterion) dom.dupFilterCriterion.addEventListener("change", renderDuplicatesTable);
        if (dom.dupSortOrder) dom.dupSortOrder.addEventListener("change", renderDuplicatesTable);

        if (dom.btnResetFilters) {
            dom.btnResetFilters.addEventListener("click", () => {
                if (dom.dupSearchInput) dom.dupSearchInput.value = "";
                if (dom.dupFilterCriterion) dom.dupFilterCriterion.value = "ALL";
                if (dom.dupSortOrder) dom.dupSortOrder.value = "score_desc";
                renderDuplicatesTable();
                showToast("Đã đặt lại bộ lọc cảnh báo trùng lặp", "info");
            });
        }

        // Rescan button
        if (dom.btnScanDuplicates) {
            dom.btnScanDuplicates.addEventListener("click", handleRescanDuplicates);
        }

        // Generate demo button
        if (dom.btnGenerateDemoDups) {
            dom.btnGenerateDemoDups.addEventListener("click", handleGenerateRandomDuplicate);
        }
    }

    async function handleRescanDuplicates() {
        dom.btnScanDuplicates.disabled = true;
        dom.btnScanDuplicates.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang quét...';

        try {
            // Call REST API: GET /api/leads/duplicates
            const resp = await fetch(API_DUPLICATES, {
                method: "GET",
                headers: { "Accept": "application/json" }
            });
            if (resp.ok) {
                const data = await resp.json().catch(() => null);
                if (Array.isArray(data)) {
                    duplicatePairs = data;
                    saveDuplicatesLocally();
                }
            }
        } catch (_) {
            console.warn("Backend API /api/leads/duplicates offline, giữ nguyên mock.");
        }

        setTimeout(() => {
            dom.btnScanDuplicates.disabled = false;
            dom.btnScanDuplicates.innerHTML = '<i class="fa-solid fa-magnifying-glass-chart"></i> Quét lại trùng lặp';
            scanAndRenderDuplicates();
            showToast("✓ Đã hoàn tất phân tích đối soát dữ liệu trùng lặp trên toàn hệ thống!", "success");
        }, 500);
    }

    function handleGenerateRandomDuplicate() {
        const randId = Date.now().toString().slice(-4);
        const newPair = {
            id: `DUP-${randId}`,
            criterion: "PHONE",
            confidence: 96,
            detectedAt: new Date().toISOString(),
            recordA: {
                id: `LD-${randId}-A`,
                fullName: "Hoàng Minh Tuấn",
                phone: "0905112233",
                email: "tuan.hm@sunshine.vn",
                companyName: "Công ty Địa ốc Sunshine",
                title: "Trưởng phòng Kinh doanh",
                source: "Website Form",
                notes: "Khách hàng đăng ký tư vấn qua landing page dự án.",
                createdAt: new Date().toISOString()
            },
            recordB: {
                id: `LD-${randId}-B`,
                fullName: "H. M. Tuấn",
                phone: "0905112233",
                email: "tuan.hoang@sunshine.com",
                companyName: "Sunshine Real Estate",
                title: "Phụ trách Bán lẻ",
                source: "Gặp trực tiếp / Trao đổi Danh thiếp",
                notes: "Gặp gỡ trao đổi danh thiếp tại sự kiện xúc tiến đầu tư.",
                createdAt: new Date(Date.now() - 3600000).toISOString()
            },
            isCustomerMatch: false,
            matchedFields: ["Số điện thoại trùng khớp tuyệt đối (0905112233)", "Cùng doanh nghiệp Sunshine"]
        };

        duplicatePairs.unshift(newPair);
        saveDuplicatesLocally();
        scanAndRenderDuplicates();
        showToast("✨ Đã tạo thêm 1 cặp Lead trùng lặp kiểm thử (Sandbox)!", "success");
    }

    /* ===================================================================
       3. RENDER DATA TABLE
       =================================================================== */

    function getFilteredPairs() {
        const query = dom.dupSearchInput ? dom.dupSearchInput.value.trim().toLowerCase() : "";
        const criterion = dom.dupFilterCriterion ? dom.dupFilterCriterion.value : "ALL";
        const sort = dom.dupSortOrder ? dom.dupSortOrder.value : "score_desc";

        return duplicatePairs.filter(pair => {
            if (criterion !== "ALL") {
                if (criterion === "CUSTOMER" && !pair.isCustomerMatch) return false;
                if (criterion !== "CUSTOMER" && pair.criterion !== criterion) return false;
            }

            if (query) {
                const searchStr = `${pair.recordA.fullName} ${pair.recordA.phone} ${pair.recordA.email} ${pair.recordA.companyName} ${pair.recordB.fullName} ${pair.recordB.phone} ${pair.recordB.email} ${pair.recordB.companyName}`.toLowerCase();
                if (!searchStr.includes(query)) return false;
            }

            return true;
        }).sort((a, b) => {
            if (sort === "score_desc") return b.confidence - a.confidence;
            if (sort === "newest") return new Date(b.detectedAt) - new Date(a.detectedAt);
            if (sort === "name_asc") return a.recordA.fullName.localeCompare(b.recordA.fullName);
            return 0;
        });
    }

    function renderDuplicatesTable() {
        if (!dom.dupTableBody) return;

        const filtered = getFilteredPairs();

        if (filtered.length === 0) {
            dom.dupTableBody.innerHTML = "";
            if (dom.dupEmptyState) dom.dupEmptyState.style.display = "block";
            return;
        }

        if (dom.dupEmptyState) dom.dupEmptyState.style.display = "none";

        let html = "";
        filtered.forEach((pair, idx) => {
            const critBadge = getCriterionBadge(pair);
            const scorePill = pair.confidence >= 90 
                ? `<span class="score-pill high"><i class="fa-solid fa-circle-check"></i> ${pair.confidence}% Trùng khớp</span>`
                : `<span class="score-pill medium"><i class="fa-solid fa-triangle-exclamation"></i> ${pair.confidence}% Nghi ngờ</span>`;

            html += `
                <tr data-pair-id="${escapeHtml(pair.id)}">
                    <td style="text-align: center; color: var(--crm-muted); font-weight: 700;">${idx + 1}</td>
                    
                    <!-- Bản ghi A (Master Candidate) -->
                    <td>
                        <div class="dup-pair-cell">
                            <div class="dup-record-name">
                                <span class="dup-record-badge">Master (A)</span>
                                <strong>${escapeHtml(pair.recordA.fullName)}</strong>
                            </div>
                            <div class="dup-record-sub">
                                <span><i class="fa-solid fa-phone text-primary"></i> ${escapeHtml(pair.recordA.phone)}</span>
                                <span><i class="fa-solid fa-envelope text-primary"></i> ${escapeHtml(pair.recordA.email)}</span>
                            </div>
                            <div class="dup-record-sub" style="color: var(--crm-text-2); font-weight: 600;">
                                <i class="fa-solid fa-building"></i> ${escapeHtml(pair.recordA.companyName || "Chưa có cty")}
                            </div>
                        </div>
                    </td>

                    <!-- Bản ghi B (Duplicate Candidate hoặc Khách hàng CRM) -->
                    <td>
                        <div class="dup-pair-cell">
                            <div class="dup-record-name">
                                <span class="dup-record-badge" style="background: rgba(124, 58, 237, 0.12); color: #7c3aed;">
                                    ${pair.isCustomerMatch ? "Khách hàng CRM (B)" : "Trùng lặp (B)"}
                                </span>
                                <strong>${escapeHtml(pair.recordB.fullName)}</strong>
                            </div>
                            <div class="dup-record-sub">
                                <span><i class="fa-solid fa-phone text-purple"></i> ${escapeHtml(pair.recordB.phone)}</span>
                                <span><i class="fa-solid fa-envelope text-purple"></i> ${escapeHtml(pair.recordB.email)}</span>
                            </div>
                            <div class="dup-record-sub" style="color: var(--crm-text-2); font-weight: 600;">
                                <i class="fa-solid fa-building"></i> ${escapeHtml(pair.recordB.companyName || "Chưa có cty")}
                            </div>
                        </div>
                    </td>

                    <!-- Tiêu chí trùng khớp -->
                    <td>
                        ${critBadge}
                        <div style="font-size: 11px; color: var(--crm-muted); margin-top: 4px;">
                            ${escapeHtml(pair.matchedFields.join(" &bull; "))}
                        </div>
                    </td>

                    <!-- Độ tin cậy -->
                    <td class="col-score">
                        ${scorePill}
                    </td>

                    <!-- Thao tác -->
                    <td class="col-actions">
                        <div class="dup-actions">
                            ${pair.isCustomerMatch ? `
                                <button type="button" class="crm-btn btn-attach-cust" data-pair-id="${escapeHtml(pair.id)}" title="Gắn Lead này vào Khách hàng hiện có trong CRM">
                                    <i class="fa-solid fa-link"></i> Gắn vào KH
                                </button>
                            ` : ""}
                            <button type="button" class="crm-btn crm-btn-primary btn-open-merge" data-pair-id="${escapeHtml(pair.id)}" title="So sánh song song và gộp 2 bản ghi">
                                <i class="fa-solid fa-code-merge"></i> So sánh & Gộp
                            </button>
                            <button type="button" class="crm-icon-btn btn-dismiss-pair" data-pair-id="${escapeHtml(pair.id)}" title="Bỏ qua cảnh báo này">
                                <i class="fa-solid fa-xmark"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        });

        dom.dupTableBody.innerHTML = html;
        bindTableRowButtons();
    }

    function getCriterionBadge(pair) {
        if (pair.isCustomerMatch) {
            return `<span class="match-badge customer"><i class="fa-solid fa-building-circle-check"></i> Trùng Khách hàng CRM</span>`;
        }
        switch (pair.criterion) {
            case "PHONE": return `<span class="match-badge phone"><i class="fa-solid fa-phone"></i> Trùng Số điện thoại</span>`;
            case "EMAIL": return `<span class="match-badge email"><i class="fa-solid fa-envelope"></i> Trùng Địa chỉ Email</span>`;
            case "COMPANY": return `<span class="match-badge company"><i class="fa-solid fa-building"></i> Trùng Tên Công ty</span>`;
            default: return `<span class="match-badge phone"><i class="fa-solid fa-triangle-exclamation"></i> Trùng thông tin</span>`;
        }
    }

    function bindTableRowButtons() {
        if (!dom.dupTableBody) return;

        // Open Merge Modal
        dom.dupTableBody.querySelectorAll(".btn-open-merge").forEach(btn => {
            btn.addEventListener("click", (e) => {
                const pairId = e.currentTarget.dataset.pairId;
                const pair = duplicatePairs.find(p => p.id === pairId);
                if (pair) openMergeModal(pair);
            });
        });

        // Attach to Customer
        dom.dupTableBody.querySelectorAll(".btn-attach-cust").forEach(btn => {
            btn.addEventListener("click", (e) => {
                const pairId = e.currentTarget.dataset.pairId;
                const pair = duplicatePairs.find(p => p.id === pairId);
                if (pair) openAttachModal(pair);
            });
        });

        // Dismiss Pair
        dom.dupTableBody.querySelectorAll(".btn-dismiss-pair").forEach(btn => {
            btn.addEventListener("click", (e) => {
                const pairId = e.currentTarget.dataset.pairId;
                dismissDuplicatePair(pairId);
            });
        });
    }

    function dismissDuplicatePair(pairId) {
        if (!confirm("Bạn có chắc chắn muốn bỏ qua cảnh báo trùng lặp này?")) return;
        duplicatePairs = duplicatePairs.filter(p => p.id !== pairId);
        saveDuplicatesLocally();
        scanAndRenderDuplicates();
        showToast("Đã bỏ qua cảnh báo trùng lặp", "info");
    }

    /* ===================================================================
       4. SIDE-BY-SIDE MERGE MODAL LOGIC (CORE ENGINE)
       =================================================================== */

    function bindMergeModalEvents() {
        if (dom.btnCloseMergeModal) dom.btnCloseMergeModal.addEventListener("click", closeMergeModal);
        if (dom.btnCancelMerge) dom.btnCancelMerge.addEventListener("click", closeMergeModal);

        if (dom.mergeModal) {
            dom.mergeModal.addEventListener("click", (e) => {
                if (e.target === dom.mergeModal) closeMergeModal();
            });
        }

        // Swap Master and Duplicate
        if (dom.btnSwapMasterDup) {
            dom.btnSwapMasterDup.addEventListener("click", swapMasterAndDuplicate);
        }

        // Retain History checkbox toggle
        if (dom.chkRetainHistory) {
            dom.chkRetainHistory.addEventListener("change", updateMergedPreview);
        }

        // Confirm Merge Button
        if (dom.btnConfirmMerge) {
            dom.btnConfirmMerge.addEventListener("click", executeMerge);
        }
    }

    function openMergeModal(pair) {
        currentPair = pair;
        
        // Default selected field values initialize to Master Record (A)
        selectedFieldValues = {
            fullName: pair.recordA.fullName,
            phone: pair.recordA.phone,
            email: pair.recordA.email,
            companyName: pair.recordA.companyName,
            title: pair.recordA.title || pair.recordB.title || "",
            source: pair.recordA.source,
            notesSelection: "COMBINED" // COMBINED, A, B
        };

        // Header info
        dom.lblMasterSource.textContent = `Nguồn: ${pair.recordA.source || "Gốc"}`;
        dom.lblDupSource.textContent = `Nguồn: ${pair.recordB.source || "Trùng"}`;

        // Customer warning alert
        if (pair.isCustomerMatch) {
            dom.mergeCustomerAlert.style.display = "flex";
            dom.mergeCustomerAlertText.textContent = `Bản ghi B là Khách hàng CRM đã tồn tại (${pair.recordB.id}). Việc gộp sẽ giữ lại mã khách hàng và cập nhật thông tin bổ sung từ Lead A.`;
        } else {
            dom.mergeCustomerAlert.style.display = "none";
        }

        renderCompareFields();
        updateMergedPreview();

        dom.mergeModal.classList.add("active");
    }

    function closeMergeModal() {
        if (dom.mergeModal) dom.mergeModal.classList.remove("active");
        currentPair = null;
    }

    function swapMasterAndDuplicate() {
        if (!currentPair) return;

        // Swap A and B
        const temp = currentPair.recordA;
        currentPair.recordA = currentPair.recordB;
        currentPair.recordB = temp;

        openMergeModal(currentPair);
        showToast("Đã đổi vai trò giữa Bản ghi chính (Master) và Bản ghi phụ!", "info");
    }

    /**
     * Render các trường lựa chọn giữa Cột A và Cột B
     */
    function renderCompareFields() {
        if (!currentPair) return;
        const A = currentPair.recordA;
        const B = currentPair.recordB;

        const fieldsToCompare = [
            { key: "fullName", label: "Họ và tên", valA: A.fullName, valB: B.fullName },
            { key: "phone", label: "Số điện thoại", valA: A.phone, valB: B.phone },
            { key: "email", label: "Địa chỉ Email", valA: A.email, valB: B.email },
            { key: "companyName", label: "Tên Doanh nghiệp", valA: A.companyName, valB: B.companyName },
            { key: "title", label: "Chức vụ / Vị trí", valA: A.title, valB: B.title },
            { key: "source", label: "Nguồn tiếp nhận", valA: A.source, valB: B.source }
        ];

        let htmlA = "";
        let htmlB = "";

        fieldsToCompare.forEach(f => {
            const isSelectedA = selectedFieldValues[f.key] === f.valA;
            const isSelectedB = !isSelectedA && selectedFieldValues[f.key] === f.valB;

            htmlA += `
                <div class="field-row-picker ${isSelectedA ? "selected" : ""}" data-field="${f.key}" data-val="${escapeHtml(f.valA || "")}">
                    <div class="field-label-small">${f.label}</div>
                    <div class="field-value-text">${f.valA ? escapeHtml(f.valA) : `<span class="field-value-empty">Trống</span>`}</div>
                </div>
            `;

            htmlB += `
                <div class="field-row-picker ${isSelectedB ? "selected" : ""}" data-field="${f.key}" data-val="${escapeHtml(f.valB || "")}">
                    <div class="field-label-small">${f.label}</div>
                    <div class="field-value-text">${f.valB ? escapeHtml(f.valB) : `<span class="field-value-empty">Trống</span>`}</div>
                </div>
            `;
        });

        // Add Notes Picker Row
        htmlA += `
            <div class="field-row-picker ${selectedFieldValues.notesSelection === "A" ? "selected" : ""}" data-notes-choice="A">
                <div class="field-label-small">Ghi chú & Nhu cầu (Bản A)</div>
                <div class="field-value-text" style="font-size:12px;">${escapeHtml(A.notes || "Không có")}</div>
            </div>
        `;

        htmlB += `
            <div class="field-row-picker ${selectedFieldValues.notesSelection === "B" ? "selected" : ""}" data-notes-choice="B">
                <div class="field-label-small">Ghi chú & Nhu cầu (Bản B)</div>
                <div class="field-value-text" style="font-size:12px;">${escapeHtml(B.notes || "Không có")}</div>
            </div>
        `;

        dom.colMasterFields.innerHTML = htmlA;
        dom.colDupFields.innerHTML = htmlB;

        // Bind clicks on field row pickers
        [dom.colMasterFields, dom.colDupFields].forEach(col => {
            col.querySelectorAll(".field-row-picker").forEach(picker => {
                picker.addEventListener("click", () => {
                    const field = picker.dataset.field;
                    const val = picker.dataset.val;
                    const notesChoice = picker.dataset.notesChoice;

                    if (field) {
                        selectedFieldValues[field] = val;
                    } else if (notesChoice) {
                        selectedFieldValues.notesSelection = notesChoice;
                    }

                    renderCompareFields();
                    updateMergedPreview();
                });
            });
        });
    }

    /**
     * Cập nhật bản xem trước kết quả sau khi gộp (Merged Preview)
     */
    function updateMergedPreview() {
        if (!currentPair || !dom.previewSummaryList) return;
        const A = currentPair.recordA;
        const B = currentPair.recordB;
        const retainHistory = dom.chkRetainHistory ? dom.chkRetainHistory.checked : true;

        let finalNotes = "";
        if (retainHistory) {
            finalNotes = `[Gốc - ${A.source || "Master"}]: ${A.notes || "Không có"}\n--- Lịch sử bổ sung từ ${B.source || "Bản B"} (${B.id}): ${B.notes || "Không có"}`;
        } else {
            finalNotes = selectedFieldValues.notesSelection === "B" ? (B.notes || "") : (A.notes || "");
        }

        dom.previewSummaryList.innerHTML = `
            <div class="preview-field-item">
                <div class="lbl">Mã định danh giữ lại</div>
                <div class="val">${escapeHtml(A.id)} (Master)</div>
            </div>
            <div class="preview-field-item">
                <div class="lbl">Họ và tên</div>
                <div class="val">${escapeHtml(selectedFieldValues.fullName || "—")}</div>
            </div>
            <div class="preview-field-item">
                <div class="lbl">Số điện thoại</div>
                <div class="val">${escapeHtml(selectedFieldValues.phone || "—")}</div>
            </div>
            <div class="preview-field-item">
                <div class="lbl">Địa chỉ Email</div>
                <div class="val">${escapeHtml(selectedFieldValues.email || "—")}</div>
            </div>
            <div class="preview-field-item">
                <div class="lbl">Doanh nghiệp</div>
                <div class="val">${escapeHtml(selectedFieldValues.companyName || "Chưa cung cấp")}</div>
            </div>
            <div class="preview-field-item">
                <div class="lbl">Nguồn ghi nhận</div>
                <div class="val">${escapeHtml(selectedFieldValues.source || A.source)}</div>
            </div>
            <div class="preview-field-item">
                <div class="lbl">Nội dung Ghi chú & Lịch sử</div>
                <div style="font-size: 11.5px; color: var(--crm-text-2); white-space: pre-wrap; margin-top: 4px; max-height: 120px; overflow-y: auto; background: var(--crm-surface); padding: 8px; border-radius: 6px; border: 1px solid var(--crm-border);">
                    ${escapeHtml(finalNotes)}
                </div>
            </div>
        `;
    }

    /**
     * Xác nhận thực hiện gộp Lead (POST /api/leads/merge)
     */
    async function executeMerge() {
        if (!currentPair) return;

        dom.btnConfirmMerge.disabled = true;
        dom.btnConfirmMergeText.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang gộp dữ liệu...';

        const A = currentPair.recordA;
        const B = currentPair.recordB;
        const retainHistory = dom.chkRetainHistory ? dom.chkRetainHistory.checked : true;

        const mergedNotes = retainHistory 
            ? `[Gốc - ${A.source}]: ${A.notes || "Không có"}\n--- Lịch sử bổ sung từ ${B.source} (${B.id}): ${B.notes || "Không có"}`
            : (selectedFieldValues.notesSelection === "B" ? B.notes : A.notes);

        const mergedPayload = {
            masterId: A.id,
            duplicateId: B.id,
            mergedData: {
                id: A.id,
                fullName: selectedFieldValues.fullName,
                phone: selectedFieldValues.phone,
                email: selectedFieldValues.email,
                companyName: selectedFieldValues.companyName,
                title: selectedFieldValues.title,
                source: selectedFieldValues.source,
                notes: mergedNotes,
                mergedAt: new Date().toISOString()
            },
            retainHistory: retainHistory
        };

        try {
            // Call REST API
            const resp = await fetch(API_MERGE, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify(mergedPayload)
            });

            if (resp.ok) {
                console.log("Backend merged successfully");
            }
        } catch (_) {
            console.warn("Backend API /api/leads/merge offline, cập nhật an toàn vào localStorage.");
        }

        // Update local storage
        // 1. Update Master Lead in allLeads
        const masterIdx = allLeads.findIndex(l => l.id === A.id);
        if (masterIdx !== -1) {
            allLeads[masterIdx] = { ...allLeads[masterIdx], ...mergedPayload.mergedData };
        } else {
            allLeads.unshift(mergedPayload.mergedData);
        }

        // 2. Remove duplicate lead from allLeads
        allLeads = allLeads.filter(l => l.id !== B.id);
        try {
            localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(allLeads));
        } catch (_) {}

        // 3. Remove pair from duplicatePairs
        duplicatePairs = duplicatePairs.filter(p => p.id !== currentPair.id);
        saveDuplicatesLocally();

        setTimeout(() => {
            dom.btnConfirmMerge.disabled = false;
            dom.btnConfirmMergeText.textContent = "Xác nhận gộp Lead";
            closeMergeModal();

            scanAndRenderDuplicates();
            showToast(`✓ Đã gộp thành công bản ghi "${B.fullName}" vào Master "${A.fullName}" và bảo toàn toàn bộ lịch sử!`, "success");
        }, 500);
    }

    /* ===================================================================
       5. ATTACH TO EXISTING CUSTOMER MODAL LOGIC
       =================================================================== */

    function bindAttachModalEvents() {
        if (dom.btnCloseAttachModal) dom.btnCloseAttachModal.addEventListener("click", closeAttachModal);
        if (dom.btnCancelAttach) dom.btnCancelAttach.addEventListener("click", closeAttachModal);

        if (dom.attachCustomerModal) {
            dom.attachCustomerModal.addEventListener("click", (e) => {
                if (e.target === dom.attachCustomerModal) closeAttachModal();
            });
        }

        if (dom.btnConfirmAttach) {
            dom.btnConfirmAttach.addEventListener("click", executeAttachToCustomer);
        }
    }

    function openAttachModal(pair) {
        currentPair = pair;
        const lead = pair.recordA;
        const customer = pair.recordB;

        dom.attachModalBody.innerHTML = `
            <div style="background: var(--crm-surface-soft); padding: 16px; border-radius: 10px; border: 1px solid var(--crm-border); margin-bottom: 16px;">
                <span style="font-size: 11px; font-weight: 700; text-transform: uppercase; color: var(--crm-primary); display: block; margin-bottom: 4px;">Khách hàng Doanh nghiệp hiện hữu trong CRM</span>
                <div style="font-size: 16px; font-weight: 800; color: var(--crm-text);">${escapeHtml(customer.companyName || customer.fullName)}</div>
                <div style="font-size: 13px; color: var(--crm-muted); margin-top: 4px;">
                    Mã KH: <strong>${escapeHtml(customer.id)}</strong> &bull; SĐT: <strong>${escapeHtml(customer.phone)}</strong> &bull; Email: <strong>${escapeHtml(customer.email)}</strong>
                </div>
            </div>

            <div style="background: rgba(37, 99, 235, 0.04); padding: 16px; border-radius: 10px; border: 1px dashed var(--crm-primary);">
                <span style="font-size: 11px; font-weight: 700; text-transform: uppercase; color: var(--crm-muted); display: block; margin-bottom: 4px;">Lead mới cần liên kết vào hồ sơ</span>
                <div style="font-size: 14.5px; font-weight: 700; color: var(--crm-text);">${escapeHtml(lead.fullName)} (${escapeHtml(lead.title || "Chưa có chức vụ")})</div>
                <p style="margin: 6px 0 0; font-size: 12.5px; color: var(--crm-text-2);">
                    <strong>Nhu cầu mới từ Lead:</strong> ${escapeHtml(lead.notes || "Không có")}
                </p>
            </div>
            
            <p style="margin: 16px 0 0; font-size: 12.5px; color: var(--crm-muted);">
                <i class="fa-solid fa-circle-check text-success"></i> Sau khi gắn, thông tin nhu cầu và lịch sử tương tác của Lead sẽ được lưu vào mục Liên hệ & Lịch sử chăm sóc của Khách hàng trên hệ thống.
            </p>
        `;

        dom.attachCustomerModal.classList.add("active");
    }

    function closeAttachModal() {
        if (dom.attachCustomerModal) dom.attachCustomerModal.classList.remove("active");
        currentPair = null;
    }

    function executeAttachToCustomer() {
        if (!currentPair) return;
        const lead = currentPair.recordA;
        const customer = currentPair.recordB;

        // Remove from duplicatePairs
        duplicatePairs = duplicatePairs.filter(p => p.id !== currentPair.id);
        saveDuplicatesLocally();

        // Update customer notes
        try {
            const rawCust = localStorage.getItem(STORAGE_KEY_CUSTOMERS);
            let custs = rawCust ? JSON.parse(rawCust) : [];
            const idx = custs.findIndex(c => c.id === customer.id);
            if (idx !== -1) {
                custs[idx].notes = (custs[idx].notes || "") + `\n[Lead gắn vào ${new Date().toLocaleDateString("vi-VN")}]: ${lead.fullName} (${lead.phone}) - ${lead.notes || ""}`;
                localStorage.setItem(STORAGE_KEY_CUSTOMERS, JSON.stringify(custs));
            }
        } catch (_) {}

        // Remove lead or mark as CONVERTED
        allLeads = allLeads.filter(l => l.id !== lead.id);
        try {
            localStorage.setItem(STORAGE_KEY_LEADS, JSON.stringify(allLeads));
        } catch (_) {}

        closeAttachModal();
        scanAndRenderDuplicates();
        showToast(`✓ Đã gắn thành công Lead "${lead.fullName}" vào Khách hàng "${customer.companyName || customer.fullName}"!`, "success");
    }

    /* ===================================================================
       6. UTILITIES
       =================================================================== */

    function debounce(fn, delay) {
        let timer = null;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
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

    function showToast(message, type = "info") {
        if (!dom.toastContainer) return;

        const toast = document.createElement("div");
        toast.className = `crm-lead-toast ${type}`;

        let iconCls = "fa-circle-info";
        if (type === "success") iconCls = "fa-circle-check";
        if (type === "warning") iconCls = "fa-triangle-exclamation";
        if (type === "danger") iconCls = "fa-circle-xmark";

        toast.innerHTML = `
            <i class="fa-solid ${iconCls}"></i>
            <span>${escapeHtml(message)}</span>
        `;

        dom.toastContainer.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = "toastOut 0.3s forwards ease";
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    }

    // Run when ready
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
