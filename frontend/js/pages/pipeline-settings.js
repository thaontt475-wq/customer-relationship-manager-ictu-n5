"use strict";

const API_BASE = (window.CRM_BACKEND_BASE || `${window.location.pathname.startsWith("/crm/") ? window.location.origin : window.location.protocol + "//" + (window.location.hostname || "localhost") + ":8080"}/crm`).replace(/\/$/, "");

const PIPELINE_ID = 1;

let stages = [];
let editingStageId = null;

const DEFAULT_B2B_STAGES = [
    { id: 1, name: "Khảo sát nhu cầu", orderNo: 1, probability: 10, exitCondition: "Thu thập đầy đủ thông tin doanh nghiệp & người đại diện", conditionRequired: false, active: true },
    { id: 2, name: "Đánh giá & Xác định nhu cầu", orderNo: 2, probability: 25, exitCondition: "Đã thực hiện cuộc gọi/gặp mặt làm rõ yêu cầu dự án", conditionRequired: false, active: true },
    { id: 3, name: "Gửi Đề xuất & Báo giá", orderNo: 3, probability: 50, exitCondition: "Báo giá chính thức đã được gửi cho khách hàng", conditionRequired: true, active: true },
    { id: 4, name: "Thương lượng & Đàm phán", orderNo: 4, probability: 75, exitCondition: "Đã thống nhất sơ bộ phạm vi công việc & biểu giá", conditionRequired: false, active: true },
    { id: 5, name: "Chốt thành công (Won)", orderNo: 5, probability: 100, exitCondition: "Hợp đồng đã được ký kết & bàn giao triển khai", conditionRequired: true, active: true },
    { id: 6, name: "Đóng Thua (Lost)", orderNo: 6, probability: 0, exitCondition: "Bắt buộc chọn lý do thất bại & đối thủ cạnh tranh", conditionRequired: true, active: true }
];

const STEP_GRADIENTS = [
    "linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)",
    "linear-gradient(135deg, #06b6d4 0%, #0e7490 100%)",
    "linear-gradient(135deg, #6366f1 0%, #4338ca 100%)",
    "linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)",
    "linear-gradient(135deg, #10b981 0%, #047857 100%)",
    "linear-gradient(135deg, #f43f5e 0%, #be123c 100%)"
];

document.addEventListener("DOMContentLoaded", initPipelineSettings);

async function initPipelineSettings() {
    bindEvents();
    await loadStages();
}

/* =========================================================
   EVENTS & DELEGATION
========================================================= */

function bindEvents() {
    // Add Stage Button
    document.getElementById("addStageButton")?.addEventListener("click", () => openStageModal());

    // Modal Close
    document.getElementById("closeStageModal")?.addEventListener("click", closeStageModal);
    document.getElementById("cancelStage")?.addEventListener("click", closeStageModal);
    document.getElementById("stageOverlay")?.addEventListener("click", closeStageModal);

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeStageModal();
        }
    });

    // Form Submit
    document.getElementById("stageForm")?.addEventListener("submit", async event => {
        event.preventDefault();
        await saveStage();
    });

    // Probability input & slider synchronization
    const probInput = document.getElementById("stageProbability");
    const probSlider = document.getElementById("stageProbSlider");

    if (probInput && probSlider) {
        probInput.addEventListener("input", () => {
            probSlider.value = probInput.value || 0;
        });
        probSlider.addEventListener("input", () => {
            probInput.value = probSlider.value || 0;
        });
    }

    // Refresh Button
    document.getElementById("btnRefreshStages")?.addEventListener("click", async () => {
        const icon = document.querySelector("#btnRefreshStages i");
        if (icon) icon.classList.add("fa-spin");
        try {
            await loadStages();
        } finally {
            if (icon) setTimeout(() => icon.classList.remove("fa-spin"), 600);
        }
    });

    // Reset Defaults Button
    document.getElementById("btnResetDefaultStages")?.addEventListener("click", () => {
        if (confirm("Bạn có muốn đặt lại 6 giai đoạn bán hàng chuẩn B2B cho Pipeline này không?")) {
            resetDefaultStages();
        }
    });

    // Stage List Actions Delegation
    document.addEventListener("click", async event => {
        // Edit Stage
        const editBtn = event.target.closest("[data-stage-edit]");
        if (editBtn) {
            openStageModal(Number(editBtn.dataset.stageEdit));
            return;
        }

        // Move Up
        const upBtn = event.target.closest("[data-stage-up]");
        if (upBtn) {
            await moveStage(Number(upBtn.dataset.stageUp), -1);
            return;
        }

        // Move Down
        const downBtn = event.target.closest("[data-stage-down]");
        if (downBtn) {
            await moveStage(Number(downBtn.dataset.stageDown), 1);
            return;
        }

        // Delete Stage
        const delBtn = event.target.closest("[data-stage-delete]");
        if (delBtn) {
            deleteStage(Number(delBtn.dataset.stageDelete));
            return;
        }
    });
}

/* =========================================================
   API HELPER
========================================================= */

async function api(path, options = {}) {
    const response = await fetch(API_BASE + path, {
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    const result = await response.json().catch(() => ({
        success: false,
        message: "Phản hồi API không hợp lệ"
    }));

    if (!response.ok) {
        throw new Error(result.message || "Có lỗi xảy ra");
    }

    return result;
}

/* =========================================================
   DATA LOADING & SYNC
========================================================= */

async function loadStages() {
    let loaded = [];

    try {
        const result = await api(`/api/pipeline-stages?pipelineId=${PIPELINE_ID}`);
        if (Array.isArray(result.data) && result.data.length > 0) {
            loaded = result.data.map(item => ({
                id: item.id,
                name: item.name || item.stageName || "Giai đoạn",
                orderNo: Number(item.orderNo || item.order_no || 1),
                probability: Number(item.probability !== undefined ? item.probability : 10),
                exitCondition: item.exitCondition || item.exit_condition || "",
                conditionRequired: Boolean(item.conditionRequired || item.condition_required),
                active: item.active !== false && item.is_active !== 0
            }));
        }
    } catch (error) {
        console.warn("Load pipeline stages notice:", error);
    }

    // Local storage fallback
    if (!loaded.length) {
        const saved = localStorage.getItem("crm_pipeline_stages");
        if (saved) {
            try { loaded = JSON.parse(saved); } catch (_) {}
        }
    }

    if (!loaded.length) {
        loaded = JSON.parse(JSON.stringify(DEFAULT_B2B_STAGES));
    }

    loaded.sort((a, b) => Number(a.orderNo) - Number(b.orderNo));
    stages = loaded;

    updateKpis();
    renderFunnelStrip();
    render();
}

function resetDefaultStages() {
    stages = JSON.parse(JSON.stringify(DEFAULT_B2B_STAGES));
    saveToLocalStorage();
    updateKpis();
    renderFunnelStrip();
    render();
}

function saveToLocalStorage() {
    localStorage.setItem("crm_pipeline_stages", JSON.stringify(stages));
}

/* =========================================================
   KPI METRICS
========================================================= */

function updateKpis() {
    const kpiCount = document.getElementById("kpiStageCount");
    const kpiWon = document.getElementById("kpiWonProb");
    const kpiLost = document.getElementById("kpiLostProb");
    const kpiFlow = document.getElementById("kpiFunnelFlow");

    if (kpiCount) kpiCount.textContent = `${stages.length} Giai đoạn`;

    const wonStage = stages.find(s => (s.name || "").toLowerCase().includes("won") || s.probability === 100);
    if (kpiWon) kpiWon.textContent = wonStage ? `${wonStage.probability}%` : "100%";

    const lostStage = stages.find(s => (s.name || "").toLowerCase().includes("lost") || s.probability === 0);
    if (kpiLost) kpiLost.textContent = lostStage ? `${lostStage.probability}%` : "0%";

    const probs = stages.map(s => s.probability).filter(p => p > 0);
    const minProb = probs.length ? Math.min(...probs) : 10;
    const maxProb = probs.length ? Math.max(...probs) : 100;
    if (kpiFlow) kpiFlow.textContent = `${minProb}% ➔ ${maxProb}%`;
}

/* =========================================================
   VISUAL FUNNEL FLOW STRIP
========================================================= */

function renderFunnelStrip() {
    const strip = document.getElementById("funnelStepsStrip");
    if (!strip) return;

    strip.innerHTML = "";

    stages.forEach((stage, idx) => {
        const step = document.createElement("div");
        const prob = Number(stage.probability);
        const isWon = (stage.name || "").toLowerCase().includes("won") || prob === 100;
        const isLost = (stage.name || "").toLowerCase().includes("lost") || prob === 0;

        let statusClass = "";
        if (isWon) statusClass = "won";
        else if (isLost) statusClass = "lost";

        step.className = `funnel-step-item ${statusClass}`;
        step.innerHTML = `
            <div class="funnel-step-top">
                <span class="funnel-step-num">BƯỚC 0${idx + 1}</span>
                <span class="funnel-step-pct">${prob}%</span>
            </div>
            <div class="funnel-step-name" title="${escapeHtml(stage.name)}">
                ${escapeHtml(stage.name)}
            </div>
        `;

        strip.appendChild(step);
    });
}

/* =========================================================
   RENDERING STAGES
========================================================= */

function render() {
    const stageList = document.getElementById("stageList");
    const stageEmpty = document.getElementById("stageEmpty");

    if (!stageList) return;

    stageList.innerHTML = "";

    const visible = stages.filter(item => item.active !== false);

    if (stageEmpty) {
        stageEmpty.style.display = visible.length ? "none" : "flex";
    }

    if (!visible.length) return;

    visible.forEach((stage, index) => {
        const item = document.createElement("article");
        item.className = "stage-item";

        const prob = Number(stage.probability);
        const isWon = (stage.name || "").toLowerCase().includes("won") || prob === 100;
        const isLost = (stage.name || "").toLowerCase().includes("lost") || prob === 0;

        let badgeType = "normal";
        let fillGradient = "linear-gradient(90deg, #38bdf8 0%, #3b82f6 100%)";

        if (isWon) {
            badgeType = "won";
            fillGradient = "linear-gradient(90deg, #34d399 0%, #10b981 100%)";
        } else if (isLost) {
            badgeType = "lost";
            fillGradient = "linear-gradient(90deg, #fb7185 0%, #f43f5e 100%)";
        } else if (prob >= 70) {
            fillGradient = "linear-gradient(90deg, #a78bfa 0%, #8b5cf6 100%)";
        }

        const grad = STEP_GRADIENTS[index % STEP_GRADIENTS.length];
        const stepNum = String(index + 1).padStart(2, "0");

        const conditionText = stage.exitCondition 
            ? escapeHtml(stage.exitCondition) 
            : "Không có điều kiện rời giai đoạn";
        const conditionClass = stage.conditionRequired ? "required" : "";
        const conditionIcon = stage.conditionRequired ? '<i class="fa-solid fa-lock"></i>' : '<i class="fa-solid fa-circle-check"></i>';

        const canDelete = !isWon && !isLost && visible.length > 2;

        item.innerHTML = `
            <div class="stage-order-buttons">
                <button type="button" data-stage-up="${stage.id}" ${index === 0 ? "disabled" : ""} title="Di chuyển lên">
                    <i class="fa-solid fa-chevron-up"></i>
                </button>
                <button type="button" data-stage-down="${stage.id}" ${index === visible.length - 1 ? "disabled" : ""} title="Di chuyển xuống">
                    <i class="fa-solid fa-chevron-down"></i>
                </button>
            </div>

            <div class="stage-step-badge" style="background:${grad};">
                ${stepNum}
            </div>

            <div class="stage-name-info">
                <strong>${escapeHtml(stage.name)}</strong>
                <span>Giai đoạn ${index + 1} của chuỗi bán hàng</span>
            </div>

            <div class="probability-wrap">
                <span class="probability-badge ${badgeType}">
                    <i class="fa-solid fa-chart-line"></i> ${prob}% thắng
                </span>
                <div class="probability-track">
                    <div class="probability-fill" style="width: ${prob}%; background: ${fillGradient};"></div>
                </div>
            </div>

            <div class="stage-condition-badge ${conditionClass}" title="${conditionText}">
                ${conditionIcon}
                <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                    ${conditionText}
                </span>
            </div>

            <div class="stage-action-wrap">
                <button type="button" class="stage-btn-action" data-stage-edit="${stage.id}" title="Chỉnh sửa giai đoạn">
                    <i class="fa-solid fa-pen"></i> Sửa
                </button>
                ${canDelete ? `
                    <button type="button" class="stage-btn-action btn-del" data-stage-delete="${stage.id}" title="Xóa giai đoạn">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                ` : ''}
            </div>
        `;

        stageList.appendChild(item);
    });
}

/* =========================================================
   MOVE STAGE ORDER
========================================================= */

async function moveStage(id, delta) {
    const index = stages.findIndex(item => Number(item.id) === Number(id));
    const targetIndex = index + delta;

    if (index < 0 || targetIndex < 0 || targetIndex >= stages.length) return;

    // Swap items in memory
    const temp = stages[index];
    stages[index] = stages[targetIndex];
    stages[targetIndex] = temp;

    // Reassign orderNo
    stages.forEach((s, idx) => {
        s.orderNo = idx + 1;
    });

    saveToLocalStorage();
    renderFunnelStrip();
    render();

    // Sync order to backend
    try {
        await api(`/api/pipeline-stages/${id}`, {
            method: "PUT",
            body: JSON.stringify(stages[targetIndex])
        });
    } catch (_) {}
}

/* =========================================================
   FLOATING CENTERED MODAL & CRUD OPERATIONS
========================================================= */

function openStageModal(id = null) {
    const modal = document.getElementById("stageModal");
    const overlay = document.getElementById("stageOverlay");
    const form = document.getElementById("stageForm");

    if (!modal || !overlay) return;

    if (form) form.reset();

    const titleEl = document.getElementById("stageModalTitle");
    const subtitleEl = document.getElementById("stageModalSubtitle");
    const badgeEl = document.getElementById("stageModalBadge");
    const errorEl = document.getElementById("stageFormError");
    const submitBtn = document.getElementById("submitStageBtn");

    if (errorEl) {
        errorEl.textContent = "";
        errorEl.classList.remove("show");
    }

    setValue("editingStageIndex", id !== null ? String(id) : "");

    if (id !== null) {
        const stage = stages.find(item => Number(item.id) === Number(id));
        if (stage) {
            editingStageId = stage.id;
            setValue("stageName", stage.name);
            setValue("stageProbability", stage.probability);
            const slider = document.getElementById("stageProbSlider");
            if (slider) slider.value = stage.probability;
            setValue("stageCondition", stage.exitCondition || "");

            const requiredBox = document.getElementById("stageConditionRequired");
            if (requiredBox) requiredBox.checked = Boolean(stage.conditionRequired);

            if (titleEl) titleEl.textContent = "Chỉnh sửa giai đoạn Pipeline";
            if (subtitleEl) subtitleEl.textContent = `Cấu hình thông số và điều kiện cho [${stage.name}]`;
            if (badgeEl) badgeEl.innerHTML = '<i class="fa-solid fa-pen-to-square" style="color:#38bdf8;"></i>';
            if (submitBtn) submitBtn.innerHTML = '<i class="fa-solid fa-check"></i> Cập nhật giai đoạn';
        }
    } else {
        editingStageId = null;
        setValue("stageProbability", "10");
        const slider = document.getElementById("stageProbSlider");
        if (slider) slider.value = 10;

        if (titleEl) titleEl.textContent = "Thêm giai đoạn mới";
        if (subtitleEl) subtitleEl.textContent = "Cấu hình tên, xác suất thắng và điều kiện chuyển giai đoạn";
        if (badgeEl) badgeEl.innerHTML = '<i class="fa-solid fa-layer-group" style="color:#38bdf8;"></i>';
        if (submitBtn) submitBtn.innerHTML = '<i class="fa-solid fa-plus"></i> Lưu giai đoạn';
    }

    modal.classList.add("open");
    overlay.classList.add("open");
    document.body.classList.add("modal-open");

    setTimeout(() => {
        document.getElementById("stageName")?.focus();
    }, 100);
}

function closeStageModal() {
    editingStageId = null;
    const modal = document.getElementById("stageModal");
    const overlay = document.getElementById("stageOverlay");

    if (modal) modal.classList.remove("open");
    if (overlay) overlay.classList.remove("open");
    document.body.classList.remove("modal-open");
}

async function saveStage() {
    const name = value("stageName");
    const probability = Number(document.getElementById("stageProbability")?.value);
    const exitCondition = value("stageCondition");
    const conditionRequired = Boolean(document.getElementById("stageConditionRequired")?.checked);
    const editingId = value("editingStageIndex");

    const errorEl = document.getElementById("stageFormError");
    if (errorEl) {
        errorEl.textContent = "";
        errorEl.classList.remove("show");
    }

    if (!name) {
        if (errorEl) {
            errorEl.textContent = "Vui lòng nhập tên giai đoạn.";
            errorEl.classList.add("show");
        }
        return;
    }

    if (!Number.isFinite(probability) || probability < 0 || probability > 100) {
        if (errorEl) {
            errorEl.textContent = "Xác suất phải nằm trong khoảng 0 - 100%.";
            errorEl.classList.add("show");
        }
        return;
    }

    try {
        if (editingId) {
            const target = stages.find(s => Number(s.id) === Number(editingId));
            if (target) {
                target.name = name;
                target.probability = probability;
                target.exitCondition = exitCondition;
                target.conditionRequired = conditionRequired;
            }

            try {
                await api(`/api/pipeline-stages/${editingId}`, {
                    method: "PUT",
                    body: JSON.stringify({
                        pipelineId: PIPELINE_ID,
                        name,
                        orderNo: target ? target.orderNo : 1,
                        probability,
                        exitCondition,
                        conditionRequired,
                        active: true
                    })
                });
            } catch (_) {}
        } else {
            const newId = Date.now();
            const newStage = {
                id: newId,
                name,
                orderNo: stages.length + 1,
                probability,
                exitCondition,
                conditionRequired,
                active: true
            };

            stages.push(newStage);

            try {
                await api("/api/pipeline-stages", {
                    method: "POST",
                    body: JSON.stringify({
                        pipelineId: PIPELINE_ID,
                        name,
                        orderNo: newStage.orderNo,
                        probability,
                        exitCondition,
                        conditionRequired,
                        active: true
                    })
                });
            } catch (_) {}
        }

        saveToLocalStorage();
        closeStageModal();
        updateKpis();
        renderFunnelStrip();
        render();

    } catch (err) {
        console.error("Save stage error:", err);
        if (errorEl) {
            errorEl.textContent = err.message || "Không thể lưu giai đoạn.";
            errorEl.classList.add("show");
        }
    }
}

function deleteStage(id) {
    const stage = stages.find(s => Number(s.id) === Number(id));
    if (!stage) return;

    if (!confirm(`Bạn có chắc chắn muốn xóa giai đoạn [${stage.name}] không?`)) return;

    stages = stages.filter(s => Number(s.id) !== Number(id));
    stages.forEach((s, idx) => { s.orderNo = idx + 1; });

    saveToLocalStorage();
    updateKpis();
    renderFunnelStrip();
    render();

    try {
        api(`/api/pipeline-stages/${id}`, { method: "DELETE" });
    } catch (_) {}
}

/* =========================================================
   UTILITIES
========================================================= */

function value(id) {
    return document.getElementById(id)?.value?.trim() ?? "";
}

function setValue(id, newValue) {
    const el = document.getElementById(id);
    if (el) el.value = newValue ?? "";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}