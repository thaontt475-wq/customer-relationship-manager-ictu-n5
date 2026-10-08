/**
 * Win/Loss Reasons & Competitors Logic (S2-10 / CRM-48)
 * Author: Hoàng Thắng (Frontend)
 */

document.addEventListener('DOMContentLoaded', () => {
    initWlTabs();
    initAddReasonModal();
});

function initWlTabs() {
    const tabs = document.querySelectorAll('.wl-tab-link');
    const contents = document.querySelectorAll('.wl-tab-content');

    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const targetId = tab.dataset.tab;

            tabs.forEach(t => t.classList.remove('active'));
            contents.forEach(c => c.classList.remove('active'));

            tab.classList.add('active');
            const targetContent = document.getElementById(targetId);
            if (targetContent) targetContent.classList.add('active');
        });
    });
}

function initAddReasonModal() {
    const openBtn = document.getElementById('btnOpenAddReason');
    const closeBtn = document.getElementById('closeReasonModal');
    const cancelBtn = document.getElementById('cancelReasonModal');
    const modal = document.getElementById('addReasonModal');
    const form = document.getElementById('addReasonForm');

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
            const type = document.getElementById('reasonType').value;
            const name = document.getElementById('reasonName').value.trim();

            alert(`Đã thêm lý do [${name}] vào danh mục [${type === 'WIN' ? 'Thắng (Win)' : 'Thua (Loss)'}] thành công!`);
            closeModal();
            location.reload();
        });
    }
}

window.toggleReasonStatus = function(btn, name) {
    if (btn.classList.contains('badge-success')) {
        btn.classList.remove('badge-success');
        btn.classList.add('badge-gray');
        btn.textContent = 'Ngừng áp dụng';
    } else {
        btn.classList.remove('badge-gray');
        btn.classList.add('badge-success');
        btn.textContent = 'Đang áp dụng';
    }
};
