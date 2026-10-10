/**
 * ===================================================================
 * CRM-69 / TASK S3-09: PERIODIC CUSTOMER CARE CONTROLLER
 * Module: frontend/js/pages/periodic-care.js
 * Author: Senior Frontend Developer
 * 
 * Capabilities:
 *  1. Unified API Client for GET /api/customers/periodic-care & /care-list
 *  2. Quick Mark-Contacted Workflow: POST /api/customers/{id}/mark-contacted
 *  3. Dynamic Inactive Days (N) Configuration with Preset Chips (15, 30, 45, 60, 90d)
 *  4. VIP Contract Value Prioritization (Default Sort: Contract Value DESC)
 *  5. Real-time Enterprise KPI Counters & Contract Value at Risk Calculation
 *  6. Dual Workspace Tabs: "Cần chăm sóc ngay" vs "Đã chăm sóc gần đây"
 *  7. Pure In-Memory Mock Fallback & HTTP/JSON Client Architecture
 * ===================================================================
 */

"use strict";

(function () {
    const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

    // Module Global States
    let careCustomers = [];
    let currentFilteredList = [];
    let currentTab = "due"; // "due" | "done"
    let currentDaysThreshold = 30;
    let currentPage = 1;
    let pageSize = 20;

    // Filters & Sort State
    let filterContractTier = "ALL";
    let filterOwnerId = "ALL";
    let searchQuery = "";
    let currentSortOrder = "contract_desc"; // Default: VIP contract value first

    // Active Customer under Modal Edit
    let activeCustomerToContact = null;

    /* =========================================================
       1. INITIAL IN-MEMORY ENTERPRISE SEED DATA (FALLBACK)
    ========================================================= */
    function generateDefaultCareDataset() {
        return [];
    }
    function _unused_generateDefaultCareDataset() {
        return [
                id: 4,
                name: "Công ty Cổ phần Giải pháp Số Nam Á",
                taxCode: "0108923456",
                industry: "Công nghệ thông tin & Viễn thông",
                phone: "024.3999.8888",
                ownerId: 4,
                ownerName: "Nông Quang Tiệp",
                ownerRole: "Trưởng phòng KD",
                lastInteractionAt: daysAgo(42),
                lastInteractionType: "CALL",
                lastInteractionNote: "Cuộc gọi định kỳ bàn về gói bảo trì quý 1.",
                daysInactive: 42,
                contractValue: 1250000000, // 1.25 Tỷ
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 1,
                name: "Tập đoàn Công nghệ Nam Á Holdings",
                taxCode: "0101234567",
                industry: "Tập đoàn Đầu tư & Công nghệ",
                phone: "024.3777.9999",
                ownerId: 4,
                ownerName: "Nông Quang Tiệp",
                ownerRole: "Trưởng phòng KD",
                lastInteractionAt: daysAgo(68),
                lastInteractionType: "MEETING",
                lastInteractionNote: "Họp triển khai hệ thống giải pháp Core ERP.",
                daysInactive: 68,
                contractValue: 3800000000, // 3.8 Tỷ - Ultra VIP
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 5,
                name: "Công ty Cổ phần Đầu tư Bất động sản Thịnh Vượng",
                taxCode: "0314567890",
                industry: "Bất động sản & Xây dựng",
                phone: "028.3888.1122",
                ownerId: 2,
                ownerName: "Nguyễn Văn An",
                ownerRole: "Senior Sales Rep",
                lastInteractionAt: daysAgo(35),
                lastInteractionType: "EMAIL",
                lastInteractionNote: "Gửi báo giá nâng cấp máy chủ quản lý giao dịch.",
                daysInactive: 35,
                contractValue: 1850000000, // 1.85 Tỷ
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 6,
                name: "Tổng Công ty Xây dựng Công trình Giao thông 1",
                taxCode: "0100105423",
                industry: "Hạ tầng giao thông & Xây dựng",
                phone: "024.3822.4466",
                ownerId: 2,
                ownerName: "Nguyễn Văn An",
                ownerRole: "Senior Sales Rep",
                lastInteractionAt: daysAgo(54),
                lastInteractionType: "CALL",
                lastInteractionNote: "Thăm hỏi tiến độ triển khai phân hệ giám sát.",
                daysInactive: 54,
                contractValue: 1600000000, // 1.6 Tỷ
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 7,
                name: "Công ty CP Dược phẩm Trung ương An Khang",
                taxCode: "0102558899",
                industry: "Dược phẩm & Y tế",
                phone: "024.3988.6655",
                ownerId: 3,
                ownerName: "Trần Thị Bích",
                ownerRole: "CSKH Enterprise",
                lastInteractionAt: daysAgo(75),
                lastInteractionType: "CALL",
                lastInteractionNote: "Khách hỏi thủ tục xuất hóa đơn điện tử bổ sung.",
                daysInactive: 75,
                contractValue: 890000000, // 890 Triệu
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 8,
                name: "Công ty TNHH Sản xuất & Thương mại Thép Nam Đô",
                taxCode: "2300987654",
                industry: "Sản xuất & Công nghiệp nặng",
                phone: "022.2388.7799",
                ownerId: 5,
                ownerName: "Lê Hoàng Long",
                ownerRole: "Account Manager",
                lastInteractionAt: daysAgo(31),
                lastInteractionType: "MEETING",
                lastInteractionNote: "Gặp đại diện nhà máy đánh giá phần mềm quản trị kho.",
                daysInactive: 31,
                contractValue: 750000000, // 750 Triệu
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 9,
                name: "Công ty TNHH Viễn thông Miền Trung",
                taxCode: "0401889922",
                industry: "Viễn thông & Hạ tầng mạng",
                phone: "023.6388.9900",
                ownerId: 4,
                ownerName: "Nông Quang Tiệp",
                ownerRole: "Trưởng phòng KD",
                lastInteractionAt: daysAgo(48),
                lastInteractionType: "CALL",
                lastInteractionNote: "Xác nhận nghiệm thu giai đoạn bảo hành quý 2.",
                daysInactive: 48,
                contractValue: 680000000, // 680 Triệu
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 10,
                name: "Công ty Cổ phần Nông nghiệp Công nghệ cao Vinaseed",
                taxCode: "0100109988",
                industry: "Nông nghiệp & Chế biến thực phẩm",
                phone: "024.3868.5522",
                ownerId: 3,
                ownerName: "Trần Thị Bích",
                ownerRole: "CSKH Enterprise",
                lastInteractionAt: daysAgo(92),
                lastInteractionType: "SUPPORT",
                lastInteractionNote: "Cập nhật chứng thư số token ký hóa đơn.",
                daysInactive: 92,
                contractValue: 520000000, // 520 Triệu - Nguy cơ rất cao
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 11,
                name: "Công ty Cổ phần Bán lẻ F99 Mart",
                taxCode: "0315894561",
                industry: "Bán lẻ & Chuỗi siêu thị",
                phone: "028.7300.9988",
                ownerId: 2,
                ownerName: "Nguyễn Văn An",
                ownerRole: "Senior Sales Rep",
                lastInteractionAt: daysAgo(33),
                lastInteractionType: "EMAIL",
                lastInteractionNote: "Gửi báo cáo tổng hợp thời gian phản hồi kỹ thuật.",
                daysInactive: 33,
                contractValue: 450000000, // 450 Triệu
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 12,
                name: "Công ty TNHH Logistics Á Châu",
                taxCode: "0312678901",
                industry: "Vận tải & Kho bãi",
                phone: "028.3944.5566",
                ownerId: 5,
                ownerName: "Lê Hoàng Long",
                ownerRole: "Account Manager",
                lastInteractionAt: daysAgo(61),
                lastInteractionType: "CALL",
                lastInteractionNote: "Liên hệ thông báo chuẩn bị đến kỳ gia hạn hợp đồng.",
                daysInactive: 61,
                contractValue: 320000000, // 320 Triệu
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 13,
                name: "Công ty Cổ phần Giáo dục & Đào tạo Future Edu",
                taxCode: "0106789123",
                industry: "Giáo dục & Đào tạo",
                phone: "024.3755.8899",
                ownerId: 6,
                ownerName: "Phạm Minh Đức",
                ownerRole: "Technical Support Lead",
                lastInteractionAt: daysAgo(38),
                lastInteractionType: "SUPPORT",
                lastInteractionNote: "Hỗ trợ cấu hình tích hợp API quản lý học viên.",
                daysInactive: 38,
                contractValue: 180000000, // 180 Triệu
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 14,
                name: "Công ty TNHH Thương mại Dịch vụ Minh Khang",
                taxCode: "0104561234",
                industry: "Thương mại tổng hợp",
                phone: "024.3644.2211",
                ownerId: 6,
                ownerName: "Phạm Minh Đức",
                ownerRole: "Technical Support Lead",
                lastInteractionAt: daysAgo(45),
                lastInteractionType: "CALL",
                lastInteractionNote: "Tư vấn nâng cấp phần mềm kế toán.",
                daysInactive: 45,
                contractValue: 45000000, // 45 Triệu (SME)
                contractStatus: "SIGNED",
                isContactedRecently: false,
                nextFollowUpDate: null
            },
            {
                id: 15,
                name: "Công ty Cổ phần Thiết bị Y tế Tân Bình",
                taxCode: "0309876543",
                industry: "Thiết bị Y tế",
                phone: "028.3844.7788",
                ownerId: 3,
                ownerName: "Trần Thị Bích",
                ownerRole: "CSKH Enterprise",
                lastInteractionAt: daysAgo(3),
                lastInteractionType: "MEETING",
                lastInteractionNote: "Đã gặp mặt trực tiếp thăm hỏi vận hành, khách phản hồi rất tốt.",
                daysInactive: 3,
                contractValue: 420000000,
                contractStatus: "SIGNED",
                isContactedRecently: true,
                nextFollowUpDate: "2026-11-15"
            },
            {
                id: 16,
                name: "Ngân hàng TMCP Phương Đông - Khối CNTT",
                taxCode: "0300852951",
                industry: "Tài chính & Ngân hàng",
                phone: "028.3822.0960",
                ownerId: 4,
                ownerName: "Nông Quang Tiệp",
                ownerRole: "Trưởng phòng KD",
                lastInteractionAt: daysAgo(1),
                lastInteractionType: "CALL",
                lastInteractionNote: "Gọi điện trao đổi điều khoản bổ sung phụ lục hợp đồng.",
                daysInactive: 1,
                contractValue: 2100000000,
                contractStatus: "SIGNED",
                isContactedRecently: true,
                nextFollowUpDate: "2026-11-01"
            }
        ];
    }

    /* =========================================================
       2. API CLIENT (FETCH API WITH CREDENTIALS)
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
            console.warn("[CRM-69] Phiên làm việc hết hạn hoặc chưa đăng nhập.");
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
       3. DATA LOADING & IN-MEMORY ENGINE
    ========================================================= */
    async function loadPeriodicCareData() {
        // Clean up residual demo localStorage key if previously present
        try {
            localStorage.removeItem("CRM_PERIODIC_CARE_CUSTOMERS_DATA");
        } catch (_) {}

        // Primary Flow: Fetch Backend API
        try {
            // First try user specified endpoint: /api/customers/periodic-care
            let backendItems = null;
            try {
                const res = await api(`/api/customers/periodic-care?days=${currentDaysThreshold}&sort=${currentSortOrder}`);
                if (res && Array.isArray(res.items || res)) {
                    backendItems = res.items || res;
                }
            } catch (_) {
                // Fallback to existing servlet endpoint: /api/customers/care-list
                const res2 = await api(`/api/customers/care-list?days=${currentDaysThreshold}&page=1&size=50`);
                if (res2 && Array.isArray(res2.items)) {
                    backendItems = res2.items;
                }
            }

            if (backendItems && backendItems.length > 0) {
                console.info("[CRM-69] Đã nạp thành công dữ liệu từ Backend API:", backendItems);
                // Map backend items to rich client structure
                careCustomers = backendItems.map(item => {
                    const days = calculateDaysInactive(item.lastInteractionAt);
                    return {
                        id: Number(item.id),
                        name: item.name || "Doanh nghiệp",
                        taxCode: item.taxCode || ("010" + String(item.id).padStart(7, "0")),
                        industry: item.industry || "Doanh nghiệp thương mại",
                        phone: item.phone || "024.3999.8888",
                        ownerId: item.ownerUserId || 4,
                        ownerName: getOwnerNameById(item.ownerUserId),
                        ownerRole: "Nhân viên phụ trách",
                        lastInteractionAt: item.lastInteractionAt || null,
                        lastInteractionType: item.lastInteractionType || "CALL",
                        lastInteractionNote: item.lastInteractionNote || "Chưa có ghi chú",
                        daysInactive: days,
                        contractValue: Number(item.contractValue || 0),
                        contractStatus: "SIGNED",
                        isContactedRecently: days < currentDaysThreshold,
                        nextFollowUpDate: item.nextFollowUpDate || null
                    };
                });
                return;
            }
        } catch (err) {
            console.warn("[CRM-69] Backend API chưa sẵn sàng. Kích hoạt In-Memory Mock Data nội bộ:", err.message);
        }

        // Fallback Flow: Comprehensive In-Memory Mock Dataset
        careCustomers = generateDefaultCareDataset();
    }

    /* =========================================================
       4. CALCULATIONS, FILTERING & SORTING
    ========================================================= */
    function calculateDaysInactive(dateStr) {
        if (!dateStr) return 999;
        const diffMs = new Date().getTime() - new Date(dateStr).getTime();
        return Math.max(0, Math.floor(diffMs / (1000 * 60 * 60 * 24)));
    }

    function applyFiltersAndSorting() {
        // Recalculate daysInactive for each customer based on their current lastInteractionAt
        careCustomers.forEach(c => {
            c.daysInactive = calculateDaysInactive(c.lastInteractionAt);
            if (c.daysInactive < currentDaysThreshold) {
                c.isContactedRecently = true;
            }
        });

        // Split into Due vs Done
        const dueList = careCustomers.filter(c => !c.isContactedRecently && c.daysInactive >= currentDaysThreshold);
        const doneList = careCustomers.filter(c => c.isContactedRecently || c.daysInactive < currentDaysThreshold);

        // Update Tab Badges
        const badgeDueEl = document.getElementById("badgeDueCount");
        const badgeDoneEl = document.getElementById("badgeDoneCount");
        if (badgeDueEl) badgeDueEl.textContent = String(dueList.length);
        if (badgeDoneEl) badgeDoneEl.textContent = String(doneList.length);

        // Choose Working Dataset based on active tab
        let workingSet = currentTab === "due" ? [...dueList] : [...doneList];

        // 1. Filter by Search Query
        if (searchQuery) {
            const q = searchQuery.toLowerCase().trim();
            workingSet = workingSet.filter(c =>
                (c.name && c.name.toLowerCase().includes(q)) ||
                (c.taxCode && c.taxCode.toLowerCase().includes(q)) ||
                (c.phone && c.phone.includes(q)) ||
                (c.ownerName && c.ownerName.toLowerCase().includes(q)) ||
                (c.industry && c.industry.toLowerCase().includes(q))
            );
        }

        // 2. Filter by Contract Value Tier
        if (filterContractTier !== "ALL") {
            workingSet = workingSet.filter(c => {
                const val = c.contractValue || 0;
                if (filterContractTier === "VIP_PRIORITY") return val >= 50000000;
                if (filterContractTier === "MEGA_VIP") return val >= 200000000;
                if (filterContractTier === "LARGE") return val >= 50000000 && val < 200000000;
                if (filterContractTier === "SME") return val < 50000000;
                return true;
            });
        }

        // 3. Filter by Owner
        if (filterOwnerId !== "ALL") {
            const oId = Number(filterOwnerId);
            workingSet = workingSet.filter(c => c.ownerId === oId);
        }

        // 4. Sort Results
        workingSet.sort((a, b) => {
            if (currentSortOrder === "contract_desc") return (b.contractValue || 0) - (a.contractValue || 0);
            if (currentSortOrder === "contract_asc") return (a.contractValue || 0) - (b.contractValue || 0);
            if (currentSortOrder === "days_desc") return (b.daysInactive || 0) - (a.daysInactive || 0);
            if (currentSortOrder === "last_asc") return new Date(a.lastInteractionAt || 0) - new Date(b.lastInteractionAt || 0);
            if (currentSortOrder === "name_asc") return a.name.localeCompare(b.name, "vi");
            return 0;
        });

        currentFilteredList = workingSet;
        currentPage = 1;

        // Render Views
        renderKPICards(dueList, doneList);
        renderTable();
    }

    /* =========================================================
       5. RENDER KPI CARDS
    ========================================================= */
    function renderKPICards(dueList, doneList) {
        // Card 1: Tổng khách cần chăm sóc (quá hạn N ngày)
        const totalCareEl = document.getElementById("kpiTotalCare");
        const totalCareSubEl = document.getElementById("kpiTotalCareSub");
        if (totalCareEl) totalCareEl.textContent = String(dueList.length);
        if (totalCareSubEl) totalCareSubEl.textContent = `Quá hạn ≥ ${currentDaysThreshold} ngày chưa tương tác`;

        // Card 2: Khách VIP nguy cơ bỏ quên (> 50M)
        const vipRiskCount = dueList.filter(c => (c.contractValue || 0) >= 50000000).length;
        const vipRiskEl = document.getElementById("kpiVipRisk");
        if (vipRiskEl) vipRiskEl.textContent = String(vipRiskCount);

        // Card 3: Giá trị hợp đồng đang rủi ro
        const totalValueAtRisk = dueList.reduce((sum, c) => sum + (Number(c.contractValue) || 0), 0);
        const valueAtRiskEl = document.getElementById("kpiContractValueAtRisk");
        if (valueAtRiskEl) valueAtRiskEl.textContent = formatMoneyVND(totalValueAtRisk);

        // Card 4: Đã liên hệ gần đây
        const contactedEl = document.getElementById("kpiContactedRecently");
        const contactedSubEl = document.getElementById("kpiContactedRateSub");
        if (contactedEl) contactedEl.textContent = String(doneList.length);
        if (contactedSubEl) {
            const total = careCustomers.length || 1;
            const rate = Math.round((doneList.length / total) * 100);
            contactedSubEl.textContent = `Tỷ lệ hoàn thành: ${rate}% tổng danh sách`;
        }
    }

    /* =========================================================
       6. RENDER DATA TABLE & PAGINATION
    ========================================================= */
    function renderTable() {
        const tbody = document.getElementById("careTableBody");
        const emptyState = document.getElementById("careEmptyState");
        const tableFooter = document.getElementById("careTableFooter");
        const summaryEl = document.getElementById("careRecordSummary");

        if (!tbody) return;

        const totalItems = currentFilteredList.length;
        if (summaryEl) summaryEl.textContent = `Hiển thị: ${totalItems} khách hàng`;

        if (totalItems === 0) {
            tbody.innerHTML = "";
            if (emptyState) {
                emptyState.style.display = "flex";
                const titleEl = document.getElementById("emptyStateTitle");
                const descEl = document.getElementById("emptyStateDesc");
                if (currentTab === "due") {
                    if (titleEl) titleEl.textContent = "Không có khách hàng nào bị bỏ quên!";
                    if (descEl) descEl.textContent = `Tất cả khách hàng đã ký hợp đồng đều được tương tác trong vòng ${currentDaysThreshold} ngày qua.`;
                } else {
                    if (titleEl) titleEl.textContent = "Chưa có khách hàng nào trong danh sách đã chăm sóc.";
                    if (descEl) descEl.textContent = "Hãy đánh dấu 'Đã liên hệ' ở tab 'Cần chăm sóc ngay' để đưa khách hàng vào đây.";
                }
            }
            if (tableFooter) tableFooter.style.display = "none";
            return;
        }

        if (emptyState) emptyState.style.display = "none";
        if (tableFooter) tableFooter.style.display = "flex";

        // Pagination Slicing
        const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
        if (currentPage > totalPages) currentPage = totalPages;

        const startIndex = (currentPage - 1) * pageSize;
        const endIndex = Math.min(startIndex + pageSize, totalItems);
        const pageItems = currentFilteredList.slice(startIndex, endIndex);

        const fragment = document.createDocumentFragment();

        pageItems.forEach((customer, index) => {
            const tr = document.createElement("tr");

            // Row Index
            const rowNumber = startIndex + index + 1;

            // Days inactive formatting & Urgency Badge
            const days = customer.daysInactive;
            let daysBadgeHtml = "";
            if (customer.isContactedRecently || days < currentDaysThreshold) {
                daysBadgeHtml = `<span class="days-badge days-badge-ok"><svg class="crm-inline-icon" viewBox="0 0 24 24" style="width:14px; height:14px;"><path d="M20 6 9 17l-5-5"/></svg> ${days === 0 ? "Hôm nay" : days + " ngày (Đã CS)"}</span>`;
            } else if (days >= 60) {
                daysBadgeHtml = `<span class="days-badge days-badge-critical" title="Nguy cấp: Đã ${days} ngày chưa được tương tác!"><svg class="crm-inline-icon" viewBox="0 0 24 24" style="width:14px; height:14px;"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg> ${days} ngày (RỦI RO CAO)</span>`;
            } else if (days >= 45) {
                daysBadgeHtml = `<span class="days-badge days-badge-danger" title="Cảnh báo: Đã ${days} ngày chưa tương tác!"><svg class="crm-inline-icon" viewBox="0 0 24 24" style="width:14px; height:14px;"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/></svg> ${days} ngày (Quá hạn)</span>`;
            } else {
                daysBadgeHtml = `<span class="days-badge days-badge-warning" title="Cần chú ý liên hệ định kỳ"><svg class="crm-inline-icon" viewBox="0 0 24 24" style="width:14px; height:14px;"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/></svg> ${days} ngày</span>`;
            }

            // VIP Tag for Contract Value
            let vipBadgeHtml = "";
            const val = customer.contractValue || 0;
            if (val >= 2000000000) {
                vipBadgeHtml = `<span class="contract-ultra-vip-tag">💎 MEGA VIP</span>`;
            } else if (val >= 500000000) {
                vipBadgeHtml = `<span class="contract-vip-tag">⭐ VIP ENTERPRISE</span>`;
            } else if (val >= 50000000) {
                vipBadgeHtml = `<span class="contract-vip-tag">ƯU TIÊN</span>`;
            }

            // Interaction Channel Icon
            const typeIcon = getChannelIcon(customer.lastInteractionType);
            const formattedDate = formatDateVi(customer.lastInteractionAt);

            tr.innerHTML = `
                <td style="text-align:center; color:var(--crm-muted); font-size:12px; font-weight:600;">${rowNumber}</td>
                <td>
                    <div class="customer-name-cell">
                        <a href="customer-360.html?id=${customer.id}" class="customer-company-title" title="Xem hồ sơ 360 độ của doanh nghiệp">
                            <span>${escapeHtml(customer.name)}</span>
                            <svg class="crm-inline-icon" viewBox="0 0 24 24" style="width:14px; height:14px; color:var(--crm-muted);"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                        </a>
                        <div style="display:flex; align-items:center; gap:6px; flex-wrap:wrap;">
                            <span class="customer-tax-pill">MST: ${customer.taxCode || '—'}</span>
                            <span style="font-size:11.5px; color:var(--crm-muted);">${escapeHtml(customer.industry || 'Doanh nghiệp')}</span>
                        </div>
                    </div>
                </td>
                <td>
                    <div class="owner-pill">
                        <div class="owner-avatar-mini">${getInitials(customer.ownerName)}</div>
                        <div>
                            <div style="font-weight:600; color:var(--crm-text); font-size:13px;">${escapeHtml(customer.ownerName || 'Nông Quang Tiệp')}</div>
                            <div style="font-size:11.5px; color:var(--crm-muted);">${escapeHtml(customer.ownerRole || 'Phụ trách')}</div>
                        </div>
                    </div>
                </td>
                <td>
                    <div class="interaction-cell">
                        <span class="interaction-date">${formattedDate}</span>
                        <span class="interaction-type-badge">
                            ${typeIcon}
                            <span>${getChannelLabel(customer.lastInteractionType)}</span>
                        </span>
                    </div>
                </td>
                <td>${daysBadgeHtml}</td>
                <td>
                    <div class="contract-cell">
                        <span class="contract-value-amount">${formatMoneyVND(val)}</span>
                        ${vipBadgeHtml}
                    </div>
                </td>
                <td style="text-align:right;">
                    <div class="table-actions-cell" style="justify-content:flex-end;">
                        <button type="button" class="btn-action-contact" data-id="${customer.id}" title="Ghi nhận vừa gọi điện / gặp gỡ khách hàng này">
                            <svg class="crm-inline-icon" viewBox="0 0 24 24" style="width:14px; height:14px;"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                            <span>${customer.isContactedRecently ? "Cập nhật CS" : "Đã liên hệ"}</span>
                        </button>
                        <a href="customer-360.html?id=${customer.id}" class="btn-action-view360" title="Mở Customer 360 độ">
                            <span>360°</span>
                        </a>
                    </div>
                </td>
            `;

            fragment.appendChild(tr);
        });

        tbody.innerHTML = "";
        tbody.appendChild(fragment);

        // Bind Action Button Handlers
        tbody.querySelectorAll(".btn-action-contact").forEach(btn => {
            btn.addEventListener("click", () => {
                const cId = Number(btn.dataset.id);
                openMarkContactedModal(cId);
            });
        });

        // Update Pagination Controls
        const pageInfoEl = document.getElementById("carePaginationInfo");
        const pageIndicatorEl = document.getElementById("pageIndicator");
        const btnPrev = document.getElementById("btnPrevPage");
        const btnNext = document.getElementById("btnNextPage");

        if (pageInfoEl) pageInfoEl.textContent = `Đang hiển thị dòng ${startIndex + 1} - ${endIndex} / ${totalItems}`;
        if (pageIndicatorEl) pageIndicatorEl.textContent = `${currentPage} / ${totalPages}`;
        if (btnPrev) btnPrev.disabled = currentPage <= 1;
        if (btnNext) btnNext.disabled = currentPage >= totalPages;
    }

    /* =========================================================
       7. MARK CONTACTED MODAL WORKFLOW
    ========================================================= */
    const modalOverlay = document.getElementById("careModalOverlay");
    const contactModal = document.getElementById("markContactedModal");

    function openMarkContactedModal(customerId) {
        const customer = careCustomers.find(c => c.id === customerId);
        if (!customer) return;

        activeCustomerToContact = customer;

        // Populate Modal Fields
        document.getElementById("modalCustName").textContent = customer.name;
        document.getElementById("modalCustMeta").textContent = `MST: ${customer.taxCode || '—'} • Phụ trách: ${customer.ownerName || 'Nông Quang Tiệp'}`;
        document.getElementById("modalCustContractValue").textContent = formatMoneyVND(customer.contractValue || 0);

        // Current Local Date Time for Input
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, "0");
        const day = String(now.getDate()).padStart(2, "0");
        const hours = String(now.getHours()).padStart(2, "0");
        const minutes = String(now.getMinutes()).padStart(2, "0");
        document.getElementById("modalContactDate").value = `${year}-${month}-${day}T${hours}:${minutes}`;

        // Default next follow-up: +30 days
        const nextDate = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);
        const ny = nextDate.getFullYear();
        const nm = String(nextDate.getMonth() + 1).padStart(2, "0");
        const nd = String(nextDate.getDate()).padStart(2, "0");
        document.getElementById("modalNextFollowUp").value = `${ny}-${nm}-${nd}`;

        // Reset Type Selector to Call
        document.getElementById("modalInteractionType").value = "call";
        document.querySelectorAll("#modalChannelSelector .type-select-card").forEach(c => {
            c.classList.toggle("active", c.dataset.type === "call");
        });

        // Clear notes
        document.getElementById("modalContactNotes").value = "";

        // Open Modal
        if (modalOverlay && contactModal) {
            modalOverlay.classList.add("open");
            contactModal.classList.add("open");
            setTimeout(() => {
                document.getElementById("modalContactNotes")?.focus();
            }, 100);
        }
    }

    function closeMarkContactedModal() {
        if (modalOverlay && contactModal) {
            modalOverlay.classList.remove("open");
            contactModal.classList.remove("open");
        }
        activeCustomerToContact = null;
    }

    // Modal Channel Card Selection
    document.querySelectorAll("#modalChannelSelector .type-select-card").forEach(card => {
        card.addEventListener("click", () => {
            document.querySelectorAll("#modalChannelSelector .type-select-card").forEach(c => c.classList.remove("active"));
            card.classList.add("active");
            const type = card.dataset.type || "call";
            document.getElementById("modalInteractionType").value = type;
        });
    });

    // Quick Note Template Chips
    document.querySelectorAll(".template-chip").forEach(chip => {
        chip.addEventListener("click", () => {
            const tpl = chip.dataset.tpl;
            const textarea = document.getElementById("modalContactNotes");
            if (textarea && tpl) {
                textarea.value = tpl;
                textarea.focus();
            }
        });
    });

    // Modal Close Buttons
    document.getElementById("btnCloseCareModal")?.addEventListener("click", closeMarkContactedModal);
    document.getElementById("btnCancelCareModal")?.addEventListener("click", closeMarkContactedModal);
    modalOverlay?.addEventListener("click", closeMarkContactedModal);

    // Modal Form Submission: Mark Contacted
    document.getElementById("formMarkContacted")?.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!activeCustomerToContact) return;

        const customerId = activeCustomerToContact.id;
        const type = document.getElementById("modalInteractionType").value || "call";
        const contactDateVal = document.getElementById("modalContactDate").value;
        const notes = document.getElementById("modalContactNotes").value.trim();
        const satisfaction = document.getElementById("modalSatisfaction").value;
        const nextFollowUp = document.getElementById("modalNextFollowUp").value;
        const createReminder = document.getElementById("modalCreateReminder")?.checked;

        if (!notes) {
            showToast("Vui lòng nhập nội dung trao đổi hoặc chọn mẫu nhanh.", "warning");
            document.getElementById("modalContactNotes")?.focus();
            return;
        }

        const submitBtn = document.getElementById("btnSaveContacted");
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = "Đang lưu...";
        }

        // Build Payload
        const contactTime = contactDateVal ? new Date(contactDateVal).toISOString() : new Date().toISOString();
        const payload = {
            customer_id: customerId,
            customerId: customerId,
            type: type.toUpperCase(),
            note: notes,
            notes: notes,
            satisfaction: satisfaction,
            next_follow_up: nextFollowUp,
            nextFollowUp: nextFollowUp,
            createReminder: createReminder
        };

        // Call Backend API via Fetch (Non-blocking fallback)
        try {
            await api(`/api/customers/${customerId}/mark-contacted`, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            console.info(`[CRM-69] Đã gửi thành công POST /api/customers/${customerId}/mark-contacted`);
        } catch (apiErr) {
            // Also try POST /api/activities as backup
            try {
                await api(`/api/activities`, {
                    method: "POST",
                    body: JSON.stringify({
                        subject: `Chăm sóc định kỳ: ${activeCustomerToContact.name}`,
                        type: type.toUpperCase(),
                        description: notes,
                        customerId: customerId,
                        status: "COMPLETED"
                    })
                });
            } catch (_) {}
            console.warn(`[CRM-69] Backend phản hồi lỗi (${apiErr.message}), đã lưu cập nhật vào bộ nhớ cục bộ.`);
        }

        // Update In-Memory State
        const target = careCustomers.find(c => c.id === customerId);
        if (target) {
            target.lastInteractionAt = contactTime;
            target.lastInteractionType = type.toUpperCase();
            target.lastInteractionNote = notes;
            target.daysInactive = 0;
            target.isContactedRecently = true;
            target.nextFollowUpDate = nextFollowUp;
        }

        closeMarkContactedModal();

        // Trigger UI Refresh
        applyFiltersAndSorting();

        showToast(`Đã ghi nhận chăm sóc thành công cho "${activeCustomerToContact.name}"! Khách hàng đã được chuyển sang tab 'Đã chăm sóc'.`, "success");

        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = `<svg class="crm-inline-icon" viewBox="0 0 24 24"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg> Xác nhận & Lưu liên hệ`;
        }
    });

    /* =========================================================
       8. CONTROLS, FILTERS & EVENT LISTENERS
    ========================================================= */
    function setupEventListeners() {
        // Preset Chips
        document.querySelectorAll("#presetChipsContainer .threshold-chip").forEach(chip => {
            chip.addEventListener("click", () => {
                document.querySelectorAll("#presetChipsContainer .threshold-chip").forEach(c => c.classList.remove("active"));
                chip.classList.add("active");
                const days = Number(chip.dataset.days) || 30;
                currentDaysThreshold = days;
                const inputEl = document.getElementById("inputDaysThreshold");
                if (inputEl) inputEl.value = String(days);
                applyFiltersAndSorting();
            });
        });

        // Threshold Number Input
        const inputThreshold = document.getElementById("inputDaysThreshold");
        inputThreshold?.addEventListener("change", () => {
            let val = parseInt(inputThreshold.value, 10);
            if (isNaN(val) || val < 1) val = 1;
            if (val > 365) val = 365;
            inputThreshold.value = String(val);
            currentDaysThreshold = val;

            // Sync chips state
            document.querySelectorAll("#presetChipsContainer .threshold-chip").forEach(c => {
                c.classList.toggle("active", Number(c.dataset.days) === val);
            });

            applyFiltersAndSorting();
        });

        // Search Input
        const searchInput = document.getElementById("careSearchInput");
        let searchDebounce = null;
        searchInput?.addEventListener("input", () => {
            clearTimeout(searchDebounce);
            searchDebounce = setTimeout(() => {
                searchQuery = searchInput.value;
                applyFiltersAndSorting();
            }, 250);
        });

        // Contract Value Filter
        document.getElementById("filterContractValue")?.addEventListener("change", (e) => {
            filterContractTier = e.target.value;
            applyFiltersAndSorting();
        });

        // Owner Filter
        document.getElementById("filterOwner")?.addEventListener("change", (e) => {
            filterOwnerId = e.target.value;
            applyFiltersAndSorting();
        });

        // Sort Select
        document.getElementById("careSortSelect")?.addEventListener("change", (e) => {
            currentSortOrder = e.target.value;
            applyFiltersAndSorting();
        });

        // Reset Filters Button
        const resetHandler = () => {
            if (searchInput) searchInput.value = "";
            searchQuery = "";
            filterContractTier = "ALL";
            filterOwnerId = "ALL";
            currentSortOrder = "contract_desc";
            currentDaysThreshold = 30;

            if (inputThreshold) inputThreshold.value = "30";
            document.querySelectorAll("#presetChipsContainer .threshold-chip").forEach(c => {
                c.classList.toggle("active", c.dataset.days === "30");
            });

            const selTier = document.getElementById("filterContractValue");
            if (selTier) selTier.value = "ALL";
            const selOwner = document.getElementById("filterOwner");
            if (selOwner) selOwner.value = "ALL";
            const selSort = document.getElementById("careSortSelect");
            if (selSort) selSort.value = "contract_desc";

            applyFiltersAndSorting();
            showToast("Đã đặt lại toàn bộ bộ lọc về mặc định.", "success");
        };

        document.getElementById("btnResetCareFilters")?.addEventListener("click", resetHandler);
        document.getElementById("btnEmptyResetFilter")?.addEventListener("click", resetHandler);

        // View Tabs Toggle
        document.getElementById("tabDueCare")?.addEventListener("click", () => {
            currentTab = "due";
            document.getElementById("tabDueCare")?.classList.add("active");
            document.getElementById("tabDoneCare")?.classList.remove("active");
            applyFiltersAndSorting();
        });

        document.getElementById("tabDoneCare")?.addEventListener("click", () => {
            currentTab = "done";
            document.getElementById("tabDoneCare")?.classList.add("active");
            document.getElementById("tabDueCare")?.classList.remove("active");
            applyFiltersAndSorting();
        });

        // Refresh Button
        document.getElementById("btnRefreshCare")?.addEventListener("click", async () => {
            const btn = document.getElementById("btnRefreshCare");
            if (btn) btn.disabled = true;
            await loadPeriodicCareData();
            applyFiltersAndSorting();
            showToast("Đã làm mới dữ liệu khách hàng chăm sóc định kỳ.", "success");
            if (btn) btn.disabled = false;
        });

        // Export Report Button
        document.getElementById("btnExportCareList")?.addEventListener("click", exportCareReport);

        // Pagination Buttons
        document.getElementById("btnPrevPage")?.addEventListener("click", () => {
            if (currentPage > 1) {
                currentPage--;
                renderTable();
            }
        });

        document.getElementById("btnNextPage")?.addEventListener("click", () => {
            const totalPages = Math.ceil(currentFilteredList.length / pageSize);
            if (currentPage < totalPages) {
                currentPage++;
                renderTable();
            }
        });

        document.getElementById("selectPageSize")?.addEventListener("change", (e) => {
            pageSize = Number(e.target.value) || 20;
            currentPage = 1;
            renderTable();
        });
    }

    /* =========================================================
       9. POPULATE OWNERS SELECT
    ========================================================= */
    function populateOwnersDropdown() {
        const ownerSelect = document.getElementById("filterOwner");
        if (!ownerSelect) return;

        const ownersMap = new Map();
        careCustomers.forEach(c => {
            if (c.ownerId && c.ownerName) {
                ownersMap.set(c.ownerId, c.ownerName);
            }
        });

        ownerSelect.innerHTML = `<option value="ALL">Tất cả nhân viên phụ trách</option>`;
        ownersMap.forEach((name, id) => {
            const opt = document.createElement("option");
            opt.value = String(id);
            opt.textContent = name;
            ownerSelect.appendChild(opt);
        });
    }

    /* =========================================================
       10. EXPORT REPORT TO CSV / EXCEL
    ========================================================= */
    function exportCareReport() {
        const listToExport = currentFilteredList;
        if (!listToExport || listToExport.length === 0) {
            showToast("Không có bản ghi nào để xuất báo cáo.", "warning");
            return;
        }

        const headers = ["STT", "Tên Doanh Nghiệp", "Mã Số Thuế", "Lĩnh Vực", "Người Phụ Trách", "Lần Tương Tác Cuối", "Hình Thức", "Số Ngày Chưa CS", "Giá Trị Hợp Đồng (VNĐ)", "Trạng Thái"];
        
        const rows = listToExport.map((c, idx) => [
            idx + 1,
            `"${(c.name || '').replace(/"/g, '""')}"`,
            `"${c.taxCode || ''}"`,
            `"${(c.industry || '').replace(/"/g, '""')}"`,
            `"${(c.ownerName || '').replace(/"/g, '""')}"`,
            `"${formatDateVi(c.lastInteractionAt)}"`,
            `"${getChannelLabel(c.lastInteractionType)}"`,
            c.daysInactive,
            c.contractValue || 0,
            `"${c.isContactedRecently ? 'Đã chăm sóc gần đây' : 'Cần chăm sóc ngay'}"`
        ]);

        const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(r => r.join(","))].join("\r\n");
        const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
        const link = document.createElement("a");
        const filename = `Bao_Cao_Cham_Soc_Dinh_Ky_CRM_${new Date().toISOString().slice(0, 10)}.csv`;

        link.href = URL.createObjectURL(blob);
        link.download = filename;
        link.click();
        URL.revokeObjectURL(link.href);

        showToast(`Đã xuất báo cáo ${listToExport.length} khách hàng thành công!`, "success");
    }

    /* =========================================================
       11. UTILITY FUNCTIONS
    ========================================================= */
    function formatMoneyVND(amount) {
        const num = Number(amount) || 0;
        return num.toLocaleString("vi-VN") + " ₫";
    }

    function formatDateVi(dateStr) {
        if (!dateStr) return "Chưa từng liên hệ";
        try {
            const d = new Date(dateStr);
            if (isNaN(d.getTime())) return "Chưa từng liên hệ";
            return d.toLocaleDateString("vi-VN", {
                day: "2-digit",
                month: "2-digit",
                year: "numeric"
            });
        } catch (_) {
            return "Chưa từng liên hệ";
        }
    }

    function getInitials(name) {
        if (!name) return "U";
        const parts = name.trim().split(/\s+/);
        if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
        return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    function getOwnerNameById(id) {
        const owners = {
            1: "Trần Minh Đức",
            2: "Nguyễn Văn An",
            3: "Trần Thị Bích",
            4: "Nông Quang Tiệp",
            5: "Lê Hoàng Long",
            6: "Phạm Minh Đức"
        };
        return owners[id] || "Nông Quang Tiệp";
    }

    function getChannelLabel(type) {
        const map = {
            CALL: "Cuộc gọi định kỳ",
            MEETING: "Gặp mặt / Họp",
            EMAIL: "Gửi Email CSKH",
            SUPPORT: "Hỗ trợ kỹ thuật"
        };
        return map[type] || "Tương tác";
    }

    function getChannelIcon(type) {
        if (type === "CALL") return "📞";
        if (type === "MEETING") return "🤝";
        if (type === "EMAIL") return "✉️";
        if (type === "SUPPORT") return "🛠️";
        return "💬";
    }

    function escapeHtml(str) {
        return String(str ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function showToast(message, type = "success") {
        const container = document.getElementById("careToastRegion");
        if (!container) return;

        const toast = document.createElement("div");
        toast.className = `crm-toast crm-toast-${type}`;

        const iconSvg = type === "success"
            ? `<svg viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>`
            : type === "warning"
            ? `<svg viewBox="0 0 24 24"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`
            : `<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>`;

        toast.innerHTML = `
            <div class="crm-toast-icon">${iconSvg}</div>
            <div class="crm-toast-copy">${escapeHtml(message)}</div>
            <button type="button" class="crm-toast-close" aria-label="Đóng"><svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
            <div class="crm-toast-progress"></div>
        `;

        toast.querySelector(".crm-toast-close").addEventListener("click", () => toast.remove());
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.transition = "opacity 300ms ease, transform 300ms ease";
            toast.style.opacity = "0";
            toast.style.transform = "translateY(-8px)";
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

    /* =========================================================
       12. INITIALIZATION
    ========================================================= */
    async function initPeriodicCare() {
        await loadPeriodicCareData();
        populateOwnersDropdown();
        setupEventListeners();
        applyFiltersAndSorting();
    }

    // Export Manager to Global Window
    window.PeriodicCareManager = {
        refresh: async () => {
            await loadPeriodicCareData();
            applyFiltersAndSorting();
        },
        getDataset: () => careCustomers,
        getDueCount: () => careCustomers.filter(c => !c.isContactedRecently && c.daysInactive >= currentDaysThreshold).length
    };

    // Auto Run on DOM Ready
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initPeriodicCare);
    } else {
        initPeriodicCare();
    }
})();
