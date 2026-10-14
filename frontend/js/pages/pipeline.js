"use strict";

const API_BASE = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`).replace(/\/$/, "");

let stages = [
    { id: 1, name: "Tiềm năng", orderNo: 1, probability: 10 },
    { id: 2, name: "Giải pháp", orderNo: 2, probability: 40 },
    { id: 3, name: "Đàm phán", orderNo: 3, probability: 70 },
    { id: 4, name: "Thành công", orderNo: 4, probability: 100, isWon: true },
    { id: 5, name: "Thất bại", orderNo: 5, probability: 0, isLost: true }
];
let opportunities = [];
let customers = [];
let winReasons = [];
let lossReasons = [];
let competitors = [];

let draggedOppId = null;
let closingOppId = null;
let editingOppId = null;

const pipelineBoard = document.getElementById("pipelineBoard");
const opportunityDrawer = document.getElementById("opportunityDrawer");
const opportunityOverlay = document.getElementById("opportunityOverlay");
const opportunityForm = document.getElementById("opportunityForm");
const searchInput = document.getElementById("pipelineSearch");
const ownerFilter = document.getElementById("ownerFilter");

const dealModal = document.getElementById("closeDealModal");
const dealOverlay = document.getElementById("closeDealOverlay");

/* =========================================================
   API HELPER
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
        localStorage.removeItem("crm_ui_session");
        window.location.href = "login.html";
        throw new Error("Phiên đăng nhập đã hết hạn.");
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
   INITIALIZATION
========================================================= */
async function initPipeline() {
    try {
        await loadStages();
        await Promise.all([
            loadWinLossAndCompetitors(),
            loadCustomers(),
            loadOpportunities()
        ]);
        render();
    } catch (err) {
        console.error("Initialization error:", err);
    }
}

async function loadStages() {
    try {
        const data = await api("/api/pipeline-stages?pipelineId=1");
        if (Array.isArray(data) && data.length > 0) {
            stages = data.map(s => {
                const name = (s.name || "").trim();
                const isWon = !!s.isWon || name.toLowerCase().includes("thành công");
                const isLost = !!s.isLost || name.toLowerCase().includes("thất bại");
                return {
                    id: s.id,
                    name: name,
                    orderNo: s.orderNo || s.stageOrder || 0,
                    probability: s.probability !== undefined ? s.probability : (s.winProbability || 0),
                    isWon: isWon,
                    isLost: isLost
                };
            }).sort((a, b) => a.orderNo - b.orderNo);
        }
    } catch (err) {
        console.warn("Could not load stages from API, using default stages:", err);
    }

    if (!stages || stages.length === 0) {
        stages = [
            { id: 1, name: "Tiềm năng", orderNo: 1, probability: 10 },
            { id: 2, name: "Giải pháp", orderNo: 2, probability: 40 },
            { id: 3, name: "Đàm phán", orderNo: 3, probability: 70 },
            { id: 4, name: "Thành công", orderNo: 4, probability: 100, isWon: true },
            { id: 5, name: "Thất bại", orderNo: 5, probability: 0, isLost: true }
        ];
    }

    populateStageSelect();
}

async function loadWinLossAndCompetitors() {
    try {
        const result = await api("/api/win-loss-reasons");
        const list = Array.isArray(result)
            ? result
            : Array.isArray(result?.data)
                ? result.data
                : (result?.data?.reasons ?? []);

        winReasons = list.filter(r => {
            const t = (r.type || r.reasonType || "").toUpperCase();
            return (t === "WON" || t === "WIN") && r.active !== false;
        });
        lossReasons = list.filter(r => {
            const t = (r.type || r.reasonType || "").toUpperCase();
            return (t === "LOST" || t === "LOSS") && r.active !== false;
        });
    } catch (err) {
        console.warn("Could not load win/loss reasons:", err);
    }

    try {
        const result = await api("/api/competitors");
        const list = Array.isArray(result)
            ? result
            : Array.isArray(result?.data)
                ? result.data
                : (result?.data?.competitors ?? []);

        competitors = list.filter(c => c.active !== false);
    } catch (err) {
        console.warn("Could not load competitors:", err);
    }

    populateWinLossSelects();
}

async function loadCustomers() {
    try {
        const result = await api("/api/customers?page=1&size=100");

        const list = Array.isArray(result)
            ? result
            : Array.isArray(result?.items)
                ? result.items
                : [];

        customers = list.filter(c => c && c.isDeleted !== true && c.is_deleted !== true);
    } catch (err) {
        customers = [];
        console.warn("Could not load customers:", err);
    }

    populateCustomerSelect();
}

async function loadOpportunities() {
    try {
        const list = await api("/api/opportunities");
        if (Array.isArray(list)) {
            opportunities = list;
            // Sync with localStorage for offline / dashboard view
            try {
                localStorage.setItem("crm_ui_opportunities", JSON.stringify(opportunities.map(o => ({
                    id: o.id,
                    name: o.name,
                    customer: o.customerName || "",
                    value: o.amount || 0,
                    stage: String(o.stageId),
                    probability: o.probability || 0,
                    closeDate: o.expectedCloseDate || "",
                    owner: o.ownerName || "Tôi",
                    status: o.status
                }))));
            } catch (_) {}
        }
    } catch (err) {
        console.warn("Could not load opportunities from backend, checking cache:", err);
        try {
            const cached = JSON.parse(localStorage.getItem("crm_ui_opportunities"));
            if (Array.isArray(cached)) opportunities = cached;
        } catch (_) {}
    }

    render();
}

/* =========================================================
   POPULATE DROPDOWNS
========================================================= */
function populateStageSelect() {
    const stageSelect = document.getElementById("opportunityStage");
    if (!stageSelect) return;
    stageSelect.innerHTML = "";
    if (stages.length === 0) {
        const opt = document.createElement("option");
        opt.value = "";
        opt.disabled = true;
        opt.selected = true;
        opt.textContent = "-- Chưa có giai đoạn --";
        stageSelect.appendChild(opt);
        return;
    }
    stages.forEach((st, idx) => {
        const opt = document.createElement("option");
        opt.value = st.id;
        opt.textContent = `${st.name} (${st.probability}%)`;
        if (idx === 0) opt.selected = true;
        stageSelect.appendChild(opt);
    });
}

function populateCustomerSelect() {
    const custSelect = document.getElementById("opportunityCustomer");
    if (!custSelect) return;
    custSelect.innerHTML = '<option value="">-- Chọn khách hàng --</option>';
    customers.forEach(c => {
        const opt = document.createElement("option");
        opt.value = c.id;
        opt.textContent = `${c.name} (${c.code || c.phone || ""})`;
        custSelect.appendChild(opt);
    });
}

function populateWinLossSelects() {
    const winSelect = document.getElementById("winReason");
    if (winSelect) {
        const curVal = winSelect.value;
        winSelect.innerHTML = '<option value="">-- Chọn lý do Thắng --</option>';
        winReasons.forEach(r => {
            const opt = document.createElement("option");
            opt.value = r.id;
            opt.textContent = r.name || r.reasonText;
            winSelect.appendChild(opt);
        });
        const newOpt = document.createElement("option");
        newOpt.value = "__NEW__";
        newOpt.textContent = "+ Nhập lý do khác...";
        winSelect.appendChild(newOpt);
        if (curVal) winSelect.value = curVal;
    }

    const lossSelect = document.getElementById("lossReason");
    if (lossSelect) {
        const curVal = lossSelect.value;
        lossSelect.innerHTML = '<option value="">-- Chọn lý do Thua --</option>';
        lossReasons.forEach(r => {
            const opt = document.createElement("option");
            opt.value = r.id;
            opt.textContent = r.name || r.reasonText;
            lossSelect.appendChild(opt);
        });
        const newOpt = document.createElement("option");
        newOpt.value = "__NEW__";
        newOpt.textContent = "+ Nhập lý do khác...";
        lossSelect.appendChild(newOpt);
        if (curVal) lossSelect.value = curVal;
    }

    const compSelect = document.getElementById("lossCompetitor");
    if (compSelect) {
        const curVal = compSelect.value;
        compSelect.innerHTML = '<option value="">-- Chọn đối thủ thắng thầu --</option>';
        competitors.forEach(c => {
            const opt = document.createElement("option");
            opt.value = c.id;
            opt.textContent = c.name;
            compSelect.appendChild(opt);
        });
        const newOpt = document.createElement("option");
        newOpt.value = "__NEW__";
        newOpt.textContent = "+ Nhập đối thủ khác...";
        compSelect.appendChild(newOpt);
        if (curVal) compSelect.value = curVal;
    }
}

document.getElementById("winReason")?.addEventListener("change", event => {
    const customGroup = document.getElementById("customWinReasonGroup");
    if (customGroup) {
        const isCustom = event.target.value === "__NEW__";
        customGroup.style.display = isCustom ? "block" : "none";
        if (isCustom) document.getElementById("customWinReason")?.focus();
    }
});

document.getElementById("lossReason")?.addEventListener("change", event => {
    const customGroup = document.getElementById("customLossReasonGroup");
    if (customGroup) {
        const isCustom = event.target.value === "__NEW__";
        customGroup.style.display = isCustom ? "block" : "none";
        if (isCustom) document.getElementById("customLossReason")?.focus();
    }
});

document.getElementById("lossCompetitor")?.addEventListener("change", event => {
    const customGroup = document.getElementById("customLossCompetitorGroup");
    if (customGroup) {
        const isCustom = event.target.value === "__NEW__";
        customGroup.style.display = isCustom ? "block" : "none";
        if (isCustom) document.getElementById("customLossCompetitor")?.focus();
    }
});

/* =========================================================
   EVENT LISTENERS
========================================================= */
document.getElementById("addOpportunityButton")?.addEventListener("click", () => {
    openOpportunityDrawer();
});

document.getElementById("closeOpportunityDrawer")?.addEventListener("click", closeOpportunityDrawer);
document.getElementById("cancelOpportunity")?.addEventListener("click", closeOpportunityDrawer);
opportunityOverlay?.addEventListener("click", closeOpportunityDrawer);

document.getElementById("opportunityStage")?.addEventListener("change", event => {
    const stageId = Number(event.target.value);
    const selected = stages.find(s => s.id === stageId);
    if (selected) {
        document.getElementById("opportunityProbability").value = selected.probability;
    }
});

searchInput?.addEventListener("input", render);
ownerFilter?.addEventListener("change", render);

document.getElementById("resetPipelineFilter")?.addEventListener("click", () => {
    if (searchInput) searchInput.value = "";
    if (ownerFilter) ownerFilter.value = "";
    render();
});

/* Drawer form submit */
opportunityForm?.addEventListener("submit", async event => {
    event.preventDefault();

    const name = fieldValue("opportunityName");
    const nameError = document.getElementById("opportunityNameError");
    if (!name) {
        if (nameError) nameError.textContent = "Vui lòng nhập tên cơ hội.";
        return;
    }
    if (nameError) nameError.textContent = "";

    const customerIdVal = document.getElementById("opportunityCustomer")?.value;
    const custError = document.getElementById("opportunityCustomerError");
    if (!customerIdVal) {
        if (custError) custError.textContent = "Vui lòng chọn khách hàng.";
        return;
    }
    if (custError) custError.textContent = "";

    const customerId = Number(customerIdVal);
    const stageId = Number(document.getElementById("opportunityStage").value) || stages[0]?.id;
    const probability = clamp(Number(document.getElementById("opportunityProbability").value) || 0, 0, 100);
    const amount = Math.max(0, Number(document.getElementById("opportunityValue").value) || 0);
    const expectedCloseDate = fieldValue("opportunityCloseDate") || null;

    const payload = {
        name,
        customerId,
        stageId,
        probability,
        amount,
        expectedCloseDate
    };

    try {
        if (editingOppId) {
            await api(`/api/opportunities/${editingOppId}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
        } else {
            await api("/api/opportunities", {
                method: "POST",
                body: JSON.stringify(payload)
            });
        }

        closeOpportunityDrawer();
        await loadOpportunities();
    } catch (err) {
        alert("Lỗi khi lưu cơ hội: " + err.message);
    }
});

/* =========================================================
   WIN / LOSS MODAL HANDLERS
========================================================= */
document.querySelectorAll(".deal-mode").forEach(button => {
    button.addEventListener("click", () => {
        setDealMode(button.dataset.dealMode);
    });
});

document.querySelectorAll("[data-close-deal]").forEach(button => {
    button.addEventListener("click", closeDeal);
});

document.getElementById("closeDealX")?.addEventListener("click", closeDeal);
dealOverlay?.addEventListener("click", closeDeal);

document.getElementById("wonForm")?.addEventListener("submit", async event => {
    event.preventDefault();

    const value = Number(document.getElementById("wonValue").value);
    const date = document.getElementById("wonDate").value;
    let winReasonIdVal = document.getElementById("winReason")?.value;
    const customWin = document.getElementById("customWinReason")?.value?.trim();
    const error = document.getElementById("wonError");

    if (!value || value <= 0 || !date) {
        if (error) error.textContent = "Vui lòng nhập giá trị chốt và ngày ký.";
        return;
    }

    if (winReasonIdVal === "__NEW__" || (!winReasonIdVal && customWin)) {
        if (!customWin) {
            if (error) error.textContent = "Vui lòng nhập lý do Thắng mới.";
            return;
        }
        try {
            const created = await api("/api/win-loss-reasons", {
                method: "POST",
                body: JSON.stringify({ type: "WON", name: customWin })
            });
            const reasonObj = created?.data?.reason || created?.data || created;
            winReasonIdVal = reasonObj.id;
            await loadWinLossAndCompetitors();
        } catch (err) {
            if (error) error.textContent = "Lỗi lưu lý do thắng: " + err.message;
            return;
        }
    } else if (!winReasonIdVal) {
        if (error) error.textContent = "Vui lòng chọn hoặc nhập Lý do Thắng.";
        return;
    }
    if (error) error.textContent = "";

    try {
        await api(`/api/opportunities/${closingOppId}/close`, {
            method: "POST",
            body: JSON.stringify({
                status: "WON",
                winReasonId: Number(winReasonIdVal),
                amount: value
            })
        });

        closeDeal();
        await loadOpportunities();
    } catch (err) {
        if (error) error.textContent = err.message;
        else alert("Lỗi đóng cơ hội Thắng: " + err.message);
    }
});

document.getElementById("lostForm")?.addEventListener("submit", async event => {
    event.preventDefault();

    let reasonIdVal = document.getElementById("lossReason")?.value;
    const customLoss = document.getElementById("customLossReason")?.value?.trim();
    let compIdVal = document.getElementById("lossCompetitor")?.value;
    const customComp = document.getElementById("customLossCompetitor")?.value?.trim();
    const lostNotes = document.getElementById("lostNotes")?.value?.trim() || "";
    const error = document.getElementById("lostError");

    if (reasonIdVal === "__NEW__" || (!reasonIdVal && customLoss)) {
        if (!customLoss) {
            if (error) error.textContent = "Vui lòng nhập lý do Thua mới.";
            return;
        }
        try {
            const created = await api("/api/win-loss-reasons", {
                method: "POST",
                body: JSON.stringify({ type: "LOST", name: customLoss })
            });
            const reasonObj = created?.data?.reason || created?.data || created;
            reasonIdVal = reasonObj.id;
            await loadWinLossAndCompetitors();
        } catch (err) {
            if (error) error.textContent = "Lỗi lưu lý do thua: " + err.message;
            return;
        }
    } else if (!reasonIdVal) {
        if (error) error.textContent = "Vui lòng chọn hoặc nhập lý do Thua.";
        return;
    }

    if (compIdVal === "__NEW__" || (!compIdVal && customComp)) {
        if (!customComp) {
            if (error) error.textContent = "Vui lòng nhập đối thủ cạnh tranh mới.";
            return;
        }
        try {
            const created = await api("/api/competitors", {
                method: "POST",
                body: JSON.stringify({ name: customComp, note: "" })
            });
            const compObj = created?.data?.competitor || created?.data || created;
            compIdVal = compObj.id;
            await loadWinLossAndCompetitors();
        } catch (err) {
            if (error) error.textContent = "Lỗi lưu đối thủ cạnh tranh: " + err.message;
            return;
        }
    } else if (!compIdVal) {
        if (error) error.textContent = "Vui lòng chọn hoặc nhập đối thủ cạnh tranh.";
        return;
    }

    if (error) error.textContent = "";

    try {
        await api(`/api/opportunities/${closingOppId}/close`, {
            method: "POST",
            body: JSON.stringify({
                status: "LOST",
                lossReasonId: Number(reasonIdVal),
                competitorId: Number(compIdVal),
                lostReason: lostNotes
            })
        });

        closeDeal();
        await loadOpportunities();
    } catch (err) {
        if (error) error.textContent = err.message;
        else alert("Lỗi đóng cơ hội Thua: " + err.message);
    }
});

/* =========================================================
   CARD MENU AND ACTION DISPATCHER
========================================================= */
document.addEventListener("click", async event => {
    const menuButton = event.target.closest("[data-card-menu]");
    if (menuButton) {
        event.stopPropagation();
        const id = menuButton.dataset.cardMenu;
        document.querySelectorAll(".card-menu").forEach(menu => {
            if (menu.dataset.menu !== id) menu.classList.remove("open");
        });
        document.querySelector(`[data-menu="${id}"]`)?.classList.toggle("open");
        return;
    }

    const editBtn = event.target.closest("[data-edit-opportunity]");
    if (editBtn) {
        openOpportunityDrawer(Number(editBtn.dataset.editOpportunity));
        return;
    }

    const deleteBtn = event.target.closest("[data-delete-opportunity]");
    if (deleteBtn) {
        const id = Number(deleteBtn.dataset.deleteOpportunity);
        if (confirm("Bạn có chắc chắn muốn xóa cơ hội bán hàng này không?")) {
            try {
                await api(`/api/opportunities/${id}`, { method: "DELETE" });
                await loadOpportunities();
            } catch (err) {
                alert("Lỗi khi xóa cơ hội: " + err.message);
            }
        }
        return;
    }

    const nextBtn = event.target.closest("[data-next-stage]");
    if (nextBtn) {
        const id = Number(nextBtn.dataset.nextStage);
        await moveStageRelatively(id, 1);
        return;
    }

    const prevBtn = event.target.closest("[data-prev-stage]");
    if (prevBtn) {
        const id = Number(prevBtn.dataset.prevStage);
        await moveStageRelatively(id, -1);
        return;
    }

    const wonBtn = event.target.closest("[data-close-won]");
    if (wonBtn) {
        openCloseDeal(Number(wonBtn.dataset.closeWon), "won");
        return;
    }

    const lostBtn = event.target.closest("[data-close-lost]");
    if (lostBtn) {
        openCloseDeal(Number(lostBtn.dataset.closeLost), "lost");
        return;
    }

    if (!event.target.closest(".opportunity-card")) {
        document.querySelectorAll(".card-menu").forEach(menu => menu.classList.remove("open"));
    }
});

/* =========================================================
   STAGE ADVANCEMENT & TRANSITION
========================================================= */
async function moveStageRelatively(oppId, direction) {
    const opp = opportunities.find(o => String(o.id) === String(oppId));
    if (!opp) return;

    const currentStageIndex = stages.findIndex(s => String(s.id) === String(opp.stageId) || String(s.orderNo) === String(opp.stageId));
    if (currentStageIndex < 0) return;

    const nextStageIndex = currentStageIndex + direction;
    if (nextStageIndex < 0 || nextStageIndex >= stages.length) return;

    const targetStage = stages[nextStageIndex];
    await moveStageTo(opp.id, targetStage.id);
}

async function moveStageTo(oppId, targetStageId) {
    const opp = opportunities.find(o => String(o.id) === String(oppId));
    if (!opp) return;

    if (String(opp.stageId) === String(targetStageId)) return;

    const targetStage = stages.find(s => String(s.id) === String(targetStageId) || String(s.orderNo) === String(targetStageId));
    if (!targetStage) return;

    const nameLower = (targetStage.name || "").toLowerCase();
    const isWonStage = targetStage.isWon === true || nameLower.includes("thành công") || nameLower.includes("thắng") || nameLower.includes("won");
    const isLostStage = targetStage.isLost === true || nameLower.includes("thất bại") || nameLower.includes("thua") || nameLower.includes("lost");

    if (isWonStage) {
        openCloseDeal(opp.id, "won");
        return;
    }

    if (isLostStage) {
        openCloseDeal(opp.id, "lost");
        return;
    }

    // Optimistic UI update: instantly move the card to the target column
    const previousStageId = opp.stageId;
    const previousProbability = opp.probability;
    const previousStageName = opp.stageName;

    opp.stageId = targetStage.id;
    opp.probability = targetStage.probability;
    opp.stageName = targetStage.name;
    render();

    try {
        await api(`/api/opportunities/${oppId}/stage`, {
            method: "POST",
            body: JSON.stringify({ stageId: targetStage.id })
        });
        await loadOpportunities();
    } catch (err) {
        console.error("Lỗi chuyển giai đoạn:", err);
        // Rollback on error
        opp.stageId = previousStageId;
        opp.probability = previousProbability;
        opp.stageName = previousStageName;
        render();
        alert("Không thể chuyển giai đoạn: " + err.message);
    }
}

/* =========================================================
   RENDER BOARD & CARDS
========================================================= */
const STAGE_THEME_COLORS = ["#3b82f6", "#06b6d4", "#f59e0b", "#10b981", "#ef4444"];

function render() {
    if (!pipelineBoard) return;
    pipelineBoard.innerHTML = "";

    const query = (searchInput?.value || "").trim().toLowerCase();
    const owner = (ownerFilter?.value || "").trim();

    const filtered = opportunities.filter(opp => {
        const text = `${opp.name || ""} ${opp.customerName || ""}`.toLowerCase();
        const searchOk = !query || text.includes(query);
        const ownerOk = !owner || (opp.ownerName || "").includes(owner) || (owner === "Tôi");
        return searchOk && ownerOk;
    });

    stages.forEach((st, idx) => {
        const themeColor = STAGE_THEME_COLORS[idx % STAGE_THEME_COLORS.length];

        const col = document.createElement("article");
        col.className = "pipeline-column";
        col.dataset.stage = String(st.id);

        const stageOpps = filtered.filter(o => String(o.stageId) === String(st.id) || String(o.stageId) === String(st.orderNo));
        const stageTotal = stageOpps.reduce((sum, o) => sum + Number(o.amount || 0), 0);

        col.innerHTML = `
            <header class="column-head" style="border-top-color: ${themeColor}">
                <div>
                    <strong>${escapeHtml(st.name)}</strong>
                    <span class="column-count">${stageOpps.length} cơ hội</span>
                </div>
                <b class="column-total">${formatMoney(stageTotal)}</b>
            </header>
            <div class="column-body" data-drop-zone="${st.id}"></div>
        `;

        const body = col.querySelector(".column-body");
        stageOpps.forEach(opp => {
            body.appendChild(createCard(opp));
        });

        setupDropZone(col, st.id);
        pipelineBoard.appendChild(col);
    });
}

function createCard(record) {
    const card = document.createElement("article");
    card.className = "opportunity-card";
    card.setAttribute("draggable", "true");
    card.draggable = true;
    card.dataset.id = String(record.id);

    const currentStageIndex = stages.findIndex(s => String(s.id) === String(record.stageId) || String(s.orderNo) === String(record.stageId));
    const canPrev = currentStageIndex > 0;
    const canNext = currentStageIndex >= 0 && currentStageIndex < stages.length - 1;

    card.innerHTML = `
        <button class="card-menu-btn" type="button" data-card-menu="${record.id}"><i class="fa-solid fa-ellipsis-vertical"></i></button>

        <div class="card-menu" data-menu="${record.id}">
            <button type="button" data-edit-opportunity="${record.id}"><i class="fa-solid fa-pen-to-square"></i> Sửa</button>
            ${canPrev ? `<button type="button" data-prev-stage="${record.id}"><i class="fa-solid fa-arrow-left"></i> Giai đoạn trước</button>` : ""}
            ${canNext ? `<button type="button" data-next-stage="${record.id}">Giai đoạn tiếp <i class="fa-solid fa-arrow-right"></i></button>` : ""}
            <button type="button" class="won" data-close-won="${record.id}"><i class="fa-solid fa-circle-check"></i> Đóng Thắng</button>
            <button type="button" class="lost" data-close-lost="${record.id}"><i class="fa-solid fa-circle-xmark"></i> Đóng Thua</button>
            <button type="button" style="color:#ef4444;" data-delete-opportunity="${record.id}"><i class="fa-solid fa-trash-can"></i> Xóa</button>
        </div>

        <h3>${escapeHtml(record.name)}</h3>

        <div class="opportunity-customer">
            ${escapeHtml(record.customerName || "Chưa chọn khách hàng")}
        </div>

        <strong class="opportunity-value">
            ${formatMoney(record.amount || 0)}
        </strong>

        <div class="opportunity-meta">
            <span>${record.probability ?? 0}% xác suất</span>
            <span>${escapeHtml(record.expectedCloseDate || "—")}</span>
        </div>

        <div style="display:flex;align-items:center;justify-content:space-between;margin-top:10px;">
            <small style="color:#64748b;font-size:10px;">
                ${escapeHtml(record.stageName || "")}
            </small>
            <div class="card-avatar" title="${escapeHtml(record.ownerName || 'Chưa phân công')}">
                ${initials(record.ownerName || "U")}
            </div>
        </div>
    `;

    card.addEventListener("dragstart", event => {
        draggedOppId = String(record.id);
        event.dataTransfer.effectAllowed = "move";
        event.dataTransfer.setData("text/plain", String(record.id));
        requestAnimationFrame(() => card.classList.add("dragging"));
    });

    card.addEventListener("dragend", () => {
        draggedOppId = null;
        card.classList.remove("dragging");
        document.querySelectorAll(".column-body, .pipeline-column").forEach(zone => zone.classList.remove("drag-over"));
    });

    return card;
}

function setupDropZone(col, stageId) {
    col.addEventListener("dragover", event => {
        event.preventDefault();
        event.dataTransfer.dropEffect = "move";
        const body = col.querySelector(".column-body") || col;
        body.classList.add("drag-over");
    });
    col.addEventListener("dragenter", event => {
        event.preventDefault();
        const body = col.querySelector(".column-body") || col;
        body.classList.add("drag-over");
    });
    col.addEventListener("dragleave", event => {
        if (!col.contains(event.relatedTarget)) {
            const body = col.querySelector(".column-body") || col;
            body.classList.remove("drag-over");
        }
    });
    col.addEventListener("drop", async event => {
        event.preventDefault();
        event.stopPropagation();
        document.querySelectorAll(".column-body, .pipeline-column").forEach(z => z.classList.remove("drag-over"));
        const rawId = draggedOppId || event.dataTransfer.getData("text/plain");
        if (!rawId) return;
        draggedOppId = null;
        await moveStageTo(rawId, stageId);
    });
}

/* =========================================================
   DRAWER OPEN / CLOSE
========================================================= */
function openOpportunityDrawer(id = null) {
    editingOppId = id;
    const titleEl = document.getElementById("opportunityDrawerTitle");
    const nameErr = document.getElementById("opportunityNameError");
    const custErr = document.getElementById("opportunityCustomerError");
    if (nameErr) nameErr.textContent = "";
    if (custErr) custErr.textContent = "";

    if (id !== null) {
        const opp = opportunities.find(o => o.id === id);
        if (opp) {
            if (titleEl) titleEl.textContent = "Sửa cơ hội";
            setField("opportunityName", opp.name);
            setField("opportunityCustomer", opp.customerId);
            setField("opportunityValue", opp.amount);
            setField("opportunityCloseDate", opp.expectedCloseDate);
            setField("opportunityStage", opp.stageId);
            setField("opportunityProbability", opp.probability);
            setField("opportunityOwner", opp.ownerName);
        }
    } else {
        if (titleEl) titleEl.textContent = "Thêm cơ hội";
        opportunityForm.reset();
        if (stages.length > 0) {
            setField("opportunityStage", stages[0].id);
            setField("opportunityProbability", stages[0].probability);
        } else {
            const firstOpt = document.getElementById("opportunityStage")?.options[0]?.value;
            if (firstOpt) setField("opportunityStage", firstOpt);
            setField("opportunityProbability", 10);
        }
    }

    opportunityDrawer?.classList.add("open");
    opportunityOverlay?.classList.add("open");
}

function closeOpportunityDrawer() {
    opportunityDrawer?.classList.remove("open");
    opportunityOverlay?.classList.remove("open");
    editingOppId = null;
}

/* =========================================================
   CLOSE DEAL MODAL
========================================================= */
function openCloseDeal(oppId, mode) {
    closingOppId = oppId;
    const opp = opportunities.find(o => String(o.id) === String(oppId));

    document.getElementById("wonForm")?.reset();
    document.getElementById("lostForm")?.reset();

    const customWinGroup = document.getElementById("customWinReasonGroup");
    if (customWinGroup) customWinGroup.style.display = "none";
    const customLossGroup = document.getElementById("customLossReasonGroup");
    if (customLossGroup) customLossGroup.style.display = "none";
    const customCompGroup = document.getElementById("customLossCompetitorGroup");
    if (customCompGroup) customCompGroup.style.display = "none";

    populateWinLossSelects();

    if (opp && mode === "won") {
        setField("wonValue", opp.amount || 0);
        setField("wonDate", todayString());
    }

    const wonErr = document.getElementById("wonError");
    const lostErr = document.getElementById("lostError");
    if (wonErr) wonErr.textContent = "";
    if (lostErr) lostErr.textContent = "";

    setDealMode(mode);
    dealModal?.classList.add("open");
    dealOverlay?.classList.add("open");
}

function closeDeal() {
    dealModal?.classList.remove("open");
    dealOverlay?.classList.remove("open");
    closingOppId = null;
}

function setDealMode(mode) {
    const won = mode === "won";
    document.querySelectorAll(".deal-mode").forEach(button => {
        button.classList.toggle("active", button.dataset.dealMode === mode);
    });

    const wonForm = document.getElementById("wonForm");
    const lostForm = document.getElementById("lostForm");
    if (wonForm) wonForm.hidden = !won;
    if (lostForm) lostForm.hidden = won;

    const emojiEl = document.getElementById("closeDealEmoji");
    const titleEl = document.getElementById("closeDealTitle");
    if (emojiEl) emojiEl.textContent = won ? "🎉" : "☹️";
    if (titleEl) titleEl.textContent = won ? "Đóng Thắng Cơ Hội" : "Đóng Thua Cơ Hội";
}

/* =========================================================
   UTILITIES
========================================================= */
function fieldValue(id) {
    return (document.getElementById(id)?.value || "").trim();
}

function setField(id, val) {
    const el = document.getElementById(id);
    if (el) el.value = val ?? "";
}

function formatMoney(val) {
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
        maximumFractionDigits: 0
    }).format(Number(val) || 0);
}

function initials(name) {
    const parts = String(name || "").trim().split(/\s+/).filter(Boolean);
    if (!parts.length) return "U";
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function clamp(val, min, max) {
    if (!Number.isFinite(val)) return min;
    return Math.min(max, Math.max(min, val));
}

function escapeHtml(val) {
    return String(val ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function todayString() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

// Start
initPipeline();
