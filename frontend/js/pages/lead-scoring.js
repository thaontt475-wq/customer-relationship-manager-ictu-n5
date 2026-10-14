/**
 * ===================================================================
 * CRM-119 / S4-05: Cấu hình Chấm điểm Lead theo Tiêu chí khai báo
 * Author: Senior Frontend Developer (Nông Quang Tiệp)
 * Scope: HTML5, CSS3, Vanilla ES6+ - Zero JSP Policy - No Backend Modification
 * API: /api/lead-scoring/*; scores and mutations are persisted by the backend.
 * ===================================================================
 */

(function () {
    "use strict";

    // 3. STATE MANAGEMENT
    let scoringRules = [];
    let scoringThresholds = { hot: null, warm: null };
    let serverConfig = null;
    let ready = false, busy = false, canManage = false, canScore = false;
    let scoredLeads = [];
    let currentTab = "ranking"; // 'ranking' | 'rules'
    let editingCriteriaId = null;

    // DOM Elements Cache
    let dom = {};

    function initDom() {
        dom = {
            // KPIs
            kpiTotalLeads: document.getElementById("kpiTotalLeads"),
            kpiAvgScore: document.getElementById("kpiAvgScore"),
            kpiHotLeads: document.getElementById("kpiHotLeads"),
            kpiWarmLeads: document.getElementById("kpiWarmLeads"),
            kpiColdLeads: document.getElementById("kpiColdLeads"),
            kpiHotThresholdBadge: document.getElementById("kpiHotThresholdBadge"),
            kpiWarmThresholdBadge: document.getElementById("kpiWarmThresholdBadge"),
            kpiColdThresholdBadge: document.getElementById("kpiColdThresholdBadge"),

            // Tabs
            tabRankingBtn: document.getElementById("tabRankingBtn"),
            tabRulesBtn: document.getElementById("tabRulesBtn"),
            paneRanking: document.getElementById("paneRanking"),
            paneRules: document.getElementById("paneRules"),
            tabLeadsCount: document.getElementById("tabLeadsCount"),
            tabRulesCount: document.getElementById("tabRulesCount"),

            // Tab 1: Filters & Table
            scoringSearchInput: document.getElementById("scoringSearchInput"),
            filterClassification: document.getElementById("filterClassification"),
            filterIndustry: document.getElementById("filterIndustry"),
            sortOrder: document.getElementById("sortOrder"),
            btnResetFilters: document.getElementById("btnResetFilters"),
            leadScoringTableBody: document.getElementById("leadScoringTableBody"),
            scoringEmptyState: document.getElementById("scoringEmptyState"),

            // Tab 2: Thresholds & Rules
            inputHotThreshold: document.getElementById("inputHotThreshold"),
            inputWarmThreshold: document.getElementById("inputWarmThreshold"),
            displayColdThreshold: document.getElementById("displayColdThreshold"),
            btnSaveThresholds: document.getElementById("btnSaveThresholds"),
            spectrumWarmMark: document.getElementById("spectrumWarmMark"),
            spectrumHotMark: document.getElementById("spectrumHotMark"),
            spectrumColdPart: document.getElementById("spectrumColdPart"),
            spectrumWarmPart: document.getElementById("spectrumWarmPart"),
            spectrumHotPart: document.getElementById("spectrumHotPart"),
            ruleGroupsGrid: document.getElementById("ruleGroupsGrid"),
            btnResetDefaultRules: document.getElementById("btnResetDefaultRules"),
            btnOpenAddCriteriaGroup: document.getElementById("btnOpenAddCriteriaGroup"),

            // Header Actions
            btnAddCriteriaBtn: document.getElementById("btnAddCriteriaBtn"),
            btnReScoreAll: document.getElementById("btnReScoreAll"),

            // Modals
            criteriaModal: document.getElementById("criteriaModal"),
            criteriaModalTitle: document.getElementById("criteriaModalTitle"),
            criteriaForm: document.getElementById("criteriaForm"),
            criteriaId: document.getElementById("criteriaId"),
            criteriaGroup: document.getElementById("criteriaGroup"),
            criteriaName: document.getElementById("criteriaName"),
            criteriaField: document.getElementById("criteriaField"),
            criteriaOperator: document.getElementById("criteriaOperator"),
            criteriaValue: document.getElementById("criteriaValue"),
            criteriaPoints: document.getElementById("criteriaPoints"),
            pointsHintBadge: document.getElementById("pointsHintBadge"),
            criteriaDescription: document.getElementById("criteriaDescription"),
            criteriaIsActive: document.getElementById("criteriaIsActive"),
            btnCloseCriteriaModal: document.getElementById("btnCloseCriteriaModal"),
            btnCancelCriteria: document.getElementById("btnCancelCriteria"),
            btnSaveCriteria: document.getElementById("btnSaveCriteria"),

            breakdownModal: document.getElementById("breakdownModal"),
            breakdownModalBody: document.getElementById("breakdownModalBody"),
            btnCloseBreakdownModal: document.getElementById("btnCloseBreakdownModal"),
            btnCloseBreakdownBtn: document.getElementById("btnCloseBreakdownBtn"),
            btnActionFromBreakdown: document.getElementById("btnActionFromBreakdown"),

            toastContainer: document.getElementById("leadScoringToastContainer")
        };
    }

    // Backend is the source of scores, classifications, criteria and thresholds.
    const endpoint = "/api/lead-scoring";
    function applyConfig(config) {
        serverConfig = config;
        scoringRules = config.rules;
        scoringThresholds = config.thresholds;
    }
    function normalizeLead(lead) {
        return { ...lead, id: String(lead.id), industry: lead.industry || "",
            title: lead.title || "", source: lead.source || "", recentActivity: lead.recentActivity || "" };
    }
    function syncControls() {
        const configured = serverConfig?.configured === true;
        const manageDisabled = !ready || busy || !canManage || !configured;
        for (const key of ["btnSaveThresholds", "btnAddCriteriaBtn", "btnOpenAddCriteriaGroup", "btnSaveCriteria", "inputHotThreshold", "inputWarmThreshold"])
            if (dom[key]) dom[key].disabled = manageDisabled;
        if (dom.btnReScoreAll) dom.btnReScoreAll.disabled = !ready || busy || !canScore || !configured;
        // No unapproved defaults can overwrite a server configuration.
        if (dom.btnResetDefaultRules) { dom.btnResetDefaultRules.disabled = true; dom.btnResetDefaultRules.title = "Chưa có bộ mặc định nghiệp vụ được phê duyệt"; }
        dom.ruleGroupsGrid?.querySelectorAll("button,input").forEach(el => { el.disabled = manageDisabled; });
    }
    function renderAll() { updateKpis(); renderRulesGrid(); renderLeadsTable(); syncControls(); }
    function showLoadError(error) {
        ready = false; syncControls();
        if (dom.scoringEmptyState) dom.scoringEmptyState.style.display = "none";
        if (dom.leadScoringTableBody) dom.leadScoringTableBody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:24px">' + escapeHtml(error.message) + ' <button type="button" id="scoringRetry">Thử tải lại</button></td></tr>';
        document.getElementById("scoringRetry")?.addEventListener("click", loadInitialData);
        showToast(error.message, "error");
    }
    async function fetchState() {
        const config = await window.CrmApi.request(endpoint + "/config");
        const items = [];
        for (let page = 1; ; page++) {
            const data = await window.CrmApi.request(endpoint + "/leads?" + new URLSearchParams({page, size:100}));
            if (data.version !== config.version) throw new Error("Cấu hình vừa thay đổi; hãy tải lại dữ liệu.");
            items.push(...data.items.map(normalizeLead));
            if (page >= data.totalPages || !data.items.length) break;
        }
        applyConfig(config); scoredLeads = items; ready = true; renderAll();
    }
    async function loadInitialData() {
        ready = false; syncControls();
        if (dom.scoringEmptyState) dom.scoringEmptyState.style.display = "none";
        if (dom.leadScoringTableBody) dom.leadScoringTableBody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:24px">Đang tải dữ liệu từ máy chủ...</td></tr>';
        try {
            if (!window.CrmApi) throw new Error("Không tải được thư viện kết nối API.");
            const session = await window.CrmApi.session();
            canManage = session.permissions?.includes("lead.scoring.manage") === true;
            canScore = session.permissions?.includes("lead.score") === true;
            await fetchState();
            if (!serverConfig.configured) showToast("Chưa có cấu hình chấm điểm nghiệp vụ được xác nhận. Lead chưa được phân loại.", "warning");
        } catch (error) { showLoadError(error); }
    }
    async function runMutation(request, message) {
        if (busy || !ready) return false;
        busy = true; syncControls();
        try {
            const result = await request();
            try { await fetchState(); }
            catch (error) { showLoadError(new Error("Máy chủ đã lưu thành công nhưng chưa tải được dữ liệu mới: " + error.message)); return true; }
            showToast(typeof message === "function" ? message(result) : message, "success");
            return true;
        } catch (error) {
            showToast(error.message, "error");
            if (error.status === 409) {
                try { await fetchState(); } catch (refreshError) { showLoadError(refreshError); }
            } else renderAll();
            return false;
        }
        finally { busy = false; syncControls(); }
    }
    async function saveRules(rules, message) {
        return runMutation(() => window.LeadScoringService.saveScoringRules(rules), message);
    }
    window.LeadScoringService = {
        getScoringRules: () => window.CrmApi.request(endpoint + "/config"),
        saveScoringRules: rules => window.CrmApi.request(endpoint + "/rules", {method:"POST", body:JSON.stringify({version:serverConfig.version,rules})}),
        reScoreAll: () => window.CrmApi.request(endpoint + "/re-score", {method:"POST", body:"{}"}),
        getScoredLeads: (filter = {}) => window.CrmApi.request(endpoint + "/leads?" + new URLSearchParams({size:100,...filter})),
        getLead: id => window.CrmApi.request(endpoint + "/leads/" + encodeURIComponent(id))
    };

    // 6. UI RENDERING: KPIS & SPECTRUM
    function updateKpis() {
        const total = scoredLeads.length;
        const hotCount = scoredLeads.filter(l => l.classification === "HOT").length;
        const warmCount = scoredLeads.filter(l => l.classification === "WARM").length;
        const coldCount = scoredLeads.filter(l => l.classification === "COLD").length;

        const sumScore = scoredLeads.reduce((acc, cur) => acc + (cur.score || 0), 0);
        const avgScore = total > 0 ? (sumScore / total).toFixed(1) : 0;

        if (dom.kpiTotalLeads) dom.kpiTotalLeads.textContent = total;
        if (dom.kpiAvgScore) dom.kpiAvgScore.textContent = `Điểm trung bình: ${avgScore} điểm`;
        if (dom.kpiHotLeads) dom.kpiHotLeads.textContent = `${hotCount} (${total > 0 ? ((hotCount / total) * 100).toFixed(0) : 0}%)`;
        if (dom.kpiWarmLeads) dom.kpiWarmLeads.textContent = `${warmCount} (${total > 0 ? ((warmCount / total) * 100).toFixed(0) : 0}%)`;
        if (dom.kpiColdLeads) dom.kpiColdLeads.textContent = `${coldCount} (${total > 0 ? ((coldCount / total) * 100).toFixed(0) : 0}%)`;

        if (dom.kpiHotThresholdBadge) dom.kpiHotThresholdBadge.textContent = `≥ ${scoringThresholds.hot}đ`;
        if (dom.kpiWarmThresholdBadge) dom.kpiWarmThresholdBadge.textContent = `${scoringThresholds.warm} - ${scoringThresholds.hot - 1}đ`;
        if (dom.kpiColdThresholdBadge) dom.kpiColdThresholdBadge.textContent = `< ${scoringThresholds.warm}đ`;

        if (dom.tabLeadsCount) dom.tabLeadsCount.textContent = total;
        if (dom.tabRulesCount) dom.tabRulesCount.textContent = scoringRules.length;

        if (!serverConfig?.configured) {
            for (const key of ["kpiHotThresholdBadge","kpiWarmThresholdBadge","kpiColdThresholdBadge"]) if (dom[key]) dom[key].textContent = "Chưa cấu hình";
            if (dom.kpiAvgScore) dom.kpiAvgScore.textContent = "Chưa có điểm được tính";
        }
        updateSpectrumDisplay();
    }

    function updateSpectrumDisplay() {
        const hot = scoringThresholds.hot;
        const warm = scoringThresholds.warm;
        if (hot == null || warm == null) {
            if (dom.inputHotThreshold) dom.inputHotThreshold.value = "";
            if (dom.inputWarmThreshold) dom.inputWarmThreshold.value = "";
            for (const key of ["displayColdThreshold","spectrumWarmMark","spectrumHotMark"]) if (dom[key]) dom[key].textContent = "Chưa cấu hình";
            return;
        }

        if (dom.inputHotThreshold) dom.inputHotThreshold.value = hot;
        if (dom.inputWarmThreshold) dom.inputWarmThreshold.value = warm;
        if (dom.displayColdThreshold) dom.displayColdThreshold.textContent = warm;

        if (dom.spectrumWarmMark) dom.spectrumWarmMark.textContent = `Ấm: ${warm}đ`;
        if (dom.spectrumHotMark) dom.spectrumHotMark.textContent = `Nóng: ${hot}đ`;

        // Calculate visual widths (out of 100)
        const coldWidth = Math.min(Math.max(warm, 15), 60);
        const warmWidth = Math.min(Math.max(hot - warm, 15), 60);
        const hotWidth = Math.max(100 - (coldWidth + warmWidth), 15);

        if (dom.spectrumColdPart) dom.spectrumColdPart.style.width = `${coldWidth}%`;
        if (dom.spectrumWarmPart) dom.spectrumWarmPart.style.width = `${warmWidth}%`;
        if (dom.spectrumHotPart) dom.spectrumHotPart.style.width = `${hotWidth}%`;
    }

    // 7. UI RENDERING: TAB 1 (LEADS TABLE)
    function getFilteredLeads() {
        const query = dom.scoringSearchInput ? dom.scoringSearchInput.value.trim().toLowerCase() : "";
        const classification = dom.filterClassification ? dom.filterClassification.value : "ALL";
        const industry = dom.filterIndustry ? dom.filterIndustry.value : "ALL";
        const sort = dom.sortOrder ? dom.sortOrder.value : "score_desc";

        return scoredLeads.filter(lead => {
            if (classification !== "ALL" && lead.classification !== classification) return false;
            if (industry !== "ALL" && !String(lead.industry || "").toLowerCase().includes(industry.toLowerCase())) return false;

            if (query) {
                const searchStr = `${lead.fullName} ${lead.phone} ${lead.email} ${lead.companyName} ${lead.title} ${lead.source}`.toLowerCase();
                if (!searchStr.includes(query)) return false;
            }
            return true;
        }).sort((a, b) => {
            if (sort === "score_desc") return b.score - a.score;
            if (sort === "score_asc") return a.score - b.score;
            if (sort === "name_asc") return a.fullName.localeCompare(b.fullName);
            if (sort === "newest") return new Date(b.createdAt) - new Date(a.createdAt);
            return 0;
        });
    }

    function renderLeadsTable() {
        if (!dom.leadScoringTableBody) return;

        const filtered = getFilteredLeads();

        if (filtered.length === 0) {
            dom.leadScoringTableBody.innerHTML = "";
            if (dom.scoringEmptyState) dom.scoringEmptyState.style.display = "block";
            return;
        }

        if (dom.scoringEmptyState) dom.scoringEmptyState.style.display = "none";

        let html = "";
        filtered.forEach((lead, idx) => {
            const classBadge = getClassificationBadge(lead.classification);
            const scorePercent = Math.max(0, Math.min(lead.score ?? 0, 100));
            const classColor = String(lead.classification || "UNCONFIGURED").toLowerCase();

            html += `
                <tr data-lead-id="${escapeHtml(lead.id)}">
                    <td class="col-seq">${idx + 1}</td>

                    <!-- Lead Identity -->
                    <td class="col-lead">
                        <div class="lead-person-cell">
                            <div class="lead-avatar-bubble">${escapeHtml(lead.avatar || "LD")}</div>
                            <div>
                                <div class="lead-person-name">
                                    <strong>${escapeHtml(lead.fullName)}</strong>
                                </div>
                                <div class="lead-person-title">${escapeHtml(lead.title || "Khách hàng")}</div>
                                <div class="lead-contact-line">
                                    <span><i class="fa-solid fa-phone text-primary"></i> ${escapeHtml(lead.phone)}</span>
                                    <span><i class="fa-solid fa-envelope text-primary"></i> ${escapeHtml(lead.email)}</span>
                                </div>
                            </div>
                        </div>
                    </td>

                    <!-- Company & Size -->
                    <td class="col-company">
                        <div class="company-cell-name">${escapeHtml(lead.companyName)}</div>
                        <div class="company-cell-meta">
                            <span><i class="fa-solid fa-building text-purple"></i> ${escapeHtml(lead.industry)}</span>
                            <span>&bull;</span>
                            <span><i class="fa-solid fa-users text-primary"></i> ${lead.employeeCount ?? "—"} nhân sự</span>
                        </div>
                    </td>

                    <!-- Source & Activity -->
                    <td class="col-source">
                        <div style="font-weight: 600; font-size: 12.5px; color: var(--crm-text);">
                            <i class="fa-solid fa-bullhorn text-warning"></i> ${escapeHtml(lead.source)}
                        </div>
                        <div style="font-size: 11.5px; color: var(--crm-muted); margin-top: 3px;">
                            ${escapeHtml(lead.recentActivity)}
                        </div>
                    </td>

                    <!-- Quality Score -->
                    <td class="col-score">
                        <div class="score-meter-wrap">
                            <div class="score-meter-head">
                                <span class="score-value-bold">${lead.score ?? "—"} <small style="font-size: 11px; font-weight: 600;">điểm</small></span>
                                <span class="score-scale-max">/ 100+</span>
                            </div>
                            <div class="score-progress-bar">
                                <div class="score-progress-fill ${classColor}" style="width: ${scorePercent}%;"></div>
                            </div>
                        </div>
                    </td>

                    <!-- Classification -->
                    <td class="col-class">
                        ${classBadge}
                    </td>

                    <!-- Score Breakdown Details -->
                    <td class="col-breakdown">
                        <button type="button" class="btn-view-breakdown" data-lead-id="${escapeHtml(lead.id)}" title="Xem lý do cộng/trừ điểm">
                            <i class="fa-solid fa-magnifying-glass-chart"></i> Chi tiết
                        </button>
                    </td>

                    <!-- Actions -->
                    <td class="col-actions">
                        <div class="table-lead-actions">
                            <button type="button" class="btn-call" data-lead-id="${escapeHtml(lead.id)}" data-phone="${escapeHtml(lead.phone)}" title="Gọi điện cho Lead ngay">
                                <i class="fa-solid fa-phone"></i>
                            </button>
                            <button type="button" class="btn-convert" data-lead-id="${escapeHtml(lead.id)}" title="Tạo cơ hội bán hàng từ Lead này">
                                <i class="fa-solid fa-bullseye"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        });

        dom.leadScoringTableBody.innerHTML = html;
        attachTableEvents();
    }

    function getClassificationBadge(classification) {
        if (classification === "UNCONFIGURED") return '<span class="class-badge">Chưa cấu hình</span>';
        if (classification === "HOT") {
            return `<span class="class-badge hot"><i class="fa-solid fa-fire-flame-curved"></i> Hot Lead</span>`;
        }
        if (classification === "WARM") {
            return `<span class="class-badge warm"><i class="fa-solid fa-cloud-sun"></i> Warm Lead</span>`;
        }
        return `<span class="class-badge cold"><i class="fa-solid fa-snowflake"></i> Cold Lead</span>`;
    }

    function attachTableEvents() {
        // View Breakdown
        const breakdownButtons = dom.leadScoringTableBody.querySelectorAll(".btn-view-breakdown");
        breakdownButtons.forEach(btn => {
            btn.addEventListener("click", () => {
                const leadId = btn.getAttribute("data-lead-id");
                openBreakdownModal(leadId);
            });
        });

        // Quick Call
        const callButtons = dom.leadScoringTableBody.querySelectorAll(".btn-call");
        callButtons.forEach(btn => {
            btn.addEventListener("click", () => {
                const phone = btn.getAttribute("data-phone");
                if (phone) location.href = "tel:" + encodeURIComponent(phone);
            });
        });

        // Convert to opportunity
        const convertButtons = dom.leadScoringTableBody.querySelectorAll(".btn-convert");
        convertButtons.forEach(btn => {
            btn.addEventListener("click", () => {
                const leadId = btn.getAttribute("data-lead-id");
                const targetLead = scoredLeads.find(l => l.id === leadId);
                showToast("Chức năng chuyển đổi Lead thuộc S4-08, chưa được tích hợp tại trang này.", "warning");
            });
        });
    }

    // 8. UI RENDERING: TAB 2 (CRITERIA RULES GROUPS)
    const GROUP_CONFIGS = {
        INDUSTRY: {
            title: "Ngành nghề phù hợp (Industry Fit)",
            icon: "fa-building",
            description: "Cộng điểm ưu tiên cho các khách hàng thuộc lĩnh vực then chốt."
        },
        COMPANY_SIZE: {
            title: "Quy mô Doanh nghiệp (Company Size)",
            icon: "fa-users",
            description: "Đánh giá mức độ tiềm năng theo số lượng nhân sự."
        },
        SOURCE: {
            title: "Nguồn tiếp cận Lead (Lead Source)",
            icon: "fa-bullhorn",
            description: "Thưởng điểm cho kênh giới thiệu, phạt điểm cho data mua ngoài."
        },
        INTENT: {
            title: "Mức độ quan tâm & Hành vi (Engagement & Intent)",
            icon: "fa-bolt",
            description: "Ghi nhận mức độ khẩn thiết qua việc yêu cầu demo, khảo giá hoặc bỏ tương tác."
        },
        CUSTOM: {
            title: "Tiêu chí mở rộng khác (Custom Criteria)",
            icon: "fa-sliders",
            description: "Các quy tắc nghiệp vụ bổ sung theo chiến dịch bán hàng."
        }
    };

    function renderRulesGrid() {
        if (!dom.ruleGroupsGrid) return;

        // Group rules by group
        const grouped = {
            INDUSTRY: [],
            COMPANY_SIZE: [],
            SOURCE: [],
            INTENT: [],
            CUSTOM: []
        };

        scoringRules.forEach(rule => {
            const grp = rule.group || "CUSTOM";
            if (!grouped[grp]) grouped[grp] = [];
            grouped[grp].push(rule);
        });

        let html = "";
        Object.keys(GROUP_CONFIGS).forEach(groupKey => {
            const config = GROUP_CONFIGS[groupKey];
            const rules = grouped[groupKey] || [];
            if (groupKey === "CUSTOM" && rules.length === 0) return; // Hide custom if empty

            html += `
                <div class="group-card" data-group="${groupKey}">
                    <div class="group-card-head">
                        <div class="group-card-title">
                            <i class="fa-solid ${config.icon}"></i>
                            ${config.title}
                            <span class="tab-badge" style="font-size: 11px;">${rules.filter(r => r.isActive).length}/${rules.length} kích hoạt</span>
                        </div>
                        <button type="button" class="btn-add-to-group" data-group="${groupKey}">
                            <i class="fa-solid fa-plus"></i> Thêm tiêu chí
                        </button>
                    </div>

                    <div class="group-criteria-list">
                        ${rules.length === 0 ? `
                            <p style="font-size: 12px; color: var(--crm-muted); text-align: center; margin: 16px 0;">Chưa có tiêu chí nào trong nhóm này.</p>
                        ` : ""}

                        ${rules.map(rule => {
                            const isPositive = rule.points > 0;
                            const isZero = rule.points === 0;
                            const pillClass = isZero ? "zero" : (isPositive ? "positive" : "negative");
                            const pointsText = isPositive ? `+${rule.points}đ` : `${rule.points}đ`;

                            return `
                                <div class="criteria-item-row ${rule.isActive ? "" : "inactive"}" data-rule-id="${escapeHtml(rule.id)}">
                                    <div class="criteria-item-left">
                                        <div class="criteria-item-name" title="${escapeHtml(rule.name)}">${escapeHtml(rule.name)}</div>
                                        <div class="criteria-item-condition">${rule.field} ${rule.operator} "${escapeHtml(rule.value)}"</div>
                                    </div>
                                    <div class="criteria-item-right">
                                        <span class="points-pill ${pillClass}">${pointsText}</span>
                                        
                                        <label class="toggle-switch-label" title="${rule.isActive ? "Đang bật" : "Đang tắt"}">
                                            <input type="checkbox" class="rule-active-toggle" data-rule-id="${escapeHtml(rule.id)}" ${rule.isActive ? "checked" : ""}>
                                            <span class="toggle-slider"></span>
                                        </label>

                                        <button type="button" class="btn-criteria-action edit btn-edit-rule" data-rule-id="${escapeHtml(rule.id)}" title="Sửa tiêu chí">
                                            <i class="fa-solid fa-pen-to-square"></i>
                                        </button>
                                        <button type="button" class="btn-criteria-action delete btn-delete-rule" data-rule-id="${escapeHtml(rule.id)}" title="Xóa tiêu chí">
                                            <i class="fa-solid fa-trash-can"></i>
                                        </button>
                                    </div>
                                </div>
                            `;
                        }).join("")}
                    </div>
                </div>
            `;
        });

        dom.ruleGroupsGrid.innerHTML = html;
        attachRuleEvents();
        syncControls();
    }

    function attachRuleEvents() {
        // Add to group
        const addGroupButtons = dom.ruleGroupsGrid.querySelectorAll(".btn-add-to-group");
        addGroupButtons.forEach(btn => {
            btn.addEventListener("click", () => {
                const group = btn.getAttribute("data-group");
                openCriteriaModal(null, group);
            });
        });

        // Toggle active status
        const toggleCheckboxes = dom.ruleGroupsGrid.querySelectorAll(".rule-active-toggle");
        toggleCheckboxes.forEach(chk => {
            chk.addEventListener("change", async () => {
                const ruleId = chk.getAttribute("data-rule-id");
                const active = chk.checked;
                const rule = scoringRules.find(r => r.id === ruleId);
                if (rule) await saveRules(scoringRules.map(r => r.id === ruleId ? {...r,isActive:active} : r), "Đã cập nhật trạng thái tiêu chí.");
                else renderRulesGrid();
            });
        });

        // Edit rule
        const editButtons = dom.ruleGroupsGrid.querySelectorAll(".btn-edit-rule");
        editButtons.forEach(btn => {
            btn.addEventListener("click", () => {
                const ruleId = btn.getAttribute("data-rule-id");
                openCriteriaModal(ruleId);
            });
        });

        // Delete rule
        const deleteButtons = dom.ruleGroupsGrid.querySelectorAll(".btn-delete-rule");
        deleteButtons.forEach(btn => {
            btn.addEventListener("click", async () => {
                const ruleId = btn.getAttribute("data-rule-id");
                const rule = scoringRules.find(r => r.id === ruleId);
                if (confirm('Xóa tiêu chí "' + rule?.name + '"?')) await saveRules(scoringRules.filter(r => r.id !== ruleId), "Đã xóa tiêu chí và cập nhật điểm Lead.");
            });
        });
    }

    // 9. MODALS: CRITERIA MODAL (ADD / EDIT)
    function openCriteriaModal(ruleId = null, prefillGroup = null) {
        editingCriteriaId = ruleId;

        if (ruleId) {
            const rule = scoringRules.find(r => r.id === ruleId);
            if (!rule) return;
            if (dom.criteriaModalTitle) dom.criteriaModalTitle.innerHTML = `<i class="fa-solid fa-pen-to-square text-primary"></i> Chỉnh sửa tiêu chí chấm điểm`;
            if (dom.criteriaId) dom.criteriaId.value = rule.id;
            if (dom.criteriaGroup) dom.criteriaGroup.value = rule.group;
            if (dom.criteriaName) dom.criteriaName.value = rule.name;
            if (dom.criteriaField) dom.criteriaField.value = rule.field;
            if (dom.criteriaOperator) dom.criteriaOperator.value = rule.operator;
            if (dom.criteriaValue) dom.criteriaValue.value = rule.value;
            if (dom.criteriaPoints) dom.criteriaPoints.value = rule.points;
            if (dom.criteriaDescription) dom.criteriaDescription.value = rule.description || "";
            if (dom.criteriaIsActive) dom.criteriaIsActive.checked = rule.isActive !== false;
        } else {
            if (dom.criteriaModalTitle) dom.criteriaModalTitle.innerHTML = `<i class="fa-solid fa-plus-circle text-primary"></i> Thêm tiêu chí chấm điểm mới`;
            if (dom.criteriaForm) dom.criteriaForm.reset();
            if (dom.criteriaId) dom.criteriaId.value = "";
            if (dom.criteriaGroup) dom.criteriaGroup.value = prefillGroup || "INDUSTRY";
            if (dom.criteriaPoints) dom.criteriaPoints.value = "15";
            if (dom.criteriaIsActive) dom.criteriaIsActive.checked = true;
        }

        updatePointsHint();
        if (dom.criteriaModal) dom.criteriaModal.classList.add("active");
    }

    function closeCriteriaModal() {
        if (dom.criteriaModal) dom.criteriaModal.classList.remove("active");
        editingCriteriaId = null;
    }

    function updatePointsHint() {
        if (!dom.criteriaPoints || !dom.pointsHintBadge) return;
        const val = parseInt(dom.criteriaPoints.value, 10) || 0;
        dom.pointsHintBadge.textContent = val >= 0 ? `+${val}đ` : `${val}đ`;
        dom.pointsHintBadge.style.color = val > 0 ? "#15803d" : (val < 0 ? "#b91c1c" : "var(--crm-text)");
    }

    async function saveCriteriaFromModal() {
        if (!canManage || busy || !ready || !dom.criteriaForm?.reportValidity()) return;
        const name = dom.criteriaName ? dom.criteriaName.value.trim() : "";
        const group = dom.criteriaGroup ? dom.criteriaGroup.value : "INDUSTRY";
        const field = dom.criteriaField ? dom.criteriaField.value : "industry";
        const operator = dom.criteriaOperator ? dom.criteriaOperator.value : "EQUALS";
        const value = dom.criteriaValue ? dom.criteriaValue.value.trim() : "";
        const points = Number(dom.criteriaPoints.value);
        if (!Number.isInteger(points)) { showToast("Điểm phải là số nguyên.", "error"); return; }
        const description = dom.criteriaDescription ? dom.criteriaDescription.value.trim() : "";
        const isActive = dom.criteriaIsActive ? dom.criteriaIsActive.checked : true;

        if (!name || !value) {
            alert("Vui lòng điền đầy đủ Tên tiêu chí và Giá trị so sánh!");
            return;
        }

        const rule = { id: editingCriteriaId || "R-" + crypto.randomUUID(), name, group, field, operator, value, points, description, isActive };
        const rules = editingCriteriaId ? scoringRules.map(r => r.id === editingCriteriaId ? rule : r) : [...scoringRules,rule];
        if (await saveRules(rules, "Đã lưu tiêu chí và cập nhật điểm Lead.")) closeCriteriaModal();
    }

    // 10. MODALS: SCORE BREAKDOWN MODAL
    async function openBreakdownModal(leadId) {
        let lead;
        try { lead = normalizeLead(await window.LeadScoringService.getLead(leadId)); }
        catch (error) { showToast(error.message, "error"); return; }
        if (!lead || !dom.breakdownModalBody) return;

        const classBadge = getClassificationBadge(lead.classification);
        const recommendation = lead.classification === "UNCONFIGURED"
            ? "Chưa có cấu hình nghiệp vụ được xác nhận. Lead chưa được chấm điểm hoặc phân loại."
            : lead.classification === "HOT"
            ? "🔥 <strong>Khuyến nghị Sales:</strong> Lead Nóng có độ quan tâm và tính phù hợp rất cao. Hãy gọi điện thoại trực tiếp trong ca làm việc để chốt cuộc hẹn demo hoặc ký hợp đồng."
            : (lead.classification === "WARM"
                ? "🌤️ <strong>Khuyến nghị Sales:</strong> Lead Ấm có tiềm năng tốt nhưng cần thêm tài liệu. Hãy gửi email case study, hồ sơ giải pháp và xếp lịch follow-up sau 3 ngày."
                : "❄️ <strong>Khuyến nghị Sales:</strong> Lead Lạnh chưa có nhu cầu cấp bách. Tạm thời đưa vào chuỗi email marketing nuôi dưỡng tự động hàng tuần.");

        let breakdownRows = "";
        if (lead.breakdown && lead.breakdown.length > 0) {
            lead.breakdown.forEach(item => {
                const isPos = item.points > 0;
                const pillClass = item.points === 0 ? "zero" : (isPos ? "positive" : "negative");
                const pointsStr = isPos ? `+${item.points}đ` : `${item.points}đ`;
                const groupTitle = GROUP_CONFIGS[item.group] ? GROUP_CONFIGS[item.group].title.split("(")[0].trim() : item.group;

                breakdownRows += `
                    <tr>
                        <td><span style="font-size: 11px; font-weight: 700; color: var(--crm-primary);">${escapeHtml(groupTitle)}</span></td>
                        <td>
                            <strong>${escapeHtml(item.ruleName)}</strong>
                            <div style="font-size: 11px; color: var(--crm-muted);">${escapeHtml(item.description || "")}</div>
                        </td>
                        <td><code>${escapeHtml(String(item.actualValue ?? ""))}</code></td>
                        <td style="text-align: right;"><span class="points-pill ${pillClass}">${pointsStr}</span></td>
                    </tr>
                `;
            });
        } else {
            breakdownRows = `
                <tr>
                    <td colspan="4" style="text-align: center; color: var(--crm-muted); padding: 18px;">
                        Lead này chưa khớp với bất kỳ tiêu chí cộng điểm nào đang được kích hoạt.
                    </td>
                </tr>
            `;
        }

        dom.breakdownModalBody.innerHTML = `
            <div class="breakdown-lead-header">
                <div class="breakdown-lead-identity">
                    <div class="lead-avatar-bubble" style="width: 44px; height: 44px; font-size: 15px;">${escapeHtml(lead.avatar || "LD")}</div>
                    <div>
                        <div style="font-size: 15px; font-weight: 800; color: var(--crm-text);">${escapeHtml(lead.fullName)}</div>
                        <div style="font-size: 12px; color: var(--crm-muted);">${escapeHtml(lead.title)} &bull; ${escapeHtml(lead.companyName)}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted); margin-top: 2px;">
                            <i class="fa-solid fa-phone text-primary"></i> ${escapeHtml(lead.phone)} | <i class="fa-solid fa-envelope text-primary"></i> ${escapeHtml(lead.email)}
                        </div>
                    </div>
                </div>

                <div class="breakdown-score-badge-large">
                    <div style="font-size: 28px; font-weight: 800; color: var(--crm-text); line-height: 1;">${lead.score ?? "—"} <small style="font-size: 13px;">điểm</small></div>
                    <div style="margin-top: 6px;">${classBadge}</div>
                </div>
            </div>

            <div style="margin-bottom: 12px; font-size: 13px; color: var(--crm-text-2);">
                Chi tiết tiêu chí do máy chủ tính. Tổng điểm trước giới hạn: ${lead.rawScore ?? "—"}; điểm lưu: ${lead.score ?? "—"}.
            </div>

            <table class="breakdown-table">
                <thead>
                    <tr>
                        <th style="width: 130px;">Nhóm tiêu chí</th>
                        <th>Quy tắc khớp</th>
                        <th style="width: 140px;">Dữ liệu của Lead</th>
                        <th style="width: 80px; text-align: right;">Điểm</th>
                    </tr>
                </thead>
                <tbody>
                    ${breakdownRows}
                </tbody>
                <tfoot>
                    <tr class="breakdown-total-row">
                        <td colspan="3" style="text-align: right; padding: 12px;">TỔNG ĐIỂM CHẤT LƯỢNG LEAD:</td>
                        <td style="text-align: right; font-size: 15px; color: var(--crm-primary); padding: 12px;">${lead.score ?? "—"}đ</td>
                    </tr>
                </tfoot>
            </table>

            <div style="margin-top: 14px; padding: 12px 14px; border-radius: var(--crm-radius-sm); background: var(--crm-surface-soft); border-left: 4px solid var(--crm-primary); font-size: 12.5px; line-height: 1.5;">
                ${recommendation}
            </div>
        `;

        if (dom.btnActionFromBreakdown) {
            dom.btnActionFromBreakdown.onclick = () => {
                if (lead.phone) location.href = "tel:" + encodeURIComponent(lead.phone);
                closeBreakdownModal();
            };
        }

        if (dom.breakdownModal) dom.breakdownModal.classList.add("active");
    }

    function closeBreakdownModal() {
        if (dom.breakdownModal) dom.breakdownModal.classList.remove("active");
    }

    // 11. TOAST NOTIFICATION UTILITY
    function showToast(message, type = "info") {
        if (!dom.toastContainer) return;

        const toast = document.createElement("div");
        toast.className = `scoring-toast ${type}`;

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

    // 12. EVENT LISTENERS SETUP
    function setupEventListeners() {
        // Tab switching
        if (dom.tabRankingBtn && dom.tabRulesBtn) {
            dom.tabRankingBtn.addEventListener("click", () => switchTab("ranking"));
            dom.tabRulesBtn.addEventListener("click", () => switchTab("rules"));
        }

        // Filters in Tab 1
        if (dom.scoringSearchInput) {
            dom.scoringSearchInput.addEventListener("input", () => renderLeadsTable());
        }
        if (dom.filterClassification) {
            dom.filterClassification.addEventListener("change", () => renderLeadsTable());
        }
        if (dom.filterIndustry) {
            dom.filterIndustry.addEventListener("change", () => renderLeadsTable());
        }
        if (dom.sortOrder) {
            dom.sortOrder.addEventListener("change", () => renderLeadsTable());
        }
        if (dom.btnResetFilters) {
            dom.btnResetFilters.addEventListener("click", () => {
                if (dom.scoringSearchInput) dom.scoringSearchInput.value = "";
                if (dom.filterClassification) dom.filterClassification.value = "ALL";
                if (dom.filterIndustry) dom.filterIndustry.value = "ALL";
                if (dom.sortOrder) dom.sortOrder.value = "score_desc";
                renderLeadsTable();
                showToast("Đã đặt lại toàn bộ bộ lọc!", "info");
            });
        }

        // Save thresholds on the server, preserving the explicitly configured policy.
        dom.btnSaveThresholds?.addEventListener("click", async () => {
            const hot = Number(dom.inputHotThreshold.value), warm = Number(dom.inputWarmThreshold.value);
            if (!Number.isInteger(hot) || !Number.isInteger(warm) || warm < 0 || warm >= hot) {
                showToast("Ngưỡng phải là số nguyên và 0 <= Ấm < Nóng.", "error"); return;
            }
            if (!serverConfig?.configured) { showToast("Chưa xác nhận cấu hình nghiệp vụ.", "warning"); return; }
            await runMutation(() => window.CrmApi.request(endpoint + "/config", {method:"PUT", body:JSON.stringify({
                version:serverConfig.version, thresholds:{hot,warm}, aggregation:serverConfig.aggregation,
                minScore:serverConfig.minScore,maxScore:serverConfig.maxScore,rules:scoringRules
            })}), "Đã lưu ngưỡng và cập nhật phân loại Lead.");
        });

        // Open Add Criteria Modal buttons
        if (dom.btnAddCriteriaBtn) {
            dom.btnAddCriteriaBtn.addEventListener("click", () => openCriteriaModal());
        }
        if (dom.btnOpenAddCriteriaGroup) {
            dom.btnOpenAddCriteriaGroup.addEventListener("click", () => openCriteriaModal());
        }

        // Criteria Modal close & save
        if (dom.btnCloseCriteriaModal) dom.btnCloseCriteriaModal.addEventListener("click", closeCriteriaModal);
        if (dom.btnCancelCriteria) dom.btnCancelCriteria.addEventListener("click", closeCriteriaModal);
        if (dom.btnSaveCriteria) dom.btnSaveCriteria.addEventListener("click", saveCriteriaFromModal);
        if (dom.criteriaPoints) dom.criteriaPoints.addEventListener("input", updatePointsHint);

        // Breakdown Modal close
        if (dom.btnCloseBreakdownModal) dom.btnCloseBreakdownModal.addEventListener("click", closeBreakdownModal);
        if (dom.btnCloseBreakdownBtn) dom.btnCloseBreakdownBtn.addEventListener("click", closeBreakdownModal);

        // Backdrop click to close
        if (dom.criteriaModal) {
            dom.criteriaModal.addEventListener("click", (e) => {
                if (e.target === dom.criteriaModal) closeCriteriaModal();
            });
        }
        if (dom.breakdownModal) {
            dom.breakdownModal.addEventListener("click", (e) => {
                if (e.target === dom.breakdownModal) closeBreakdownModal();
            });
        }

        // Re-score the current user's data scope on the backend.
        if (dom.btnReScoreAll) {
            dom.btnReScoreAll.addEventListener("click", handleReScoreAll);
        }

        // Escape key to close modals
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape") {
                closeCriteriaModal();
                closeBreakdownModal();
            }
        });
    }

    function switchTab(tabName) {
        currentTab = tabName;
        if (tabName === "ranking") {
            if (dom.tabRankingBtn) dom.tabRankingBtn.classList.add("active");
            if (dom.tabRulesBtn) dom.tabRulesBtn.classList.remove("active");
            if (dom.paneRanking) dom.paneRanking.classList.add("active");
            if (dom.paneRules) dom.paneRules.classList.remove("active");
            renderLeadsTable();
        } else {
            if (dom.tabRankingBtn) dom.tabRankingBtn.classList.remove("active");
            if (dom.tabRulesBtn) dom.tabRulesBtn.classList.add("active");
            if (dom.paneRanking) dom.paneRanking.classList.remove("active");
            if (dom.paneRules) dom.paneRules.classList.add("active");
            renderRulesGrid();
            updateSpectrumDisplay();
        }
    }

    async function handleReScoreAll() {
        await runMutation(() => window.LeadScoringService.reScoreAll(), result => "Đã chấm lại điểm cho " + result.count + " Lead trong phạm vi dữ liệu.");
    }

    // 13. BOOTSTRAP INITIALIZATION
    document.addEventListener("DOMContentLoaded", () => {
        initDom();
        setupEventListeners();
        loadInitialData();
    });

})();
