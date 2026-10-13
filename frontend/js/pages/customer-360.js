"use strict";

/**
 * ===================================================================
 * CRM-63 / S3-03: ENTERPRISE CUSTOMER 360 VIEW CLIENT ENGINE
 * Module: frontend/js/pages/customer-360.js
 * Author: Senior Frontend Developer
 *
 * Capabilities:
 *  1. Unified API Client for GET /api/customers/{id}/360
 *  2. Authenticated HTTP/JSON Client Architecture
 *  3. Enterprise KPI Cards & Company Profile Representation
 *  4. High-Performance DOM Rendering (< 1.5s for 500 activities) with client pagination
 *  5. Complete 6-Tab Workspace: Timeline, Contacts, Deals, Attachments, Tickets, Hierarchy
 *  6. 100% Backward Compatibility with CRM-61, CRM-62, CRM-65, CRM-68
 * ===================================================================
 */

const API_BASE = (window.CRM_BACKEND_BASE || `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`);

// Global View States
let currentCustomer = null;
let customer360Data = null;
let timelineData = [];
let filteredTimeline = [];
let opportunitiesData = { open: [], closed: [] };
let attachmentsData = [];
let contactsList = [];

// Timeline Pagination & Performance State
let timelineCurrentPage = 1;
const timelinePageSize = 20;
let timelineFilterType = "all";
let timelineSearchQuery = "";

// URL Parameters
const params = new URLSearchParams(window.location.search);
const customerId = params.get("id");
let customerLookups = {industries:[], companySizes:[], owners:[]};

/* =========================================================
   1. API CLIENT WITH CREDENTIALS
========================================================= */
async function api(path, options = {}) {
    return CrmApi.request(path,options);
}

/* =========================================================
   2. BACKEND DATA SOURCE
========================================================= */


/* =========================================================
   3. DATA FETCHING & NORMALIZATION PIPELINE
========================================================= */
async function loadCustomer360Data(cId) {
    const id=Number(cId);
    if (!Number.isSafeInteger(id) || id<=0) throw new Error("Mã khách hàng không hợp lệ.");
    const raw=await CrmApi.request(`/api/customers/${id}/360`);
    return normalizePayload(raw);
}


function normalizePayload(raw) {
    if (!raw?.customer || raw.totalContractValue == null || raw.openOpportunityValue == null ||
        ![raw.contacts, raw.openOpportunities, raw.closedOpportunities, raw.activities, raw.attachments].every(Array.isArray)) {
        throw new Error("API Customer 360 trả dữ liệu không đầy đủ.");
    }
    const cust = raw.customer || raw;
    const oppsOpen = raw.openOpportunities || (Array.isArray(raw.opportunities) ? raw.opportunities.filter(o => o.status === "OPEN") : (raw.opportunities?.open || []));
    const oppsClosed = raw.closedOpportunities || (Array.isArray(raw.opportunities) ? raw.opportunities.filter(o => o.status !== "OPEN") : (raw.opportunities?.closed || []));

    // Financial KPIs calculation
    const oppsWon = oppsClosed.filter(o => o.status === "WON");
    const oppsWonSum = oppsWon.reduce((sum, o) => sum + (Number(o.amount) || 0), 0);
    const wonSum = (raw.totalContractValue != null && Number(raw.totalContractValue) > 0)
        ? Number(raw.totalContractValue)
        : oppsWonSum;

    const pipelineSum = (raw.openOpportunityValue != null && Number(raw.openOpportunityValue) > 0)
        ? Number(raw.openOpportunityValue)
        : oppsOpen.reduce((sum, o) => sum + (Number(o.amount) || 0), 0);

    const contacts = (raw.contacts || []).map(c=>({...c,jobTitle:c.title,decisionRole:c.buyingRole,isPrimary:!!c.isPrimary}));
    const activities = raw.activities || [];
    const attachments = raw.attachments || [];

    return {
        customer: cust,
        kpis: {
            totalWon: wonSum,
            wonDealsCount: (raw.kpis?.wonDealsCount ?? 0) || oppsWon.length,
            openPipeline: pipelineSum,
            openDealsCount: (raw.kpis?.openDealsCount ?? 0) || oppsOpen.length,
            contactsCount: raw.kpis?.contactsCount ?? contacts.length,
            activitiesCount: raw.kpis?.activitiesCount ?? activities.length
        },
        contacts,
        openOpportunities: oppsOpen,
        closedOpportunities: oppsClosed,
        activities,
        attachments,
        churnRisk: raw.churnRisk || null
    };
}

/* =========================================================
   4. INITIALIZATION FLOW
========================================================= */
async function initCustomer360() {
    window.__CRM_360_READY = false;
    document.getElementById("pageCustomerTitle").textContent="Đang tải hồ sơ...";
    document.querySelector("main.customer360")?.setAttribute("aria-busy","true");
    document.getElementById("crm360Retry")?.remove();
    for(const id of ["company360Name","company360Tax","company360Industry","company360Scale","company360Address","company360Owner","company360Phone","company360Email","company360Website","company360Parent","primaryContactName","primaryContactTitle","primaryContactEmail","primaryContactPhone"])writeDisplay(id,null);
    document.querySelectorAll(".c360-kpi-value").forEach(el=>el.textContent="—");
    document.getElementById("customer360HierarchyBanner").textContent="Đang tải cấu trúc công ty...";

    try {
        const [data,lookups,session] = await Promise.all([loadCustomer360Data(customerId),CrmApi.request("/api/customer-lookups"),CrmApi.session()]);
        customer360Data=data; customerLookups=lookups;
        document.getElementById("editCompanyButton").hidden=!session.permissions.includes("customer.update");
        if (composer) composer.hidden=!session.permissions.includes("activity.create");
        currentCustomer = customer360Data.customer;
        window.ContactsManager?.ingest(currentCustomer,customer360Data.contacts);


        // Render UI Sections
        renderHeaderAndCompanyProfile();
        renderKPIs();
        renderPrimaryContactCard();

        // Prepare Timeline dataset
        timelineData = customer360Data.activities.map(a => ({
            id: a.id,
            type: (a.type || "note").toLowerCase(),
            subject: a.subject || a.title || "",
            text: a.description || a.content || a.subject || "",
            author: a.authorName || customerLookups.owners.find(owner => Number(owner.id) === Number(a.ownerUserId))?.fullName || (a.ownerUserId ? `Người dùng #${a.ownerUserId}` : "Chưa có thông tin"),
            status: a.status,
            time: a.createdAt ? new Date(a.createdAt) : null
        }));
        applyTimelineFilter();

        // Render Deals Tab
        opportunitiesData = {
            open: customer360Data.openOpportunities,
            closed: customer360Data.closedOpportunities
        };
        renderDealsTab();

        // Render Attachments Tab
        attachmentsData = customer360Data.attachments;
        renderAttachmentsTab();

        // Render Contacts Tab (CRM-62 Integration)
        renderContactsTab();

        // Render CRM-68 Churn Risk Banner & Support Tickets
        renderCustomerChurnRisk360();
        // Load the tickets and company group concurrently.
        await Promise.all([renderCustomerTickets360(),renderCorporateHierarchy360()]);

        // Update Tab Badges
        updateAllTabBadges();
        await new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
        performance.mark("crm360-ready");
        window.__CRM_360_READY=true;
        document.querySelector("main.customer360")?.setAttribute("aria-busy","false");

    } catch (err) {
        const title=document.getElementById("pageCustomerTitle");
        if (title) title.textContent="Không thể tải hồ sơ: "+err.message;
        const retry=document.createElement("button"); retry.id="crm360Retry"; retry.textContent="Thử lại"; retry.className="crm-btn crm-btn-secondary";
        retry.onclick=()=>{retry.remove();initCustomer360();}; title?.after(retry);
        document.querySelectorAll(".c360-kpi-value").forEach(el=>el.textContent="—");
        document.getElementById("customer360HierarchyBanner").textContent="Không thể tải dữ liệu.";
        for(const id of ["timelineList","contactsPanel","dealsListContainer","attachmentsListContainer","customerTicketsList"]) {
            const container=document.getElementById(id);if(container)container.textContent=err.message;
        }
        document.querySelector("main.customer360")?.setAttribute("aria-busy","false");
    }
}

/* =========================================================
   5. RENDER HEADER, COMPANY PROFILE & FINANCIAL KPIS
========================================================= */
function renderHeaderAndCompanyProfile() {
    if (!currentCustomer) return;

    const name = currentCustomer.companyName || currentCustomer.name || "Doanh nghiệp";
    const titleEl = document.getElementById("pageCustomerTitle");
    if (titleEl) titleEl.textContent = name;

    const statusBadge = document.getElementById("customer360StatusBadge");
    if (statusBadge) {
        const isClosed = currentCustomer.status === "INACTIVE" || currentCustomer.status === "CLOSED";
        statusBadge.textContent = ({TIEM_NANG:"TIỀM NĂNG",DANG_GIAO_DICH:"ĐANG GIAO DỊCH",CHINH_THUC:"CHÍNH THỨC",NGUNG_HOP_TAC:"NGỪNG HỢP TÁC"})[currentCustomer.status] || currentCustomer.status;
        statusBadge.className = isClosed ? "c360-status-pill" : "c360-status-pill c360-status-active";
    }

    writeDisplay("company360Name", name);
    writeDisplay("company360Tax", currentCustomer.taxCode);
    writeDisplay("company360Industry", customerLookups.industries.find(x=>x.id===currentCustomer.industryId)?.name);
    writeDisplay("company360Scale", customerLookups.companySizes.find(x=>x.id===currentCustomer.companySizeId)?.name);
    writeDisplay("company360Address", currentCustomer.address || "Chưa cập nhật địa chỉ");
    writeDisplay("company360Owner", customerLookups.owners.find(x=>x.id===currentCustomer.ownerUserId)?.fullName);
    writeDisplay("company360Phone", currentCustomer.phone);
    writeDisplay("company360Email", currentCustomer.email);
    writeDisplay("company360Website", currentCustomer.website);
    writeDisplay("company360Parent", currentCustomer.parentName || "— (Độc lập)");
}

function renderKPIs() {
    if (!customer360Data) return;
    const { kpis } = customer360Data;

    // Card 1: Won Deals (Tổng giá trị đã ký)
    const wonValEl = document.getElementById("kpiTotalWonValue");
    const wonCountEl = document.getElementById("kpiTotalWonCount");
    if (wonValEl) wonValEl.textContent = formatMoney(kpis.totalWon || 0);
    const wonCount = kpis.wonDealsCount || (customer360Data.closedOpportunities || []).filter(o => o.status === "WON").length;
    if (wonCountEl) wonCountEl.textContent = `${wonCount} hợp đồng`;

    // Card 2: Open Pipeline (Giá trị cơ hội đang mở)
    const pipeValEl = document.getElementById("kpiOpenPipelineValue");
    const pipeCountEl = document.getElementById("kpiOpenPipelineCount");
    if (pipeValEl) pipeValEl.textContent = formatMoney(kpis.openPipeline || 0);
    if (pipeCountEl) pipeCountEl.textContent = `${kpis.openDealsCount || 0} cơ hội mở`;

    // Card 3: Key Contacts
    const contactCountEl = document.getElementById("kpiContactsCount");
    const contactMetaEl = document.getElementById("kpiContactsMeta");
    const dmCount = (customer360Data.contacts || []).filter(c => c.decisionRole === "DECISION_MAKER").length;
    if (contactCountEl) contactCountEl.textContent = String(kpis.contactsCount || 0);
    if (contactMetaEl) contactMetaEl.textContent = `${dmCount} Người quyết định (DM) • ${(customer360Data.contacts || []).filter(c=>c.isPrimary).length} Đầu mối chính`;

    // Card 4: Activity Stream
    const actCountEl = document.getElementById("kpiActivitiesCount");
    const actMetaEl = document.getElementById("kpiHealthMeta");
    if (actCountEl) actCountEl.textContent = String(kpis.activitiesCount || 0);
    if (actMetaEl) actMetaEl.textContent = customer360Data.activities[0]?.subject || "Chưa có hoạt động";
}

function renderPrimaryContactCard() {
    const primary = (customer360Data?.contacts || []).find(c => c.isPrimary);
    const nameEl = document.getElementById("primaryContactName");
    const titleEl = document.getElementById("primaryContactTitle");
    const emailEl = document.getElementById("primaryContactEmail");
    const phoneEl = document.getElementById("primaryContactPhone");
    const roleBadge = document.getElementById("primaryContactRoleBadge");

    if (!primary) {
        if (nameEl) nameEl.textContent = "Chưa có đầu mối liên hệ chính";
        [titleEl,emailEl,phoneEl,roleBadge].forEach(el=>{if(el)el.textContent="—";});
        return;
    }

    if (nameEl) nameEl.textContent = primary.fullName || "—";
    if (titleEl) titleEl.textContent = primary.jobTitle ? `${primary.jobTitle} · ${primary.department || ''}` : "—";
    if (emailEl) emailEl.innerHTML = `✉ <a href="mailto:${escapeHtml(primary.email)}" style="color:inherit; text-decoration:none;">${escapeHtml(primary.email || '—')}</a>`;
    if (phoneEl) phoneEl.innerHTML = `☎ <a href="tel:${escapeHtml(primary.phone)}" style="color:inherit; text-decoration:none;">${escapeHtml(primary.phone || '—')}</a>`;
    if (roleBadge) roleBadge.innerHTML = renderDecisionRoleBadge(primary.decisionRole);
}

/* =========================================================
   6. HIGH-PERFORMANCE TIMELINE ENGINE (< 1.5s FOR 500 ITEMS)
========================================================= */
function applyTimelineFilter() {
    const q = timelineSearchQuery.toLowerCase().trim();

    filteredTimeline = timelineData.filter(item => {
        const matchesType = timelineFilterType === "all" || item.type === timelineFilterType;
        const matchesQuery = !q || item.text.toLowerCase().includes(q) || item.subject.toLowerCase().includes(q) || item.author.toLowerCase().includes(q);
        return matchesType && matchesQuery;
    });

    timelineCurrentPage = 1;
    renderTimelineOptimized();
    updateFilterCounts();
}

function renderTimelineOptimized() {
    const list = document.getElementById("timelineList");
    const empty = document.getElementById("timelineEmpty");
    const paginationBar = document.getElementById("timelinePaginationBar");
    const pageInfo = document.getElementById("timelinePageInfo");
    const paginationControls = document.getElementById("timelinePaginationControls");
    const perfBadge = document.getElementById("timelinePerfBadge");

    if (!list) return;

    // Start performance timer
    const t0 = performance.now();

    // Clear previous items (keep empty placeholder)
    list.querySelectorAll(".c360-timeline-card, .timeline-item").forEach(el => el.remove());

    if (filteredTimeline.length === 0) {
        if (empty) empty.style.display = "block";
        if (paginationBar) paginationBar.style.display = "none";
        if (perfBadge) perfBadge.textContent = "⚡ Render: 0ms";
        return;
    }

    if (empty) empty.style.display = "none";

    // Client-side Pagination Slicing
    const totalItems = filteredTimeline.length;
    const totalPages = Math.ceil(totalItems / timelinePageSize);
    if (timelineCurrentPage > totalPages) timelineCurrentPage = totalPages;
    if (timelineCurrentPage < 1) timelineCurrentPage = 1;

    const startIdx = (timelineCurrentPage - 1) * timelinePageSize;
    const endIdx = Math.min(startIdx + timelinePageSize, totalItems);
    const pageItems = filteredTimeline.slice(startIdx, endIdx);

    // Fast DOM mounting via DocumentFragment
    const fragment = document.createDocumentFragment();

    for (const item of pageItems) {
        const article = document.createElement("article");
        article.className = "c360-timeline-card";

        const typeClass = `timeline-type-${item.type}`;
        const typeName = typeLabel(item.type);
        const timeFormatted = item.time && Number.isFinite(item.time.getTime()) ? item.time.toLocaleDateString("vi-VN", {
            day: "2-digit", month: "2-digit", year: "numeric",
            hour: "2-digit", minute: "2-digit"
        }) : "Chưa có thời gian";

        article.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; gap:10px; margin-bottom:6px;">
                <div style="display:flex; align-items:center; gap:8px;">
                    <span class="timeline-type-pill ${typeClass}">${typeName}</span>
                    <strong style="font-size:13.5px; color:var(--crm-text);">${escapeHtml(item.subject || typeName)}</strong>
                </div>
                <time style="font-size:11.5px; color:var(--crm-muted); white-space:nowrap;">
                    ${timeFormatted}
                </time>
            </div>
            <div style="font-size:13px; color:var(--crm-text-2); line-height:1.5;">
                ${escapeHtml(item.text)}
            </div>
            <div style="display:flex; justify-content:space-between; align-items:center; font-size:11.5px; color:var(--crm-muted); margin-top:8px; padding-top:6px; border-top:1px dashed var(--crm-border);">
                <span>👤 Thực hiện bởi: <strong>${escapeHtml(item.author)}</strong></span>
                <span style="font-weight:600;">${escapeHtml(({COMPLETED:"Hoàn tất",PLANNED:"Đã lên kế hoạch",IN_PROGRESS:"Đang thực hiện",CANCELLED:"Đã hủy"})[item.status] || item.status || "—")}</span>
            </div>
        `;
        fragment.appendChild(article);
    }

    list.appendChild(fragment);

    // Measure Render Performance
    const t1 = performance.now();
    const duration = (t1 - t0).toFixed(1);
    if (perfBadge) {
        perfBadge.textContent = `⚡ Render: ${duration}ms (${totalItems} items)`;
    }

    // Render Pagination Controls
    if (paginationBar && pageInfo && paginationControls) {
        paginationBar.style.display = totalPages > 1 ? "flex" : "none";
        pageInfo.textContent = `Hiển thị ${startIdx + 1} - ${endIdx} trong tổng số ${totalItems} hoạt động`;

        paginationControls.innerHTML = `
            <button type="button" class="pagination-btn" id="btnPagePrev" ${timelineCurrentPage === 1 ? 'disabled' : ''} title="Trang trước">‹</button>
            <span style="font-size:12px; font-weight:600; padding:0 8px;">Trang ${timelineCurrentPage} / ${totalPages}</span>
            <button type="button" class="pagination-btn" id="btnPageNext" ${timelineCurrentPage === totalPages ? 'disabled' : ''} title="Trang tiếp">›</button>
        `;

        document.getElementById("btnPagePrev")?.addEventListener("click", () => {
            if (timelineCurrentPage > 1) {
                timelineCurrentPage--;
                renderTimelineOptimized();
            }
        });

        document.getElementById("btnPageNext")?.addEventListener("click", () => {
            if (timelineCurrentPage < totalPages) {
                timelineCurrentPage++;
                renderTimelineOptimized();
            }
        });
    }
}

function updateFilterCounts() {
    const countAll = timelineData.length;
    const countCall = timelineData.filter(i => i.type === "call").length;
    const countMeeting = timelineData.filter(i => i.type === "meeting").length;
    const countEmail = timelineData.filter(i => i.type === "email").length;
    const countNote = timelineData.filter(i => i.type === "note").length;

    writeText("countFilterAll", countAll);
    writeText("countFilterCall", countCall);
    writeText("countFilterMeeting", countMeeting);
    writeText("countFilterEmail", countEmail);
    writeText("countFilterNote", countNote);
}

/* =========================================================
   7. DEALS / OPPORTUNITIES TAB RENDERER
========================================================= */
let currentDealsFilter = "all";

function renderDealsTab() {
    const openDeals = opportunitiesData.open || [];
    const closedDeals = opportunitiesData.closed || [];
    const allDeals = [...openDeals, ...closedDeals];

    const openSum = openDeals.reduce((sum, d) => sum + (Number(d.amount) || 0), 0);
    const wonSum = closedDeals.filter(d => d.status === "WON").reduce((sum, d) => sum + (Number(d.amount) || 0), 0);
    const totalClosed = closedDeals.length;
    const wonCount = closedDeals.filter(d => d.status === "WON").length;
    const winRate = totalClosed > 0 ? Math.round((wonCount / totalClosed) * 100) : 0;

    writeText("dealsStatOpenAmount", formatMoney(openSum));
    writeText("dealsStatWonAmount", formatMoney(wonSum));
    writeText("dealsStatWinRate", `${winRate}%`);

    writeText("dealCountAll", allDeals.length);
    writeText("dealCountOpen", openDeals.length);
    writeText("dealCountWon", wonCount);
    writeText("dealCountLost", closedDeals.filter(d => d.status === "LOST").length);

    // Filter deals
    let displayedDeals = allDeals;
    if (currentDealsFilter === "open") displayedDeals = openDeals;
    else if (currentDealsFilter === "won") displayedDeals = closedDeals.filter(d => d.status === "WON");
    else if (currentDealsFilter === "lost") displayedDeals = closedDeals.filter(d => d.status === "LOST");

    const container = document.getElementById("dealsListContainer");
    if (!container) return;

    if (displayedDeals.length === 0) {
        container.innerHTML = '<div class="empty-small" style="padding:24px; text-align:center; color:var(--crm-muted);">Không có cơ hội bán hàng nào phù hợp bộ lọc.</div>';
        return;
    }

    container.innerHTML = `
        <table class="c360-deals-table">
            <thead>
                <tr>
                    <th>Tên cơ hội bán hàng</th>
                    <th>Giai đoạn</th>
                    <th>Doanh số dự kiến</th>
                    <th>Xác suất</th>
                    <th>Doanh số kỳ vọng</th>
                    <th>Dự kiến đóng</th>
                    <th>Trạng thái</th>
                </tr>
            </thead>
            <tbody>
                ${displayedDeals.map(deal => {
                    const statusClass = deal.status === "WON" ? "deal-status-won" : (deal.status === "LOST" ? "deal-status-lost" : "deal-status-open");
                    const statusText = deal.status === "WON" ? "✓ Ký thành công" : (deal.status === "LOST" ? "✕ Thất bại" : "● Đang mở");
                    const prob = deal.probability == null ? null : Number(deal.probability);
                    const expected = prob == null ? null : Number(deal.amount) * (prob / 100);

                    return `
                        <tr>
                            <td>
                                <strong>${escapeHtml(deal.name)}</strong>
                                <div style="font-size:11.5px; color:var(--crm-muted); margin-top:2px;">
                                    Phụ trách: ${escapeHtml(deal.ownerName || customerLookups.owners.find(owner => Number(owner.id) === Number(deal.ownerUserId))?.fullName || "Chưa có thông tin")}
                                </div>
                            </td>
                            <td>
                                <span style="font-weight:600; color:var(--crm-text);">${escapeHtml(deal.stageName || deal.stage || "—")}</span>
                            </td>
                            <td>
                                <strong style="color:var(--crm-primary); font-size:14px;">${formatMoney(deal.amount || 0)}</strong>
                            </td>
                            <td>
                                <div style="font-weight:700; font-size:12px;">${prob == null ? "—" : `${prob}%`}</div>
                                <div class="prob-bar-wrap" ${prob == null ? "hidden" : ""}>
                                    <div class="prob-bar-fill" style="width:${prob}%;"></div>
                                </div>
                            </td>
                            <td>
                                <span style="color:var(--crm-text-2); font-weight:600;">${deal.expectedRevenue == null && expected == null ? "—" : formatMoney(deal.expectedRevenue ?? expected)}</span>
                            </td>
                            <td>
                                <span style="font-size:12px; color:var(--crm-muted);">${escapeHtml(deal.closeDate || deal.expectedCloseDate || "—")}</span>
                            </td>
                            <td>
                                <span class="deal-status-pill ${statusClass}">${statusText}</span>
                            </td>
                        </tr>
                    `;
                }).join("")}
            </tbody>
        </table>
    `;
}

/* =========================================================
   8. ATTACHMENTS TAB RENDERER & UPLOAD MODAL
========================================================= */
function renderAttachmentsTab() {
    const container=document.getElementById("attachmentsListContainer");
    if (!container) return;
    document.getElementById("btnOpenUploadAttachment")?.setAttribute("hidden","");
    const counter=document.getElementById("attachmentsCountHeader"); if (counter) counter.textContent=`${attachmentsData.length} tệp`;
    container.innerHTML=attachmentsData.length ? attachmentsData.map(file=>`
        <div class="attachment-card"><strong>${escapeHtml(file.fileName)}</strong><p>${escapeHtml(file.uploadedAt || "")}</p>
        <button class="crm-btn crm-btn-secondary" data-download-id="${file.id}">Tải tài liệu</button></div>`).join("") : '<div class="empty-small">Chưa có tài liệu đính kèm.</div>';
    container.querySelectorAll("[data-download-id]").forEach(button=>button.addEventListener("click",async()=>{
        try {
            const response=await fetch(`${CrmApi.base}/api/customer-attachments/${button.dataset.downloadId}/download`,{credentials:"include"});
            if (!response.ok) { const error=await response.json();throw new Error(error.message); }
            const file=attachmentsData.find(f=>String(f.id)===button.dataset.downloadId);
            const url=URL.createObjectURL(await response.blob()); const link=document.createElement("a");link.href=url;link.download=file.fileName;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
        } catch(error) { alert(error.message); }
    }));
}

/* =========================================================
   9. CONTACTS TAB RENDERER (CRM-62 INTEGRATION)
========================================================= */
function renderContactsTab() {
    const container = document.getElementById("contactsPanel");
    if (!container || !customerId) return;

    if (window.ContactsManager && typeof window.ContactsManager.renderCustomer360Contacts === "function") {
        window.ContactsManager.renderCustomer360Contacts(container, customerId);
        return;
    }

    // Render the same API contacts when ContactsManager is not initialized yet
    const contacts = customer360Data?.contacts || [];
    if (contacts.length === 0) {
        container.innerHTML = `
            <div class="empty-small" style="padding:20px; text-align:center; color:var(--crm-muted);">
                Chưa có người liên hệ nào cho doanh nghiệp này.
            </div>
            <button id="addContact" class="crm-btn crm-btn-secondary full-button" type="button">
                + Thêm người liên hệ
            </button>
        `;
        return;
    }

    container.innerHTML = `
        <div style="padding:4px 0 12px 0; display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid var(--crm-border); margin-bottom:12px;">
            <span style="font-size:13px; font-weight:700; color:var(--crm-text);">
                Danh sách người liên hệ B2B (${contacts.length})
            </span>
            <a href="contacts.html?customerId=${customerId}" class="crm-btn crm-btn-secondary" style="font-size:12px; padding:3px 10px; text-decoration:none;">
                Quản lý toàn diện →
            </a>
        </div>
        <div style="display:grid; grid-template-columns:repeat(auto-fill, minmax(280px, 1fr)); gap:12px;">
            ${contacts.map(c => `
                <div style="background:var(--crm-surface-soft); border:1px solid var(--crm-border); border-radius:8px; padding:12px; display:flex; flex-direction:column; gap:8px;">
                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <strong>${escapeHtml(c.fullName)}</strong>
                        ${c.isPrimary ? '<span title="Đầu mối chính" style="font-size:16px;">⭐</span>' : ''}
                    </div>
                    <div style="font-size:12px; color:var(--crm-muted);">
                        ${escapeHtml(c.jobTitle)} · ${escapeHtml(c.department || '')}
                    </div>
                    <div>
                        ${renderDecisionRoleBadge(c.decisionRole)}
                    </div>
                    <div style="font-size:12px; color:var(--crm-text-2); display:flex; flex-direction:column; gap:2px; margin-top:4px;">
                        <span>✉ ${escapeHtml(c.email)}</span>
                        <span>☎ ${escapeHtml(c.phone)}</span>
                    </div>
                </div>
            `).join("")}
        </div>
    `;
}

function renderDecisionRoleBadge(role) {
    switch (role) {
        case "DECISION_MAKER":
            return `<span style="background:#fee2e2; color:#991b1b; border:1px solid #f87171; font-size:11px; font-weight:700; padding:1px 6px; border-radius:4px;">👑 Quyết định (DM)</span>`;
        case "INFLUENCER":
            return `<span style="background:#fef3c7; color:#92400e; border:1px solid #fcd34d; font-size:11px; font-weight:700; padding:1px 6px; border-radius:4px;">⚡ Người ảnh hưởng</span>`;
        case "BLOCKER":
            return `<span style="background:#f1f5f9; color:#b91c1c; border:1px solid #fca5a5; font-size:11px; font-weight:700; padding:1px 6px; border-radius:4px;">⚠️ Người cản trở</span>`;
        default:
            return `<span style="background:#e0f2fe; color:#0369a1; border:1px solid #7dd3fc; font-size:11px; font-weight:700; padding:1px 6px; border-radius:4px;">👤 Người dùng cuối</span>`;
    }
}

/* =========================================================
   10. CRM-65 & CRM-68 PRESERVED INTEGRATIONS
========================================================= */
function getCustomerRecord(cId) {
    return currentCustomer && Number(currentCustomer.id)===Number(cId) ? currentCustomer : null;
}

function renderCustomerChurnRisk360() {
    const bannerContainer = document.getElementById("customer360ChurnBanner");
    if (!bannerContainer || !customerId) return;

    const churnInfo=customer360Data.churnRisk;

    if (!churnInfo || !churnInfo.isRisk) {
        bannerContainer.innerHTML = "";
        bannerContainer.style.display = "none";
        return;
    }

    bannerContainer.style.display = "block";
    const customer = getCustomerRecord(customerId);
    const salesRepName = customerLookups.owners.find(x=>x.id===customer?.ownerUserId)?.fullName || "Chưa có thông tin owner";
    const salesRepEmail = customer?.ownerEmail || "—";
    const salesRepPhone = customer?.ownerPhone || "—";

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
                                ${({CRITICAL:'RẤT NGUY CẤP',HIGH:'RỦI RO CAO',MEDIUM:'TRUNG BÌNH',LOW:'THẤP'})[churnInfo.riskLevel] || churnInfo.riskLevel}
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

async function renderCustomerTickets360() {
    const listEl = document.getElementById("customerTicketsList");
    const countBadge = document.getElementById("ticketCountBadge");
    if (!listEl || !customerId) return;

    try {
        const session=await CrmApi.session();
        if (!session.permissions.includes("support.read")) { listEl.textContent="Bạn không có quyền xem phiếu hỗ trợ."; return; }
        const data=await CrmApi.request(`/api/support-requests?customerId=${customerId}&size=100`);
        var tickets=data.items.map(t=>({...t,ticketCode:`SR-${t.id}`}));
    } catch(error) { listEl.textContent=error.message; return; }
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

async function renderCorporateHierarchy360() {
    if (window.CorporateHierarchyManager) await window.CorporateHierarchyManager.renderCustomer360Hierarchy(document.getElementById("customer360HierarchyBanner"),Number(customerId));
}

/* =========================================================
   11. TABS WORKSPACE SWITCHING ENGINE
========================================================= */
function updateAllTabBadges() {
    writeText("badgeTimelineCount", timelineData.length);
    writeText("badgeContactsCount", (customer360Data?.contacts || []).length);
    writeText("badgeOpportunitiesCount", (opportunitiesData.open.length + opportunitiesData.closed.length));
    writeText("badgeAttachmentsCount", attachmentsData.length);
}

document.querySelectorAll(".c360-tab-btn, .right-tab").forEach(tab => {
    tab.addEventListener("click", () => {
        const target = tab.dataset.tab;
        if (!target) return;

        // Update active class on tab buttons
        document.querySelectorAll(".c360-tab-btn, .right-tab").forEach(item => {
            if (item.dataset.tab === target) {
                item.classList.add("active");
                item.setAttribute("aria-selected", "true");
            } else {
                item.classList.remove("active");
                item.setAttribute("aria-selected", "false");
            }
        });

        // Toggle Tab Panes
        const panes = {
            timeline: document.getElementById("timelinePanel"),
            contacts: document.getElementById("contactsPanel"),
            opportunities: document.getElementById("opportunitiesPanel"),
            attachments: document.getElementById("attachmentsPanel"),
            tickets: document.getElementById("ticketsPanel"),
            hierarchy: document.getElementById("hierarchyPanel")
        };

        Object.keys(panes).forEach(k => {
            const pane = panes[k];
            if (pane) {
                if (k === target) {
                    pane.hidden = false;
                    pane.removeAttribute("hidden");
                } else {
                    pane.hidden = true;
                    pane.setAttribute("hidden", "true");
                }
            }
        });
    });
});

/* =========================================================
   12. TIMELINE COMPOSER & TOOLBAR EVENT LISTENERS
========================================================= */
const composer = document.getElementById("activityComposer");
const noteInput = document.getElementById("activityNote");
const typeInput = document.getElementById("activityType");

composer?.addEventListener("submit", async event => {
    event.preventDefault();
    const text = noteInput.value.trim();
    if (!text) {
        noteInput.focus();
        return;
    }

    const type = typeInput.value || "note";
    const button=composer.querySelector("button[type=submit]");
    if (button) button.disabled=true;
    try {
        await api("/api/activities", {method:"POST",body:JSON.stringify({subject:text.slice(0,50),type:type.toUpperCase(),description:text,customerId:Number(customerId),status:"COMPLETED"})});
        noteInput.value="";
        await initCustomer360();
    } catch(error) { alert(error.message); }
    finally { if(button) button.disabled=false; }
});

// Quick Action Buttons (Gọi ngay, Gửi Email, Ghi chú)
document.querySelectorAll("[data-quick]").forEach(button => {
    button.addEventListener("click", () => {
        const type = button.dataset.quick;
        if (typeInput) typeInput.value = type === "call" ? "call" : type === "email" ? "email" : "note";
        if (noteInput) {
            noteInput.placeholder = type === "call" ? "Ghi nhận nội dung cuộc gọi..." : (type === "email" ? "Ghi nhận nội dung email đã gửi..." : "Ghi chú công việc...");
            noteInput.focus();
        }
    });
});

// Refresh Button
document.getElementById("btnRefresh360")?.addEventListener("click", async () => {
    const btn = document.getElementById("btnRefresh360");
    if (btn) btn.classList.add("loading");
    await initCustomer360();
    if (btn) btn.classList.remove("loading");
});

// Timeline Filter Chips
document.querySelectorAll(".timeline-chip").forEach(chip => {
    chip.addEventListener("click", () => {
        document.querySelectorAll(".timeline-chip").forEach(c => c.classList.remove("active"));
        chip.classList.add("active");
        timelineFilterType = chip.dataset.filter || "all";
        applyTimelineFilter();
    });
});

// Timeline Search Input
const searchInput = document.getElementById("timelineSearchInput");
searchInput?.addEventListener("input", () => {
    timelineSearchQuery = searchInput.value;
    applyTimelineFilter();
});

// Deals Filter Buttons
["All", "Open", "Won", "Lost"].forEach(k => {
    const btn = document.getElementById(`filterDeals${k}`);
    btn?.addEventListener("click", () => {
        ["All", "Open", "Won", "Lost"].forEach(x => document.getElementById(`filterDeals${x}`)?.classList.remove("active"));
        btn.classList.add("active");
        currentDealsFilter = k.toLowerCase();
        renderDealsTab();
    });
});

/* =========================================================
   13. ATTACHMENT UPLOAD MODAL
========================================================= */
const attachModal = document.getElementById("attachmentUploadModal");
const attachOverlay = document.getElementById("attachmentModalOverlay");

document.getElementById("btnOpenUploadAttachment")?.addEventListener("click", () => {
    if (attachModal && attachOverlay) {
        attachModal.classList.add("open");
        attachOverlay.classList.add("open");
    }
});

function closeAttachModal() {
    if (attachModal && attachOverlay) {
        attachModal.classList.remove("open");
        attachOverlay.classList.remove("open");
    }
}

document.getElementById("closeAttachmentModal")?.addEventListener("click", closeAttachModal);
document.getElementById("cancelAttachUpload")?.addEventListener("click", closeAttachModal);
attachOverlay?.addEventListener("click", closeAttachModal);


/* =========================================================
   14. COMPANY EDIT MODAL
========================================================= */
const modal = document.getElementById("companyModal");
const modalOverlay = document.getElementById("companyModalOverlay");

function openCompanyModal() {
    if (!currentCustomer) return;
    for (const [id,key] of Object.entries({edit360Name:"name",edit360Tax:"taxCode",edit360Address:"address",edit360Phone:"phone",edit360Email:"email",edit360Website:"website"})) setValue(id,currentCustomer[key]);
    for (const [id,items,key,label] of [["edit360Industry",customerLookups.industries,"industryId","name"],["edit360Scale",customerLookups.companySizes,"companySizeId","name"],["edit360Owner",customerLookups.owners,"ownerUserId","fullName"]]) {
        const select=document.getElementById(id);
        select.innerHTML='<option value="">Chọn...</option>'+items.map(x=>`<option value="${x.id}">${escapeHtml(x[label])}</option>`).join("");
        setValue(id,currentCustomer[key]);
    }
    modal.classList.add("open"); modalOverlay.classList.add("open");
}

function closeCompanyModal() {
    modal.classList.remove("open");
    modalOverlay.classList.remove("open");
}

document.getElementById("editCompanyButton")?.addEventListener("click", openCompanyModal);
document.getElementById("closeCompanyModal")?.addEventListener("click", closeCompanyModal);
document.getElementById("cancelCompanyEdit")?.addEventListener("click", closeCompanyModal);
modalOverlay?.addEventListener("click", closeCompanyModal);

document.getElementById("company360Form")?.addEventListener("submit", async event => {
    event.preventDefault();

    const button=event.target.querySelector("button[type=submit]");
    if(button)button.disabled=true;
    try {
        const payload={name:value("edit360Name"),taxCode:value("edit360Tax")||null,address:value("edit360Address"),phone:value("edit360Phone"),email:value("edit360Email"),website:value("edit360Website"),industryId:Number(value("edit360Industry"))||null,companySizeId:Number(value("edit360Scale"))||null,ownerUserId:Number(value("edit360Owner"))||currentCustomer.ownerUserId,status:currentCustomer.status,type:currentCustomer.type||"ENTERPRISE",region:currentCustomer.region,customFields:currentCustomer.customFields};
        await api(`/api/customers/${customerId}`,{method:"PUT",body:JSON.stringify(payload)});
        closeCompanyModal(); await initCustomer360();
    } catch(error) { alert(error.message); }
    finally { if(button)button.disabled=false; }
});

document.addEventListener("keydown", event => {
    if (event.key === "Escape") {
        closeCompanyModal();
        closeAttachModal();
    }
});

// Re-render contacts on CRM-62 event
document.addEventListener("crm:contacts-updated", initCustomer360);

/* =========================================================
   15. UTILITY FUNCTIONS
========================================================= */
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

function writeText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = String(val ?? "");
}

function escapeHtml(val) {
    return String(val ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function formatMoney(val) {
    return new Intl.NumberFormat("vi-VN", {
        style: "currency",
        currency: "VND",
        maximumFractionDigits: 0
    }).format(Number(val) || 0);
}

// Bootstrap Initialization
document.addEventListener("DOMContentLoaded", () => {
    initCustomer360();
});

if (document.readyState === "interactive" || document.readyState === "complete") {
    initCustomer360();
}
