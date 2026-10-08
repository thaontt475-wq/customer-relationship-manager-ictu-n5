/**
 * Campaign Leads Management Logic (S4-03 / CRM-117)
 * Author: Hoàng Thắng (Frontend)
 */

document.addEventListener('DOMContentLoaded', () => {
    initCampaignSelect();
    initAssignLeadModal();
});

// Mock Data for Campaigns
const campaignData = {
    '1': {
        name: 'Chiến dịch Ra mắt Cloud ERP 2026',
        code: 'CAMP-2026-Q4',
        budget: '50.000.000 ₫',
        totalLeads: 128,
        converted: 24,
        rate: '18.75%',
        leads: [
            { id: 'LEAD-901', name: 'Đặng Tuấn Anh', phone: '0912 345 678', company: 'Công ty CP Đầu tư Nam Việt', source: 'Facebook Ads', score: 85, status: 'Tiềm năng (Warm)', sales: 'Hoàng Thắng' },
            { id: 'LEAD-902', name: 'Phạm Thu Trang', phone: '0988 765 432', company: 'Logistics Toàn Cầu', source: 'Google Search Ads', score: 92, status: 'Đã chuyển đổi (Won)', sales: 'Nguyễn Thắng' },
            { id: 'LEAD-903', name: 'Lê Hoàng Long', phone: '0903 112 233', company: 'Dệt may Đông Nam', source: 'Web Form', score: 70, status: 'Mới tiếp cận (New)', sales: 'Tiến' }
        ]
    },
    '2': {
        name: 'Hội thảo Chuyển đổi số ICTU Q3',
        code: 'CAMP-2026-SEMINAR',
        budget: '25.000.000 ₫',
        totalLeads: 65,
        converted: 15,
        rate: '23.07%',
        leads: [
            { id: 'LEAD-881', name: 'Nguyễn Đình Phúc', phone: '0977 445 566', company: 'Bệnh viện Đa khoa Quốc tế', source: 'Sự kiện Offline', score: 95, status: 'Đã chuyển đổi (Won)', sales: 'Hoàng Thắng' },
            { id: 'LEAD-882', name: 'Vũ Thị Minh', phone: '0933 667 889', company: 'Trường Quốc tế Sky', source: 'Sự kiện Offline', score: 62, status: 'Đang nuôi dưỡng (Nurture)', sales: 'Toàn' }
        ]
    }
};

function initCampaignSelect() {
    const select = document.getElementById('campaignSelector');
    if (!select) return;

    select.addEventListener('change', (e) => {
        const campId = e.target.value;
        const data = campaignData[campId];
        if (!data) return;

        // Update KPIs
        document.getElementById('kpiTotalLeads').textContent = data.totalLeads;
        document.getElementById('kpiConverted').textContent = data.converted;
        document.getElementById('kpiConversionRate').textContent = data.rate;
        document.getElementById('campaignCodeBadge').textContent = data.code;

        // Render Table
        renderLeadTable(data.leads);
    });
}

function renderLeadTable(leads) {
    const tbody = document.getElementById('campaignLeadTableBody');
    if (!tbody) return;

    tbody.innerHTML = '';
    leads.forEach(lead => {
        const scoreClass = lead.score >= 80 ? 'lead-score-high' : (lead.score >= 60 ? 'lead-score-mid' : 'lead-score-low');
        const row = document.createElement('tr');
        row.innerHTML = `
            <td><strong>${lead.id}</strong></td>
            <td>
                <div style="font-weight: 700; color: var(--text-primary);">${escapeHtml(lead.name)}</div>
                <div style="font-size: 0.76rem; color: var(--text-muted);">${escapeHtml(lead.phone)}</div>
            </td>
            <td>${escapeHtml(lead.company)}</td>
            <td><span class="badge badge-info">${escapeHtml(lead.source)}</span></td>
            <td><span class="lead-score-pill ${scoreClass}">${lead.score} pts</span></td>
            <td><span class="badge badge-purple">${escapeHtml(lead.status)}</span></td>
            <td>${escapeHtml(lead.sales)}</td>
            <td>
                <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem chi tiết Lead: ${lead.id}')">Xem</button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function initAssignLeadModal() {
    const openBtn = document.getElementById('openAssignModal');
    const closeBtn = document.getElementById('closeAssignModal');
    const cancelBtn = document.getElementById('cancelAssignModal');
    const modal = document.getElementById('assignLeadModalOverlay');
    const form = document.getElementById('assignLeadForm');

    function openModal() { if (modal) modal.classList.add('active'); }
    function closeModal() { if (modal) modal.classList.remove('active'); }

    if (openBtn) openBtn.addEventListener('click', openModal);
    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (cancelBtn) cancelBtn.addEventListener('click', closeModal);

    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeModal();
        });
    }

    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            const leadName = document.getElementById('assignLeadSelect').value;
            alert(`Đã gán thành công [${leadName}] vào chiến dịch!`);
            closeModal();
        });
    }
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
