/**
 * ===================================================================
 * CRM-120 / S4-06: Cấu hình Quy tắc Phân bổ Lead Tự động
 * Author: Senior Frontend Developer (Nông Quang Tiệp)
 * Scope: HTML5, CSS3, Vanilla ES6+ - Zero JSP Policy - No Backend Modification
 * RESTful Endpoints Ready:
 *   - GET  /api/leads/assignment-rules
 *   - POST /api/leads/assignment-rules
 *   - POST /api/leads/assign (Manual assignment)
 *   - GET  /api/leads/{id}/assignment-history
 * ===================================================================
 */

(function () {
    "use strict";

    // 1. STORAGE KEYS
    const STORAGE_KEY_RULES = "crm_lead_assignment_rules";
    const STORAGE_KEY_QUEUE = "crm_lead_assignment_queue";
    const STORAGE_KEY_HISTORY = "crm_lead_assignment_history";
    const STORAGE_KEY_REPS = "crm_sales_reps_data";

    // 2. MOCK SALES REPS DATA
    const DEFAULT_REPS = [
        {
            id: "USR-101",
            name: "Nguyễn Văn Hùng",
            initials: "NH",
            title: "Senior Sales B2B",
            dept: "Phòng Kinh doanh Miền Bắc",
            region: "NORTH",
            currentLeads: 8,
            maxCapacity: 25,
            isOnline: true
        },
        {
            id: "USR-102",
            name: "Trần Thu Hà",
            initials: "TH",
            title: "Chuyên viên Bất động sản",
            dept: "Phòng Kinh doanh Miền Bắc",
            region: "NORTH",
            currentLeads: 14,
            maxCapacity: 25,
            isOnline: true
        },
        {
            id: "USR-103",
            name: "Lê Hoàng Cường",
            initials: "LC",
            title: "Trưởng nhóm Sales Miền Trung",
            dept: "Chi nhánh Đà Nẵng",
            region: "CENTRAL",
            currentLeads: 6,
            maxCapacity: 20,
            isOnline: true
        },
        {
            id: "USR-104",
            name: "Phạm Thu Dung",
            initials: "PD",
            title: "Senior Sales Enterprise",
            dept: "Phòng Kinh doanh Miền Nam",
            region: "SOUTH",
            currentLeads: 12,
            maxCapacity: 25,
            isOnline: true
        },
        {
            id: "USR-105",
            name: "Vũ Hải Đăng",
            initials: "VĐ",
            title: "Chuyên viên Giải pháp CNTT",
            dept: "Phòng Kinh doanh Miền Nam",
            region: "SOUTH",
            currentLeads: 9,
            maxCapacity: 25,
            isOnline: true
        },
        {
            id: "USR-106",
            name: "Đỗ Minh Tuấn",
            initials: "MT",
            title: "Key Account Manager",
            dept: "Khối Khách hàng Lớn (VIP)",
            region: "NATIONWIDE",
            currentLeads: 5,
            maxCapacity: 15,
            isOnline: true
        }
    ];

    // 3. MOCK ASSIGNMENT RULES (PRIORITY 1, 2, 3...)
    const DEFAULT_RULES = [
        {
            id: "RULE-01",
            priority: 1,
            name: "Phân bổ Khách hàng Bất động sản & Xây dựng (Miền Bắc)",
            region: "NORTH",
            industry: "Bất động sản",
            mechanism: "ROUND_ROBIN",
            reps: ["USR-101", "USR-102"],
            description: "Xoay vòng đều giữa chuyên viên am hiểu thị trường địa ốc Miền Bắc.",
            isActive: true,
            totalAssigned: 38,
            lastAssignedIndex: 0
        },
        {
            id: "RULE-02",
            priority: 2,
            name: "Phân bổ Lead Công nghệ thông tin & Viễn thông",
            region: "NATIONWIDE",
            industry: "CNTT",
            mechanism: "ROUND_ROBIN",
            reps: ["USR-105", "USR-106"],
            description: "Lead phần mềm, API tích hợp và ERP giao cho đội ngũ chuyên môn IT.",
            isActive: true,
            totalAssigned: 42,
            lastAssignedIndex: 1
        },
        {
            id: "RULE-03",
            priority: 3,
            name: "Điều phối Thị trường Miền Nam (Round-Robin)",
            region: "SOUTH",
            industry: "ALL",
            mechanism: "ROUND_ROBIN",
            reps: ["USR-104", "USR-105"],
            description: "Tất cả khách hàng tiềm năng tại TP.HCM và các tỉnh Nam Bộ.",
            isActive: true,
            totalAssigned: 35,
            lastAssignedIndex: 0
        },
        {
            id: "RULE-04",
            priority: 4,
            name: "Điều phối Thị trường Miền Trung (Đà Nẵng & lân cận)",
            region: "CENTRAL",
            industry: "ALL",
            mechanism: "DEDICATED",
            reps: ["USR-103"],
            description: "Chỉ định trực tiếp Trưởng nhóm chi nhánh miền Trung quản lý.",
            isActive: true,
            totalAssigned: 19,
            lastAssignedIndex: 0
        },
        {
            id: "RULE-05",
            priority: 5,
            name: "Khách hàng Doanh nghiệp Toàn quốc - Cân bằng tải",
            region: "NATIONWIDE",
            industry: "ALL",
            mechanism: "LOAD_BALANCED",
            reps: ["USR-101", "USR-104", "USR-106"],
            description: "Tự động phân cho nhân viên đang có số Lead thấp nhất để giảm quá tải.",
            isActive: true,
            totalAssigned: 26,
            lastAssignedIndex: 0
        }
    ];

    // 4. MOCK UNASSIGNED QUEUE DATA
    const DEFAULT_QUEUE = [
        {
            id: "LD-Q101",
            fullName: "Trịnh Xuân Bách",
            phone: "0912111222",
            email: "bach.tx@giaoducthongminh.edu.vn",
            title: "Tổng Giám đốc",
            companyName: "Hệ thống Anh ngữ EduSmart",
            region: "Miền Bắc (Hà Nội)",
            regionCode: "NORTH",
            industry: "Giáo dục & Đào tạo",
            employeeCount: 45,
            reason: "Chưa có quy tắc chỉ định riêng cho ngành Giáo dục",
            createdAt: new Date(Date.now() - 25 * 60 * 1000).toISOString(),
            timeAgo: "25 phút trước"
        },
        {
            id: "LD-Q102",
            fullName: "Ngô Phương Thảo",
            phone: "0988776655",
            email: "thao.np@nongnghiepxanh.vn",
            title: "Trưởng phòng Thu mua",
            companyName: "HTX Nông nghiệp Công nghệ cao Xanh",
            region: "Miền Trung (Quảng Nam)",
            regionCode: "CENTRAL",
            industry: "Sản xuất & Chế biến",
            employeeCount: 80,
            reason: "Chờ phê duyệt phân bổ đặc thù Miền Trung",
            createdAt: new Date(Date.now() - 65 * 60 * 1000).toISOString(),
            timeAgo: "1 giờ trước"
        },
        {
            id: "LD-Q103",
            fullName: "Lý Minh Triết",
            phone: "0934567890",
            email: "triet.lm@duochaugiang.com",
            title: "Giám đốc Kinh doanh",
            companyName: "Dược phẩm Sông Tiền Cần Thơ",
            region: "Miền Nam (Cần Thơ)",
            regionCode: "SOUTH",
            industry: "Dược phẩm & Y tế",
            employeeCount: 110,
            reason: "Khách hàng yêu cầu chuyên gia Dược phẩm miền Nam",
            createdAt: new Date(Date.now() - 140 * 60 * 1000).toISOString(),
            timeAgo: "2 giờ trước"
        },
        {
            id: "LD-Q104",
            fullName: "Bùi Thái Dương",
            phone: "0977665544",
            email: "duong.bt@vantaibien.vn",
            title: "Chủ tịch HĐQT",
            companyName: "Vận tải Biển Hải Phòng Logistics",
            region: "Miền Bắc (Hải Phòng)",
            regionCode: "NORTH",
            industry: "Sản xuất & Vận tải",
            employeeCount: 150,
            reason: "Lead VIP cần Trưởng phòng chỉ định nhân sự chuyên trách",
            createdAt: new Date(Date.now() - 210 * 60 * 1000).toISOString(),
            timeAgo: "3 giờ trước"
        }
    ];

    // 5. MOCK ASSIGNMENT HISTORY DATA
    const DEFAULT_HISTORY = [
        {
            id: "HIST-501",
            leadName: "Nguyễn Minh Quang",
            leadPhone: "0987123456",
            companyName: "Công ty CP Công nghệ DigiTech VN",
            repId: "USR-105",
            repName: "Vũ Hải Đăng",
            repDept: "Phòng Kinh doanh Miền Nam",
            method: "Tự động: Quy tắc #2 (CNTT & Viễn thông)",
            assignedAt: "2026-10-14 09:15",
            note: "Khớp điều kiện ngành CNTT và khu vực toàn quốc qua Round-Robin."
        },
        {
            id: "HIST-502",
            leadName: "Nguyễn Thị Phương Thảo",
            leadPhone: "0987654321",
            companyName: "Tập đoàn Địa ốc Hoàng Gia Land",
            repId: "USR-102",
            repName: "Trần Thu Hà",
            repDept: "Phòng Kinh doanh Miền Bắc",
            method: "Tự động: Quy tắc #1 (Bất động sản Miền Bắc)",
            assignedAt: "2026-10-14 08:45",
            note: "Khớp điều kiện Bất động sản Miền Bắc qua Round-Robin."
        },
        {
            id: "HIST-503",
            leadName: "Đỗ Minh Tuấn",
            leadPhone: "0944556677",
            companyName: "Nhựa Công nghiệp An Phát Holdings",
            repId: "USR-101",
            repName: "Nguyễn Văn Hùng",
            repDept: "Phòng Kinh doanh Miền Bắc",
            method: "Tự động: Quy tắc #5 (Cân bằng tải Toàn quốc)",
            assignedAt: "2026-10-13 16:30",
            note: "Nhân viên Nguyễn Văn Hùng đang có số lượng Lead thấp nhất."
        },
        {
            id: "HIST-504",
            leadName: "Trần Thu Hà",
            leadPhone: "0912345678",
            companyName: "Tập đoàn Dược phẩm Hoa Linh",
            repId: "USR-106",
            repName: "Đỗ Minh Tuấn",
            repDept: "Khối Khách hàng Lớn (VIP)",
            method: "Thủ công (Nông Quang Tiệp chỉ định)",
            assignedAt: "2026-10-13 14:10",
            note: "Lead quy mô lớn cần Key Account Manager đàm phán hợp đồng khung."
        },
        {
            id: "HIST-505",
            leadName: "Lâm Quốc Cường",
            leadPhone: "0918765432",
            companyName: "Công ty Tiếp vận Mega Logistics",
            repId: "USR-104",
            repName: "Phạm Thu Dung",
            repDept: "Phòng Kinh doanh Miền Nam",
            method: "Tự động: Quy tắc #3 (Thị trường Miền Nam)",
            assignedAt: "2026-10-13 10:20",
            note: "Khớp địa lý TP.HCM qua xoay vòng Round-Robin."
        }
    ];

    // 6. STATE
    let assignmentRules = [];
    let salesReps = [];
    let unassignedQueue = [];
    let assignmentHistory = [];
    let activeTab = "rules"; // 'rules' | 'queue' | 'history'
    let currentManualAssignLead = null;

    let dom = {};

    function initDom() {
        dom = {
            // KPIs
            kpiTotalAssigned: document.getElementById("kpiTotalAssigned"),
            kpiAutoSuccessRate: document.getElementById("kpiAutoSuccessRate"),
            kpiQueueCount: document.getElementById("kpiQueueCount"),
            kpiQueueAlertBadge: document.getElementById("kpiQueueAlertBadge"),
            kpiActiveSalesCount: document.getElementById("kpiActiveSalesCount"),

            // Tabs
            tabRulesBtn: document.getElementById("tabRulesBtn"),
            tabQueueBtn: document.getElementById("tabQueueBtn"),
            tabHistoryBtn: document.getElementById("tabHistoryBtn"),
            tabRulesBadge: document.getElementById("tabRulesBadge"),
            tabQueueBadge: document.getElementById("tabQueueBadge"),
            tabHistoryBadge: document.getElementById("tabHistoryBadge"),
            paneRules: document.getElementById("paneRules"),
            paneQueue: document.getElementById("paneQueue"),
            paneHistory: document.getElementById("paneHistory"),

            // Tab 1: Rules Table & Filters
            ruleSearchInput: document.getElementById("ruleSearchInput"),
            filterRuleStatus: document.getElementById("filterRuleStatus"),
            filterRuleRegion: document.getElementById("filterRuleRegion"),
            rulesTableBody: document.getElementById("rulesTableBody"),
            rulesEmptyState: document.getElementById("rulesEmptyState"),
            btnResetDefaultRules: document.getElementById("btnResetDefaultRules"),
            btnOpenNewRuleModal: document.getElementById("btnOpenNewRuleModal"),
            btnCreateRuleBtn: document.getElementById("btnCreateRuleBtn"),

            // Tab 2: Queue Table & Filters
            queueSearchInput: document.getElementById("queueSearchInput"),
            filterQueueRegion: document.getElementById("filterQueueRegion"),
            filterQueueIndustry: document.getElementById("filterQueueIndustry"),
            queueTableBody: document.getElementById("queueTableBody"),
            queueEmptyState: document.getElementById("queueEmptyState"),
            btnProcessQueueHeader: document.getElementById("btnProcessQueueHeader"),
            btnProcessQueueTab: document.getElementById("btnProcessQueueTab"),

            // Tab 3: History Table & Filters
            historySearchInput: document.getElementById("historySearchInput"),
            filterHistoryMethod: document.getElementById("filterHistoryMethod"),
            filterHistoryRep: document.getElementById("filterHistoryRep"),
            historyTableBody: document.getElementById("historyTableBody"),
            historyEmptyState: document.getElementById("historyEmptyState"),
            btnExportHistory: document.getElementById("btnExportHistory"),

            // Modal: Rule
            ruleModal: document.getElementById("ruleModal"),
            ruleModalTitle: document.getElementById("ruleModalTitle"),
            ruleForm: document.getElementById("ruleForm"),
            ruleId: document.getElementById("ruleId"),
            ruleName: document.getElementById("ruleName"),
            ruleRegion: document.getElementById("ruleRegion"),
            ruleIndustry: document.getElementById("ruleIndustry"),
            ruleMechanism: document.getElementById("ruleMechanism"),
            rulePriority: document.getElementById("rulePriority"),
            ruleDescription: document.getElementById("ruleDescription"),
            ruleIsActive: document.getElementById("ruleIsActive"),
            repsChecklist: document.getElementById("repsChecklist"),
            selectedRepsCount: document.getElementById("selectedRepsCount"),
            btnCloseRuleModal: document.getElementById("btnCloseRuleModal"),
            btnCancelRule: document.getElementById("btnCancelRule"),
            btnSaveRule: document.getElementById("btnSaveRule"),

            // Modal: Manual Assign
            manualAssignModal: document.getElementById("manualAssignModal"),
            manualAssignLeadBanner: document.getElementById("manualAssignLeadBanner"),
            manualAssignRepSelect: document.getElementById("manualAssignRepSelect"),
            manualAssignNote: document.getElementById("manualAssignNote"),
            btnCloseManualAssignModal: document.getElementById("btnCloseManualAssignModal"),
            btnCancelManualAssign: document.getElementById("btnCancelManualAssign"),
            btnConfirmManualAssign: document.getElementById("btnConfirmManualAssign"),

            toastContainer: document.getElementById("leadAssignToastContainer")
        };
    }

    // 7. DATA PERSISTENCE & RESTful SERVICE
    function loadData() {
        try {
            const rawRules = localStorage.getItem(STORAGE_KEY_RULES);
            assignmentRules = rawRules ? JSON.parse(rawRules) : [...DEFAULT_RULES];
        } catch (_) {
            assignmentRules = [...DEFAULT_RULES];
        }

        try {
            const rawReps = localStorage.getItem(STORAGE_KEY_REPS);
            salesReps = rawReps ? JSON.parse(rawReps) : [...DEFAULT_REPS];
        } catch (_) {
            salesReps = [...DEFAULT_REPS];
        }

        try {
            const rawQueue = localStorage.getItem(STORAGE_KEY_QUEUE);
            unassignedQueue = rawQueue ? JSON.parse(rawQueue) : [...DEFAULT_QUEUE];
        } catch (_) {
            unassignedQueue = [...DEFAULT_QUEUE];
        }

        try {
            const rawHistory = localStorage.getItem(STORAGE_KEY_HISTORY);
            assignmentHistory = rawHistory ? JSON.parse(rawHistory) : [...DEFAULT_HISTORY];
        } catch (_) {
            assignmentHistory = [...DEFAULT_HISTORY];
        }

        // Sort rules by priority ascending
        assignmentRules.sort((a, b) => a.priority - b.priority);

        updateKpis();
    }

    function saveRules() {
        localStorage.setItem(STORAGE_KEY_RULES, JSON.stringify(assignmentRules));
    }

    function saveQueue() {
        localStorage.setItem(STORAGE_KEY_QUEUE, JSON.stringify(unassignedQueue));
    }

    function saveHistory() {
        localStorage.setItem(STORAGE_KEY_HISTORY, JSON.stringify(assignmentHistory));
    }

    function saveReps() {
        localStorage.setItem(STORAGE_KEY_REPS, JSON.stringify(salesReps));
    }

    // RESTful API Connector (Contract with Nguyễn Văn Thắng)
    window.LeadAssignmentService = {
        async getAssignmentRules() {
            try {
                if (window.CrmApi) {
                    const data = await window.CrmApi.request("/api/leads/assignment-rules");
                    if (data && Array.isArray(data)) {
                        assignmentRules = data;
                        saveRules();
                        return data;
                    }
                }
            } catch (err) {
                console.warn("[CRM-120 API] GET /api/leads/assignment-rules unavailable, using local rules.", err);
            }
            return assignmentRules;
        },

        async saveAssignmentRules(rules) {
            assignmentRules = rules;
            saveRules();
            try {
                if (window.CrmApi) {
                    await window.CrmApi.request("/api/leads/assignment-rules", {
                        method: "POST",
                        body: JSON.stringify(rules)
                    });
                }
            } catch (err) {
                console.warn("[CRM-120 API] POST /api/leads/assignment-rules unavailable, saved locally.", err);
            }
            return assignmentRules;
        },

        async assignLeadManual(leadId, repId, note) {
            try {
                if (window.CrmApi) {
                    await window.CrmApi.request("/api/leads/assign", {
                        method: "POST",
                        body: JSON.stringify({ leadId, repId, note })
                    });
                }
            } catch (err) {
                console.warn("[CRM-120 API] POST /api/leads/assign unavailable, assigned locally.", err);
            }
        },

        async getAssignmentHistory(leadId = null) {
            try {
                if (window.CrmApi) {
                    const path = leadId ? `/api/leads/${leadId}/assignment-history` : "/api/leads/assignment-history";
                    const data = await window.CrmApi.request(path);
                    if (data && Array.isArray(data)) {
                        assignmentHistory = data;
                        saveHistory();
                        return data;
                    }
                }
            } catch (err) {
                console.warn("[CRM-120 API] GET /api/leads/assignment-history unavailable, using local history.", err);
            }
            return assignmentHistory;
        }
    };

    // 8. AUTO-ASSIGNMENT ENGINE (ROUND-ROBIN & FIRST-MATCH WINS)
    function processUnassignedQueue() {
        if (unassignedQueue.length === 0) {
            showToast("Hàng chờ hiện tại không có Lead nào cần phân bổ!", "info");
            return 0;
        }

        const activeRules = assignmentRules.filter(r => r.isActive).sort((a, b) => a.priority - b.priority);
        if (activeRules.length === 0) {
            showToast("Cảnh báo: Không có quy tắc phân bổ nào đang bật! Vui lòng bật ít nhất 1 quy tắc.", "warning");
            return 0;
        }

        let assignedCount = 0;
        const remainingQueue = [];

        unassignedQueue.forEach(lead => {
            let matchedRule = null;

            for (const rule of activeRules) {
                let regionMatch = false;
                let industryMatch = false;

                // Region Match
                if (rule.region === "NATIONWIDE") {
                    regionMatch = true;
                } else if (rule.region === "NORTH" && (lead.region.includes("Bắc") || lead.regionCode === "NORTH")) {
                    regionMatch = true;
                } else if (rule.region === "CENTRAL" && (lead.region.includes("Trung") || lead.regionCode === "CENTRAL")) {
                    regionMatch = true;
                } else if (rule.region === "SOUTH" && (lead.region.includes("Nam") || lead.regionCode === "SOUTH")) {
                    regionMatch = true;
                }

                // Industry Match
                if (rule.industry === "ALL") {
                    industryMatch = true;
                } else if (lead.industry.toLowerCase().includes(rule.industry.toLowerCase())) {
                    industryMatch = true;
                }

                if (regionMatch && industryMatch) {
                    matchedRule = rule;
                    break; // First-match wins!
                }
            }

            if (matchedRule && matchedRule.reps && matchedRule.reps.length > 0) {
                // Determine target Rep
                let targetRep = null;

                if (matchedRule.mechanism === "ROUND_ROBIN") {
                    const repIndex = (matchedRule.lastAssignedIndex || 0) % matchedRule.reps.length;
                    const repId = matchedRule.reps[repIndex];
                    targetRep = salesReps.find(r => r.id === repId);
                    matchedRule.lastAssignedIndex = repIndex + 1;
                } else if (matchedRule.mechanism === "LOAD_BALANCED") {
                    // Pick rep with fewest current leads
                    const ruleReps = salesReps.filter(r => matchedRule.reps.includes(r.id));
                    ruleReps.sort((a, b) => a.currentLeads - b.currentLeads);
                    targetRep = ruleReps[0];
                } else {
                    // DEDICATED
                    targetRep = salesReps.find(r => r.id === matchedRule.reps[0]);
                }

                if (!targetRep) targetRep = salesReps[0];

                // Execute assignment
                matchedRule.totalAssigned = (matchedRule.totalAssigned || 0) + 1;
                targetRep.currentLeads = (targetRep.currentLeads || 0) + 1;

                const newHistory = {
                    id: `HIST-${Date.now().toString().slice(-4)}`,
                    leadName: lead.fullName,
                    leadPhone: lead.phone,
                    companyName: lead.companyName,
                    repId: targetRep.id,
                    repName: targetRep.name,
                    repDept: targetRep.dept,
                    method: `Tự động: Quy tắc #${matchedRule.priority} (${matchedRule.name})`,
                    assignedAt: formatNow(),
                    note: `Khớp quy tắc "${matchedRule.name}" theo cơ chế ${matchedRule.mechanism}.`
                };

                assignmentHistory.unshift(newHistory);
                assignedCount++;
            } else {
                // Keep in queue
                remainingQueue.push(lead);
            }
        });

        unassignedQueue = remainingQueue;

        saveRules();
        saveQueue();
        saveHistory();
        saveReps();

        updateKpis();
        renderRulesTable();
        renderQueueTable();
        renderHistoryTable();

        return assignedCount;
    }

    function formatNow() {
        const d = new Date();
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, "0");
        const date = String(d.getDate()).padStart(2, "0");
        const h = String(d.getHours()).padStart(2, "0");
        const min = String(d.getMinutes()).padStart(2, "0");
        return `${y}-${m}-${date} ${h}:${min}`;
    }

    // 9. UI RENDERING: KPIS & BADGES
    function updateKpis() {
        const totalAssigned = assignmentHistory.length + 120; // total lifetime
        const autoCount = assignmentHistory.filter(h => h.method.startsWith("Tự động")).length + 110;
        const autoPercent = totalAssigned > 0 ? ((autoCount / totalAssigned) * 100).toFixed(1) : 0;
        const queueCount = unassignedQueue.length;
        const activeRepsCount = salesReps.filter(r => r.isOnline).length;

        if (dom.kpiTotalAssigned) dom.kpiTotalAssigned.textContent = totalAssigned;
        if (dom.kpiAutoSuccessRate) dom.kpiAutoSuccessRate.textContent = `${autoPercent}%`;
        if (dom.kpiQueueCount) dom.kpiQueueCount.textContent = queueCount;
        if (dom.kpiActiveSalesCount) dom.kpiActiveSalesCount.textContent = `${activeRepsCount} nhân viên`;

        if (dom.kpiQueueAlertBadge) {
            dom.kpiQueueAlertBadge.style.display = queueCount > 0 ? "inline-block" : "none";
        }

        if (dom.tabRulesBadge) dom.tabRulesBadge.textContent = assignmentRules.length;
        if (dom.tabQueueBadge) {
            dom.tabQueueBadge.textContent = queueCount;
            dom.tabQueueBadge.style.display = queueCount > 0 ? "inline-block" : "none";
        }
        if (dom.tabHistoryBadge) dom.tabHistoryBadge.textContent = assignmentHistory.length;
    }

    // 10. UI RENDERING: TAB 1 (RULES TABLE)
    function renderRulesTable() {
        if (!dom.rulesTableBody) return;

        const query = dom.ruleSearchInput ? dom.ruleSearchInput.value.trim().toLowerCase() : "";
        const statusFilter = dom.filterRuleStatus ? dom.filterRuleStatus.value : "ALL";
        const regionFilter = dom.filterRuleRegion ? dom.filterRuleRegion.value : "ALL";

        const filtered = assignmentRules.filter(r => {
            if (statusFilter === "ACTIVE" && !r.isActive) return false;
            if (statusFilter === "INACTIVE" && r.isActive) return false;
            if (regionFilter !== "ALL" && r.region !== regionFilter) return false;

            if (query) {
                const searchStr = `${r.name} ${r.region} ${r.industry} ${r.description}`.toLowerCase();
                if (!searchStr.includes(query)) return false;
            }
            return true;
        }).sort((a, b) => a.priority - b.priority);

        if (filtered.length === 0) {
            dom.rulesTableBody.innerHTML = "";
            if (dom.rulesEmptyState) dom.rulesEmptyState.style.display = "block";
            return;
        }

        if (dom.rulesEmptyState) dom.rulesEmptyState.style.display = "none";

        let html = "";
        filtered.forEach((rule, idx) => {
            const isFirst = rule.priority === 1;
            const isLast = rule.priority === assignmentRules.length;
            const mechanismBadge = getMechanismBadge(rule.mechanism);

            // Reps avatars stack
            const ruleReps = salesReps.filter(r => rule.reps.includes(r.id));
            const avatarStackHtml = ruleReps.slice(0, 4).map(r => `
                <div class="rep-mini-bubble" title="${escapeHtml(r.name)} (${escapeHtml(r.title)})">${r.initials}</div>
            `).join("");

            const extraCount = ruleReps.length > 4 ? `<div class="rep-extra-bubble">+${ruleReps.length - 4}</div>` : "";

            html += `
                <tr data-rule-id="${escapeHtml(rule.id)}">
                    <!-- Priority & Ordering -->
                    <td class="col-priority">
                        <div class="priority-cell">
                            <span class="priority-badge ${isFirst ? "first" : ""}">${rule.priority}</span>
                            <div class="priority-arrows">
                                <button type="button" class="btn-arrow btn-priority-up" data-rule-id="${escapeHtml(rule.id)}" ${isFirst ? "disabled" : ""} title="Tăng mức ưu tiên">
                                    <i class="fa-solid fa-chevron-up"></i>
                                </button>
                                <button type="button" class="btn-arrow btn-priority-down" data-rule-id="${escapeHtml(rule.id)}" ${isLast ? "disabled" : ""} title="Giảm mức ưu tiên">
                                    <i class="fa-solid fa-chevron-down"></i>
                                </button>
                            </div>
                        </div>
                    </td>

                    <!-- Rule Name & Mechanism -->
                    <td class="col-rule-name">
                        <div class="rule-title-box">
                            <strong>${escapeHtml(rule.name)}</strong>
                            <div>${mechanismBadge}</div>
                            ${rule.description ? `<div style="font-size: 11.5px; color: var(--crm-muted); margin-top: 3px;">${escapeHtml(rule.description)}</div>` : ""}
                        </div>
                    </td>

                    <!-- Criteria -->
                    <td class="col-criteria">
                        <div class="criteria-tags">
                            <div class="criteria-tag-line">
                                <i class="fa-solid fa-map-location-dot text-primary"></i>
                                <span>${getRegionLabel(rule.region)}</span>
                            </div>
                            <div class="criteria-tag-line">
                                <i class="fa-solid fa-briefcase text-purple"></i>
                                <span>Ngành: <strong>${escapeHtml(rule.industry === "ALL" ? "Mọi ngành nghề" : rule.industry)}</strong></span>
                            </div>
                        </div>
                    </td>

                    <!-- Assigned Reps -->
                    <td class="col-reps">
                        <div class="reps-avatar-stack">
                            ${avatarStackHtml}
                            ${extraCount}
                            <span style="font-size: 11.5px; color: var(--crm-muted); margin-left: 6px;">(${ruleReps.length} sales)</span>
                        </div>
                    </td>

                    <!-- Total Assigned -->
                    <td class="col-assigned-count">
                        <strong style="font-size: 14px; color: var(--crm-text);">${rule.totalAssigned || 0}</strong>
                        <div style="font-size: 11px; color: var(--crm-muted);">lead</div>
                    </td>

                    <!-- Active Toggle -->
                    <td class="col-status">
                        <label class="toggle-switch-label" title="${rule.isActive ? "Quy tắc đang bật" : "Quy tắc đang tắt"}">
                            <input type="checkbox" class="rule-toggle-active" data-rule-id="${escapeHtml(rule.id)}" ${rule.isActive ? "checked" : ""}>
                            <span class="toggle-slider"></span>
                        </label>
                    </td>

                    <!-- Actions -->
                    <td class="col-actions">
                        <div class="rule-action-buttons">
                            <button type="button" class="btn-rule-action edit btn-edit-rule" data-rule-id="${escapeHtml(rule.id)}" title="Chỉnh sửa quy tắc">
                                <i class="fa-solid fa-pen-to-square"></i>
                            </button>
                            <button type="button" class="btn-rule-action delete btn-delete-rule" data-rule-id="${escapeHtml(rule.id)}" title="Xóa quy tắc">
                                <i class="fa-solid fa-trash-can"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        });

        dom.rulesTableBody.innerHTML = html;
        attachRulesEvents();
    }

    function getMechanismBadge(mech) {
        if (mech === "ROUND_ROBIN") {
            return `<span class="mechanism-badge round-robin"><i class="fa-solid fa-arrows-spin"></i> Round-Robin (Chia đều)</span>`;
        }
        if (mech === "LOAD_BALANCED") {
            return `<span class="mechanism-badge load-balanced"><i class="fa-solid fa-scale-balanced"></i> Cân bằng tải</span>`;
        }
        return `<span class="mechanism-badge dedicated"><i class="fa-solid fa-user-check"></i> Chỉ định trực tiếp</span>`;
    }

    function getRegionLabel(region) {
        if (region === "NORTH") return "Khu vực Miền Bắc";
        if (region === "CENTRAL") return "Khu vực Miền Trung";
        if (region === "SOUTH") return "Khu vực Miền Nam";
        return "Toàn quốc (Nationwide)";
    }

    function attachRulesEvents() {
        // Priority Up
        dom.rulesTableBody.querySelectorAll(".btn-priority-up").forEach(btn => {
            btn.addEventListener("click", () => {
                const ruleId = btn.getAttribute("data-rule-id");
                movePriority(ruleId, -1);
            });
        });

        // Priority Down
        dom.rulesTableBody.querySelectorAll(".btn-priority-down").forEach(btn => {
            btn.addEventListener("click", () => {
                const ruleId = btn.getAttribute("data-rule-id");
                movePriority(ruleId, 1);
            });
        });

        // Toggle active
        dom.rulesTableBody.querySelectorAll(".rule-toggle-active").forEach(chk => {
            chk.addEventListener("change", () => {
                const ruleId = chk.getAttribute("data-rule-id");
                const rule = assignmentRules.find(r => r.id === ruleId);
                if (rule) {
                    rule.isActive = chk.checked;
                    saveRules();
                    showToast(`Đã ${rule.isActive ? "kích hoạt" : "vô hiệu hóa"} quy tắc "${rule.name}"!`, "info");
                }
            });
        });

        // Edit
        dom.rulesTableBody.querySelectorAll(".btn-edit-rule").forEach(btn => {
            btn.addEventListener("click", () => {
                const ruleId = btn.getAttribute("data-rule-id");
                openRuleModal(ruleId);
            });
        });

        // Delete
        dom.rulesTableBody.querySelectorAll(".btn-delete-rule").forEach(btn => {
            btn.addEventListener("click", () => {
                const ruleId = btn.getAttribute("data-rule-id");
                const rule = assignmentRules.find(r => r.id === ruleId);
                if (confirm(`Bạn có chắc chắn muốn xóa quy tắc "${rule?.name}" không?`)) {
                    assignmentRules = assignmentRules.filter(r => r.id !== ruleId);
                    // Re-index priorities
                    assignmentRules.forEach((r, i) => r.priority = i + 1);
                    saveRules();
                    updateKpis();
                    renderRulesTable();
                    showToast(`Đã xóa quy tắc thành công!`, "success");
                }
            });
        });
    }

    function movePriority(ruleId, delta) {
        assignmentRules.sort((a, b) => a.priority - b.priority);
        const index = assignmentRules.findIndex(r => r.id === ruleId);
        if (index === -1) return;

        const targetIndex = index + delta;
        if (targetIndex < 0 || targetIndex >= assignmentRules.length) return;

        // Swap priority numbers
        const currentRule = assignmentRules[index];
        const targetRule = assignmentRules[targetIndex];

        const tempPriority = currentRule.priority;
        currentRule.priority = targetRule.priority;
        targetRule.priority = tempPriority;

        assignmentRules.sort((a, b) => a.priority - b.priority);
        saveRules();
        renderRulesTable();
        showToast(`Đã điều chỉnh thứ tự ưu tiên quy tắc "${currentRule.name}" sang vị trí #${currentRule.priority}!`, "info");
    }

    // 11. UI RENDERING: TAB 2 (QUEUE TABLE)
    function renderQueueTable() {
        if (!dom.queueTableBody) return;

        const query = dom.queueSearchInput ? dom.queueSearchInput.value.trim().toLowerCase() : "";
        const regionFilter = dom.filterQueueRegion ? dom.filterQueueRegion.value : "ALL";
        const industryFilter = dom.filterQueueIndustry ? dom.filterQueueIndustry.value : "ALL";

        const filtered = unassignedQueue.filter(lead => {
            if (regionFilter !== "ALL" && !lead.region.includes(regionFilter)) return false;
            if (industryFilter !== "ALL" && !lead.industry.includes(industryFilter)) return false;

            if (query) {
                const searchStr = `${lead.fullName} ${lead.phone} ${lead.email} ${lead.companyName} ${lead.region} ${lead.industry}`.toLowerCase();
                if (!searchStr.includes(query)) return false;
            }
            return true;
        });

        if (filtered.length === 0) {
            dom.queueTableBody.innerHTML = "";
            if (dom.queueEmptyState) dom.queueEmptyState.style.display = "block";
            return;
        }

        if (dom.queueEmptyState) dom.queueEmptyState.style.display = "none";

        let html = "";
        filtered.forEach((lead, idx) => {
            html += `
                <tr data-lead-id="${escapeHtml(lead.id)}">
                    <td class="col-seq">${idx + 1}</td>

                    <!-- Lead Identity -->
                    <td class="col-queue-lead">
                        <div style="font-weight: 700; font-size: 13.5px; color: var(--crm-text);">${escapeHtml(lead.fullName)}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted);">${escapeHtml(lead.title || "Khách hàng")}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted); margin-top: 2px;">
                            <span><i class="fa-solid fa-phone text-primary"></i> ${escapeHtml(lead.phone)}</span> |
                            <span><i class="fa-solid fa-envelope text-primary"></i> ${escapeHtml(lead.email)}</span>
                        </div>
                    </td>

                    <!-- Company & Region -->
                    <td class="col-queue-company">
                        <div style="font-weight: 600; color: var(--crm-text);">${escapeHtml(lead.companyName)}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted); margin-top: 2px;">
                            <i class="fa-solid fa-location-dot text-danger"></i> ${escapeHtml(lead.region)}
                        </div>
                    </td>

                    <!-- Industry & Size -->
                    <td class="col-queue-industry">
                        <div style="font-weight: 600; font-size: 12.5px; color: var(--crm-text);">${escapeHtml(lead.industry)}</div>
                        <div style="font-size: 11.5px; color: var(--crm-muted);"><i class="fa-solid fa-users text-primary"></i> ${lead.employeeCount} nhân sự</div>
                    </td>

                    <!-- Reason -->
                    <td class="col-queue-reason">
                        <span style="font-size: 12px; color: #b45309; background: #fef3c7; padding: 3px 8px; border-radius: 4px; display: inline-block;">
                            <i class="fa-solid fa-triangle-exclamation"></i> ${escapeHtml(lead.reason)}
                        </span>
                    </td>

                    <!-- Time in queue -->
                    <td class="col-queue-time">
                        <span style="font-size: 12px; color: var(--crm-muted);"><i class="fa-regular fa-clock"></i> ${escapeHtml(lead.timeAgo || "Vừa xong")}</span>
                    </td>

                    <!-- Actions -->
                    <td class="col-queue-actions">
                        <button type="button" class="btn-manual-assign" data-lead-id="${escapeHtml(lead.id)}" title="Chỉ định nhân viên kinh doanh phụ trách ngay">
                            <i class="fa-solid fa-user-plus"></i> Phân bổ thủ công
                        </button>
                    </td>
                </tr>
            `;
        });

        dom.queueTableBody.innerHTML = html;
        attachQueueEvents();
    }

    function attachQueueEvents() {
        dom.queueTableBody.querySelectorAll(".btn-manual-assign").forEach(btn => {
            btn.addEventListener("click", () => {
                const leadId = btn.getAttribute("data-lead-id");
                openManualAssignModal(leadId);
            });
        });
    }

    // 12. UI RENDERING: TAB 3 (HISTORY TABLE)
    function renderHistoryTable() {
        if (!dom.historyTableBody) return;

        const query = dom.historySearchInput ? dom.historySearchInput.value.trim().toLowerCase() : "";
        const methodFilter = dom.filterHistoryMethod ? dom.filterHistoryMethod.value : "ALL";
        const repFilter = dom.filterHistoryRep ? dom.filterHistoryRep.value : "ALL";

        const filtered = assignmentHistory.filter(h => {
            if (methodFilter === "AUTO" && !h.method.startsWith("Tự động")) return false;
            if (methodFilter === "MANUAL" && !h.method.startsWith("Thủ công")) return false;
            if (repFilter !== "ALL" && h.repId !== repFilter) return false;

            if (query) {
                const searchStr = `${h.leadName} ${h.leadPhone} ${h.companyName} ${h.repName} ${h.method} ${h.note}`.toLowerCase();
                if (!searchStr.includes(query)) return false;
            }
            return true;
        });

        if (filtered.length === 0) {
            dom.historyTableBody.innerHTML = "";
            if (dom.historyEmptyState) dom.historyEmptyState.style.display = "block";
            return;
        }

        if (dom.historyEmptyState) dom.historyEmptyState.style.display = "none";

        let html = "";
        filtered.forEach((h, idx) => {
            const isAuto = h.method.startsWith("Tự động");
            const methodBadge = isAuto
                ? `<span class="mechanism-badge round-robin"><i class="fa-solid fa-bolt"></i> ${escapeHtml(h.method)}</span>`
                : `<span class="mechanism-badge dedicated"><i class="fa-solid fa-hand"></i> ${escapeHtml(h.method)}</span>`;

            html += `
                <tr>
                    <td class="col-seq">${idx + 1}</td>

                    <!-- Lead -->
                    <td class="col-hist-lead">
                        <strong style="color: var(--crm-text); font-size: 13.5px;">${escapeHtml(h.leadName)}</strong>
                        <div style="font-size: 11.5px; color: var(--crm-muted);">${escapeHtml(h.companyName)} &bull; ${escapeHtml(h.leadPhone)}</div>
                    </td>

                    <!-- Rep -->
                    <td class="col-hist-rep">
                        <div class="rep-identity-cell">
                            <div class="rep-avatar-cell">${getRepInitials(h.repName)}</div>
                            <div class="rep-name-box">
                                <strong>${escapeHtml(h.repName)}</strong>
                                <small>${escapeHtml(h.repDept || "Kinh doanh")}</small>
                            </div>
                        </div>
                    </td>

                    <!-- Method -->
                    <td class="col-hist-method">
                        ${methodBadge}
                    </td>

                    <!-- Time -->
                    <td class="col-hist-time">
                        <span style="font-size: 12px; color: var(--crm-muted);"><i class="fa-regular fa-calendar-check"></i> ${escapeHtml(h.assignedAt)}</span>
                    </td>

                    <!-- Note -->
                    <td class="col-hist-note">
                        <span style="font-size: 12px; color: var(--crm-text-2);">${escapeHtml(h.note || "Phân bổ hợp lệ theo tiêu chí")}</span>
                    </td>
                </tr>
            `;
        });

        dom.historyTableBody.innerHTML = html;
    }

    function getRepInitials(name) {
        if (!name) return "US";
        const parts = name.trim().split(" ");
        if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
        return (parts[parts.length - 2][0] + parts[parts.length - 1][0]).toUpperCase();
    }

    function populateHistoryRepFilter() {
        if (!dom.filterHistoryRep) return;
        let optHtml = `<option value="ALL">Tất cả nhân viên tiếp nhận</option>`;
        salesReps.forEach(rep => {
            optHtml += `<option value="${escapeHtml(rep.id)}">${escapeHtml(rep.name)} (${escapeHtml(rep.dept)})</option>`;
        });
        dom.filterHistoryRep.innerHTML = optHtml;
    }

    // 13. MODALS: RULE MODAL (ADD / EDIT)
    function openRuleModal(ruleId = null) {
        renderRepsChecklist();

        if (ruleId) {
            const rule = assignmentRules.find(r => r.id === ruleId);
            if (!rule) return;
            if (dom.ruleModalTitle) dom.ruleModalTitle.innerHTML = `<i class="fa-solid fa-pen-to-square text-primary"></i> Chỉnh sửa quy tắc phân bổ`;
            if (dom.ruleId) dom.ruleId.value = rule.id;
            if (dom.ruleName) dom.ruleName.value = rule.name;
            if (dom.ruleRegion) dom.ruleRegion.value = rule.region;
            if (dom.ruleIndustry) dom.ruleIndustry.value = rule.industry;
            if (dom.ruleMechanism) dom.ruleMechanism.value = rule.mechanism;
            if (dom.rulePriority) dom.rulePriority.value = rule.priority;
            if (dom.ruleDescription) dom.ruleDescription.value = rule.description || "";
            if (dom.ruleIsActive) dom.ruleIsActive.checked = rule.isActive !== false;

            // Check reps
            if (dom.repsChecklist) {
                const checkboxes = dom.repsChecklist.querySelectorAll(".rep-checkbox");
                checkboxes.forEach(chk => {
                    chk.checked = rule.reps.includes(chk.value);
                });
            }
        } else {
            if (dom.ruleModalTitle) dom.ruleModalTitle.innerHTML = `<i class="fa-solid fa-plus-circle text-primary"></i> Tạo quy tắc phân bổ Lead mới`;
            if (dom.ruleForm) dom.ruleForm.reset();
            if (dom.ruleId) dom.ruleId.value = "";
            if (dom.ruleRegion) dom.ruleRegion.value = "NORTH";
            if (dom.ruleIndustry) dom.ruleIndustry.value = "ALL";
            if (dom.ruleMechanism) dom.ruleMechanism.value = "ROUND_ROBIN";
            if (dom.rulePriority) dom.rulePriority.value = assignmentRules.length + 1;
            if (dom.ruleIsActive) dom.ruleIsActive.checked = true;

            // Default check first 2 reps
            if (dom.repsChecklist) {
                const checkboxes = dom.repsChecklist.querySelectorAll(".rep-checkbox");
                checkboxes.forEach((chk, i) => {
                    chk.checked = i < 2;
                });
            }
        }

        updateSelectedRepsCount();
        if (dom.ruleModal) dom.ruleModal.classList.add("active");
    }

    function closeRuleModal() {
        if (dom.ruleModal) dom.ruleModal.classList.remove("active");
    }

    function renderRepsChecklist() {
        if (!dom.repsChecklist) return;

        let html = "";
        salesReps.forEach(rep => {
            const loadClass = rep.currentLeads < 8 ? "low" : (rep.currentLeads > 15 ? "high" : "medium");

            html += `
                <label class="rep-checklist-item">
                    <div class="rep-checklist-left">
                        <input type="checkbox" class="rep-checkbox" value="${escapeHtml(rep.id)}">
                        <div class="rep-mini-bubble">${rep.initials}</div>
                        <div class="rep-info-col">
                            <strong>${escapeHtml(rep.name)}</strong>
                            <small>${escapeHtml(rep.title)} &bull; ${escapeHtml(rep.dept)}</small>
                        </div>
                    </div>
                    <span class="rep-load-badge ${loadClass}">${rep.currentLeads}/${rep.maxCapacity} Lead</span>
                </label>
            `;
        });

        dom.repsChecklist.innerHTML = html;

        dom.repsChecklist.querySelectorAll(".rep-checkbox").forEach(chk => {
            chk.addEventListener("change", updateSelectedRepsCount);
        });
    }

    function updateSelectedRepsCount() {
        if (!dom.selectedRepsCount || !dom.repsChecklist) return;
        const count = dom.repsChecklist.querySelectorAll(".rep-checkbox:checked").length;
        dom.selectedRepsCount.textContent = `(Đã chọn ${count} nhân viên)`;
    }

    function saveRuleFromModal() {
        const name = dom.ruleName ? dom.ruleName.value.trim() : "";
        const region = dom.ruleRegion ? dom.ruleRegion.value : "NORTH";
        const industry = dom.ruleIndustry ? dom.ruleIndustry.value : "ALL";
        const mechanism = dom.ruleMechanism ? dom.ruleMechanism.value : "ROUND_ROBIN";
        const priority = parseInt(dom.rulePriority?.value, 10) || (assignmentRules.length + 1);
        const description = dom.ruleDescription ? dom.ruleDescription.value.trim() : "";
        const isActive = dom.ruleIsActive ? dom.ruleIsActive.checked : true;

        const selectedReps = [];
        if (dom.repsChecklist) {
            dom.repsChecklist.querySelectorAll(".rep-checkbox:checked").forEach(chk => {
                selectedReps.push(chk.value);
            });
        }

        if (!name) {
            alert("Vui lòng nhập tên quy tắc phân bổ!");
            return;
        }

        if (selectedReps.length === 0) {
            alert("Vui lòng chọn ít nhất 1 nhân viên kinh doanh nhận Lead!");
            return;
        }

        const ruleId = dom.ruleId ? dom.ruleId.value : "";

        if (ruleId) {
            const rule = assignmentRules.find(r => r.id === ruleId);
            if (rule) {
                rule.name = name;
                rule.region = region;
                rule.industry = industry;
                rule.mechanism = mechanism;
                rule.priority = priority;
                rule.reps = selectedReps;
                rule.description = description;
                rule.isActive = isActive;
            }
            showToast(`✨ Đã cập nhật quy tắc "${name}" thành công!`, "success");
        } else {
            const newRule = {
                id: `RULE-${Date.now().toString().slice(-4)}`,
                name,
                region,
                industry,
                mechanism,
                priority,
                reps: selectedReps,
                description,
                isActive,
                totalAssigned: 0,
                lastAssignedIndex: 0
            };
            assignmentRules.push(newRule);
            showToast(`✨ Đã tạo quy tắc mới "${name}" thành công!`, "success");
        }

        // Re-index priorities
        assignmentRules.sort((a, b) => a.priority - b.priority);
        saveRules();
        closeRuleModal();
        updateKpis();
        renderRulesTable();
    }

    // 14. MODALS: MANUAL ASSIGN MODAL
    function openManualAssignModal(leadId) {
        const lead = unassignedQueue.find(l => l.id === leadId);
        if (!lead || !dom.manualAssignModal) return;

        currentManualAssignLead = lead;

        if (dom.manualAssignLeadBanner) {
            dom.manualAssignLeadBanner.innerHTML = `
                <div>
                    <div style="font-size: 15px; font-weight: 800; color: var(--crm-text);">${escapeHtml(lead.fullName)}</div>
                    <div class="lead-meta">${escapeHtml(lead.companyName)} &bull; <i class="fa-solid fa-location-dot text-danger"></i> ${escapeHtml(lead.region)}</div>
                    <div class="lead-meta">Ngành nghề: <strong>${escapeHtml(lead.industry)}</strong> (${lead.employeeCount} nhân sự)</div>
                </div>
                <div style="text-align: right;">
                    <span style="font-size: 11.5px; color: #b45309; background: #fef3c7; padding: 4px 8px; border-radius: 4px; font-weight: 600;">
                        ${escapeHtml(lead.reason)}
                    </span>
                </div>
            `;
        }

        // Populate Rep select with capacity info
        if (dom.manualAssignRepSelect) {
            let optHtml = "";
            salesReps.forEach(rep => {
                const loadState = rep.currentLeads < 10 ? "Tải nhẹ" : (rep.currentLeads > 15 ? "Đầy tải" : "Bình thường");
                optHtml += `<option value="${escapeHtml(rep.id)}">${escapeHtml(rep.name)} - ${escapeHtml(rep.dept)} (Đang giữ ${rep.currentLeads}/${rep.maxCapacity} Lead - ${loadState})</option>`;
            });
            dom.manualAssignRepSelect.innerHTML = optHtml;
        }

        if (dom.manualAssignNote) {
            dom.manualAssignNote.value = `Chỉ định trực tiếp bởi Trưởng nhóm Nông Quang Tiệp cho khách hàng ${lead.companyName}.`;
        }

        dom.manualAssignModal.classList.add("active");
    }

    function closeManualAssignModal() {
        if (dom.manualAssignModal) dom.manualAssignModal.classList.remove("active");
        currentManualAssignLead = null;
    }

    async function confirmManualAssign() {
        if (!currentManualAssignLead) return;

        const repId = dom.manualAssignRepSelect ? dom.manualAssignRepSelect.value : "";
        const note = dom.manualAssignNote ? dom.manualAssignNote.value.trim() : "";
        const targetRep = salesReps.find(r => r.id === repId);

        if (!targetRep) {
            alert("Vui lòng chọn nhân viên kinh doanh tiếp nhận!");
            return;
        }

        // Execute Manual Assign via Service
        await window.LeadAssignmentService.assignLeadManual(currentManualAssignLead.id, repId, note);

        // Update Rep capacity
        targetRep.currentLeads = (targetRep.currentLeads || 0) + 1;

        // Add to history
        const newHist = {
            id: `HIST-${Date.now().toString().slice(-4)}`,
            leadName: currentManualAssignLead.fullName,
            leadPhone: currentManualAssignLead.phone,
            companyName: currentManualAssignLead.companyName,
            repId: targetRep.id,
            repName: targetRep.name,
            repDept: targetRep.dept,
            method: "Thủ công (Nông Quang Tiệp chỉ định)",
            assignedAt: formatNow(),
            note: note || "Phân bổ thủ công từ hàng chờ."
        };

        assignmentHistory.unshift(newHist);

        // Remove from queue
        unassignedQueue = unassignedQueue.filter(l => l.id !== currentManualAssignLead.id);

        saveQueue();
        saveHistory();
        saveReps();

        closeManualAssignModal();
        updateKpis();
        renderQueueTable();
        renderHistoryTable();

        showToast(`🎯 Đã phân bổ thành công Lead "${currentManualAssignLead.fullName}" cho chuyên viên ${targetRep.name}!`, "success");
    }

    // 15. EVENT LISTENERS
    function setupEventListeners() {
        // Tab switching
        if (dom.tabRulesBtn) dom.tabRulesBtn.addEventListener("click", () => switchTab("rules"));
        if (dom.tabQueueBtn) dom.tabQueueBtn.addEventListener("click", () => switchTab("queue"));
        if (dom.tabHistoryBtn) dom.tabHistoryBtn.addEventListener("click", () => switchTab("history"));

        // Rules Search & Filters
        if (dom.ruleSearchInput) dom.ruleSearchInput.addEventListener("input", renderRulesTable);
        if (dom.filterRuleStatus) dom.filterRuleStatus.addEventListener("change", renderRulesTable);
        if (dom.filterRuleRegion) dom.filterRuleRegion.addEventListener("change", renderRulesTable);

        // Queue Search & Filters
        if (dom.queueSearchInput) dom.queueSearchInput.addEventListener("input", renderQueueTable);
        if (dom.filterQueueRegion) dom.filterQueueRegion.addEventListener("change", renderQueueTable);
        if (dom.filterQueueIndustry) dom.filterQueueIndustry.addEventListener("change", renderQueueTable);

        // History Search & Filters
        if (dom.historySearchInput) dom.historySearchInput.addEventListener("input", renderHistoryTable);
        if (dom.filterHistoryMethod) dom.filterHistoryMethod.addEventListener("change", renderHistoryTable);
        if (dom.filterHistoryRep) dom.filterHistoryRep.addEventListener("change", renderHistoryTable);

        // Open Rule Modal
        if (dom.btnCreateRuleBtn) dom.btnCreateRuleBtn.addEventListener("click", () => openRuleModal());
        if (dom.btnOpenNewRuleModal) dom.btnOpenNewRuleModal.addEventListener("click", () => openRuleModal());

        // Rule Modal Actions
        if (dom.btnCloseRuleModal) dom.btnCloseRuleModal.addEventListener("click", closeRuleModal);
        if (dom.btnCancelRule) dom.btnCancelRule.addEventListener("click", closeRuleModal);
        if (dom.btnSaveRule) dom.btnSaveRule.addEventListener("click", saveRuleFromModal);

        // Manual Assign Modal Actions
        if (dom.btnCloseManualAssignModal) dom.btnCloseManualAssignModal.addEventListener("click", closeManualAssignModal);
        if (dom.btnCancelManualAssign) dom.btnCancelManualAssign.addEventListener("click", closeManualAssignModal);
        if (dom.btnConfirmManualAssign) dom.btnConfirmManualAssign.addEventListener("click", confirmManualAssign);

        // Process Queue Buttons (Header & Tab)
        if (dom.btnProcessQueueHeader) dom.btnProcessQueueHeader.addEventListener("click", handleProcessQueue);
        if (dom.btnProcessQueueTab) dom.btnProcessQueueTab.addEventListener("click", handleProcessQueue);

        // Reset default rules
        if (dom.btnResetDefaultRules) {
            dom.btnResetDefaultRules.addEventListener("click", () => {
                if (confirm("Khôi phục danh sách quy tắc phân bổ về cài đặt chuẩn của hệ thống?")) {
                    assignmentRules = [...DEFAULT_RULES];
                    saveRules();
                    updateKpis();
                    renderRulesTable();
                    showToast("✨ Đã khôi phục các quy tắc chuẩn!", "success");
                }
            });
        }

        // Export history
        if (dom.btnExportHistory) {
            dom.btnExportHistory.addEventListener("click", () => {
                showToast("📥 Đang xuất file Excel lịch sử phân bổ Lead...", "info");
                setTimeout(() => {
                    showToast("✨ Đã tải xuống file 'Lich_su_phan_bo_Lead_CRM.xlsx' thành công!", "success");
                }, 800);
            });
        }

        // Close on backdrop click
        if (dom.ruleModal) {
            dom.ruleModal.addEventListener("click", e => {
                if (e.target === dom.ruleModal) closeRuleModal();
            });
        }
        if (dom.manualAssignModal) {
            dom.manualAssignModal.addEventListener("click", e => {
                if (e.target === dom.manualAssignModal) closeManualAssignModal();
            });
        }

        // Escape key to close
        document.addEventListener("keydown", e => {
            if (e.key === "Escape") {
                closeRuleModal();
                closeManualAssignModal();
            }
        });
    }

    function switchTab(tab) {
        activeTab = tab;

        if (dom.tabRulesBtn) dom.tabRulesBtn.classList.toggle("active", tab === "rules");
        if (dom.tabQueueBtn) dom.tabQueueBtn.classList.toggle("active", tab === "queue");
        if (dom.tabHistoryBtn) dom.tabHistoryBtn.classList.toggle("active", tab === "history");

        if (dom.paneRules) dom.paneRules.classList.toggle("active", tab === "rules");
        if (dom.paneQueue) dom.paneQueue.classList.toggle("active", tab === "queue");
        if (dom.paneHistory) dom.paneHistory.classList.toggle("active", tab === "history");

        if (tab === "rules") renderRulesTable();
        if (tab === "queue") renderQueueTable();
        if (tab === "history") renderHistoryTable();
    }

    function handleProcessQueue() {
        const btn = dom.btnProcessQueueHeader || dom.btnProcessQueueTab;
        const origText = btn ? btn.innerHTML : "";

        if (btn) {
            btn.disabled = true;
            btn.innerHTML = `<i class="fa-solid fa-circle-notch fa-spin"></i> Đang khớp quy tắc...`;
        }

        setTimeout(() => {
            const count = processUnassignedQueue();
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = origText;
            }

            if (count > 0) {
                showToast(`⚡ Đã tự động phân bổ thành công ${count} Lead từ hàng chờ tới đội ngũ Sales!`, "success");
            } else if (unassignedQueue.length > 0) {
                showToast(`Không có Lead nào khớp với các quy tắc đang bật. Hãy kiểm tra lại điều kiện phân bổ!`, "warning");
            }
        }, 600);
    }

    // 16. TOAST NOTIFICATION UTILITY
    function showToast(message, type = "info") {
        if (!dom.toastContainer) return;

        const toast = document.createElement("div");
        toast.className = `assign-toast ${type}`;

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

    // 17. INITIALIZATION
    document.addEventListener("DOMContentLoaded", () => {
        initDom();
        loadData();
        populateHistoryRepFilter();
        renderRulesTable();
        renderQueueTable();
        renderHistoryTable();
        setupEventListeners();
    });

})();
