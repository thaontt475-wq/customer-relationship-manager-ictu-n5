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
            writeDisplay("company360Name", data.name);
            writeDisplay("company360Tax", data.taxCode);
            writeDisplay("company360Industry", data.industry);
            writeDisplay("company360Phone", data.phone);
            writeDisplay("company360Website", data.website);
        }
    } catch (err) {
        console.warn("Could not load customer info:", err);
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
}

document.querySelectorAll(".right-tab").forEach(tab => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".right-tab").forEach(item => item.classList.remove("active"));
        tab.classList.add("active");

        const target = tab.dataset.tab;
        const cPanel = document.getElementById("contactsPanel");
        const oPanel = document.getElementById("opportunitiesPanel");
        if (cPanel) cPanel.hidden = target !== "contacts";
        if (oPanel) oPanel.hidden = target !== "opportunities";
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