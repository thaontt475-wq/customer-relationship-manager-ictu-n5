"use strict";

const API_BASE = "http://localhost:8080/crm";

const timeline = [];
let currentCustomer = null;

const timelineList = document.getElementById("timelineList");
const timelineEmpty = document.getElementById("timelineEmpty");
const composer = document.getElementById("activityComposer");
const noteInput = document.getElementById("activityNote");
const typeInput = document.getElementById("activityType");
const modal = document.getElementById("companyModal");
const modalOverlay = document.getElementById("companyModalOverlay");

const params = new URLSearchParams(window.location.search);
const customerId = params.get("id");

/* =========================================================
   API CLIENT
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
async function initCustomer360() {
    if (!customerId) return;

    try {
        const data = await api(`/api/customers/${customerId}`);
        if (data) {
            currentCustomer = data;
        }
    } catch (err) {
        console.warn("Could not load customer info from API, checking local storage:", err);
    }

    if (!currentCustomer) {
        try {
            const raw = localStorage.getItem("CRM_CUSTOMERS_DATA");
            if (raw) {
                const list = JSON.parse(raw);
                currentCustomer = list.find(c => Number(c.id) === Number(customerId)) || null;
            }
        } catch (_) {}
    }

    if (currentCustomer) {
        writeDisplay("company360Name", currentCustomer.companyName || currentCustomer.name);
        writeDisplay("company360Tax", currentCustomer.taxCode);
        writeDisplay("company360Industry", currentCustomer.industry);
        writeDisplay("company360Phone", currentCustomer.phone);
        writeDisplay("company360Website", currentCustomer.website);
    }

    // Load activities for timeline
    try {
        const acts = await api(`/api/activities?customerId=${customerId}`);
        if (Array.isArray(acts)) {
            acts.forEach(a => {
                timeline.push({
                    type: (a.type || "note").toLowerCase(),
                    text: a.description || a.subject || "",
                    time: a.createdAt ? new Date(a.createdAt) : new Date()
                });
            });
            renderTimeline();
        }
    } catch (err) {
        console.warn("Could not load activities:", err);
    }

    // Load opportunities
    try {
        const opps = await api(`/api/opportunities?customerId=${customerId}`);
        const panel = document.getElementById("opportunitiesPanel");
        if (panel && Array.isArray(opps)) {
            panel.innerHTML = opps.length ? "" : '<p style="color:#64748b;padding:12px;">Chưa có cơ hội bán hàng nào.</p>';
            opps.forEach(o => {
                const item = document.createElement("div");
                item.style.padding = "10px";
                item.style.borderBottom = "1px solid var(--crm-border)";
                item.innerHTML = `
                    <strong>${escapeHtml(o.name)}</strong>
                    <div style="font-size:12px;color:#64748b;margin-top:4px;">
                        ${formatMoney(o.amount || 0)} · ${escapeHtml(o.stageName || "Giai đoạn")} · ${o.probability || 0}%
                    </div>
                `;
                panel.appendChild(item);
            });
        }
    } catch (err) {
        console.warn("Could not load opportunities:", err);
    }

    // CRM-68: Check & Render Churn Risk Banner with Sales Alert Box
    renderCustomerChurnRisk360();

    // CRM-68: Load and Render Customer Support Tickets
    renderCustomerTickets360();

    // CRM-62: Load and Render Customer Contacts & Decision Roles
    if (window.ContactsManager && customerId) {
        window.ContactsManager.renderCustomer360Contacts(document.getElementById("contactsPanel"), customerId);
    }
}

// CRM-62: Re-render contacts on update
document.addEventListener("crm:contacts-updated", () => {
    if (window.ContactsManager && customerId) {
        window.ContactsManager.renderCustomer360Contacts(document.getElementById("contactsPanel"), customerId);
    }
});


/* =========================================================
   CRM-68: CHURN RISK & SALES ALERT IN CUSTOMER 360
========================================================= */
function getCustomerRecord(cId) {
    if (currentCustomer && Number(currentCustomer.id) === Number(cId)) return currentCustomer;
    try {
        const raw = localStorage.getItem("CRM_CUSTOMERS_DATA");
        if (raw) {
            const list = JSON.parse(raw);
            return list.find(c => Number(c.id) === Number(cId)) || null;
        }
    } catch (_) {}
    return null;
}

function renderCustomerChurnRisk360() {
    const bannerContainer = document.getElementById("customer360ChurnBanner");
    if (!bannerContainer || !customerId) return;

    // Get churn risk status from SupportTicketsManager if available, or compute from localStorage
    let churnInfo = null;
    if (window.SupportTicketsManager && typeof window.SupportTicketsManager.getChurnRiskStatus === "function") {
        churnInfo = window.SupportTicketsManager.getChurnRiskStatus(Number(customerId));
    } else {
        try {
            const rawChurn = localStorage.getItem("CRM_CHURN_RISK_DATA");
            const churnFlags = rawChurn ? JSON.parse(rawChurn) : {};
            const manualFlag = churnFlags[Number(customerId)];

            const rawTickets = localStorage.getItem("CRM_SUPPORT_TICKETS_DATA");
            const allTickets = rawTickets ? JSON.parse(rawTickets) : [];
            const custTickets = allTickets.filter(t => Number(t.customerId) === Number(customerId) && t.status !== "CLOSED" && t.status !== "RESOLVED");

            const isManual = !!manualFlag;
            const hasOverdueUrgent = custTickets.some(t => t.priority === "URGENT" || t.priority === "HIGH");
            const hasMultipleOpen = custTickets.length >= 2;
            const isRisk = isManual || hasOverdueUrgent || hasMultipleOpen;

            const reasons = [];
            if (isManual) reasons.push(manualFlag.manualReason || "CSKH gắn cờ thủ công rủi ro rời bỏ.");
            if (hasOverdueUrgent) reasons.push("Có yêu cầu mức độ Khẩn cấp/Cao chưa được giải quyết dứt điểm.");
            if (hasMultipleOpen) reasons.push(`Có ${custTickets.length} phiếu khiếu nại/hỗ trợ đang tồn đọng mở.`);

            churnInfo = { isRisk, reasons, riskLevel: (isManual || hasOverdueUrgent) ? "HIGH" : (hasMultipleOpen ? "MEDIUM" : "LOW") };
        } catch (_) {
            churnInfo = { isRisk: false, reasons: [], riskLevel: "LOW" };
        }
    }

    if (!churnInfo || !churnInfo.isRisk) {
        bannerContainer.innerHTML = "";
        bannerContainer.style.display = "none";
        return;
    }

    bannerContainer.style.display = "block";
    const customer = getCustomerRecord(customerId);
    const salesRepName = customer?.ownerName || customer?.owner || "Nông Quang Tiệp (Trưởng phòng KD)";
    const salesRepEmail = customer?.ownerEmail || "tiep.nq@crm.vn";
    const salesRepPhone = customer?.ownerPhone || "0912.888.999";

    bannerContainer.innerHTML = `
        <div class="churn-banner-container" style="margin-bottom:20px; animation:fadeIn 0.3s ease;">
            <div class="churn-alert-banner" style="display:flex; flex-wrap:wrap; align-items:flex-start; justify-content:space-between; gap:16px; padding:18px 22px; border-radius:12px; background:linear-gradient(135deg, rgba(239,68,68,0.1), rgba(245,158,11,0.08)); border:1.5px solid var(--crm-danger); box-shadow:0 4px 16px rgba(239,68,68,0.12);">
                <div style="display:flex; gap:14px; align-items:flex-start; flex:1; min-width:300px;">
                    <div style="font-size:28px; line-height:1; flex-shrink:0;">⚠️</div>
                    <div>
                        <div style="display:flex; align-items:center; gap:10px; margin-bottom:6px; flex-wrap:wrap;">
                            <h3 style="margin:0; font-size:16px; font-weight:700; color:var(--crm-danger);">
                                CẢNH BÁO RỦI RO RỜI BỎ (CHURN RISK DETECTED)
                            </h3>
                            <span class="churn-risk-badge" style="background:#fee2e2; color:#b91c1c; border:1px solid #fca5a5; font-size:11.5px; padding:2px 8px; border-radius:4px; font-weight:700;">
                                ${churnInfo.riskLevel === 'CRITICAL' ? 'RẤT NGUY CẤP' : 'RỦI RO CAO'}
                            </span>
                        </div>
                        <p style="margin:0 0 8px; font-size:13.5px; color:var(--crm-text);">
                            Khách hàng có nhiều phản ánh tồn đọng hoặc khiếu nại kỹ thuật mức độ cao. Cần Sales & CSKH phối hợp can thiệp gấp!
                        </p>
                        <div style="font-size:12.5px; color:var(--crm-danger); font-weight:500;">
                            <strong>Yếu tố rủi ro:</strong> ${escapeHtml(churnInfo.reasons.join(" • ") || "Nhiều phản ánh tồn đọng")}
                        </div>
                    </div>
                </div>

                <!-- SALES REP ALERT NOTIFICATION BOX (CRM-68) -->
                <div class="sales-alert-box" style="background:var(--crm-surface); border:1.5px solid #fecaca; border-radius:10px; padding:14px 18px; min-width:260px; box-shadow:0 2px 8px rgba(0,0,0,0.06); flex-shrink:0;">
                    <div style="font-size:11px; text-transform:uppercase; letter-spacing:0.5px; font-weight:700; color:var(--crm-danger); margin-bottom:6px; display:flex; align-items:center; gap:6px;">
                        <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" fill="none" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
                        Phụ trách kinh doanh (Sales Rep)
                    </div>
                    <div style="font-weight:700; font-size:14.5px; color:var(--crm-text); margin-bottom:4px;">
                        ${escapeHtml(salesRepName)}
                    </div>
                    <div style="font-size:12px; color:var(--crm-muted); display:flex; flex-direction:column; gap:2px;">
                        <span>📧 ${escapeHtml(salesRepEmail)}</span>
                        <span>☎ ${escapeHtml(salesRepPhone)}</span>
                    </div>
                    <div style="margin-top:10px; padding-top:8px; border-top:1px dashed var(--crm-border);">
                        <a href="support-tickets.html" class="crm-btn crm-btn-secondary" style="font-size:11.5px; padding:4px 10px; width:100%; text-align:center; display:block; text-decoration:none;">
                            Xem phiếu & Xử lý ngay →
                        </a>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function renderCustomerTickets360() {
    const listEl = document.getElementById("customerTicketsList");
    const countBadge = document.getElementById("ticketCountBadge");
    if (!listEl || !customerId) return;

    let tickets = [];
    if (window.SupportTicketsManager && typeof window.SupportTicketsManager.getTicketsByCustomer === "function") {
        tickets = window.SupportTicketsManager.getTicketsByCustomer(Number(customerId));
    } else {
        try {
            const raw = localStorage.getItem("CRM_SUPPORT_TICKETS_DATA");
            if (raw) {
                const parsed = JSON.parse(raw);
                tickets = parsed.filter(t => Number(t.customerId) === Number(customerId));
            }
        } catch (_) {}
    }

    if (countBadge) countBadge.textContent = String(tickets.length);

    if (tickets.length === 0) {
        listEl.innerHTML = '<div class="empty-small" style="padding:16px; text-align:center; color:var(--crm-muted);">Chưa có phiếu hỗ trợ nào cho khách hàng này.</div>';
        return;
    }

    listEl.innerHTML = tickets.map(t => {
        const priorityClass = t.priority === "URGENT" ? "priority-urgent" : (t.priority === "HIGH" ? "priority-high" : (t.priority === "MEDIUM" ? "priority-medium" : "priority-low"));
        const priorityLabel = t.priority === "URGENT" ? "Khẩn cấp" : (t.priority === "HIGH" ? "Cao" : (t.priority === "MEDIUM" ? "Trung bình" : "Thấp"));
        const statusLabel = t.status === "NEW" ? "Mới tiếp nhận" : (t.status === "PROCESSING" ? "Đang xử lý" : (t.status === "WAITING_CUSTOMER" ? "Chờ khách" : (t.status === "RESOLVED" ? "Đã giải quyết" : "Đóng")));
        const statusClass = t.status === "NEW" ? "status-new" : (t.status === "PROCESSING" ? "status-processing" : (t.status === "RESOLVED" ? "status-resolved" : "status-waiting"));

        return `
            <div style="padding:12px; border-bottom:1px solid var(--crm-border); display:flex; flex-direction:column; gap:6px;">
                <div style="display:flex; justify-content:space-between; align-items:center; gap:8px;">
                    <a href="support-tickets.html" style="font-weight:700; font-size:12px; color:var(--crm-primary); text-decoration:none;">
                        ${escapeHtml(t.ticketCode)}
                    </a>
                    <span class="priority-badge ${priorityClass}" style="font-size:10.5px; padding:1px 6px;">
                        ${priorityLabel}
                    </span>
                </div>
                <div style="font-size:13px; font-weight:600; color:var(--crm-text); line-height:1.35;">
                    ${escapeHtml(t.title)}
                </div>
                <div style="display:flex; justify-content:space-between; align-items:center; font-size:11.5px; color:var(--crm-muted); margin-top:2px;">
                    <span class="status-pill ${statusClass}" style="font-size:11px; padding:1px 8px;">
                        <span class="status-dot"></span>
                        ${statusLabel}
                    </span>
                    <span>👤 ${escapeHtml(t.assigneeName || "Chưa phân công")}</span>
                </div>
            </div>
        `;
    }).join("");
}

document.querySelectorAll(".right-tab").forEach(tab => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".right-tab").forEach(item => item.classList.remove("active"));
        tab.classList.add("active");

        const target = tab.dataset.tab;
        const cPanel = document.getElementById("contactsPanel");
        const oPanel = document.getElementById("opportunitiesPanel");
        const tPanel = document.getElementById("ticketsPanel");
        if (cPanel) cPanel.hidden = target !== "contacts";
        if (oPanel) oPanel.hidden = target !== "opportunities";
        if (tPanel) tPanel.hidden = target !== "tickets";
    });
});

composer?.addEventListener("submit", async event => {
    event.preventDefault();
    const text = noteInput.value.trim();
    if (!text) {
        noteInput.focus();
        return;
    }

    const type = typeInput.value;
    try {
        if (customerId) {
            await api("/api/activities", {
                method: "POST",
                body: JSON.stringify({
                    subject: text.slice(0, 50),
                    type: type.toUpperCase(),
                    description: text,
                    customerId: Number(customerId),
                    status: "COMPLETED"
                })
            });
        }
    } catch (err) {
        console.warn("Could not save activity to backend:", err);
    }

    timeline.unshift({
        type: type,
        text,
        time: new Date()
    });

    noteInput.value = "";
    renderTimeline();
});

document.querySelectorAll("[data-quick]").forEach(button => {
    button.addEventListener("click", () => {
        const type = button.dataset.quick;
        typeInput.value = type === "call" ? "call" : type === "email" ? "email" : "note";
        noteInput.focus();
    });
});

document.getElementById("editCompanyButton")?.addEventListener("click", openCompanyModal);
document.getElementById("closeCompanyModal")?.addEventListener("click", closeCompanyModal);
document.getElementById("cancelCompanyEdit")?.addEventListener("click", closeCompanyModal);
modalOverlay?.addEventListener("click", closeCompanyModal);

document.getElementById("company360Form")?.addEventListener("submit", async event => {
    event.preventDefault();

    const name = value("edit360Name");
    const tax = value("edit360Tax");
    const ind = value("edit360Industry");
    const ph = value("edit360Phone");
    const web = value("edit360Website");

    if (customerId) {
        try {
            await api(`/api/customers/${customerId}`, {
                method: "PUT",
                body: JSON.stringify({
                    name,
                    taxCode: tax,
                    industry: ind,
                    phone: ph,
                    website: web,
                    email: currentCustomer?.email || "",
                    address: currentCustomer?.address || "",
                    type: currentCustomer?.type || "ENTERPRISE",
                    status: currentCustomer?.status || "ACTIVE"
                })
            });
        } catch (err) {
            alert("Lỗi khi cập nhật thông tin công ty: " + err.message);
            return;
        }
    }

    writeDisplay("company360Name", name);
    writeDisplay("company360Tax", tax);
    writeDisplay("company360Industry", ind);
    writeDisplay("company360Phone", ph);
    writeDisplay("company360Website", web);

    closeCompanyModal();
});

document.addEventListener("keydown", event => {
    if (event.key === "Escape") closeCompanyModal();
});

function renderTimeline() {
    timelineList.querySelectorAll(".timeline-item").forEach(item => item.remove());
    timelineEmpty.style.display = timeline.length ? "none" : "block";

    for (const item of timeline) {
        const article = document.createElement("article");
        article.className = "timeline-item";
        article.innerHTML = `
            <div class="timeline-item-head">
                <span class="timeline-type">${typeLabel(item.type)}</span>
                <time class="timeline-time">
                    ${item.time.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}
                </time>
            </div>
            <div class="timeline-text">${escapeHtml(item.text)}</div>
        `;
        timelineList.appendChild(article);
    }
}

function openCompanyModal() {
    setValue("edit360Name", displayValue("company360Name"));
    setValue("edit360Tax", displayValue("company360Tax"));
    setValue("edit360Industry", displayValue("company360Industry"));
    setValue("edit360Phone", displayValue("company360Phone"));
    setValue("edit360Website", displayValue("company360Website"));

    modal.classList.add("open");
    modalOverlay.classList.add("open");
}

function closeCompanyModal() {
    modal.classList.remove("open");
    modalOverlay.classList.remove("open");
}

function typeLabel(type) {
    switch (type) {
        case "call": return "☎ Cuộc gọi";
        case "email": return "✉ Email";
        case "meeting": return "▣ Cuộc gặp";
        default: return "✎ Ghi chú";
    }
}

function value(id) {
    return (document.getElementById(id)?.value || "").trim();
}

function displayValue(id) {
    const val = (document.getElementById(id)?.textContent || "").trim();
    return val === "—" ? "" : val;
}

function setValue(id, val) {
    const el = document.getElementById(id);
    if (el) el.value = val || "";
}

function writeDisplay(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val || "—";
}

function escapeHtml(val) {
    return String(val ?? "")
        .replace(/&/g,"&amp;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;")
        .replace(/"/g,"&quot;")
        .replace(/'/g,"&#039;");
}

function formatMoney(val) {
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
        maximumFractionDigits: 0
    }).format(Number(val) || 0);
}

// Start
initCustomer360();