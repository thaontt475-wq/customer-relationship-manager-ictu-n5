/**
 * After-sales Support & Risk Warning Logic (S3-08 / CRM-68)
 * Author: Hoàng Thắng (Frontend)
 */

document.addEventListener('DOMContentLoaded', () => {
    initTicketModal();
});

function initTicketModal() {
    const openBtn = document.getElementById('openTicketModal');
    const closeBtn = document.getElementById('closeTicketModal');
    const cancelBtn = document.getElementById('cancelTicketModal');
    const modal = document.getElementById('ticketModalOverlay');
    const form = document.getElementById('createTicketForm');

    function openModal() {
        if (modal) modal.classList.add('active');
    }

    function closeModal() {
        if (modal) {
            modal.classList.remove('active');
            if (form) form.reset();
        }
    }

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

            const subject = document.getElementById('ticketSubject').value.trim();
            const category = document.getElementById('ticketCategory').value;
            const priority = document.getElementById('ticketPriority').value;
            const description = document.getElementById('ticketDesc').value.trim();

            if (!subject) {
                alert('Vui lòng nhập tiêu đề yêu cầu hỗ trợ!');
                return;
            }

            // Append new ticket row to table
            const tableBody = document.getElementById('ticketTableBody');
            if (tableBody) {
                const randomId = 'TCK-' + Math.floor(1000 + Math.random() * 9000);
                const newRow = document.createElement('tr');
                
                let priorityBadge = '<span class="badge badge-warning">Trung bình</span>';
                if (priority === 'HIGH') priorityBadge = '<span class="badge badge-danger">Khẩn cấp</span>';
                if (priority === 'LOW') priorityBadge = '<span class="badge badge-gray">Thấp</span>';

                newRow.innerHTML = `
                    <td><strong>${randomId}</strong></td>
                    <td>${escapeHtml(subject)}</td>
                    <td>${escapeHtml(category)}</td>
                    <td>${priorityBadge}</td>
                    <td><span class="badge badge-info">Mới tạo</span></td>
                    <td>Hoàng Thắng</td>
                    <td><span style="color: var(--primary); font-weight: 600;">Còn 24h</span></td>
                    <td>
                        <button type="button" class="btn btn-secondary btn-sm" onclick="alert('Xem chi tiết ticket ${randomId}')">Xem</button>
                    </td>
                `;
                tableBody.insertBefore(newRow, tableBody.firstChild);
            }

            alert('Tạo ticket hỗ trợ thành công!');
            closeModal();
        });
    }
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
