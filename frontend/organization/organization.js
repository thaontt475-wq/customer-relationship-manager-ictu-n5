document.addEventListener("DOMContentLoaded", function () {
    loadOrganizationData();
});

function loadOrganizationData() {
    
    fetch("/api/organization")
        .then(response => {
            if (!response.ok) throw new Error("Không thể kết nối API Backend");
            return response.json();
        })
        .then(data => {
            renderOrganizationTree(data);
        })
        .catch(error => {
            console.error("Lỗi:", error);
            document.getElementById("orgTree").innerHTML = "<p style='color: red;'>Lỗi tải dữ liệu phòng ban từ server.</p>";
        });
}

function renderOrganizationTree(data) {
    const container = document.getElementById("orgTree");
    container.innerHTML = "";

    if (!data || data.length === 0) {
        container.innerHTML = "<p>Chưa có dữ liệu phòng ban.</p>";
        return;
    }

    let html = "<ul>";
    data.forEach(item => {
        html += `<li><strong>${item.name}</strong> - Trưởng nhóm: ${item.leaderName || 'Chưa phân công'}</li>`;
    });
    html += "</ul>";

    container.innerHTML = html;
}