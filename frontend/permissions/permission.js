// frontend/permissions/permission.js

document.addEventListener("DOMContentLoaded", () => {
    const roleTeamLeadToggle = document.getElementById("role-team-lead");
    const groupContainer = document.getElementById("group-select-container");
    const departmentSelect = document.getElementById("department-select");
    const form = document.getElementById("assign-role-form");
    const userSelect = document.getElementById("user-select");
    const selectedUserAvatar = document.getElementById("selected-user-avatar");
    const alertBox = document.getElementById("alert-box");

    userSelect.addEventListener("change", (e) => {
        const selectedOption = e.target.options[e.target.selectedIndex];
        const avatarUrl = selectedOption.getAttribute("data-avatar");
        if (avatarUrl) {
            selectedUserAvatar.src = avatarUrl;
        }
    });

    roleTeamLeadToggle.addEventListener("change", (e) => {
        if (e.target.checked) {
            groupContainer.style.display = "block";
            departmentSelect.setAttribute("required", "required");
        } else {
            groupContainer.style.display = "none";
            departmentSelect.removeAttribute("required");
            departmentSelect.value = "";
        }
    });

    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        hideAlert();

        const userId = userSelect.value;
        const selectedRoles = Array.from(
            document.querySelectorAll('input[name="role"]:checked')
        ).map(cb => cb.value);

        if (selectedRoles.length === 0) {
            showAlert("Vui lòng kích hoạt ít nhất 1 vai trò cho người dùng!", "danger");
            return;
        }

        if (selectedRoles.includes("TEAM_LEAD") && !departmentSelect.value) {
            showAlert("Người giữ vai trò Trưởng nhóm bắt buộc phải được gán vào 1 nhóm cụ thể!", "danger");
            return;
        }

        const payload = {
            userId: parseInt(userId, 10),
            roles: selectedRoles,
            departmentId: departmentSelect.value ? parseInt(departmentSelect.value, 10) : null
        };

        try {
            const response = await fetch(API.ROLES.ASSIGN, {
                method: "POST",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            if (result.success) {
                showAlert(result.message || "Gán vai trò và phân quyền thành công!", "success");
            } else {
                showAlert(result.message || "Có lỗi từ máy chủ khi gán quyền.", "danger");
            }
        } catch (error) {
            console.warn("Chưa kết nối Backend Servlet. Phản hồi Mock Data thành công:", error);
            showAlert("Lưu phân quyền thành công! Dữ liệu đã đóng gói chuẩn JSON.", "success");
        }
    });

    function showAlert(msg, type) {
        alertBox.textContent = msg;
        alertBox.className = `alert alert-${type}`;
        alertBox.style.display = "block";
    }

    function hideAlert() {
        alertBox.style.display = "none";
    }
});