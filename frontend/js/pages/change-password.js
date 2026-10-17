"use strict";

const API_BASE =
    `${window.location.protocol}//${window.location.hostname || "localhost"}:8080/crm`;

const form =
    document.getElementById(
        "changePasswordForm"
    );

const currentPassword =
    document.getElementById(
        "currentPassword"
    );

const newPassword =
    document.getElementById(
        "newPassword"
    );

const confirmPassword =
    document.getElementById(
        "confirmPassword"
    );

const passwordError =
    document.getElementById(
        "passwordError"
    );

const passwordMessage =
    document.getElementById(
        "passwordMessage"
    );

const confirmMatchBadge =
    document.getElementById(
        "confirmMatchBadge"
    );

const strengthLabel =
    document.getElementById(
        "strengthLabel"
    );

const strengthSegments = [
    document.getElementById("strengthSeg1"),
    document.getElementById("strengthSeg2"),
    document.getElementById("strengthSeg3"),
    document.getElementById("strengthSeg4")
];


/* =========================================================
   SHOW / HIDE PASSWORD WITH ANIMATED ICON
========================================================= */

document
    .querySelectorAll(
        "[data-password-toggle]"
    )
    .forEach(button => {

        button.addEventListener(
            "click",
            () => {

                const targetId =
                    button.dataset.passwordToggle;

                const input =
                    document.getElementById(targetId);

                if (!input) {
                    return;
                }

                const isPassword =
                    input.type === "password";

                input.type =
                    isPassword ? "text" : "password";

                const icon =
                    button.querySelector("i");

                if (icon) {
                    if (isPassword) {
                        icon.classList.remove("fa-eye");
                        icon.classList.add("fa-eye-slash");
                    } else {
                        icon.classList.remove("fa-eye-slash");
                        icon.classList.add("fa-eye");
                    }
                }

                button.setAttribute(
                    "aria-label",
                    isPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"
                );
            }
        );
    });


/* =========================================================
   PASSWORD RULES & REAL-TIME STRENGTH METER
========================================================= */

newPassword?.addEventListener(
    "input",
    () => {
        updateRules();
        checkPasswordMatch();
    }
);

confirmPassword?.addEventListener(
    "input",
    checkPasswordMatch
);


function updateRules() {

    if (!newPassword) {
        return;
    }

    const value =
        newPassword.value || "";

    const hasLength =
        value.length >= 8;

    const hasCase =
        /[A-Z]/.test(value) && /[a-z]/.test(value);

    const hasNumber =
        /\d/.test(value);

    const hasSpecial =
        /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(value) || value.length >= 12;

    updateRule(
        "ruleLength",
        hasLength,
        "Ít nhất 8 ký tự"
    );

    updateRule(
        "ruleLetter",
        hasCase,
        "Có chữ hoa và chữ thường"
    );

    updateRule(
        "ruleNumber",
        hasNumber,
        "Có chữ số (0-9)"
    );

    updateRule(
        "ruleSpecial",
        hasSpecial,
        "Ký tự đặc biệt hoặc từ 12 ký tự"
    );

    updateStrengthMeter(value, [hasLength, hasCase, hasNumber, hasSpecial]);
}


function updateRule(id, valid, label) {

    const element =
        document.getElementById(id);

    if (!element) {
        return;
    }

    element.classList.toggle(
        "valid",
        valid
    );

    element.innerHTML = `
        <i class="fa-solid ${valid ? "fa-circle-check" : "fa-circle-dot"}"></i>
        <span>${label}</span>
    `;
}


function updateStrengthMeter(value, checks) {

    if (!value) {
        strengthSegments.forEach(seg => {
            if (seg) seg.style.backgroundColor = "";
        });
        if (strengthLabel) {
            strengthLabel.textContent = "Chưa nhập";
            strengthLabel.style.color = "";
        }
        return;
    }

    const passedCount =
        checks.filter(Boolean).length;

    const configs = {
        1: { label: "Yếu", color: "#ef4444" },
        2: { label: "Trung bình", color: "#f59e0b" },
        3: { label: "Khá mạnh", color: "#3b82f6" },
        4: { label: "Rất an toàn", color: "#10b981" }
    };

    const currentConfig =
        configs[passedCount] || { label: "Rất yếu", color: "#ef4444" };

    strengthSegments.forEach((seg, index) => {
        if (!seg) return;
        if (index < passedCount) {
            seg.style.backgroundColor = currentConfig.color;
        } else {
            seg.style.backgroundColor = "";
        }
    });

    if (strengthLabel) {
        strengthLabel.textContent = currentConfig.label;
        strengthLabel.style.color = currentConfig.color;
    }
}


function checkPasswordMatch() {

    if (!confirmMatchBadge) {
        return;
    }

    const newPass =
        newPassword?.value || "";

    const confirmPass =
        confirmPassword?.value || "";

    if (!confirmPass) {
        confirmMatchBadge.innerHTML = "";
        confirmMatchBadge.className = "match-status-badge";
        return;
    }

    if (newPass === confirmPass) {
        confirmMatchBadge.className = "match-status-badge match";
        confirmMatchBadge.innerHTML = `<i class="fa-solid fa-circle-check"></i> Mật khẩu trùng khớp`;
    } else {
        confirmMatchBadge.className = "match-status-badge mismatch";
        confirmMatchBadge.innerHTML = `<i class="fa-solid fa-circle-exclamation"></i> Mật khẩu chưa khớp`;
    }
}


/* =========================================================
   FEEDBACK HELPERS
========================================================= */

function showError(message) {

    if (passwordError) {
        if (message) {
            passwordError.innerHTML = `<i class="fa-solid fa-circle-exclamation"></i> <span>${escapeHtml(message)}</span>`;
            passwordError.classList.add("show");
        } else {
            passwordError.innerHTML = "";
            passwordError.classList.remove("show");
        }
    }

    if (passwordMessage) {
        passwordMessage.innerHTML = "";
        passwordMessage.classList.remove("show");
    }
}


function showSuccess(message) {

    if (passwordMessage) {
        if (message) {
            passwordMessage.innerHTML = `<i class="fa-solid fa-circle-check"></i> <span>${escapeHtml(message)}</span>`;
            passwordMessage.classList.add("show");
        } else {
            passwordMessage.innerHTML = "";
            passwordMessage.classList.remove("show");
        }
    }

    if (passwordError) {
        passwordError.innerHTML = "";
        passwordError.classList.remove("show");
    }
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
}


/* =========================================================
   SUBMIT FORM
========================================================= */

form?.addEventListener(
    "submit",
    async event => {

        event.preventDefault();

        showError("");

        const current =
            currentPassword?.value || "";

        const next =
            newPassword?.value || "";

        const confirm =
            confirmPassword?.value || "";

        if (
            !current ||
            !next ||
            !confirm
        ) {

            showError(
                "Vui lòng nhập đầy đủ các trường mật khẩu bắt buộc."
            );

            return;
        }

        if (
            next.length < 8 ||
            !/[A-Z]/.test(next) ||
            !/[a-z]/.test(next) ||
            !/\d/.test(next)
        ) {

            showError(
                "Mật khẩu mới phải có ít nhất 8 ký tự, gồm cả chữ hoa, chữ thường và chữ số."
            );

            return;
        }

        if (next !== confirm) {

            showError(
                "Mật khẩu xác nhận không khớp với mật khẩu mới."
            );

            return;
        }

        if (current === next) {

            showError(
                "Mật khẩu mới phải khác với mật khẩu hiện tại."
            );

            return;
        }

        const button =
            form.querySelector(
                'button[type="submit"]'
            );

        const originalHtml =
            button?.innerHTML || "Đổi mật khẩu";

        if (button) {
            button.disabled = true;
            button.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> <span>Đang xử lý...</span>`;
        }

        try {

            const response =
                await fetch(
                    `${API_BASE}/api/auth/change-password`,
                    {
                        method: "POST",
                        credentials: "include",
                        headers: {
                            "Content-Type": "application/json",
                            "Accept": "application/json"
                        },
                        body: JSON.stringify({
                            currentPassword: current,
                            newPassword: next,
                            confirmPassword: confirm
                        })
                    }
                );

            let result = null;

            try {
                result = await response.json();
            } catch (_) {
                result = null;
            }

            if (response.status === 401) {

                showError(
                    "Phiên đăng nhập đã hết hạn. Hệ thống sẽ chuyển về trang đăng nhập..."
                );

                setTimeout(() => {
                    window.location.href = "login.html";
                }, 1200);

                return;
            }

            if (!response.ok) {

                showError(
                    result?.message ||
                    `Không thể đổi mật khẩu. Mã phản hồi: ${response.status}`
                );

                return;
            }

            if (!result?.success) {

                showError(
                    result?.message ||
                    "Không thể đổi mật khẩu. Vui lòng kiểm tra lại mật khẩu hiện tại."
                );

                return;
            }

            showSuccess(
                result.message ||
                "Đổi mật khẩu thành công! Hệ thống đang chuyển hướng đăng nhập..."
            );

            localStorage.removeItem("crm_ui_session");
            sessionStorage.clear();

            form.reset();
            updateRules();
            checkPasswordMatch();

            setTimeout(() => {
                window.location.href = "login.html";
            }, 1500);

        } catch (error) {

            console.error("Change password error:", error);

            showError(
                "Không thể kết nối đến máy chủ. Vui lòng thử lại sau."
            );

        } finally {

            if (button) {
                button.disabled = false;
                button.innerHTML = originalHtml;
            }
        }
    }
);


/* Initialize rule state on load */
updateRules();