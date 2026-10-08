/**
 * Business Organization Hierarchy Logic (S2-06 / CRM-42)
 * Author: Hoàng Thắng (Frontend)
 */

document.addEventListener('DOMContentLoaded', () => {
    initOrgTreeSelection();
    initOrgModal();
});

function initOrgTreeSelection() {
    const nodeCards = document.querySelectorAll('.org-node-card');
    const detailName = document.getElementById('detailDeptName');
    const detailCode = document.getElementById('detailDeptCode');
    const detailLeader = document.getElementById('detailDeptLeader');
    const detailMembers = document.getElementById('detailDeptMembers');

    nodeCards.forEach(card => {
        card.addEventListener('click', (e) => {
            if (e.target.closest('.org-node-actions')) return;

            nodeCards.forEach(c => c.classList.remove('selected'));
            card.classList.add('selected');

            if (detailName) detailName.textContent = card.dataset.name || 'Phòng ban';
            if (detailCode) detailCode.textContent = card.dataset.code || 'DEPT-00';
            if (detailLeader) detailLeader.textContent = card.dataset.leader || 'Chưa chỉ định';
            if (detailMembers) detailMembers.textContent = (card.dataset.members || '0') + ' Nhân sự';
        });
    });
}

function initOrgModal() {
    const openBtn = document.getElementById('btnOpenAddDept');
    const closeBtn = document.getElementById('closeDeptModal');
    const cancelBtn = document.getElementById('cancelDeptModal');
    const modal = document.getElementById('addDeptModal');
    const form = document.getElementById('addDeptForm');

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
            const name = document.getElementById('deptName').value.trim();
            const parent = document.getElementById('deptParent').value;

            alert(`Đã thêm mới phòng ban [${name}] trực thuộc [${parent}] thành công!`);
            closeModal();
            location.reload();
        });
    }
}

window.deleteDept = function(e, name, memberCount) {
    e.stopPropagation();
    if (memberCount > 0) {
        alert(`❌ KHÔNG THỂ XÓA!\nPhòng ban [${name}] đang có ${memberCount} nhân sự đang hoạt động.\nVui lòng chuyển nhân sự sang phòng ban khác trước.`);
        return;
    }

    if (confirm(`Bạn có chắc chắn muốn xóa phòng ban [${name}]?`)) {
        alert(`Đã xóa phòng ban [${name}] thành công!`);
        location.reload();
    }
};
