/**
 * Customer Care & Follow-Up Logic (S3-09 / CRM-69)
 * Author: Hoàng Văn Thắng (Frontend Lead)
 * 
 * Features:
 * 1. Filter by N days of non-interaction
 * 2. Last interaction & Days count warning badges
 * 3. Contract value formatting & summary
 * 4. Multi-criteria sorting
 * 5. Mark Contacted (Nút "Đã liên hệ" with quick log modal)
 * 6. Refresh List button with spinner animation
 */

// Dữ liệu mẫu khách hàng cần chăm sóc
let customerCareData = [
    {
        id: 'CUST-001',
        code: 'CUST-2026-089',
        name: 'Tập đoàn Công nghệ VNG',
        owner: 'Hoàng Văn Thắng',
        lastContactDate: '2026-09-12',
        daysSinceContact: 25,
        contractValue: 1250000000,
        riskLevel: 'HIGH',
        priority: 'Khẩn cấp',
        contactedToday: false,
        phone: '028 3962 3888'
    },
    {
        id: 'CUST-002',
        code: 'CUST-2026-090',
        name: 'VNG Cloud Solutions',
        owner: 'Hoàng Văn Thắng',
        lastContactDate: '2026-08-20',
        daysSinceContact: 48,
        contractValue: 1850000000,
        riskLevel: 'CRITICAL',
        priority: 'Báo động đỏ',
        contactedToday: false,
        phone: '028 7300 3988'
    },
    {
        id: 'CUST-003',
        code: 'CUST-2026-091',
        name: 'ZaloPay - Zion JSC',
        owner: 'Hoàng Văn Thắng',
        lastContactDate: '2026-09-02',
        daysSinceContact: 35,
        contractValue: 1680000000,
        riskLevel: 'HIGH',
        priority: 'Khẩn cấp',
        contactedToday: false,
        phone: '1900 54 54 36'
    },
    {
        id: 'CUST-004',
        code: 'CUST-2026-050',
        name: 'Tập đoàn Bán lẻ Masan Group',
        owner: 'Hoàng Văn Thắng',
        lastContactDate: '2026-09-21',
        daysSinceContact: 16,
        contractValue: 3400000000,
        riskLevel: 'MEDIUM',
        priority: 'Cần lưu ý',
        contactedToday: false,
        phone: '028 6256 3862'
    },
    {
        id: 'CUST-005',
        code: 'CUST-2026-012',
        name: 'Tổng công ty Viễn thông Viettel',
        owner: 'Hoàng Văn Thắng',
        lastContactDate: '2026-07-28',
        daysSinceContact: 71,
        contractValue: 4100000000,
        riskLevel: 'CRITICAL',
        priority: 'Báo động đỏ',
        contactedToday: false,
        phone: '1800 8098'
    },
    {
        id: 'CUST-006',
        code: 'CUST-2026-077',
        name: 'Công ty Cổ phần Chứng khoán SSI',
        owner: 'Nguyễn Văn Nam',
        lastContactDate: '2026-09-18',
        daysSinceContact: 19,
        contractValue: 950000000,
        riskLevel: 'MEDIUM',
        priority: 'Cần lưu ý',
        contactedToday: false,
        phone: '024 3936 6390'
    },
    {
        id: 'CUST-007',
        code: 'CUST-2026-033',
        name: 'Tập đoàn Dược phẩm Nam Hà',
        owner: 'Trần Thu Hà',
        lastContactDate: '2026-09-27',
        daysSinceContact: 10,
        contractValue: 480000000,
        riskLevel: 'LOW',
        priority: 'Bình thường',
        contactedToday: false,
        phone: '0228 3649 408'
    },
    {
        id: 'CUST-008',
        code: 'CUST-2026-044',
        name: 'Ngân hàng TMCP Quân Đội (MB Bank)',
        owner: 'Hoàng Văn Thắng',
        lastContactDate: '2026-10-07',
        daysSinceContact: 0,
        contractValue: 2800000000,
        riskLevel: 'LOW',
        priority: 'Đã chăm sóc',
        contactedToday: true,
        phone: '1900 54 54 26'
    },
    {
        id: 'CUST-009',
        code: 'CUST-2026-062',
        name: 'Công ty Cổ phần MISA',
        owner: 'Lê Minh Trí',
        lastContactDate: '2026-10-07',
        daysSinceContact: 0,
        contractValue: 620000000,
        riskLevel: 'LOW',
        priority: 'Đã chăm sóc',
        contactedToday: true,
        phone: '024 3795 9595'
    }
];

let contactedTodayCount = 2;

document.addEventListener('DOMContentLoaded', () => {
    initCareFilters();
    initCareSort();
    initCareSearch();
    initRefreshButton();
    initContactModal();
    renderCareList();
});

// Format VND currency
function formatVND(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

// Format Date DD/MM/YYYY
function formatDate(dateStr) {
    if (!dateStr) return 'Chưa tương tác';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
        return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
}

// ==========================================
// 1. RENDER DANH SÁCH & KPIs
// ==========================================
function renderCareList() {
    const inputNDays = parseInt(document.getElementById('inputNDays').value) || 0;
    const sortVal = document.getElementById('careSortSelect').value;
    const searchVal = document.getElementById('careSearchInput').value.trim().toLowerCase();
    const tbody = document.getElementById('careTableBody');

    // 1. Lọc theo N ngày chưa tương tác
    let filtered = customerCareData.filter(item => {
        if (inputNDays > 0) {
            return item.daysSinceContact >= inputNDays;
        }
        return true;
    });

    // 2. Lọc theo từ khóa tìm kiếm
    if (searchVal) {
        filtered = filtered.filter(item => 
            item.name.toLowerCase().includes(searchVal) ||
            item.code.toLowerCase().includes(searchVal) ||
            item.owner.toLowerCase().includes(searchVal)
        );
    }

    // 3. Sắp xếp (Sort)
    filtered.sort((a, b) => {
        switch (sortVal) {
            case 'days_desc': return b.daysSinceContact - a.daysSinceContact;
            case 'days_asc': return a.daysSinceContact - b.daysSinceContact;
            case 'value_desc': return b.contractValue - a.contractValue;
            case 'value_asc': return a.contractValue - b.contractValue;
            case 'name_asc': return a.name.localeCompare(b.name, 'vi');
            case 'risk_desc': {
                const weight = { 'CRITICAL': 3, 'HIGH': 2, 'MEDIUM': 1, 'LOW': 0 };
                return (weight[b.riskLevel] || 0) - (weight[a.riskLevel] || 0);
            }
            default: return b.daysSinceContact - a.daysSinceContact;
        }
    });

    // 4. Cập nhật các KPI đầu trang
    updateKpis(filtered);

    // 5. Render vào Table
    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; color: var(--text-muted); padding: 36px;">
                    <div style="font-size: 1.8rem; margin-bottom: 8px;">🎉</div>
                    <div style="font-weight: 700;">Không có khách hàng nào chưa liên hệ quá ${inputNDays} ngày!</div>
                    <div style="font-size: 0.85rem; margin-top: 4px;">Tất cả khách hàng đều được chăm sóc trong giới hạn thời gian.</div>
                </td>
            </tr>
        `;
        document.getElementById('tableRecordCountBadge').textContent = 'Đang hiển thị: 0 khách hàng';
        return;
    }

    document.getElementById('tableRecordCountBadge').textContent = `Đang hiển thị: ${filtered.length} khách hàng`;

    tbody.innerHTML = filtered.map(c => {
        let overdueBadgeClass = 'normal';
        let overdueText = `${c.daysSinceContact} ngày trước`;
        let overdueIcon = '🟢';

        if (c.daysSinceContact === 0) {
            overdueBadgeClass = 'normal';
            overdueText = 'Hôm nay (Vừa xong)';
            overdueIcon = '✅';
        } else if (c.daysSinceContact > 30) {
            overdueBadgeClass = 'critical';
            overdueIcon = '🚨';
        } else if (c.daysSinceContact >= 14) {
            overdueBadgeClass = 'warning';
            overdueIcon = '⚠️';
        }

        let priorityBadge = '<span class="badge badge-secondary">Bình thường</span>';
        if (c.riskLevel === 'CRITICAL') priorityBadge = '<span class="badge badge-danger">Báo động đỏ</span>';
        else if (c.riskLevel === 'HIGH') priorityBadge = '<span class="badge badge-warning">Khẩn cấp</span>';
        else if (c.riskLevel === 'MEDIUM') priorityBadge = '<span class="badge badge-purple">Cần lưu ý</span>';
        else if (c.contactedToday) priorityBadge = '<span class="badge badge-success">Đã chăm sóc</span>';

        return `
            <tr>
                <td>
                    <div style="font-weight: 800; font-size: 0.92rem; color: var(--text-primary);">${c.name}</div>
                    <div style="font-size: 0.78rem; color: var(--text-muted);">
                        <code>${c.code}</code> • 📞 ${c.phone}
                    </div>
                </td>
                <td>
                    <div style="font-weight: 600; font-size: 0.85rem;">👤 ${c.owner}</div>
                </td>
                <td>
                    <div style="font-weight: 600; font-size: 0.88rem;">${formatDate(c.lastContactDate)}</div>
                </td>
                <td>
                    <span class="care-overdue-badge ${overdueBadgeClass}">
                        <span>${overdueIcon}</span> ${overdueText}
                    </span>
                </td>
                <td>
                    <strong style="color: var(--primary); font-size: 0.92rem;">${formatVND(c.contractValue)}</strong>
                </td>
                <td>
                    ${priorityBadge}
                </td>
                <td style="text-align: right; white-space: nowrap;">
                    <button type="button" class="btn-mark-contacted" onclick="openContactModal('${c.id}')" title="Ghi nhận liên hệ chăm sóc">
                        <span>📞</span> Đã liên hệ
                    </button>
                    <a href="customer-360.jsp?id=${c.id}" class="btn btn-secondary btn-sm" style="margin-left: 6px;" title="Xem hồ sơ 360">
                        Xem 360
                    </a>
                </td>
            </tr>
        `;
    }).join('');
}

function updateKpis(filteredList) {
    // Tổng cần chăm sóc
    document.getElementById('kpiTotalPending').textContent = filteredList.length;

    // Quá hạn > 30 ngày trong toàn bộ danh sách
    const overdue30 = customerCareData.filter(c => c.daysSinceContact > 30).length;
    document.getElementById('kpiOverdue30').textContent = overdue30;

    // Tổng giá trị hợp đồng trong danh sách đang lọc
    const totalVal = filteredList.reduce((acc, c) => acc + c.contractValue, 0);
    document.getElementById('kpiValueAtRisk').textContent = formatVND(totalVal);

    // Đã liên hệ hôm nay
    document.getElementById('kpiContactedToday').textContent = contactedTodayCount;
}

// ==========================================
// 2. BỘ LỌC N NGÀY & PRESETS
// ==========================================
function initCareFilters() {
    const inputNDays = document.getElementById('inputNDays');
    const presetChips = document.querySelectorAll('.care-preset-chip');

    if (inputNDays) {
        inputNDays.addEventListener('input', () => {
            const val = parseInt(inputNDays.value) || 0;
            // Bỏ active tất cả presets nếu không khớp
            presetChips.forEach(chip => {
                if (parseInt(chip.getAttribute('data-days')) === val) {
                    chip.classList.add('active');
                } else {
                    chip.classList.remove('active');
                }
            });
            renderCareList();
        });
    }

    presetChips.forEach(chip => {
        chip.addEventListener('click', () => {
            presetChips.forEach(c => c.classList.remove('active'));
            chip.classList.add('active');

            const days = parseInt(chip.getAttribute('data-days'));
            inputNDays.value = days;
            renderCareList();
        });
    });
}

// ==========================================
// 3. SORT & SEARCH
// ==========================================
function initCareSort() {
    const sortSelect = document.getElementById('careSortSelect');
    if (sortSelect) {
        sortSelect.addEventListener('change', () => {
            renderCareList();
        });
    }
}

function initCareSearch() {
    const searchInput = document.getElementById('careSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            renderCareList();
        });
    }
}

// ==========================================
// 4. REFRESH DANH SÁCH (NÚT REFRESH)
// ==========================================
function initRefreshButton() {
    const btnRefresh = document.getElementById('btnRefreshList');
    if (!btnRefresh) return;

    btnRefresh.addEventListener('click', () => {
        btnRefresh.classList.add('spinning');
        btnRefresh.disabled = true;

        // Mô phỏng tải dữ liệu API từ máy chủ
        setTimeout(() => {
            btnRefresh.classList.remove('spinning');
            btnRefresh.disabled = false;
            renderCareList();
            showCareToast('🔄 Đã làm mới dữ liệu danh sách khách hàng cần chăm sóc thành công!');
        }, 500);
    });
}

// ==========================================
// 5. MODAL "ĐÃ LIÊN HỆ" (QUICK LOG CONTACT)
// ==========================================
function initContactModal() {
    const modal = document.getElementById('modalMarkContacted');
    const form = document.getElementById('formMarkContacted');
    const btnClose = document.getElementById('btnCloseContactModal');
    const btnCancel = document.getElementById('btnCancelContactModal');

    window.openContactModal = (customerId) => {
        const cust = customerCareData.find(c => c.id === customerId);
        if (!cust) return;

        document.getElementById('modalCustId').value = cust.id;
        document.getElementById('modalCustNameDisplay').textContent = `${cust.name} (${cust.code})`;
        modal.style.display = 'flex';
    };

    const closeModal = () => {
        modal.style.display = 'none';
        form.reset();
    };

    if (btnClose) btnClose.addEventListener('click', closeModal);
    if (btnCancel) btnCancel.addEventListener('click', closeModal);

    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            const custId = document.getElementById('modalCustId').value;
            const channel = document.getElementById('contactChannelSelect').value;
            const note = document.getElementById('contactNoteText').value.trim();

            const targetCust = customerCareData.find(c => c.id === custId);
            if (targetCust) {
                const now = new Date();
                const yyyy = now.getFullYear();
                const mm = String(now.getMonth() + 1).padStart(2, '0');
                const dd = String(now.getDate()).padStart(2, '0');

                // Cập nhật số ngày thành 0, ngày tương tác thành hôm nay
                targetCust.lastContactDate = `${yyyy}-${mm}-${dd}`;
                targetCust.daysSinceContact = 0;
                targetCust.riskLevel = 'LOW';
                targetCust.priority = 'Đã chăm sóc';

                if (!targetCust.contactedToday) {
                    targetCust.contactedToday = true;
                    contactedTodayCount++;
                }

                closeModal();
                renderCareList();
                showCareToast(`📞 Đã ghi nhận liên hệ thành công: ${targetCust.name}!`);
            }
        });
    }
}

// ==========================================
// 6. TOAST NOTIFICATION
// ==========================================
function showCareToast(msg) {
    const toast = document.getElementById('careToast');
    const msgEl = document.getElementById('careToastMsg');
    if (!toast || !msgEl) return;

    msgEl.textContent = msg;
    toast.style.display = 'flex';

    setTimeout(() => {
        toast.style.display = 'none';
    }, 3000);
}
