/**
 * ===================================================================
 * CRM-121 / S4-07: Nhận hoặc từ chối Lead được phân, có ràng buộc SLA
 * Author: Senior Frontend Developer (Nông Quang Tiệp)
 * Scope: HTML5, CSS3, Vanilla ES6+ - Zero JSP Policy - No Backend Modification
 * RESTful Endpoints Ready:
 *   - GET  /api/leads/my-assigned
 *   - POST /api/leads/{id}/accept
 *   - POST /api/leads/{id}/reject (reason, details)
 * ===================================================================
 */

(function () {
    "use strict";

    // 1. STORAGE KEYS & CONSTANTS
    const STORAGE_KEY_ASSIGNED_LEADS = "crm_my_assigned_leads";
    const STORAGE_KEY_QUEUE = "crm_lead_assignment_queue";

    // Standard SLA duration for B2B response (2 hours = 120 minutes)
    const SLA_STANDARD_MINUTES = 120;

    // 2. MOCK DATA INITIALIZATION
    const nowMs = Date.now();

    const DEFAULT_ASSIGNED_LEADS = [
        // Lead 1: 🟢 SAFE (> 50% SLA remaining)
        {
            id: "LD-SLA-01",
            fullName: "Trần Anh Quân",
            title: "Tổng Giám đốc (CEO)",
            phone: "0912888999",
            email: "quan.ta@deltavina.com",
            companyName: "Tập đoàn Xây dựng Delta Vina",
            industry: "Bất động sản & Xây dựng",
            region: "Miền Bắc (Hà Nội)",
            employeeCount: 220,
            score: 88,
            assignedAt: new Date(nowMs - 25 * 60 * 1000).toISOString(), // 25 min ago
            slaTotalMinutes: SLA_STANDARD_MINUTES,
            slaDeadline: nowMs + 95 * 60 * 1000, // 95 min left
            status: "PENDING_RESPONSE" // PENDING_RESPONSE | CONTACTING | REJECTED
        },
        // Lead 2: 🟡 URGENT (< 30 minutes remaining)
        {
            id: "LD-SLA-02",
            fullName: "Hoàng Mai Linh",
            title: "Trưởng phòng Thu mua & Cung ứng",
            phone: "0987333444",
            email: "linh.hm@tocotoco.vn",
            companyName: "Chuỗi Cửa hàng Trà Sữa TocoToco",
            industry: "Bán lẻ & Chuỗi F&B",
            region: "Miền Bắc (Hà Nội)",
            employeeCount: 95,
            score: 74,
            assignedAt: new Date(nowMs - 102 * 60 * 1000).toISOString(), // 102 min ago
            slaTotalMinutes: SLA_STANDARD_MINUTES,
            slaDeadline: nowMs + 18 * 60 * 1000, // 18 min left (URGENT!)
            status: "PENDING_RESPONSE"
        },
        // Lead 3: 🔴 OVERDUE (SLA expired)
        {
            id: "LD-SLA-03",
            fullName: "Đặng Quốc Việt",
            title: "Giám đốc Công nghệ (CTO)",
            phone: "0903555666",
            email: "viet.dq@vinasoftware.com",
            companyName: "Công ty Cổ phần VinaSoftware Global",
            industry: "CNTT & Viễn thông",
            region: "Miền Nam (TP.HCM)",
            employeeCount: 160,
            score: 92,
            assignedAt: new Date(nowMs - 145 * 60 * 1000).toISOString(), // 145 min ago
            slaTotalMinutes: SLA_STANDARD_MINUTES,
            slaDeadline: nowMs - 25 * 60 * 1000, // expired 25 min ago!
            status: "PENDING_RESPONSE"
        },
        // Lead 4: 📞 CONTACTING (Already accepted on time)
        {
            id: "LD-SLA-04",
            fullName: "Lê Bích Ngọc",
            title: "Phó Giám đốc Kỹ thuật",
            phone: "0915667788",
            email: "ngoc.lb@duochoalinh.vn",
            companyName: "Tập đoàn Dược phẩm Hoa Linh Pharma",
            industry: "Dược phẩm & Y tế",
            region: "Miền Bắc (Hải Phòng)",
            employeeCount: 130,
            score: 82,
            assignedAt: new Date(nowMs - 3 * 3600 * 1000).toISOString(),
            slaTotalMinutes: SLA_STANDARD_MINUTES,
            slaDeadline: nowMs - 1 * 3600 * 1000,
            status: "CONTACTING",
            acceptedAt: new Date(nowMs - 2.7 * 3600 * 1000).toISOString(),
            slaResult: "ON_TIME",
            responseTimeText: "Tiếp nhận đúng hạn (sau 18 phút)",
            contactingStage: "Đã gọi điện thoại lần 1 - Khách hẹn gửi hồ sơ giải pháp"
        },
        // Lead 5: 📞 CONTACTING (Accepted overdue)
        {
            id: "LD-SLA-05",
            fullName: "Vũ Đình Trọng",
            title: "Trưởng phòng Tiếp vận",
            phone: "0944112233",
            email: "trong.vd@achaucorp.com",
            companyName: "Logistics Á Châu Toàn Cầu",
            industry: "Sản xuất & Vận tải",
            region: "Miền Nam (Bình Dương)",
            employeeCount: 85,
            score: 68,
            assignedAt: new Date(nowMs - 5 * 3600 * 1000).toISOString(),
            slaTotalMinutes: SLA_STANDARD_MINUTES,
            slaDeadline: nowMs - 3 * 3600 * 1000,
            status: "CONTACTING",
            acceptedAt: new Date(nowMs - 2.8 * 3600 * 1000).toISOString(),
            slaResult: "OVERDUE",
            responseTimeText: "Tiếp nhận trễ hạn (sau 132 phút - trễ 12 phút)",
            contactingStage: "Đang xếp lịch họp Demo trực tuyến qua MS Teams"
        },
        // Lead 6: ❌ REJECTED
        {
            id: "LD-SLA-06",
            fullName: "Nguyễn Thanh Sơn",
            title: "Cửa hàng trưởng",
            phone: "0977001122",
            email: "son.nt@bachhoamientrung.vn",
            companyName: "Chuỗi Bách Hóa Miền Trung",
            industry: "Bán lẻ",
            region: "Miền Trung (Đà Nẵng)",
            employeeCount: 20,
            score: 45,
            assignedAt: new Date(nowMs - 6 * 3600 * 1000).toISOString(),
            slaTotalMinutes: SLA_STANDARD_MINUTES,
            slaDeadline: nowMs - 4 * 3600 * 1000,
            status: "REJECTED",
            rejectedAt: new Date(nowMs - 5.8 * 3600 * 1000).toISOString(),
            rejectReason: "INVALID_PHONE",
            rejectReasonLabel: "Sai số điện thoại / Không liên lạc được",
            rejectDetails: "Đã gọi 3 cuộc chuông đổ không người nhấc máy, kiểm tra lại số Zalo không tồn tại."
        }
    ];

    // 3. STATE
    let myAssignedLeads = [];
    let activeTab = "pending"; // 'pending' | 'contacting' | 'rejected'
    let currentRejectLead = null;
    let slaTimerInterval = null;

    let dom = {};

    function initDom() {
        dom = {
            // KPIs
            kpiPendingCount: document.getElementById("kpiPendingCount"),
            kpiPendingBadge: document.getElementById("kpiPendingBadge"),
            kpiUrgentOverdueCount: document.getElementById("kpiUrgentOverdueCount"),
            kpiContactingCount: document.getElementById("kpiContactingCount"),
            kpiSlaComplianceRate: document.getElementById("kpiSlaComplianceRate"),

            // Tabs
            tabPendingBtn: document.getElementById("tabPendingBtn"),
            tabContactingBtn: document.getElementById("tabContactingBtn"),
            tabRejectedBtn: document.getElementById("tabRejectedBtn"),
            tabPendingBadge: document.getElementById("tabPendingBadge"),
            tabContactingBadge: document.getElementById("tabContactingBadge"),
            tabRejectedBadge: document.getElementById("tabRejectedBadge"),
            panePending: document.getElementById("panePending"),
            paneContacting: document.getElementById("paneContacting"),
            paneRejected: document.getElementById("paneRejected"),

            // Tab 1: Pending Table & Filters
            pendingSearchInput: document.getElementById("pendingSearchInput"),
            filterPendingUrgency: document.getElementById("filterPendingUrgency"),
            filterPendingIndustry: document.getElementById("filterPendingIndustry"),
            sortPendingOrder: document.getElementById("sortPendingOrder"),
            btnResetPendingFilters: document.getElementById("btnResetPendingFilters"),
            pendingTableBody: document.getElementById("pendingTableBody"),
            pendingEmptyState: document.getElementById("pendingEmptyState"),

            // Tab 2: Contacting Table & Filters
            contactingSearchInput: document.getElementById("contactingSearchInput"),
            filterContactingSlaResult: document.getElementById("filterContactingSlaResult"),
            btnResetContactingFilters: document.getElementById("btnResetContactingFilters"),
            contactingTableBody: document.getElementById("contactingTableBody"),
            contactingEmptyState: document.getElementById("contactingEmptyState"),

            // Tab 3: Rejected Table & Filters
            rejectedSearchInput: document.getElementById("rejectedSearchInput"),
            filterRejectReason: document.getElementById("filterRejectReason"),
            btnResetRejectedFilters: document.getElementById("btnResetRejectedFilters"),
            rejectedTableBody: document.getElementById("rejectedTableBody"),
            rejectedEmptyState: document.getElementById("rejectedEmptyState"),

            // Header Actions
            btnRefreshLeads: document.getElementById("btnRefreshLeads"),
            btnAcceptAllValid: document.getElementById("btnAcceptAllValid"),

            // Modal: Reject
            rejectModal: document.getElementById("rejectModal"),
            rejectLeadBanner: document.getElementById("rejectLeadBanner"),
            rejectReasonSelect: document.getElementById("rejectReasonSelect"),
            rejectDetails: document.getElementById("rejectDetails"),
            btnCloseRejectModal: document.getElementById("btnCloseRejectModal"),
            btnCancelReject: document.getElementById("btnCancelReject"),
            btnConfirmReject: document.getElementById("btnConfirmReject"),

            toastContainer: document.getElementById("leadSlaToastContainer")
        };
    }

    // 4. DATA LOAD & PERSISTENCE
    function loadData() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_ASSIGNED_LEADS);
            if (raw) {
                myAssignedLeads = JSON.parse(raw);
            } else {
                myAssignedLeads = [...DEFAULT_ASSIGNED_LEADS];
                saveLeads();
            }
        } catch (_) {
            myAssignedLeads = [...DEFAULT_ASSIGNED_LEADS];
        }

        updateKpis();
    }

    function saveLeads() {
        try {
            localStorage.setItem(STORAGE_KEY_ASSIGNED_LEADS, JSON.stringify(myAssignedLeads));
        } catch (e) {
            console.error("Failed to save assigned leads to localStorage", e);
        }
    }

    // RESTful API Connector (Contract with Nguyễn Văn Thắng)
    window.LeadSlaService = {
        async getMyAssignedLeads() {
            try {
                if (window.CrmApi) {
                    const data = await window.CrmApi.request("/api/leads/my-assigned");
                    if (data && Array.isArray(data)) {
                        myAssignedLeads = data;
                        saveLeads();
                        return data;
                    }
                }
            } catch (err) {
                console.warn("[CRM-121 API] GET /api/leads/my-assigned unavailable, using local mock.", err);
            }
            return myAssignedLeads;
        },

        async acceptLead(leadId) {
            try {
                if (window.CrmApi) {
                    await window.CrmApi.request(`/api/leads/${leadId}/accept`, { method: "POST" });
                }
            } catch (err) {
                console.warn(`[CRM-121 API] POST /api/leads/${leadId}/accept unavailable, processing locally.`, err);
            }
        },

        async rejectLead(leadId, reason, details) {
            try {
                if (window.CrmApi) {
                    await window.CrmApi.request(`/api/leads/${leadId}/reject`, {
                        method: "POST",
                        body: JSON.stringify({ reason, details })
                    });
                }
            } catch (err) {
                console.warn(`[CRM-121 API] POST /api/leads/${leadId}/reject unavailable, processing locally.`, err);
            }
        }
    };

    // 5. REAL-TIME SLA TICKER & COMPUTATIONS
    function getSlaState(lead) {
        const now = Date.now();
        const diffMs = lead.slaDeadline - now;

        if (diffMs <= 0) {
            const overdueMs = Math.abs(diffMs);
            const overdueMin = Math.floor(overdueMs / 60000);
            const hours = Math.floor(overdueMin / 60);
            const mins = overdueMin % 60;
            const secs = Math.floor((overdueMs % 60000) / 1000);

            const timeStr = `${String(hours).padStart(2, "0")}:${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;

            return {
                state: "overdue",
                isOverdue: true,
                timeText: `Quá hạn: -${timeStr}`,
                pillText: "🔴 Quá hạn SLA",
                percent: 100,
                hours,
                mins,
                secs
            };
        }

        const totalMs = (lead.slaTotalMinutes || SLA_STANDARD_MINUTES) * 60000;
        const remainingMin = Math.floor(diffMs / 60000);
        const hours = Math.floor(remainingMin / 60);
        const mins = remainingMin % 60;
        const secs = Math.floor((diffMs % 60000) / 1000);

        const timeStr = `${String(hours).padStart(2, "0")}:${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
        const percent = Math.max(0, Math.min(100, Math.round((diffMs / totalMs) * 100)));

        const isUrgent = diffMs <= 30 * 60000 || percent <= 25;

        return {
            state: isUrgent ? "urgent" : "safe",
            isOverdue: false,
            timeText: `${timeStr} còn lại`,
            pillText: isUrgent ? "🟡 Sắp hết hạn" : "🟢 Trong hạn SLA",
            percent: percent,
            hours,
            mins,
            secs
        };
    }

    function startSlaTimer() {
        if (slaTimerInterval) clearInterval(slaTimerInterval);

        slaTimerInterval = setInterval(() => {
            const pendingLeads = myAssignedLeads.filter(l => l.status === "PENDING_RESPONSE");
            if (pendingLeads.length === 0) return;

            pendingLeads.forEach(lead => {
                const sla = getSlaState(lead);
                const row = document.querySelector(`tr[data-lead-id="${lead.id}"]`);
                if (!row) return;

                const timerBox = row.querySelector(".sla-countdown-box");
                const clockEl = row.querySelector(".sla-timer-clock");
                const pillEl = row.querySelector(".sla-status-pill");
                const fillEl = row.querySelector(".sla-progress-fill");
                const acceptBtn = row.querySelector(".btn-accept-lead");

                if (timerBox) {
                    timerBox.className = `sla-countdown-box ${sla.state}`;
                }
                if (clockEl) {
                    clockEl.textContent = sla.timeText;
                }
                if (pillEl) {
                    pillEl.textContent = sla.pillText;
                }
                if (fillEl) {
                    fillEl.style.width = `${sla.percent}%`;
                }
                if (acceptBtn) {
                    if (sla.isOverdue) {
                        acceptBtn.classList.add("overdue-accept");
                        acceptBtn.innerHTML = `<i class="fa-solid fa-clock-rotate-left"></i> Nhận trễ hạn`;
                    } else {
                        acceptBtn.classList.remove("overdue-accept");
                        acceptBtn.innerHTML = `<i class="fa-solid fa-circle-check"></i> Tiếp nhận`;
                    }
                }
            });

            updateKpis();
        }, 1000);
    }

    // 6. UI RENDERING: KPIS
    function updateKpis() {
        const pending = myAssignedLeads.filter(l => l.status === "PENDING_RESPONSE");
        const contacting = myAssignedLeads.filter(l => l.status === "CONTACTING");
        const rejected = myAssignedLeads.filter(l => l.status === "REJECTED");

        // Count urgent / overdue in pending
        let urgentOverdueCount = 0;
        pending.forEach(l => {
            const sla = getSlaState(l);
            if (sla.state === "urgent" || sla.state === "overdue") {
                urgentOverdueCount++;
            }
        });

        // SLA Compliance: On-time accepted vs total accepted
        const totalHandled = contacting.length;
        const onTimeCount = contacting.filter(l => l.slaResult === "ON_TIME").length;
        const complianceRate = totalHandled > 0 ? ((onTimeCount / totalHandled) * 100).toFixed(1) : 100;

        if (dom.kpiPendingCount) dom.kpiPendingCount.textContent = pending.length;
        if (dom.kpiUrgentOverdueCount) dom.kpiUrgentOverdueCount.textContent = urgentOverdueCount;
        if (dom.kpiContactingCount) dom.kpiContactingCount.textContent = contacting.length;
        if (dom.kpiSlaComplianceRate) dom.kpiSlaComplianceRate.textContent = `${complianceRate}%`;

        if (dom.tabPendingBadge) dom.tabPendingBadge.textContent = pending.length;
        if (dom.tabContactingBadge) dom.tabContactingBadge.textContent = contacting.length;
        if (dom.tabRejectedBadge) dom.tabRejectedBadge.textContent = rejected.length;

        if (dom.kpiPendingBadge) {
            dom.kpiPendingBadge.style.display = pending.length > 0 ? "inline-block" : "none";
        }
    }

    // 7. UI RENDERING: TAB 1 (PENDING RESPONSE)
    function renderPendingTable() {
        if (!dom.pendingTableBody) return;

        const query = dom.pendingSearchInput ? dom.pendingSearchInput.value.trim().toLowerCase() : "";
        const urgencyFilter = dom.filterPendingUrgency ? dom.filterPendingUrgency.value : "ALL";
        const industryFilter = dom.filterPendingIndustry ? dom.filterPendingIndustry.value : "ALL";
        const sort = dom.sortPendingOrder ? dom.sortPendingOrder.value : "sla_asc";

        const filtered = myAssignedLeads.filter(l => {
            if (l.status !== "PENDING_RESPONSE") return false;

            const sla = getSlaState(l);
            if (urgencyFilter === "SAFE" && sla.state !== "safe") return false;
            if (urgencyFilter === "URGENT" && sla.state !== "urgent") return false;
            if (urgencyFilter === "OVERDUE" && sla.state !== "overdue") return false;

            if (industryFilter !== "ALL" && !l.industry.toLowerCase().includes(industryFilter.toLowerCase())) return false;

            if (query) {
                const searchStr = `${l.fullName} ${l.phone} ${l.email} ${l.companyName} ${l.title}`.toLowerCase();
                if (!searchStr.includes(query)) return false;
            }
            return true;
        }).sort((a, b) => {
            if (sort === "sla_asc") return a.slaDeadline - b.slaDeadline;
            if (sort === "assigned_desc") return new Date(b.assignedAt) - new Date(a.assignedAt);
            if (sort === "score_desc") return (b.score || 0) - (a.score || 0);
            return 0;
        });

        if (filtered.length === 0) {
            dom.pendingTableBody.innerHTML = "";
            if (dom.pendingEmptyState) dom.pendingEmptyState.style.display = "block";
            return;
        }

        if (dom.pendingEmptyState) dom.pendingEmptyState.style.display = "none";

        let html = "";
        filtered.forEach((lead, idx) => {
            const sla = getSlaState(lead);
            const assignedTimeStr = formatDateTime(lead.assignedAt);
            const scoreClass = (lead.score || 0) >= 70 ? "text-danger" : ((lead.score || 0) >= 40 ? "text-warning" : "text-primary");

            html += `
                <tr data-lead-id="${escapeHtml(lead.id)}">
                    <td class="col-seq">${idx + 1}</td>

                    <!-- Lead Identity -->
                    <td class="col-lead">
                        <div style="font-weight: 700; font-size: 13.5px; color: var(--crm-text);">${escapeHtml(lead.fullName)}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted);">${escapeHtml(lead.title || "Khách hàng")}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted); margin-top: 2px;">
                            <span><i class="fa-solid fa-phone text-primary"></i> ${escapeHtml(lead.phone)}</span> |
                            <span><i class="fa-solid fa-envelope text-primary"></i> ${escapeHtml(lead.email)}</span>
                        </div>
                    </td>

                    <!-- Company & Region -->
                    <td class="col-company">
                        <div style="font-weight: 600; color: var(--crm-text);">${escapeHtml(lead.companyName)}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted); margin-top: 2px;">
                            <i class="fa-solid fa-building text-purple"></i> ${escapeHtml(lead.industry)} &bull; <i class="fa-solid fa-location-dot text-danger"></i> ${escapeHtml(lead.region)}
                        </div>
                    </td>

                    <!-- Assigned Time -->
                    <td class="col-assigned-time">
                        <span style="font-size: 12px; color: var(--crm-muted);"><i class="fa-regular fa-clock"></i> ${assignedTimeStr}</span>
                    </td>

                    <!-- SLA Real-time Countdown Timer -->
                    <td class="col-sla-timer">
                        <div class="sla-countdown-box ${sla.state}">
                            <div class="sla-timer-display">
                                <span class="sla-timer-clock">${sla.timeText}</span>
                                <span class="sla-status-pill">${sla.pillText}</span>
                            </div>
                            <div class="sla-progress-track">
                                <div class="sla-progress-fill" style="width: ${sla.percent}%;"></div>
                            </div>
                        </div>
                    </td>

                    <!-- Quality Score -->
                    <td class="col-score">
                        <span style="font-size: 14px; font-weight: 800;" class="${scoreClass}">${lead.score || 0}đ</span>
                    </td>

                    <!-- Action Buttons -->
                    <td class="col-actions">
                        <div class="sla-action-buttons">
                            <button type="button" class="btn-accept-lead ${sla.isOverdue ? "overdue-accept" : ""}" data-lead-id="${escapeHtml(lead.id)}" title="Tiếp nhận và chuyển sang trạng thái đang chăm sóc">
                                <i class="fa-solid ${sla.isOverdue ? "fa-clock-rotate-left" : "fa-circle-check"}"></i>
                                ${sla.isOverdue ? "Nhận trễ hạn" : "Tiếp nhận"}
                            </button>
                            <button type="button" class="btn-reject-lead" data-lead-id="${escapeHtml(lead.id)}" title="Từ chối nhận Lead này và hoàn trả về hàng chờ">
                                <i class="fa-solid fa-ban"></i> Từ chối
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        });

        dom.pendingTableBody.innerHTML = html;
        attachPendingEvents();
    }

    function attachPendingEvents() {
        // Accept button
        dom.pendingTableBody.querySelectorAll(".btn-accept-lead").forEach(btn => {
            btn.addEventListener("click", () => {
                const leadId = btn.getAttribute("data-lead-id");
                handleAcceptLead(leadId);
            });
        });

        // Reject button
        dom.pendingTableBody.querySelectorAll(".btn-reject-lead").forEach(btn => {
            btn.addEventListener("click", () => {
                const leadId = btn.getAttribute("data-lead-id");
                openRejectModal(leadId);
            });
        });
    }

    // 8. UI RENDERING: TAB 2 (CONTACTING)
    function renderContactingTable() {
        if (!dom.contactingTableBody) return;

        const query = dom.contactingSearchInput ? dom.contactingSearchInput.value.trim().toLowerCase() : "";
        const resultFilter = dom.filterContactingSlaResult ? dom.filterContactingSlaResult.value : "ALL";

        const filtered = myAssignedLeads.filter(l => {
            if (l.status !== "CONTACTING") return false;
            if (resultFilter === "ON_TIME" && l.slaResult !== "ON_TIME") return false;
            if (resultFilter === "OVERDUE" && l.slaResult !== "OVERDUE") return false;

            if (query) {
                const searchStr = `${l.fullName} ${l.phone} ${l.email} ${l.companyName} ${l.contactingStage}`.toLowerCase();
                if (!searchStr.includes(query)) return false;
            }
            return true;
        });

        if (filtered.length === 0) {
            dom.contactingTableBody.innerHTML = "";
            if (dom.contactingEmptyState) dom.contactingEmptyState.style.display = "block";
            return;
        }

        if (dom.contactingEmptyState) dom.contactingEmptyState.style.display = "none";

        let html = "";
        filtered.forEach((lead, idx) => {
            const acceptedTimeStr = formatDateTime(lead.acceptedAt);
            const isOnTime = lead.slaResult === "ON_TIME";
            const resultBadge = isOnTime
                ? `<span class="sla-result-badge on-time"><i class="fa-solid fa-circle-check"></i> ${escapeHtml(lead.responseTimeText || "Đúng hạn SLA")}</span>`
                : `<span class="sla-result-badge overdue"><i class="fa-solid fa-clock-rotate-left"></i> ${escapeHtml(lead.responseTimeText || "Quá hạn SLA")}</span>`;

            html += `
                <tr>
                    <td class="col-seq">${idx + 1}</td>

                    <!-- Lead Identity -->
                    <td class="col-lead">
                        <div style="font-weight: 700; font-size: 13.5px; color: var(--crm-text);">${escapeHtml(lead.fullName)}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted);">${escapeHtml(lead.phone)} &bull; ${escapeHtml(lead.email)}</div>
                    </td>

                    <!-- Company -->
                    <td class="col-company">
                        <div style="font-weight: 600; color: var(--crm-text);">${escapeHtml(lead.companyName)}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted);">${escapeHtml(lead.industry)}</div>
                    </td>

                    <!-- Accepted Time -->
                    <td class="col-assigned-time">
                        <span style="font-size: 12px; color: var(--crm-muted);"><i class="fa-regular fa-calendar-check"></i> ${acceptedTimeStr}</span>
                    </td>

                    <!-- SLA Result -->
                    <td class="col-sla-result">
                        ${resultBadge}
                    </td>

                    <!-- Stage -->
                    <td class="col-status">
                        <span style="font-size: 12px; color: #0284c7; background: rgba(14, 165, 233, 0.12); padding: 4px 8px; border-radius: 4px; display: inline-block;">
                            <i class="fa-solid fa-comments"></i> ${escapeHtml(lead.contactingStage || "Đang tư vấn khách hàng")}
                        </span>
                    </td>

                    <!-- Quick Actions -->
                    <td class="col-actions">
                        <div class="sla-action-buttons">
                            <button type="button" class="crm-btn crm-btn-secondary crm-btn-sm" onclick="alert('Đang kết nối cuộc gọi thoại tới khách hàng ${escapeHtml(lead.phone)}...')">
                                <i class="fa-solid fa-phone text-success"></i> Gọi ngay
                            </button>
                            <button type="button" class="crm-btn crm-btn-primary crm-btn-sm" onclick="alert('Đã chuyển Lead ${escapeHtml(lead.fullName)} sang màn hình Cơ hội bán hàng!')">
                                <i class="fa-solid fa-bullseye"></i> Tạo cơ hội
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        });

        dom.contactingTableBody.innerHTML = html;
    }

    // 9. UI RENDERING: TAB 3 (REJECTED)
    function renderRejectedTable() {
        if (!dom.rejectedTableBody) return;

        const query = dom.rejectedSearchInput ? dom.rejectedSearchInput.value.trim().toLowerCase() : "";
        const reasonFilter = dom.filterRejectReason ? dom.filterRejectReason.value : "ALL";

        const filtered = myAssignedLeads.filter(l => {
            if (l.status !== "REJECTED") return false;
            if (reasonFilter !== "ALL" && l.rejectReason !== reasonFilter) return false;

            if (query) {
                const searchStr = `${l.fullName} ${l.phone} ${l.companyName} ${l.rejectDetails}`.toLowerCase();
                if (!searchStr.includes(query)) return false;
            }
            return true;
        });

        if (filtered.length === 0) {
            dom.rejectedTableBody.innerHTML = "";
            if (dom.rejectedEmptyState) dom.rejectedEmptyState.style.display = "block";
            return;
        }

        if (dom.rejectedEmptyState) dom.rejectedEmptyState.style.display = "none";

        let html = "";
        filtered.forEach((lead, idx) => {
            const rejectedTimeStr = formatDateTime(lead.rejectedAt);

            html += `
                <tr>
                    <td class="col-seq">${idx + 1}</td>

                    <!-- Lead Identity -->
                    <td class="col-lead">
                        <div style="font-weight: 700; font-size: 13.5px; color: var(--crm-text);">${escapeHtml(lead.fullName)}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted);">${escapeHtml(lead.phone)} &bull; ${escapeHtml(lead.email)}</div>
                    </td>

                    <!-- Company -->
                    <td class="col-company">
                        <div style="font-weight: 600; color: var(--crm-text);">${escapeHtml(lead.companyName)}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted);">${escapeHtml(lead.industry)}</div>
                    </td>

                    <!-- Reason Badge -->
                    <td class="col-reason-badge">
                        <span style="font-size: 11.5px; font-weight: 700; color: #b91c1c; background: #fee2e2; padding: 3px 8px; border-radius: 4px; display: inline-block;">
                            <i class="fa-solid fa-ban"></i> ${escapeHtml(lead.rejectReasonLabel || lead.rejectReason)}
                        </span>
                    </td>

                    <!-- Detail Explanation -->
                    <td class="col-reason-detail">
                        <span style="font-size: 12px; color: var(--crm-text-2);">${escapeHtml(lead.rejectDetails || "Không có ghi chú thêm.")}</span>
                    </td>

                    <!-- Time -->
                    <td class="col-reject-time">
                        <span style="font-size: 12px; color: var(--crm-muted);"><i class="fa-regular fa-clock"></i> ${rejectedTimeStr}</span>
                    </td>

                    <!-- Status -->
                    <td class="col-status">
                        <span style="font-size: 11.5px; font-weight: 600; color: #d97706; background: rgba(245, 158, 11, 0.12); padding: 3px 8px; border-radius: 4px; display: inline-flex; align-items: center; gap: 4px;">
                            <i class="fa-solid fa-arrows-spin"></i> Hàng chờ phân bổ
                        </span>
                    </td>
                </tr>
            `;
        });

        dom.rejectedTableBody.innerHTML = html;
    }

    // 10. ACCEPT LEAD LOGIC
    async function handleAcceptLead(leadId) {
        const lead = myAssignedLeads.find(l => l.id === leadId);
        if (!lead) return;

        const sla = getSlaState(lead);
        const wasOnTime = !sla.isOverdue;
        const now = new Date();

        const responseMinutes = Math.max(1, Math.round((now.getTime() - new Date(lead.assignedAt).getTime()) / 60000));

        // Call Service
        await window.LeadSlaService.acceptLead(leadId);

        // Update local object
        lead.status = "CONTACTING";
        lead.acceptedAt = now.toISOString();
        lead.slaResult = wasOnTime ? "ON_TIME" : "OVERDUE";
        lead.responseTimeText = wasOnTime
            ? `Tiếp nhận đúng hạn (sau ${responseMinutes} phút)`
            : `Tiếp nhận trễ hạn (sau ${responseMinutes} phút - trễ ${sla.hours * 60 + sla.mins} phút)`;
        lead.contactingStage = "Mới tiếp nhận - Sẵn sàng gọi điện tư vấn";

        saveLeads();
        updateKpis();
        renderPendingTable();
        renderContactingTable();

        if (wasOnTime) {
            showToast(`✨ Đã tiếp nhận Lead "${lead.fullName}" thành công! Trạng thái chuyển sang "Đang chăm sóc".`, "success");
        } else {
            showToast(`⚠️ Đã tiếp nhận Lead "${lead.fullName}" (Quá hạn cam kết SLA). Hãy liên hệ khách hàng ngay!`, "warning");
        }
    }

    function acceptAllValid() {
        const pendingSafeLeads = myAssignedLeads.filter(l => l.status === "PENDING_RESPONSE" && !getSlaState(l).isOverdue);
        if (pendingSafeLeads.length === 0) {
            showToast("Không có Lead nào trong hạn cam kết để nhận nhanh!", "info");
            return;
        }

        pendingSafeLeads.forEach(lead => {
            const now = new Date();
            const responseMinutes = Math.max(1, Math.round((now.getTime() - new Date(lead.assignedAt).getTime()) / 60000));
            lead.status = "CONTACTING";
            lead.acceptedAt = now.toISOString();
            lead.slaResult = "ON_TIME";
            lead.responseTimeText = `Tiếp nhận đúng hạn (sau ${responseMinutes} phút)`;
            lead.contactingStage = "Mới tiếp nhận hàng loạt - Đang chuẩn bị tư vấn";
        });

        saveLeads();
        updateKpis();
        renderPendingTable();
        renderContactingTable();

        showToast(`✨ Đã tiếp nhận nhanh thành công ${pendingSafeLeads.length} Lead trong hạn SLA!`, "success");
    }

    // 11. REJECT LEAD MODAL & RETURN TO QUEUE
    function openRejectModal(leadId) {
        const lead = myAssignedLeads.find(l => l.id === leadId);
        if (!lead || !dom.rejectModal) return;

        currentRejectLead = lead;

        if (dom.rejectLeadBanner) {
            dom.rejectLeadBanner.innerHTML = `
                <div>
                    <strong>${escapeHtml(lead.fullName)} (${escapeHtml(lead.title || "Khách hàng")})</strong>
                    <span>${escapeHtml(lead.companyName)} &bull; SĐT: ${escapeHtml(lead.phone)}</span>
                </div>
                <div style="text-align: right;">
                    <span style="font-size: 11px; color: var(--crm-muted);">Thời điểm giao:</span>
                    <div style="font-size: 12px; font-weight: 600; color: var(--crm-text);">${formatDateTime(lead.assignedAt)}</div>
                </div>
            `;
        }

        if (dom.rejectReasonSelect) dom.rejectReasonSelect.value = "";
        if (dom.rejectDetails) dom.rejectDetails.value = "";

        dom.rejectModal.classList.add("active");
    }

    function closeRejectModal() {
        if (dom.rejectModal) dom.rejectModal.classList.remove("active");
        currentRejectLead = null;
    }

    async function confirmReject() {
        if (!currentRejectLead) return;

        const reasonCode = dom.rejectReasonSelect ? dom.rejectReasonSelect.value : "";
        const details = dom.rejectDetails ? dom.rejectDetails.value.trim() : "";

        if (!reasonCode) {
            alert("Vui lòng chọn lý do từ chối chính đáng!");
            return;
        }

        if (!details) {
            alert("Vui lòng nhập chi tiết giải trình lý do từ chối để chuyển Trưởng nhóm!");
            return;
        }

        const reasonSelect = dom.rejectReasonSelect;
        const reasonLabel = reasonSelect.options[reasonSelect.selectedIndex]?.text || reasonCode;

        // Call API
        await window.LeadSlaService.rejectLead(currentRejectLead.id, reasonCode, details);

        // Update local Lead object
        currentRejectLead.status = "REJECTED";
        currentRejectLead.rejectedAt = new Date().toISOString();
        currentRejectLead.rejectReason = reasonCode;
        currentRejectLead.rejectReasonLabel = reasonLabel;
        currentRejectLead.rejectDetails = details;

        // Push back to Unassigned Queue in localStorage
        try {
            const rawQueue = localStorage.getItem(STORAGE_KEY_QUEUE);
            const queue = rawQueue ? JSON.parse(rawQueue) : [];
            const queueItem = {
                id: `LD-REJ-${Date.now().toString().slice(-4)}`,
                fullName: currentRejectLead.fullName,
                phone: currentRejectLead.phone,
                email: currentRejectLead.email,
                title: currentRejectLead.title,
                companyName: currentRejectLead.companyName,
                region: currentRejectLead.region,
                regionCode: "NORTH",
                industry: currentRejectLead.industry,
                employeeCount: currentRejectLead.employeeCount,
                reason: `Bị từ chối bởi Nông Quang Tiệp (${reasonLabel})`,
                createdAt: new Date().toISOString(),
                timeAgo: "Vừa từ chối"
            };
            queue.unshift(queueItem);
            localStorage.setItem(STORAGE_KEY_QUEUE, JSON.stringify(queue));
        } catch (e) {
            console.error("Failed to return rejected lead to queue", e);
        }

        saveLeads();
        closeRejectModal();
        updateKpis();
        renderPendingTable();
        renderRejectedTable();

        showToast(`⚠️ Đã từ chối Lead "${currentRejectLead.fullName}". Khách hàng đã được hoàn trả về Hàng chờ phân bổ.`, "warning");
    }

    function formatDateTime(isoString) {
        if (!isoString) return "--";
        const d = new Date(isoString);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, "0");
        const date = String(d.getDate()).padStart(2, "0");
        const h = String(d.getHours()).padStart(2, "0");
        const min = String(d.getMinutes()).padStart(2, "0");
        return `${y}-${m}-${date} ${h}:${min}`;
    }

    // 12. EVENT LISTENERS
    function setupEventListeners() {
        // Tab switching
        if (dom.tabPendingBtn) dom.tabPendingBtn.addEventListener("click", () => switchTab("pending"));
        if (dom.tabContactingBtn) dom.tabContactingBtn.addEventListener("click", () => switchTab("contacting"));
        if (dom.tabRejectedBtn) dom.tabRejectedBtn.addEventListener("click", () => switchTab("rejected"));

        // Tab 1 Filters
        if (dom.pendingSearchInput) dom.pendingSearchInput.addEventListener("input", renderPendingTable);
        if (dom.filterPendingUrgency) dom.filterPendingUrgency.addEventListener("change", renderPendingTable);
        if (dom.filterPendingIndustry) dom.filterPendingIndustry.addEventListener("change", renderPendingTable);
        if (dom.sortPendingOrder) dom.sortPendingOrder.addEventListener("change", renderPendingTable);
        if (dom.btnResetPendingFilters) {
            dom.btnResetPendingFilters.addEventListener("click", () => {
                if (dom.pendingSearchInput) dom.pendingSearchInput.value = "";
                if (dom.filterPendingUrgency) dom.filterPendingUrgency.value = "ALL";
                if (dom.filterPendingIndustry) dom.filterPendingIndustry.value = "ALL";
                if (dom.sortPendingOrder) dom.sortPendingOrder.value = "sla_asc";
                renderPendingTable();
                showToast("Đã đặt lại bộ lọc chờ phản hồi!", "info");
            });
        }

        // Tab 2 Filters
        if (dom.contactingSearchInput) dom.contactingSearchInput.addEventListener("input", renderContactingTable);
        if (dom.filterContactingSlaResult) dom.filterContactingSlaResult.addEventListener("change", renderContactingTable);
        if (dom.btnResetContactingFilters) {
            dom.btnResetContactingFilters.addEventListener("click", () => {
                if (dom.contactingSearchInput) dom.contactingSearchInput.value = "";
                if (dom.filterContactingSlaResult) dom.filterContactingSlaResult.value = "ALL";
                renderContactingTable();
            });
        }

        // Tab 3 Filters
        if (dom.rejectedSearchInput) dom.rejectedSearchInput.addEventListener("input", renderRejectedTable);
        if (dom.filterRejectReason) dom.filterRejectReason.addEventListener("change", renderRejectedTable);
        if (dom.btnResetRejectedFilters) {
            dom.btnResetRejectedFilters.addEventListener("click", () => {
                if (dom.rejectedSearchInput) dom.rejectedSearchInput.value = "";
                if (dom.filterRejectReason) dom.filterRejectReason.value = "ALL";
                renderRejectedTable();
            });
        }

        // Header actions
        if (dom.btnRefreshLeads) {
            dom.btnRefreshLeads.addEventListener("click", () => {
                loadData();
                renderPendingTable();
                renderContactingTable();
                renderRejectedTable();
                showToast("🔄 Đã làm mới danh sách Lead phân bổ!", "info");
            });
        }
        if (dom.btnAcceptAllValid) {
            dom.btnAcceptAllValid.addEventListener("click", acceptAllValid);
        }

        // Reject Modal
        if (dom.btnCloseRejectModal) dom.btnCloseRejectModal.addEventListener("click", closeRejectModal);
        if (dom.btnCancelReject) dom.btnCancelReject.addEventListener("click", closeRejectModal);
        if (dom.btnConfirmReject) dom.btnConfirmReject.addEventListener("click", confirmReject);

        // Backdrop click to close
        if (dom.rejectModal) {
            dom.rejectModal.addEventListener("click", e => {
                if (e.target === dom.rejectModal) closeRejectModal();
            });
        }

        // Escape key to close
        document.addEventListener("keydown", e => {
            if (e.key === "Escape") closeRejectModal();
        });
    }

    function switchTab(tab) {
        activeTab = tab;

        if (dom.tabPendingBtn) dom.tabPendingBtn.classList.toggle("active", tab === "pending");
        if (dom.tabContactingBtn) dom.tabContactingBtn.classList.toggle("active", tab === "contacting");
        if (dom.tabRejectedBtn) dom.tabRejectedBtn.classList.toggle("active", tab === "rejected");

        if (dom.panePending) dom.panePending.classList.toggle("active", tab === "pending");
        if (dom.paneContacting) dom.paneContacting.classList.toggle("active", tab === "contacting");
        if (dom.paneRejected) dom.paneRejected.classList.toggle("active", tab === "rejected");

        if (tab === "pending") renderPendingTable();
        if (tab === "contacting") renderContactingTable();
        if (tab === "rejected") renderRejectedTable();
    }

    // 13. TOAST NOTIFICATION UTILITY
    function showToast(message, type = "info") {
        if (!dom.toastContainer) return;

        const toast = document.createElement("div");
        toast.className = `sla-toast ${type}`;

        const iconClass = type === "success"
            ? "fa-circle-check"
            : (type === "warning" ? "fa-triangle-exclamation" : (type === "error" ? "fa-circle-xmark" : "fa-circle-info"));

        toast.innerHTML = `
            <i class="fa-solid ${iconClass}"></i>
            <span>${escapeHtml(message)}</span>
        `;

        dom.toastContainer.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = "0";
            toast.style.transform = "translateY(10px)";
            toast.style.transition = "all 0.25s ease";
            setTimeout(() => toast.remove(), 250);
        }, 3500);
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

    // 14. BOOTSTRAP INITIALIZATION
    document.addEventListener("DOMContentLoaded", () => {
        initDom();
        loadData();
        renderPendingTable();
        renderContactingTable();
        renderRejectedTable();
        setupEventListeners();
        startSlaTimer();
    });

})();
