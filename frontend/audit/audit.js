// frontend/audit/audit.js

document.addEventListener("DOMContentLoaded", () => {
    const tbody = document.getElementById("audit-tbody");
    const filterForm = document.getElementById("audit-filter-form");
    const btnReset = document.getElementById("btn-reset");
    const btnExport = document.getElementById("btn-export");

    const mockAuditList = [
        {
            time: "2026-09-24 10:15:20",
            actor: "Trần Thị Bình (Director)",
            avatar: "https://ui-avatars.com/api/?name=Tran+Thi+Binh&background=f59e0b&color=fff",
            action: "Cập nhật",
            actionType: "update",
            entity: "Chiết khấu #BG-009",
            beforeVal: "5%",
            afterVal: "12%"
        },
        {
            time: "2026-09-24 09:30:10",
            actor: "Nông Quang Tiệp (Admin)",
            avatar: "https://ui-avatars.com/api/?name=Nong+Quang+Tiep&background=2563eb&color=fff",
            action: "Gán quyền",
            actionType: "role",
            entity: "Nguyễn Văn An",
            beforeVal: "SALES_REP",
            afterVal: "SALES_REP, TEAM_LEAD"
        },
        {
            time: "2026-09-23 16:45:00",
            actor: "Trần Thị Bình (Director)",
            avatar: "https://ui-avatars.com/api/?name=Tran+Thi+Binh&background=f59e0b&color=fff",
            action: "Điều chỉnh",
            actionType: "adjust",
            entity: "Chỉ tiêu Q3 - Nhóm Bắc",
            beforeVal: "500.000.000 đ",
            afterVal: "650.000.000 đ"
        },
        {
            time: "2026-09-23 14:02:11",
            actor: "Lý Minh Triết (Manager)",
            avatar: "https://ui-avatars.com/api/?name=Ly+Minh+Triet&background=10b981&color=fff",
            action: "Thêm mới",
            actionType: "create",
            entity: "Hợp đồng Cty XYZ",
            beforeVal: "-",
            afterVal: "Đã tạo mới"
        },
        {
            time: "2026-09-23 11:20:00",
            actor: "Nông Quang Tiệp (Admin)",
            avatar: "https://ui-avatars.com/api/?name=Nong+Quang+Tiep&background=2563eb&color=fff",
            action: "Xóa",
            actionType: "delete",
            entity: "Cấu hình Server #SVR-4",
            beforeVal: "Kích hoạt",
            afterVal: "-"
        }
    ];

    function renderAuditTable(items) {
        if (!items || items.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; padding: 24px; color: #94a3b8;">Không có dữ liệu nhật ký phù hợp.</td></tr>`;
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td style="color: #64748b;">${item.time}</td>
                <td>
                    <div class="actor-cell">
                        <img src="${item.avatar}" alt="${item.actor}" class="actor-avatar">
                        <span class="actor-name">${item.actor}</span>
                    </div>
                </td>
                <td>
                    <span class="badge-action badge-${item.actionType}">
                        ${item.action}
                    </span>
                </td>
                <td style="font-weight: 500;">${item.entity}</td>
                <td>
                    ${item.beforeVal !== '-' 
                        ? `<span class="val-pill val-before-pill">${item.beforeVal}</span>` 
                        : `<span class="val-null">-</span>`}
                </td>
                <td>
                    ${item.afterVal !== '-' 
                        ? `<span class="val-pill val-after-pill">${item.afterVal}</span>` 
                        : `<span class="val-null">-</span>`}
                </td>
            </tr>
        `).join("");
    }

    async function fetchAuditLogs() {
        const user = document.getElementById("filter-user").value;
        const action = document.getElementById("filter-action").value;
        const keyword = document.getElementById("filter-keyword").value.toLowerCase();

        try {
            const response = await fetch(`${API.AUDIT.LIST}?page=1&size=10`, {
                method: "GET",
                credentials: "include",
                headers: { "Accept": "application/json" }
            });
            const result = await response.json();
            if (result.success && result.data && result.data.items) {
                renderAuditTable(result.data.items);
                return;
            }
        } catch (e) {
            console.warn("Chưa kết nối Backend Jakarta Servlet. Dùng dữ liệu Mock hiển thị:", e);
        }

        let filtered = mockAuditList;
        if (user) {
            filtered = filtered.filter(x => x.actor.includes(user));
        }
        if (action) {
            filtered = filtered.filter(x => x.action === action);
        }
        if (keyword) {
            filtered = filtered.filter(x => 
                x.entity.toLowerCase().includes(keyword) || 
                x.actor.toLowerCase().includes(keyword)
            );
        }
        renderAuditTable(filtered);
    }

    filterForm.addEventListener("submit", (e) => {
        e.preventDefault();
        fetchAuditLogs();
    });

    btnReset.addEventListener("click", () => {
        filterForm.reset();
        fetchAuditLogs();
    });

    btnExport.addEventListener("click", () => {
        alert("Đang chuẩn bị xuất dữ liệu nhật ký ra tệp Excel (.xlsx)...");
    });

    fetchAuditLogs();
});