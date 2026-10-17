/**
 * ===================================================================
 * ENTERPRISE B2B CUSTOMER SUPPORT & CHURN RISK CONTROLLER (CRM-68)
 * Pure HTML5 / CSS3 / ES6+ JavaScript client-side module.
 * Manages post-sale support tickets, SLA monitoring, automatic & manual
 * churn risk detection, drawer forms, and authenticated Backend persistence.
 * ===================================================================
 */

"use strict";

(function () {
    const API_BASE = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`);

    let sessionUser;
    let customerOwners = [];

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
        TECHNICAL: { label: "Sự cố kỹ thuật / Lỗi", icon: '<i class="fa-solid fa-wrench"></i>' },
        COMPLAINT: { label: "Khiếu nại dịch vụ / SLA", icon: '<i class="fa-solid fa-triangle-exclamation"></i>' },
        BILLING: { label: "Hóa đơn / Hợp đồng", icon: '<i class="fa-solid fa-file-invoice-dollar"></i>' },
        FEATURE_REQUEST: { label: "Đề xuất tính năng / Quota", icon: '<i class="fa-solid fa-lightbulb"></i>' }
    };

    const SUPPORT_AGENTS=[];
    const riskByCustomer=new Map();
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
       API PERSISTENCE AND IN-MEMORY VIEW
    ========================================================= */
    async function loadRisks() {
        riskByCustomer.clear();
        await Promise.all([...new Set(ticketsList.map(t=>Number(t.customerId)))].map(async id=>riskByCustomer.set(id,await CrmApi.request(`/api/customers/${id}/risk`))));
    }
    async function loadTickets() {
        const items=[];
        for (let page=1;;page++) {
            const data=await CrmApi.request(`/api/support-requests?page=${page}&size=100`);
            items.push(...data.items);
            if (data.items.length<100) break;
        }
        return items.map(t=>({...t,ticketCode:`SR-${t.id}`,customerName:customersList.find(c=>Number(c.id)===Number(t.customerId))?.name || `#${t.customerId}`,assigneeName:SUPPORT_AGENTS.find(a=>Number(a.id)===Number(t.assigneeId))?.name || `#${t.assigneeId}`,timeline:[]}));
    }

    function saveTickets(list) {
        throw new Error("Thay đổi phiếu hỗ trợ phải được Backend lưu.");
    }

    function loadChurnFlags() {
        return {};
    }

    function saveChurnFlags(map) {
        throw new Error("Risk được Backend tính theo số phiếu đang mở.");
    }

    async function loadCustomers() {
        return CrmApi.customers();
    }

    /* =========================================================
       CHURN RISK LOGIC (CRM-68 CORE)
       - Auto Condition 1: Customer has >= 2 unresolved tickets
       - Auto Condition 2: Customer has unresolved High or Urgent tickets
       - Auto Condition 3: Customer has complaint category tickets
       - Manual Override: CSKH manual flag with custom reason
    ========================================================= */
    function evaluateCustomerChurnRisk(customerId) {
        return riskByCustomer.get(Number(customerId)) || {isRisk:false,reasons:[],openTicketCount:0,riskLevel:"NONE"};
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
    async function init() {
        exposeGlobalApi();
        if (!document.getElementById("ticketsTableBody")) return;
        try {
            const session=await CrmApi.session();
            sessionUser = session;
            const createButton=document.getElementById("btnOpenCreateTicket");
            if(createButton)createButton.hidden=!session.permissions.includes("support.create");
            if(!session.permissions.includes("support.read")) {
                document.getElementById("ticketsTableBody").innerHTML='<tr><td colspan="9">Bạn không có quyền xem yêu cầu hỗ trợ.</td></tr>';
                return;
            }
            customersList=await loadCustomers();
            const lookups=await CrmApi.request("/api/customer-lookups");
            customerOwners = lookups.owners;
            SUPPORT_AGENTS.splice(0,SUPPORT_AGENTS.length,...lookups.owners.map(u=>({id:u.id,name:u.fullName,role:""})));
            ticketsList=await loadTickets();
            await loadRisks();
            setupSupportPage();
            const manualFlag=document.getElementById("modalChurnFlagCheckbox");
            if(manualFlag)manualFlag.closest("label").parentElement.style.display="none";
            const saveFlag=document.getElementById("btnSaveChurnModal");if(saveFlag)saveFlag.style.display="none";
            document.querySelector(".churn-manual-box")?.setAttribute("hidden","");
            document.getElementById("ticketTimelineSection")?.setAttribute("hidden","");
            document.getElementById("filterCategory")?.setAttribute("hidden","");
            const create=document.getElementById("btnOpenCreateTicket");
            if (create) create.hidden=!session.permissions.includes("support.create");
            for (const id of ["ticketCategory","ticketDueDate","manualChurnCheckbox","manualChurnReason","ticketTimelineNote","btnAddTimelineNote"]) {
                const el=document.getElementById(id);
                if (el) { el.required=false; el.disabled=true; el.closest(".form-group,.field-group")?.setAttribute("hidden",""); }
            }
        } catch(error) {
            const table=document.getElementById("ticketsTableBody");
            if (table) table.innerHTML=`<tr><td colspan="9">${escapeHtml(error.message)}</td></tr>`;
            showToast(error.message,"error");
        }
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

        document.getElementById("btnRefreshTickets")?.addEventListener("click", async () => {
            ticketsList = await loadTickets();
            await loadRisks();
            churnFlagsMap = loadChurnFlags();
            customersList = await loadCustomers();
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

        const salesRepName = cust.ownerName || cust.owner || "Chưa phân công";
        const salesRepEmail = cust.ownerEmail || "Chưa có email";
        const salesRepPhone = cust.ownerPhone || "Chưa có số điện thoại";

        container.innerHTML = `
            <div class="churn-banner-container" style="animation:fadeIn 0.3s ease;">
                <div class="churn-alert-banner">
                    <div style="display:flex; gap:16px; align-items:flex-start; flex:1; min-width:300px;">
                        <div style="font-size:32px; line-height:1; flex-shrink:0; color:var(--crm-danger);"><i class="fa-solid fa-triangle-exclamation"></i></div>
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
                                <a href="customer-360.html?id=${cust.id}" class="crm-btn crm-btn-secondary" style="font-size:12px; padding:4px 12px; text-decoration:none; display:inline-flex; align-items:center; gap:4px;">
                                    <i class="fa-solid fa-eye"></i> Xem hồ sơ 360° khách hàng
                                </a>
                                <button type="button" class="crm-btn crm-btn-secondary btn-quick-churn-modal" data-id="${cust.id}" style="font-size:12px; padding:4px 12px; display:inline-flex; align-items:center; gap:4px;">
                                    <i class="fa-solid fa-gear"></i> Quản lý cờ rủi ro
                                </button>
                            </div>
                        </div>
                    </div>

                    <!-- Sales Alert Box -->
                    <div class="sales-alert-box" style="min-width:280px; flex-shrink:0;">
                        <div style="font-size:11px; text-transform:uppercase; letter-spacing:0.5px; font-weight:700; color:var(--crm-danger); margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                            <i class="fa-solid fa-user-tie" style="font-size:14px;"></i>
                            Sale phụ trách cần phối hợp
                        </div>
                        <div style="font-weight:700; font-size:14.5px; color:var(--crm-text); margin-bottom:4px;">
                            ${escapeHtml(salesRepName)}
                        </div>
                        <div style="font-size:12px; color:var(--crm-muted); display:flex; flex-direction:column; gap:4px;">
                            <span><i class="fa-solid fa-envelope"></i> ${escapeHtml(salesRepEmail)}</span>
                            <span><i class="fa-solid fa-phone"></i> ${escapeHtml(salesRepPhone)}</span>
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
                if (Number(t.assigneeId) !== Number(sessionUser.id)) return false;
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
                     <i class="fa-solid fa-triangle-exclamation"></i> Rủi ro rời bỏ
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
                        <span>${t.category ? categoryDef.label : ""}</span>
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
                            <i class="fa-solid fa-pen-to-square"></i>
                        </button>
                        <button type="button" class="crm-btn crm-btn-secondary btn-delete-ticket" data-id="${t.id}" title="Xóa phiếu"><i class="fa-solid fa-trash-can"></i></button>
                        <button hidden type="button" class="crm-btn crm-btn-secondary btn-manage-churn" data-customer-id="${t.customerId}" style="padding:4px 8px; font-size:12px;" title="Cờ rủi ro rời bỏ">
                            <i class="fa-solid fa-flag"></i>
                        </button>
                    </div>
                </td>
            `;

            tr.querySelector(".btn-delete-ticket")?.addEventListener("click",async()=>{
                if (!confirm(`Xóa phiếu ${t.ticketCode}?`)) return;
                try {
                    await CrmApi.request(`/api/support-requests/${t.id}`,{method:"DELETE"});
                    ticketsList=await loadTickets();
                    const data=await CrmApi.request(`/api/customers/${t.customerId}/risk`);
                    riskByCustomer.set(Number(t.customerId),data);
                    updateKpis();renderChurnBanner();renderTicketsTable();showToast("Đã xóa phiếu hỗ trợ.");
                } catch(error) { showToast(error.message,"error"); }
            });
            tbody.appendChild(tr);
        });

        // Bind table row buttons
        if (!CrmApi.canSync("support.update")) tbody.querySelectorAll("button.btn-edit-ticket").forEach(button => button.remove());
        if (!CrmApi.canSync("support.delete")) tbody.querySelectorAll(".btn-delete-ticket").forEach(button => button.remove());
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
            setVal("ticketAssignee", SUPPORT_AGENTS[0]?.id || "");
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
            ? `<span class="churn-risk-badge" style="background:#fee2e2; color:#b91c1c; font-size:11px; padding:2px 7px; border-radius:4px; font-weight:700; display:inline-flex; align-items:center; gap:4px;">
                 <i class="fa-solid fa-triangle-exclamation"></i> RỦI RO RỜI BỎ (${churnEval.riskLevel})
               </span>`
            : `<span style="color:var(--crm-success); font-weight:600; font-size:11.5px; display:inline-flex; align-items:center; gap:4px;"><i class="fa-solid fa-circle-check"></i> Khách hàng ổn định</span>`;

        preview.style.display = "block";
        preview.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
                <strong style="font-size:13.5px; color:var(--crm-text);">${escapeHtml(cust.companyName || cust.name)}</strong>
                ${churnBadge}
            </div>
            <div style="font-size:12px; color:var(--crm-muted); display:grid; grid-template-columns:1fr 1fr; gap:6px;">
                <div>MST: <strong style="color:var(--crm-text);">${escapeHtml(cust.taxCode || "N/A")}</strong></div>
                <div>Đại diện Sale: <strong style="color:var(--crm-primary);">${escapeHtml(cust.ownerName || cust.owner || "Chưa phân công")}</strong></div>
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
        showToast("Ghi chú tiến độ chưa được hỗ trợ bởi API.","error");
    }

    async function handleTicketFormSubmit(e) {
        e.preventDefault();
        const id=getVal("ticketId");
        const payload={customerId:Number(getVal("ticketCustomerSelect")),title:getVal("ticketTitle"),description:getVal("ticketDescription"),priority:getVal("ticketPriority"),status:getVal("ticketStatus"),assigneeId:Number(getVal("ticketAssignee"))};
        if (!payload.customerId || !payload.title || !payload.assigneeId) { showToast("Vui lòng chọn khách hàng, người xử lý và nhập tiêu đề.","error"); return; }
        const button=document.getElementById("btnSubmitTicket");
        if (button) button.disabled=true;
        try {
            await CrmApi.request(`/api/support-requests${id ? "/"+id : ""}`,{method:id ? "PUT":"POST",body:JSON.stringify(payload)});
            ticketsList=await loadTickets();
            const data=await CrmApi.request(`/api/customers/${payload.customerId}/risk`);
                    riskByCustomer.set(payload.customerId,data);
            closeTicketDrawer(); updateKpis(); renderChurnBanner(); renderTicketsTable();
            showToast("Đã lưu phiếu hỗ trợ.");
        } catch(error) { showToast(error.message,"error"); }
        finally { if (button) button.disabled=false; }
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
                    ${escapeHtml(cust.ownerName || cust.owner || "Chưa phân công")}
                </div>
                <div style="font-size:12px; color:var(--crm-muted); margin-top:2px;">
                    Email: ${escapeHtml(customerOwners.find(owner => Number(owner.id) === Number(cust.ownerUserId))?.email || "Chưa có email")}
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
        showToast("Risk được Backend tính tự động từ phiếu đang mở.","error");
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

        const columns = [
            { label: "Mã Phiếu", key: "ticketCode", width: 120, align: "center" },
            { label: "Khách hàng", key: "customerName", width: 220, align: "left" },
            { label: "Tiêu đề yêu cầu", key: "title", width: 250, align: "left" },
            { label: "Danh mục", key: "category", width: 150, align: "left" },
            { label: "Độ ưu tiên", key: "priority", width: 120, align: "center" },
            { label: "Trạng thái", key: "status", width: 130, align: "center" },
            { label: "Người xử lý", key: "assignee", width: 160, align: "left" },
            { label: "Ngày tạo", key: "createdAt", width: 140, align: "center" },
            { label: "Hạn cam kết SLA", key: "dueDate", width: 140, align: "center" },
            { label: "Vi phạm SLA", key: "slaBreached", width: 120, align: "center" }
        ];

        const formattedData = items.map(t => ({
            ticketCode: t.ticketCode || "",
            customerName: t.customerName || "",
            title: t.title || "",
            category: CATEGORIES[t.category]?.label || t.category || "",
            priority: PRIORITIES[t.priority]?.label || t.priority || "",
            status: STATUSES[t.status]?.label || t.status || "",
            assignee: t.assigneeName || "Chưa phân công",
            createdAt: t.createdAt || "",
            dueDate: t.dueDate || "",
            slaBreached: t.slaBreached ? "CÓ (Quá hạn)" : "Không"
        }));

        if (window.ExcelExportHelper) {
            window.ExcelExportHelper.exportStyledExcel({
                title: "BÁO CÁO PHIẾU HỖ TRỢ & CHĂM SÓC KHÁCH HÀNG (SUPPORT TICKETS)",
                filename: `Danh_Sach_Phieu_Ho_Tro_${new Date().toISOString().slice(0, 10)}.xls`,
                meta: `Xuất ngày: ${new Date().toLocaleDateString('vi-VN')} | Tổng: ${items.length} phiếu | NovaCRM`,
                columns: columns,
                data: formattedData
            });
            showToast("✓ Đã xuất báo cáo phiếu hỗ trợ ra file Excel thành công!", "success");
        } else {
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
    }


    /* =========================================================
       GLOBAL API EXPORT (window.SupportTicketsManager)
    ========================================================= */
    function exposeGlobalApi() {
        window.SupportTicketsManager={
            getTicketsByCustomer:id=>ticketsList.filter(t=>Number(t.customerId)===Number(id)),
            getChurnRiskStatus:evaluateCustomerChurnRisk,
            getAllChurnRiskCustomers,
            createTicket:payload=>CrmApi.request("/api/support-requests",{method:"POST",body:JSON.stringify(payload)})
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
