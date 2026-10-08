/**
 * Lead Filter & Saved Filter Logic (S4-09 / CRM-123)
 * Author: Hoàng Thắng (Frontend)
 */

document.addEventListener('DOMContentLoaded', () => {
    initSavedFilters();
    initFilterActions();
});

// Default Saved Filters
let savedFilters = [
    { id: 'sf-all', name: 'Tất cả Lead', criteria: { source: '', status: '', minScore: '' }, isDefault: true },
    { id: 'sf-hot', name: '🔥 Lead Nóng (Điểm > 80)', criteria: { source: '', status: 'WARM', minScore: '80' }, isDefault: false },
    { id: 'sf-web', name: '🌐 Lead từ Website', criteria: { source: 'WEB', status: '', minScore: '' }, isDefault: false }
];

function initSavedFilters() {
    // Load from localStorage if present
    const stored = localStorage.getItem('crm_saved_lead_filters');
    if (stored) {
        try { savedFilters = JSON.parse(stored); } catch (e) { }
    }
    renderSavedFilterChips();
    initSaveFilterModal();
}

function renderSavedFilterChips() {
    const container = document.getElementById('savedFilterChips');
    if (!container) return;

    container.innerHTML = '';
    savedFilters.forEach((sf, index) => {
        const chip = document.createElement('div');
        chip.className = `filter-chip ${index === 0 ? 'active' : ''}`;
        chip.dataset.id = sf.id;

        let deleteBtn = '';
        if (!sf.isDefault) {
            deleteBtn = `<span class="chip-delete" title="Xóa bộ lọc" onclick="deleteSavedFilter(event, '${sf.id}')">&times;</span>`;
        }

        chip.innerHTML = `<span>${escapeHtml(sf.name)}</span> ${deleteBtn}`;
        chip.addEventListener('click', (e) => {
            if (e.target.classList.contains('chip-delete')) return;
            applySavedFilter(sf, chip);
        });

        container.appendChild(chip);
    });
}

function applySavedFilter(sf, chipEl) {
    document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
    if (chipEl) chipEl.classList.add('active');

    // Populate form inputs
    const sourceSelect = document.getElementById('filterSource');
    const statusSelect = document.getElementById('filterStatus');
    const minScoreInput = document.getElementById('filterMinScore');

    if (sourceSelect) sourceSelect.value = sf.criteria.source || '';
    if (statusSelect) statusSelect.value = sf.criteria.status || '';
    if (minScoreInput) minScoreInput.value = sf.criteria.minScore || '';

    applyFilterToTable();
}

window.deleteSavedFilter = function(e, id) {
    e.stopPropagation();
    if (!confirm('Bạn có chắc chắn muốn xóa bộ lọc này?')) return;

    savedFilters = savedFilters.filter(f => f.id !== id);
    localStorage.setItem('crm_saved_lead_filters', JSON.stringify(savedFilters));
    renderSavedFilterChips();
};

function initFilterActions() {
    const applyBtn = document.getElementById('btnApplyFilter');
    const resetBtn = document.getElementById('btnResetFilter');

    if (applyBtn) {
        applyBtn.addEventListener('click', () => {
            document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
            applyFilterToTable();
        });
    }

    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            document.getElementById('filterSource').value = '';
            document.getElementById('filterStatus').value = '';
            document.getElementById('filterMinScore').value = '';
            const allChip = document.querySelector('.filter-chip[data-id="sf-all"]');
            if (allChip) allChip.classList.add('active');
            applyFilterToTable();
        });
    }
}

function applyFilterToTable() {
    const sourceVal = document.getElementById('filterSource')?.value.toUpperCase() || '';
    const statusVal = document.getElementById('filterStatus')?.value.toUpperCase() || '';
    const minScore = parseInt(document.getElementById('filterMinScore')?.value, 10) || 0;

    const rows = document.querySelectorAll('#leadsTableBody tr');
    let visibleCount = 0;

    rows.forEach(row => {
        const rowSource = (row.dataset.source || '').toUpperCase();
        const rowStatus = (row.dataset.status || '').toUpperCase();
        const rowScore = parseInt(row.dataset.score, 10) || 0;

        let match = true;
        if (sourceVal && rowSource !== sourceVal) match = false;
        if (statusVal && rowStatus !== statusVal) match = false;
        if (minScore > 0 && rowScore < minScore) match = false;

        if (match) {
            row.style.display = '';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    const countDisplay = document.getElementById('filterResultCount');
    if (countDisplay) {
        countDisplay.textContent = visibleCount;
    }
}

function initSaveFilterModal() {
    const openBtn = document.getElementById('btnOpenSaveFilter');
    const closeBtn = document.getElementById('closeSaveFilterModal');
    const cancelBtn = document.getElementById('cancelSaveFilterModal');
    const modal = document.getElementById('saveFilterModal');
    const form = document.getElementById('saveFilterForm');

    function openModal() { if (modal) modal.classList.add('active'); }
    function closeModal() { if (modal) modal.classList.remove('active'); }

    if (openBtn) openBtn.addEventListener('click', openModal);
    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (cancelBtn) cancelBtn.addEventListener('click', closeModal);

    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            const name = document.getElementById('savedFilterName').value.trim();
            if (!name) return;

            const newFilter = {
                id: 'sf-' + Date.now(),
                name: name,
                criteria: {
                    source: document.getElementById('filterSource')?.value || '',
                    status: document.getElementById('filterStatus')?.value || '',
                    minScore: document.getElementById('filterMinScore')?.value || ''
                },
                isDefault: false
            };

            savedFilters.push(newFilter);
            localStorage.setItem('crm_saved_lead_filters', JSON.stringify(savedFilters));
            renderSavedFilterChips();
            closeModal();
            form.reset();

            // Set the new filter as active
            const newChip = document.querySelector(`.filter-chip[data-id="${newFilter.id}"]`);
            if (newChip) applySavedFilter(newFilter, newChip);
        });
    }
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
