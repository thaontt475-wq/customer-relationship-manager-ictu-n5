/**
 * ===================================================================
 * ENTERPRISE B2B CUSTOMER SUPPORT & CHURN RISK CONTROLLER (CRM-68)
 * Pure HTML5 / CSS3 / ES6+ JavaScript client-side module.
 * Manages post-sale support tickets, SLA monitoring, automatic & manual
 * churn risk detection, drawer forms, and localStorage persistence.
 * ===================================================================
 */

"use strict";

(function () {
    const API_BASE = `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

    // Required LocalStorage Keys
    const STORAGE_KEY_TICKETS = "CRM_SUPPORT_TICKETS_DATA";
    const STORAGE_KEY_CHURN = "CRM_CHURN_RISK_DATA";
    const STORAGE_KEY_CUSTOMERS = "CRM_CUSTOMERS_DATA";

    // Priority Definitions
    const PRIORITIES = {
        URGENT: { label: "Khẩn cấp", class: "priority-urgent", weight: 4 },
        HIGH: { label: "Cao", class: "priority-high", weight: 3 },
        MEDIUM: { label: "Trung bình", class: "priority-medium", weight: 2 },
        LOW: { label: "Thấp", class: "priority-low", weight: 1 }
    };

    // Status Definitions
    const STATUSES = {
        NEW: { label: "Mới tiếp nhận", class: "status-new" },
        PROCESSING: { label: "Đang xử lý", class: "status-processing" },
        WAITING_CUSTOMER: { label: "Chờ khách phản hồi", class: "status-waiting" },
        RESOLVED: { label: "Đã giải quyết", class: "status-resolved" },
        CLOSED: { label: "Đóng phiếu", class: "status-closed" }
    };

    // Categories
    const CATEGORIES = {
        TECHNICAL: { label: "Sự cố kỹ thuật / Lỗi", icon: "⚙️" },
        COMPLAINT: { label: "Khiếu nại dịch vụ / SLA", icon: "⚠️" },
        BILLING: { label: "Hóa đơn / Hợp đồng", icon: "💳" },
        FEATURE_REQUEST: { label: "Đề xuất tính năng / Quota", icon: "💡" }
    };

    // Support Assignees
    const SUPPORT_AGENTS = [
        { id: 1, name: "Trần Minh Đức", role: "Kỹ thuật Senior", email: "duc.tm@crm.vn", phone: "0912.345.678" },
        { id: 2, name: "Nguyễn Hải Yến", role: "CSKH Lead", email: "yen.nh@crm.vn", phone: "0988.765.432" },
        { id: 3, name: "Lê Hoàng Long", role: "CSKH Enterprise", email: "long.lh@crm.vn", phone: "0934.567.890" },
        { id: 4, name: "Nông Quang Tiệp", role: "Quản trị viên / CSKH", email: "tiep.nq@crm.vn", phone: "0912.888.999" }
    ];

    // Seed Mock Tickets for Customer Care
    const DEFAULT_MOCK_TICKETS = [];
    const _UNUSED_DEFAULT_MOCK_TICKETS = [
        {
            id: 1,
            ticketCode: "TCK-2026-001",
            customerId: 1,
            customerName: "Tập đoàn Công nghệ FPT",
            title: "Sự cố Timeout cổng API Gateway đồng bộ dữ liệu chi nhánh",
            description: "Hệ thống gateway báo lỗi 504 Gateway Timeout vào đầu giờ sáng, gây ách tắc hơn 500 yêu cầu đồng bộ. Khách hàng yêu cầu kiểm tra khẩn cấp và nâng mức trần timeout quota.",
            category: "TECHNICAL",
            priority: "URGENT",
            status: "PROCESSING",
            assigneeId: 1,
            assigneeName: "Trần Minh Đức",
            creatorName: "Nông Quang Tiệp",
            createdAt: "2026-03-27T08:30:00",
            dueDate: "2026-03-27T12:00:00",
            slaBreached: true,
            timeline: [
                { time: "2026-03-27 08:30", author: "FPT IT Admin", text: "Gửi cảnh báo sự cố gián đoạn API gateway qua Hotline khẩn cấp." },
                { time: "2026-03-27 09:00", author: "Trần Minh Đức", text: "Đã trích xuất log server, phát hiện nghẽn kết nối cơ sở dữ liệu vùng miền Bắc." },
                { time: "2026-03-27 10:15", author: "Trần Minh Đức", text: "Đang tiến hành mở rộng cụm node dự phòng để giảm tải." }
            ]
        },
        {
            id: 2,
            ticketCode: "TCK-2026-002",
            customerId: 1,
            customerName: "Tập đoàn Công nghệ FPT",
            title: "Khiếu nại thời gian phản hồi kỹ thuật chậm theo cam kết SLA Gold",
            description: "Đại diện ban giám đốc FPT gửi công văn phàn nàn thời gian phản hồi sự cố tuần qua không đúng cam kết 15 phút, yêu cầu giải trình nguyên nhân và họp đối soát.",
            category: "COMPLAINT",
            priority: "HIGH",
            status: "NEW",
            assigneeId: 2,
            assigneeName: "Nguyễn Hải Yến",
            creatorName: "Nông Quang Tiệp",
            createdAt: "2026-03-28T09:15:00",
            dueDate: "2026-03-28T17:00:00",
            slaBreached: false,
            timeline: [
                { time: "2026-03-28 09:15", author: "Nguyễn Hải Yến", text: "Tiếp nhận văn bản khiếu nại SLA từ đại diện khách hàng FPT." },
                { time: "2026-03-28 09:45", author: "Nguyễn Hải Yến", text: "Đã liên hệ nhân viên Sale phụ trách Nông Quang Tiệp để phối hợp giải trình." }
            ]
        },
        {
            id: 3,
            ticketCode: "TCK-2026-003",
            customerId: 2,
            customerName: "Công ty Cổ phần VNG",
            title: "Hỗ trợ mở rộng quota lưu trữ và cấp thêm 50 tài khoản người dùng",
            description: "VNG chuẩn bị ra mắt dịch vụ mới, cần nâng hạn mức gói dịch vụ lưu trữ thêm 5TB và tạo thêm 50 tài khoản người dùng phụ quyền quản lý tài nguyên.",
            category: "FEATURE_REQUEST",
            priority: "MEDIUM",
            status: "WAITING_CUSTOMER",
            assigneeId: 3,
            assigneeName: "Lê Hoàng Long",
            creatorName: "Nông Quang Tiệp",
            createdAt: "2026-03-25T14:00:00",
            dueDate: "2026-03-29T18:00:00",
            slaBreached: false,
            timeline: [
                { time: "2026-03-25 14:00", author: "VNG Team", text: "Gửi yêu cầu nâng cấp gói qua cổng Portal." },
                { time: "2026-03-26 10:00", author: "Lê Hoàng Long", text: "Đã gửi bảng báo giá bổ sung quota và đang chờ VNG xác nhận ký duyệt." }
            ]
        },
        {
            id: 4,
            ticketCode: "TCK-2026-004",
            customerId: 3,
            customerName: "Công ty TNHH Phần mềm MISA",
            title: "Lỗi xuất báo cáo tổng hợp thuế GTGT bị sai định dạng XML theo TT78",
            description: "Khách hàng MISA phản ánh tính năng kết xuất dữ liệu hóa đơn điện tử bị sai thẻ cấu trúc số hóa đơn mới, khiến cơ quan thuế từ chối tiếp nhận hồ sơ kê khai.",
            category: "TECHNICAL",
            priority: "HIGH",
            status: "PROCESSING",
            assigneeId: 1,
            assigneeName: "Trần Minh Đức",
            creatorName: "Nông Quang Tiệp",
            createdAt: "2026-03-26T10:00:00",
            dueDate: "2026-03-27T18:00:00",
            slaBreached: true,
            timeline: [
                { time: "2026-03-26 10:00", author: "MISA Kế toán", text: "Báo lỗi qua email kèm file XML mẫu bị từ chối." },
                { time: "2026-03-26 14:00", author: "Trần Minh Đức", text: "Đang cập nhật lại schema XML parser để tương thích bản vá của Tổng cục Thuế." }
            ]
        },
        {
            id: 5,
            ticketCode: "TCK-2026-005",
            customerId: 5,
            customerName: "Công ty CP Đầu tư Thế Giới Di Động",
            title: "Hướng dẫn tích hợp Webhook cảnh báo đơn hàng phát sinh vào Telegram BOT",
            description: "Đội ngũ vận hành của Thế Giới Di Động cần tài liệu và cấu hình mẫu để đẩy thông báo realtime qua webhook về kênh Telegram nội bộ.",
            category: "TECHNICAL",
            priority: "LOW",
            status: "RESOLVED",
            assigneeId: 2,
            assigneeName: "Nguyễn Hải Yến",
            creatorName: "Nguyễn Văn An",
            createdAt: "2026-03-20T11:00:00",
            dueDate: "2026-03-22T17:00:00",
            slaBreached: false,
            timeline: [
                { time: "2026-03-20 11:00", author: "TGDD Tech", text: "Gửi câu hỏi hỗ trợ kỹ thuật." },
                { time: "2026-03-21 15:30", author: "Nguyễn Hải Yến", text: "Đã gửi tài liệu Postman collection và hướng dẫn tích hợp chi tiết. Khách xác nhận thành công." }
            ]
        }
    ];

    // Module State
    let ticketsList = [];
    let churnFlagsMap = {}; // customerId -> { isManualFlag: boolean, manualReason: string, flaggedAt: string, riskLevel: string }
    let customersList = [];

    let filterState = {
        keyword: "",
        tab: "all", // 'all', 'my', 'urgent', 'churn'
        priority: "",
        status: "",
        category: "",
        assigneeId: "",
        page: 1,
        pageSize: 10
    };

    let activeDrawerTicketId = null;

    /* =========================================================
       PERSISTENCE ENGINE (localStorage)
    ========================================================= */
    function loadTickets() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_TICKETS);
            if (raw) {
                const parsed = JSON.parse(raw);
                if (Array.isArray(parsed) && parsed.length > 0) {
                    return parsed;
                }
            }
        } catch (e) {
            console.warn("Lỗi đọc dữ liệu CRM_SUPPORT_TICKETS_DATA:", e);
        }
        // Save default seed tickets
        saveTickets(DEFAULT_MOCK_TICKETS);
        return [...DEFAULT_MOCK_TICKETS];
    }

    function saveTickets(list) {
        try {
            ticketsList = list;
            localStorage.setItem(STORAGE_KEY_TICKETS, JSON.stringify(list));
        } catch (e) {
            console.error("Lỗi lưu CRM_SUPPORT_TICKETS_DATA:", e);
        }
    }

    function loadChurnFlags() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_CHURN);
            if (raw) {
                const parsed = JSON.parse(raw);
                if (typeof parsed === "object" && parsed !== null) {
                    return parsed;
                }
            }
        } catch (e) {
            console.warn("Lỗi đọc dữ liệu CRM_CHURN_RISK_DATA:", e);
        }
        return {};
    }

    function saveChurnFlags(map) {
        try {
            churnFlagsMap = map;
            localStorage.setItem(STORAGE_KEY_CHURN, JSON.stringify(map));
        } catch (e) {
            console.error("Lỗi lưu CRM_CHURN_RISK_DATA:", e);
        }
    }

    function loadCustomers() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_CUSTOMERS);
            if (raw) {
                const parsed = JSON.parse(raw);
                if (Array.isArray(parsed) && parsed.length > 0) {
                    return parsed;
                }
            }
        } catch (e) {
            console.warn("Lỗi đọc CRM_CUSTOMERS_DATA:", e);
        }
        return [
            { id: 1, companyName: "Tập đoàn Công nghệ FPT", taxCode: "0101248141", ownerName: "Nông Quang Tiệp", email: "contact@fpt.com.vn", phone: "02473007300" },
            { id: 2, companyName: "Công ty Cổ phần VNG", taxCode: "0303579890", ownerName: "Nông Quang Tiệp", email: "partner@vng.com.vn", phone: "02839623888" },
            { id: 3, companyName: "Công ty TNHH Phần mềm MISA", taxCode: "0100779774", ownerName: "Nguyễn Văn An", email: "contact@misa.vn", phone: "02437959595" },
            { id: 4, companyName: "Tập đoàn Bưu chính Viễn thông VNPT", taxCode: "0100684378", ownerName: "Nông Quang Tiệp", email: "vanphong@vnpt.vn", phone: "02437741091" },
            { id: 5, companyName: "Công ty CP Đầu tư Thế Giới Di Động", taxCode: "0303274391", ownerName: "Nguyễn Văn An", email: "lienhe@thegioididong.com", phone: "02838125960" },
            { id: 6, companyName: "Ngân hàng TMCP Quân Đội (MBBank)", taxCode: "0100283873", ownerName: "Nông Quang Tiệp", email: "mb247@mbbank.com.vn", phone: "1900545426" },
            { id: 7, companyName: "Công ty CP Dược phẩm Imexpharm", taxCode: "1400384433", ownerName: "Nông Quang Tiệp", email: "imexpharm@imexpharm.com", phone: "02773851941" },
            { id: 8, companyName: "Công ty Cổ phần Tập đoàn Hòa Phát", taxCode: "0900189284", ownerName: "Nguyễn Văn An", email: "contact@hoaphat.com.vn", phone: "02462848666" }
        ];
    }

    /* =========================================================
       CHURN RISK LOGIC (CRM-68 CORE)
       - Auto Condition 1: Customer has >= 2 unresolved tickets
       - Auto Condition 2: Customer has unresolved High or Urgent tickets
       - Auto Condition 3: Customer has complaint category tickets
       - Manual Override: CSKH manual flag with custom reason
    ========================================================= */
    function evaluateCustomerChurnRisk(customerId) {
        const cId = Number(customerId);
        const customer = customersList.find(c => Number(c.id) === cId);
        const customerName = customer ? (customer.companyName || customer.name) : `Khách hàng #${cId}`;

        // Get all open tickets for this customer
        const customerOpenTickets = ticketsList.filter(t =>
            Number(t.customerId) === cId &&
            t.status !== "RESOLVED" &&
            t.status !== "CLOSED"
        );

        const reasons = [];
        let autoRisk = false;
        let riskLevel = "LOW";

        // Check for urgent or high priority unresolved tickets
        const severeTickets = customerOpenTickets.filter(t => t.priority === "URGENT" || t.priority === "HIGH");
        if (severeTickets.length > 0) {
            autoRisk = true;
            riskLevel = severeTickets.some(t => t.priority === "URGENT") ? "CRITICAL" : "HIGH";
            reasons.push(`Có ${severeTickets.length} yêu cầu hỗ trợ mức độ [Khẩn cấp / Cao] đang chờ xử lý.`);
        }

        // Check for complaints
        const complaintTickets = customerOpenTickets.filter(t => t.category === "COMPLAINT");
        if (complaintTickets.length > 0) {
            autoRisk = true;
            if (riskLevel === "LOW") riskLevel = "HIGH";
            reasons.push(`Khách hàng có khiếu nại dịch vụ / SLA chưa giải quyết.`);
        }

        // Check for >= 2 open tickets
        if (customerOpenTickets.length >= 2) {
            autoRisk = true;
            if (riskLevel === "LOW") riskLevel = "MEDIUM";
            reasons.push(`Tồn đọng ${customerOpenTickets.length} phiếu yêu cầu hỗ trợ chưa đóng.`);
        }

        // Check SLA breaches
        const breachedTickets = customerOpenTickets.filter(t => t.slaBreached);
        if (breachedTickets.length > 0) {
            autoRisk = true;
            reasons.push(`Có ${breachedTickets.length} phiếu đã quá thời hạn cam kết SLA.`);
        }

        // Check manual flag
        const manualFlag = churnFlagsMap[cId];
        const isManual = !!manualFlag?.isManualFlag;
        if (isManual) {
            if (manualFlag.manualReason) {
                reasons.unshift(`[Cờ thủ công CSKH]: ${manualFlag.manualReason}`);
            } else {
                reasons.unshift(`[Cờ thủ công CSKH]: Nhân viên đánh giá nguy cơ rời bỏ cao.`);
            }
            if (riskLevel === "LOW") riskLevel = "HIGH";
        }

        const isRisk = autoRisk || isManual;

        return {
            customerId: cId,
            customerName,
            isRisk,
            riskLevel: isRisk ? (riskLevel === "LOW" ? "MEDIUM" : riskLevel) : "NONE",
            reasons,
            openTicketCount: customerOpenTickets.length,
            severeTicketCount: severeTickets.length,
            isManual,
            manualReason: manualFlag?.manualReason || ""
        };
    }

    function getAllChurnRiskCustomers() {
        const atRiskList = [];
        customersList.forEach(c => {
            const evaluation = evaluateCustomerChurnRisk(c.id);
            if (evaluation.isRisk) {
                atRiskList.push({
                    customer: c,
                    evaluation
                });
            }
        });
        return atRiskList;
    }

    /* =========================================================
       PAGE RENDER & CONTROLLER
    ========================================================= */
    function init() {
        ticketsList = loadTickets();
        churnFlagsMap = loadChurnFlags();
        customersList = loadCustomers();

        // Check if we are on support-tickets.html page
        const isSupportPage = !!document.getElementById("ticketsTableBody");
        if (isSupportPage) {
            setupSupportPage();
        }

        // Expose Public API on window for cross-page usage (customers.html, customer-360.html)
        exposeGlobalApi();
    }

    function setupSupportPage() {
        populateSelectOptions();
        bindEvents();
        updateKpis();
        renderChurnBanner();
        renderTicketsTable();
    }

    function populateSelectOptions() {
        // Customer Select in Drawer
        const custSelect = document.getElementById("ticketCustomerSelect");
        if (custSelect) {
            custSelect.innerHTML = '<option value="">-- Chọn khách hàng doanh nghiệp --</option>';
            customersList.forEach(c => {
                const opt = document.createElement("option");
                opt.value = c.id;
                opt.textContent = `${c.companyName || c.name} (MST: ${c.taxCode || "N/A"})`;
                custSelect.appendChild(opt);
            });
        }

        // Assignee selects (both in Drawer and filter toolbar)
        const assigneeDrawer = document.getElementById("ticketAssignee");
        const filterAssignee = document.getElementById("filterAssignee");

        if (assigneeDrawer) {
            assigneeDrawer.innerHTML = '<option value="">-- Chọn nhân viên xử lý --</option>';
            SUPPORT_AGENTS.forEach(a => {
                const opt = document.createElement("option");
                opt.value = a.id;
                opt.textContent = `${a.name} (${a.role})`;
                assigneeDrawer.appendChild(opt);
            });
        }

        if (filterAssignee) {
            filterAssignee.innerHTML = '<option value="">-- Người xử lý --</option>';
            SUPPORT_AGENTS.forEach(a => {
                const opt = document.createElement("option");
                opt.value = a.id;
                opt.textContent = a.name;
                filterAssignee.appendChild(opt);
            });
        }
    }

    function bindEvents() {
        // Open/Close Drawer
        document.getElementById("btnOpenCreateTicket")?.addEventListener("click", () => openTicketDrawer(null));
        document.getElementById("btnCloseDrawer")?.addEventListener("click", closeTicketDrawer);
        document.getElementById("btnCancelTicket")?.addEventListener("click", closeTicketDrawer);
        document.getElementById("ticketDrawerOverlay")?.addEventListener("click", closeTicketDrawer);

        // Filter Tabs
        document.querySelectorAll(".support-tab-btn").forEach(btn => {
            btn.addEventListener("click", () => {
                document.querySelectorAll(".support-tab-btn").forEach(b => b.classList.remove("active"));
                btn.classList.add("active");
                filterState.tab = btn.dataset.filterTab || "all";
                filterState.page = 1;
                renderTicketsTable();
            });
        });

        // Search & Filters
        document.getElementById("ticketSearchInput")?.addEventListener("input", (e) => {
            filterState.keyword = e.target.value.trim().toLowerCase();
            filterState.page = 1;
            renderTicketsTable();
        });

        document.getElementById("filterPriority")?.addEventListener("change", (e) => {
            filterState.priority = e.target.value;
            filterState.page = 1;
            renderTicketsTable();
        });

        document.getElementById("filterStatus")?.addEventListener("change", (e) => {
            filterState.status = e.target.value;
            filterState.page = 1;
            renderTicketsTable();
        });

        document.getElementById("filterCategory")?.addEventListener("change", (e) => {
            filterState.category = e.target.value;
            filterState.page = 1;
            renderTicketsTable();
        });

        document.getElementById("filterAssignee")?.addEventListener("change", (e) => {
            filterState.assigneeId = e.target.value;
            filterState.page = 1;
            renderTicketsTable();
        });

        document.getElementById("btnRefreshTickets")?.addEventListener("click", () => {
            ticketsList = loadTickets();
            churnFlagsMap = loadChurnFlags();
            customersList = loadCustomers();
            updateKpis();
            renderChurnBanner();
            renderTicketsTable();
            showToast("Đã tải lại danh sách phiếu hỗ trợ", "success");
        });

        // Export CSV
        document.getElementById("btnExportTickets")?.addEventListener("click", exportTicketsCsv);

        // Form Submit
        document.getElementById("ticketForm")?.addEventListener("submit", handleTicketFormSubmit);

        // Drawer Customer Change -> Preview Info
        document.getElementById("ticketCustomerSelect")?.addEventListener("change", (e) => {
            renderDrawerCustomerPreview(Number(e.target.value));
        });

        // Manual Churn Checkbox Toggle in Drawer
        document.getElementById("manualChurnCheckbox")?.addEventListener("change", (e) => {
            const wrap = document.getElementById("manualChurnReasonWrap");
            if (wrap) wrap.style.display = e.target.checked ? "block" : "none";
        });

        // Add Timeline Note
        document.getElementById("btnAddTimelineNote")?.addEventListener("click", handleAddTimelineNote);

        // Modal Churn Events
        document.getElementById("btnCancelChurnModal")?.addEventListener("click", closeChurnModal);
        document.getElementById("churnRiskModalOverlay")?.addEventListener("click", closeChurnModal);
        document.getElementById("btnSaveChurnModal")?.addEventListener("click", handleSaveChurnModal);

        // ESC Key
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") {
                closeTicketDrawer();
                closeChurnModal();
            }
        });
    }

    /* =========================================================
       KPI CARDS & CHURN BANNER
    ========================================================= */
    function updateKpis() {
        const totalOpen = ticketsList.filter(t => t.status !== "RESOLVED" && t.status !== "CLOSED").length;
        const urgentCount = ticketsList.filter(t => (t.priority === "URGENT" || t.slaBreached) && t.status !== "RESOLVED" && t.status !== "CLOSED").length;
        
        const churnRiskCustomers = getAllChurnRiskCustomers();
        const churnCount = churnRiskCustomers.length;

        const resolvedCount = ticketsList.filter(t => t.status === "RESOLVED" || t.status === "CLOSED").length;
        const totalClosedOrResolved = resolvedCount;
        const totalTickets = ticketsList.length;
        const resolutionRate = totalTickets > 0 ? Math.round((totalClosedOrResolved / totalTickets) * 100) : 100;

        const elOpen = document.getElementById("kpiTotalOpen");
        const elUrgent = document.getElementById("kpiUrgent");
        const elChurn = document.getElementById("kpiChurnRisk");
        const elRate = document.getElementById("kpiResolutionRate");

        if (elOpen) elOpen.textContent = String(totalOpen);
        if (elUrgent) elUrgent.textContent = String(urgentCount);
        if (elChurn) elChurn.textContent = String(churnCount);
        if (elRate) elRate.textContent = `${resolutionRate}%`;
    }

    function renderChurnBanner() {
        const container = document.getElementById("churnAlertBanner");
        if (!container) return;

        const churnRiskCustomers = getAllChurnRiskCustomers();
        if (churnRiskCustomers.length === 0) {
            container.innerHTML = "";
            container.style.display = "none";
            return;
        }

        container.style.display = "block";
        const topCustomerItem = churnRiskCustomers[0];
        const cust = topCustomerItem.customer;
        const evalInfo = topCustomerItem.evaluation;

        const salesRepName = cust.ownerName || cust.owner || "Nông Quang Tiệp (Trưởng phòng KD)";
        const salesRepEmail = cust.ownerEmail || "tiep.nq@crm.vn";
        const salesRepPhone = cust.ownerPhone || "0912.888.999";

        container.innerHTML = `
            <div class="churn-banner-container" style="animation:fadeIn 0.3s ease;">
                <div class="churn-alert-banner">
                    <div style="display:flex; gap:16px; align-items:flex-start; flex:1; min-width:300px;">
                        <div style="font-size:32px; line-height:1; flex-shrink:0;">⚠️</div>
                        <div>
                            <div style="display:flex; align-items:center; gap:10px; margin-bottom:6px; flex-wrap:wrap;">
                                <h3 style="margin:0; font-size:16.5px; font-weight:800; color:var(--crm-danger);">
                                    PHÁT HIỆN ${churnRiskCustomers.length} KHÁCH HÀNG CÓ CỜ RỦI RO RỜI BỎ CAO (CHURN RISK)
                                </h3>
                                <span class="churn-risk-badge" style="background:#fee2e2; color:#b91c1c; font-size:11px; padding:3px 8px; border-radius:4px; font-weight:700;">
                                    CẦN CAN THIỆP KHẨN CẤP
                                </span>
                            </div>
                            <p style="margin:0 0 10px; font-size:13.5px; color:var(--crm-text); line-height:1.5;">
                                Đơn vị tiêu biểu: <strong>${escapeHtml(cust.companyName || cust.name)}</strong> (MST: ${escapeHtml(cust.taxCode || "N/A")})
                                — ${escapeHtml(evalInfo.reasons.join(" • ") || "Nhiều phản ánh kỹ thuật chưa giải quyết")}.
                            </p>
                            <div style="display:flex; gap:12px; align-items:center; flex-wrap:wrap;">
                                <a href="customer-360.html?id=${cust.id}" class="crm-btn crm-btn-secondary" style="font-size:12px; padding:4px 12px; text-decoration:none;">
                                    🔍 Xem hồ sơ 360° khách hàng
                                </a>
                                <button type="button" class="crm-btn crm-btn-secondary btn-quick-churn-modal" data-id="${cust.id}" style="font-size:12px; padding:4px 12px;">
                                    ⚙️ Quản lý cờ rủi ro
                                </button>
                            </div>
                        </div>
                    </div>

                    <!-- Sales Alert Box -->
                    <div class="sales-alert-box" style="min-width:280px; flex-shrink:0;">
                        <div style="font-size:11px; text-transform:uppercase; letter-spacing:0.5px; font-weight:700; color:var(--crm-danger); margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                            <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" fill="none" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
                            Sale phụ trách cần phối hợp
                        </div>
                        <div style="font-weight:700; font-size:14.5px; color:var(--crm-text); margin-bottom:4px;">
                            ${escapeHtml(salesRepName)}
                        </div>
                        <div style="font-size:12px; color:var(--crm-muted); display:flex; flex-direction:column; gap:3px;">
                            <span>📧 ${escapeHtml(salesRepEmail)}</span>
                            <span>☎ ${escapeHtml(salesRepPhone)}</span>
                        </div>
                    </div>
                </div>
            </div>
        `;

        container.querySelector(".btn-quick-churn-modal")?.addEventListener("click", (e) => {
            const cId = Number(e.currentTarget.dataset.id);
            openChurnModal(cId);
        });
    }

    /* =========================================================
       TICKETS TABLE FILTERING & RENDERING
    ========================================================= */
    function filterTickets() {
        return ticketsList.filter(t => {
            // Keyword filter
            if (filterState.keyword) {
                const kw = filterState.keyword;
                const matchCode = t.ticketCode.toLowerCase().includes(kw);
                const matchCustomer = (t.customerName || "").toLowerCase().includes(kw);
                const matchTitle = (t.title || "").toLowerCase().includes(kw);
                const matchDesc = (t.description || "").toLowerCase().includes(kw);
                if (!matchCode && !matchCustomer && !matchTitle && !matchDesc) return false;
            }

            // Tab filter
            if (filterState.tab === "my") {
                // Assignee id 1 or 4 (current user mock)
                if (Number(t.assigneeId) !== 1 && Number(t.assigneeId) !== 4) return false;
            } else if (filterState.tab === "urgent") {
                if (t.priority !== "URGENT" && !t.slaBreached) return false;
                if (t.status === "RESOLVED" || t.status === "CLOSED") return false;
            } else if (filterState.tab === "churn") {
                const churnInfo = evaluateCustomerChurnRisk(t.customerId);
                if (!churnInfo.isRisk) return false;
            }

            // Dropdown filters
            if (filterState.priority && t.priority !== filterState.priority) return false;
            if (filterState.status && t.status !== filterState.status) return false;
            if (filterState.category && t.category !== filterState.category) return false;
            if (filterState.assigneeId && Number(t.assigneeId) !== Number(filterState.assigneeId)) return false;

            return true;
        });
    }

    function renderTicketsTable() {
        const tbody = document.getElementById("ticketsTableBody");
        const emptyState = document.getElementById("ticketsEmptyState");
        const paginationInfo = document.getElementById("ticketsPaginationInfo");
        const paginationControls = document.getElementById("ticketsPaginationControls");

        if (!tbody) return;
        tbody.innerHTML = "";

        const filtered = filterTickets();
        const total = filtered.length;

        if (total === 0) {
            if (emptyState) emptyState.style.display = "block";
            if (paginationInfo) paginationInfo.textContent = "Hiển thị 0 / 0 yêu cầu";
            if (paginationControls) paginationControls.innerHTML = "";
            return;
        }

        if (emptyState) emptyState.style.display = "none";

        // Pagination slice
        const totalPages = Math.ceil(total / filterState.pageSize);
        const curPage = Math.min(filterState.page, totalPages);
        const startIndex = (curPage - 1) * filterState.pageSize;
        const pageItems = filtered.slice(startIndex, startIndex + filterState.pageSize);

        pageItems.forEach(t => {
            const tr = document.createElement("tr");

            // Evaluate customer churn risk
            const churnInfo = evaluateCustomerChurnRisk(t.customerId);
            const churnBadge = churnInfo.isRisk
                ? `<span class="churn-risk-badge" title="${escapeHtml(churnInfo.reasons.join(' • ') || 'Rủi ro rời bỏ')}">
                     ⚠️ Rủi ro rời bỏ
                   </span>`
                : "";

            const priorityDef = PRIORITIES[t.priority] || PRIORITIES.MEDIUM;
            const statusDef = STATUSES[t.status] || STATUSES.NEW;
            const categoryDef = CATEGORIES[t.category] || CATEGORIES.TECHNICAL;

            tr.innerHTML = `
                <td>
                    <strong style="font-family:monospace; font-size:13px; color:var(--crm-primary); cursor:pointer;" class="btn-edit-ticket" data-id="${t.id}">
                        ${escapeHtml(t.ticketCode)}
                    </strong>
                    <div style="font-size:11.5px; color:var(--crm-muted); margin-top:2px;">
                        ${formatDateTime(t.createdAt)}
                    </div>
                </td>
                <td>
                    <div style="display:flex; align-items:center; gap:6px; flex-wrap:wrap;">
                        <a href="customer-360.html?id=${t.customerId}" style="font-weight:700; color:var(--crm-text); text-decoration:none;" title="Xem Customer 360">
                            ${escapeHtml(t.customerName)}
                        </a>
                        ${churnBadge}
                    </div>
                    <div style="font-size:11.5px; color:var(--crm-muted); margin-top:2px;">
                        ID #${t.customerId}
                    </div>
                </td>
                <td>
                    <div style="font-weight:600; color:var(--crm-text); margin-bottom:3px; max-width:380px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${escapeHtml(t.title)}">
                        ${escapeHtml(t.title)}
                    </div>
                    <div style="font-size:12px; color:var(--crm-muted); display:flex; align-items:center; gap:6px;">
                        <span>${categoryDef.icon} ${categoryDef.label}</span>
                        ${t.slaBreached ? `<span style="color:var(--crm-danger); font-weight:700;">• Quá hạn SLA</span>` : ""}
                    </div>
                </td>
                <td>
                    <span class="priority-badge ${priorityDef.class}">
                        ${priorityDef.label}
                    </span>
                </td>
                <td>
                    <span class="status-pill ${statusDef.class}">
                        <span class="status-dot"></span>
                        ${statusDef.label}
                    </span>
                </td>
                <td>
                    <div style="font-weight:600; color:var(--crm-text); font-size:13px;">
                        ${escapeHtml(t.assigneeName || "Chưa phân công")}
                    </div>
                    <div style="font-size:11.5px; color:${t.slaBreached ? 'var(--crm-danger)' : 'var(--crm-muted)'}; margin-top:2px;">
                        Hạn: ${formatDateTime(t.dueDate)}
                    </div>
                </td>
                <td style="text-align:right;">
                    <div style="display:inline-flex; gap:6px;">
                        <button type="button" class="crm-btn crm-btn-secondary btn-edit-ticket" data-id="${t.id}" style="padding:4px 8px; font-size:12px;" title="Chỉnh sửa & cập nhật tiến độ">
                            ✏️
                        </button>
                        <button type="button" class="crm-btn crm-btn-secondary btn-manage-churn" data-customer-id="${t.customerId}" style="padding:4px 8px; font-size:12px;" title="Cờ rủi ro rời bỏ">
                            🚩
                        </button>
                    </div>
                </td>
            `;

            tbody.appendChild(tr);
        });

        // Bind table row buttons
        tbody.querySelectorAll(".btn-edit-ticket").forEach(btn => {
            btn.addEventListener("click", () => {
                const id = Number(btn.dataset.id);
                openTicketDrawer(id);
            });
        });

        tbody.querySelectorAll(".btn-manage-churn").forEach(btn => {
            btn.addEventListener("click", () => {
                const cId = Number(btn.dataset.customerId);
                openChurnModal(cId);
            });
        });

        // Pagination Info & Buttons
        if (paginationInfo) {
            paginationInfo.textContent = `Hiển thị ${startIndex + 1} - ${Math.min(startIndex + filterState.pageSize, total)} / ${total} yêu cầu`;
        }

        if (paginationControls) {
            paginationControls.innerHTML = "";
            for (let i = 1; i <= totalPages; i++) {
                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = `crm-btn crm-btn-secondary ${i === curPage ? 'crm-btn-primary' : ''}`;
                btn.style.cssText = "min-width:32px; height:32px; padding:0 8px; font-size:12px;";
                btn.textContent = String(i);
                btn.addEventListener("click", () => {
                    filterState.page = i;
                    renderTicketsTable();
                });
                paginationControls.appendChild(btn);
            }
        }
    }

    /* =========================================================
       DRAWER FORM & TIMELINE
    ========================================================= */
    function openTicketDrawer(ticketId = null) {
        activeDrawerTicketId = ticketId;
        const drawer = document.getElementById("ticketDrawer");
        const overlay = document.getElementById("ticketDrawerOverlay");
        const drawerTitle = document.getElementById("drawerTitle");
        const timelineSection = document.getElementById("ticketTimelineSection");

        if (!drawer || !overlay) return;

        if (ticketId) {
            // Edit Mode
            const t = ticketsList.find(item => item.id === ticketId);
            if (!t) return;

            drawerTitle.textContent = `Cập nhật Phiếu Hỗ trợ [${t.ticketCode}]`;
            setVal("ticketId", t.id);
            setVal("ticketCustomerSelect", t.customerId);
            setVal("ticketTitle", t.title);
            setVal("ticketCategory", t.category);
            setVal("ticketPriority", t.priority);
            setVal("ticketStatus", t.status);
            setVal("ticketAssignee", t.assigneeId);
            setVal("ticketDescription", t.description);

            // Due Date formatting for input datetime-local
            if (t.dueDate) {
                try {
                    const d = new Date(t.dueDate);
                    const iso = d.toISOString().slice(0, 16);
                    setVal("ticketDueDate", iso);
                } catch (_) {
                    setVal("ticketDueDate", "");
                }
            } else {
                setVal("ticketDueDate", "");
            }

            // Customer details preview
            renderDrawerCustomerPreview(t.customerId);

            // Manual churn checkbox
            const churnEval = evaluateCustomerChurnRisk(t.customerId);
            const churnCheck = document.getElementById("manualChurnCheckbox");
            const reasonWrap = document.getElementById("manualChurnReasonWrap");
            const reasonInput = document.getElementById("manualChurnReason");

            if (churnCheck) churnCheck.checked = churnEval.isManual;
            if (reasonWrap) reasonWrap.style.display = churnEval.isManual ? "block" : "none";
            if (reasonInput) reasonInput.value = churnEval.manualReason || "";

            // Timeline
            if (timelineSection) timelineSection.style.display = "block";
            renderDrawerTimeline(t.timeline || []);

        } else {
            // Create Mode
            drawerTitle.textContent = "Ghi nhận Yêu cầu Hỗ trợ Sau bán";
            setVal("ticketId", "");
            setVal("ticketCustomerSelect", "");
            setVal("ticketTitle", "");
            setVal("ticketCategory", "TECHNICAL");
            setVal("ticketPriority", "MEDIUM");
            setVal("ticketStatus", "NEW");
            setVal("ticketAssignee", "1");
            setVal("ticketDescription", "");

            // Default due date: now + 24 hours
            const now = new Date();
            now.setHours(now.getHours() + 24);
            setVal("ticketDueDate", now.toISOString().slice(0, 16));

            // Hide customer preview
            const preview = document.getElementById("drawerCustomerDetails");
            if (preview) preview.style.display = "none";

            const churnCheck = document.getElementById("manualChurnCheckbox");
            const reasonWrap = document.getElementById("manualChurnReasonWrap");
            const reasonInput = document.getElementById("manualChurnReason");
            if (churnCheck) churnCheck.checked = false;
            if (reasonWrap) reasonWrap.style.display = "none";
            if (reasonInput) reasonInput.value = "";

            if (timelineSection) timelineSection.style.display = "none";
        }

        drawer.classList.add("open");
        overlay.classList.add("open");
    }

    function closeTicketDrawer() {
        document.getElementById("ticketDrawer")?.classList.remove("open");
        document.getElementById("ticketDrawerOverlay")?.classList.remove("open");
        activeDrawerTicketId = null;
    }

    function renderDrawerCustomerPreview(customerId) {
        const preview = document.getElementById("drawerCustomerDetails");
        if (!preview) return;

        if (!customerId) {
            preview.style.display = "none";
            preview.innerHTML = "";
            return;
        }

        const cust = customersList.find(c => Number(c.id) === Number(customerId));
        if (!cust) {
            preview.style.display = "none";
            return;
        }

        const churnEval = evaluateCustomerChurnRisk(customerId);
        const churnBadge = churnEval.isRisk
            ? `<span class="churn-risk-badge" style="background:#fee2e2; color:#b91c1c; font-size:11px; padding:2px 7px; border-radius:4px; font-weight:700;">
                 ⚠️ RỦI RO RỜI BỎ (${churnEval.riskLevel})
               </span>`
            : `<span style="color:var(--crm-success); font-weight:600; font-size:11.5px;">✓ Khách hàng ổn định</span>`;

        preview.style.display = "block";
        preview.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
                <strong style="font-size:13.5px; color:var(--crm-text);">${escapeHtml(cust.companyName || cust.name)}</strong>
                ${churnBadge}
            </div>
            <div style="font-size:12px; color:var(--crm-muted); display:grid; grid-template-columns:1fr 1fr; gap:6px;">
                <div>MST: <strong style="color:var(--crm-text);">${escapeHtml(cust.taxCode || "N/A")}</strong></div>
                <div>Đại diện Sale: <strong style="color:var(--crm-primary);">${escapeHtml(cust.ownerName || cust.owner || "Nông Quang Tiệp")}</strong></div>
                <div>Hotline: ${escapeHtml(cust.phone || "—")}</div>
                <div>Email: ${escapeHtml(cust.email || "—")}</div>
            </div>
            ${churnEval.isRisk ? `
                <div style="margin-top:8px; padding-top:6px; border-top:1px dashed var(--crm-border); font-size:11.5px; color:var(--crm-danger);">
                    <strong>Cảnh báo:</strong> ${escapeHtml(churnEval.reasons.join(" • "))}
                </div>
            ` : ""}
        `;
    }

    function renderDrawerTimeline(timeline) {
        const list = document.getElementById("ticketTimelineList");
        if (!list) return;

        if (!timeline || timeline.length === 0) {
            list.innerHTML = '<div style="font-size:12px; color:var(--crm-muted); text-align:center; padding:10px;">Chưa có ghi chú tiến độ.</div>';
            return;
        }

        list.innerHTML = timeline.map(item => `
            <div class="timeline-item-entry">
                <div class="timeline-item-header">
                    <span class="author">${escapeHtml(item.author || "CSKH")}</span>
                    <span class="time">${escapeHtml(item.time || "")}</span>
                </div>
                <div class="timeline-item-body">
                    ${escapeHtml(item.text || "")}
                </div>
            </div>
        `).join("");
    }

    function handleAddTimelineNote() {
        const noteInput = document.getElementById("newTimelineNote");
        if (!noteInput || !activeDrawerTicketId) return;

        const text = noteInput.value.trim();
        if (!text) {
            noteInput.focus();
            return;
        }

        const ticket = ticketsList.find(t => t.id === activeDrawerTicketId);
        if (!ticket) return;

        if (!Array.isArray(ticket.timeline)) ticket.timeline = [];

        const now = new Date();
        const timeStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")} ${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;

        ticket.timeline.push({
            time: timeStr,
            author: "Nông Quang Tiệp (CSKH)",
            text
        });

        saveTickets(ticketsList);
        renderDrawerTimeline(ticket.timeline);
        noteInput.value = "";
        showToast("Đã thêm ghi nhận tiến độ mới", "success");
    }

    function handleTicketFormSubmit(e) {
        e.preventDefault();

        const ticketId = getVal("ticketId");
        const customerId = Number(getVal("ticketCustomerSelect"));
        const title = getVal("ticketTitle");
        const category = getVal("ticketCategory");
        const priority = getVal("ticketPriority");
        const status = getVal("ticketStatus");
        const assigneeId = Number(getVal("ticketAssignee"));
        const dueDate = getVal("ticketDueDate");
        const description = getVal("ticketDescription");

        if (!customerId) {
            alert("Vui lòng chọn khách hàng doanh nghiệp!");
            document.getElementById("ticketCustomerSelect")?.focus();
            return;
        }

        if (!title) {
            alert("Vui lòng nhập tiêu đề yêu cầu!");
            document.getElementById("ticketTitle")?.focus();
            return;
        }

        const customer = customersList.find(c => Number(c.id) === customerId);
        const customerName = customer ? (customer.companyName || customer.name) : `Khách hàng #${customerId}`;
        const assignee = SUPPORT_AGENTS.find(a => a.id === assigneeId);
        const assigneeName = assignee ? assignee.name : "Chưa phân công";

        // Handle manual churn risk toggle
        const manualChurnCheck = document.getElementById("manualChurnCheckbox")?.checked;
        const manualChurnReason = getVal("manualChurnReason");

        if (manualChurnCheck) {
            churnFlagsMap[customerId] = {
                customerId,
                isManualFlag: true,
                manualReason: manualChurnReason || "CSKH gắn cờ thủ công rủi ro rời bỏ.",
                flaggedAt: new Date().toISOString(),
                riskLevel: "HIGH"
            };
        } else {
            delete churnFlagsMap[customerId];
        }
        saveChurnFlags(churnFlagsMap);

        if (ticketId) {
            // Update Existing
            const index = ticketsList.findIndex(t => t.id === Number(ticketId));
            if (index !== -1) {
                const existing = ticketsList[index];
                ticketsList[index] = {
                    ...existing,
                    customerId,
                    customerName,
                    title,
                    category,
                    priority,
                    status,
                    assigneeId,
                    assigneeName,
                    dueDate: dueDate || existing.dueDate,
                    description,
                    slaBreached: priority === "URGENT" && status !== "RESOLVED" && status !== "CLOSED"
                };
                showToast("Cập nhật phiếu hỗ trợ thành công", "success");
            }
        } else {
            // Create New
            const newId = ticketsList.length > 0 ? Math.max(...ticketsList.map(t => t.id)) + 1 : 1;
            const now = new Date();
            const ticketCode = `TCK-${now.getFullYear()}-${String(newId).padStart(3, "0")}`;

            const newTicket = {
                id: newId,
                ticketCode,
                customerId,
                customerName,
                title,
                category,
                priority,
                status,
                assigneeId,
                assigneeName,
                creatorName: "Nông Quang Tiệp",
                createdAt: now.toISOString(),
                dueDate: dueDate || new Date(now.getTime() + 24 * 3600 * 1000).toISOString(),
                description,
                slaBreached: false,
                timeline: [
                    {
                        time: formatDateTime(now.toISOString()),
                        author: "Nông Quang Tiệp (Tiếp nhận)",
                        text: `Mở mới phiếu hỗ trợ: "${title}"`
                    }
                ]
            };
            ticketsList.unshift(newTicket);
            showToast(`Đã tạo phiếu hỗ trợ [${ticketCode}] thành công`, "success");
        }

        saveTickets(ticketsList);
        closeTicketDrawer();
        updateKpis();
        renderChurnBanner();
        renderTicketsTable();
    }

    /* =========================================================
       CHURN RISK MODAL
    ========================================================= */
    let currentModalCustomerId = null;

    function openChurnModal(customerId) {
        currentModalCustomerId = customerId;
        const modal = document.getElementById("churnRiskModal");
        const overlay = document.getElementById("churnRiskModalOverlay");
        if (!modal || !overlay) return;

        const cust = customersList.find(c => Number(c.id) === Number(customerId));
        if (!cust) return;

        const evalInfo = evaluateCustomerChurnRisk(customerId);

        const nameEl = document.getElementById("churnModalCustomerName");
        const taxEl = document.getElementById("churnModalCustomerTax");
        const reasonsList = document.getElementById("churnModalReasonsList");
        const salesAlert = document.getElementById("churnModalSalesAlert");
        const flagCheckbox = document.getElementById("modalChurnFlagCheckbox");
        const reasonInput = document.getElementById("modalChurnReasonInput");

        if (nameEl) nameEl.textContent = cust.companyName || cust.name;
        if (taxEl) taxEl.textContent = `MST: ${cust.taxCode || 'N/A'} • Mã KH: #${cust.id}`;

        if (salesAlert) {
            salesAlert.innerHTML = `
                <div style="font-size:11px; text-transform:uppercase; font-weight:700; color:var(--crm-danger); margin-bottom:4px;">
                    Nhân viên kinh doanh phụ trách (Sales Rep):
                </div>
                <div style="font-weight:700; font-size:14px; color:var(--crm-text);">
                    ${escapeHtml(cust.ownerName || cust.owner || "Nông Quang Tiệp")}
                </div>
                <div style="font-size:12px; color:var(--crm-muted); margin-top:2px;">
                    Email: ${escapeHtml(cust.email || "tiep.nq@crm.vn")} • Hotline: ${escapeHtml(cust.phone || "0912.888.999")}
                </div>
            `;
        }

        if (reasonsList) {
            if (evalInfo.reasons.length === 0) {
                reasonsList.innerHTML = '<li style="color:var(--crm-success);">Chưa phát hiện rủi ro rời bỏ từ dữ liệu phiếu hỗ trợ.</li>';
            } else {
                reasonsList.innerHTML = evalInfo.reasons.map(r => `<li>${escapeHtml(r)}</li>`).join("");
            }
        }

        if (flagCheckbox) flagCheckbox.checked = evalInfo.isManual;
        if (reasonInput) reasonInput.value = evalInfo.manualReason || "";

        modal.style.display = "block";
        overlay.classList.add("open");
    }

    function closeChurnModal() {
        const modal = document.getElementById("churnRiskModal");
        const overlay = document.getElementById("churnRiskModalOverlay");
        if (modal) modal.style.display = "none";
        if (overlay) overlay.classList.remove("open");
        currentModalCustomerId = null;
    }

    function handleSaveChurnModal() {
        if (!currentModalCustomerId) return;

        const flagCheckbox = document.getElementById("modalChurnFlagCheckbox");
        const reasonInput = document.getElementById("modalChurnReasonInput");

        const isFlag = !!flagCheckbox?.checked;
        const reason = reasonInput ? reasonInput.value.trim() : "";

        if (isFlag) {
            churnFlagsMap[currentModalCustomerId] = {
                customerId: currentModalCustomerId,
                isManualFlag: true,
                manualReason: reason || "CSKH gắn cờ thủ công rủi ro rời bỏ.",
                flaggedAt: new Date().toISOString(),
                riskLevel: "HIGH"
            };
            showToast("Đã gắn cờ rủi ro rời bỏ cho khách hàng", "warning");
        } else {
            delete churnFlagsMap[currentModalCustomerId];
            showToast("Đã hủy cờ rủi ro rời bỏ", "success");
        }

        saveChurnFlags(churnFlagsMap);
        closeChurnModal();
        updateKpis();
        renderChurnBanner();
        renderTicketsTable();
    }

    /* =========================================================
       EXPORT TICKETS CSV
    ========================================================= */
    function exportTicketsCsv() {
        const items = filterTickets();
        if (items.length === 0) {
            alert("Không có phiếu hỗ trợ nào để xuất dữ liệu.");
            return;
        }

        let csv = "\uFEFFMã phiếu,Khách hàng,Tiêu đề,Danh mục,Mức ưu tiên,Trạng thái,Người xử lý,Ngày tạo,Hạn cam kết,Quá hạn SLA\n";
        items.forEach(t => {
            const p = PRIORITIES[t.priority]?.label || t.priority;
            const s = STATUSES[t.status]?.label || t.status;
            const c = CATEGORIES[t.category]?.label || t.category;
            csv += `"${t.ticketCode}","${t.customerName}","${t.title.replace(/"/g, '""')}","${c}","${p}","${s}","${t.assigneeName || ''}","${t.createdAt}","${t.dueDate}","${t.slaBreached ? 'CÓ' : 'KHÔNG'}"\n`;
        });

        const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `Danh_sach_phieu_ho_tro_${new Date().toISOString().slice(0, 10)}.csv`;
        a.click();
        URL.revokeObjectURL(url);
        showToast("Đã xuất file báo cáo CSV thành công", "success");
    }

    /* =========================================================
       GLOBAL API EXPORT (window.SupportTicketsManager)
    ========================================================= */
    function exposeGlobalApi() {
        window.SupportTicketsManager = {
            getTickets: () => [...ticketsList],
            getTicketsByCustomer: (customerId) => {
                const cId = Number(customerId);
                return ticketsList.filter(t => Number(t.customerId) === cId);
            },
            getChurnRiskStatus: (customerId) => {
                return evaluateCustomerChurnRisk(customerId);
            },
            getAllChurnRiskCustomers: () => {
                return getAllChurnRiskCustomers();
            },
            createTicket: (data) => {
                const newId = ticketsList.length > 0 ? Math.max(...ticketsList.map(t => t.id)) + 1 : 1;
                const now = new Date();
                const ticketCode = `TCK-${now.getFullYear()}-${String(newId).padStart(3, "0")}`;

                const newTicket = {
                    id: newId,
                    ticketCode,
                    customerId: Number(data.customerId),
                    customerName: data.customerName || `Khách hàng #${data.customerId}`,
                    title: data.title || "Yêu cầu hỗ trợ",
                    category: data.category || "TECHNICAL",
                    priority: data.priority || "MEDIUM",
                    status: data.status || "NEW",
                    assigneeId: Number(data.assigneeId) || 1,
                    assigneeName: data.assigneeName || "Trần Minh Đức",
                    creatorName: data.creatorName || "Nông Quang Tiệp",
                    createdAt: now.toISOString(),
                    dueDate: data.dueDate || new Date(now.getTime() + 24 * 3600 * 1000).toISOString(),
                    description: data.description || "",
                    slaBreached: false,
                    timeline: [
                        { time: formatDateTime(now.toISOString()), author: "Hệ thống", text: "Tiếp nhận yêu cầu hỗ trợ sau bán" }
                    ]
                };
                ticketsList.unshift(newTicket);
                saveTickets(ticketsList);
                return newTicket;
            },
            toggleManualChurnRisk: (customerId, isFlag, reason = "") => {
                const cId = Number(customerId);
                if (isFlag) {
                    churnFlagsMap[cId] = {
                        customerId: cId,
                        isManualFlag: true,
                        manualReason: reason || "Đánh dấu thủ công rủi ro rời bỏ.",
                        flaggedAt: new Date().toISOString(),
                        riskLevel: "HIGH"
                    };
                } else {
                    delete churnFlagsMap[cId];
                }
                saveChurnFlags(churnFlagsMap);
                return evaluateCustomerChurnRisk(cId);
            }
        };
    }

    // Helper Functions
    function getVal(id) {
        return (document.getElementById(id)?.value || "").trim();
    }

    function setVal(id, val) {
        const el = document.getElementById(id);
        if (el) el.value = val !== undefined && val !== null ? String(val) : "";
    }

    function formatDateTime(isoString) {
        if (!isoString) return "—";
        try {
            const d = new Date(isoString);
            return `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}/${d.getFullYear()} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
        } catch (_) {
            return isoString;
        }
    }

    function escapeHtml(str) {
        return String(str || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function showToast(message, type = "success") {
        const toast = document.createElement("div");
        toast.className = `crm-toast crm-toast-${type}`;
        toast.style.cssText = "position:fixed; bottom:24px; right:24px; z-index:1150; min-width:300px; padding:14px 18px; border-radius:8px; box-shadow:var(--crm-shadow); animation:fadeInUp 0.3s ease; display:flex; align-items:center; gap:10px; background:var(--crm-surface); border:1px solid var(--crm-border);";

        const icon = type === "success" ? "✓" : "⚠️";
        toast.innerHTML = `<span style="font-weight:bold; color:var(--crm-${type}); font-size:16px;">${icon}</span> <span style="font-size:13.5px; color:var(--crm-text);">${escapeHtml(message)}</span>`;

        document.body.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = "0";
            toast.style.transition = "opacity 0.3s ease";
            setTimeout(() => toast.remove(), 300);
        }, 3200);
    }

    // Auto bootstrap on DOM load
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
