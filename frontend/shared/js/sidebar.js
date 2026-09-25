document.addEventListener("DOMContentLoaded", function () {
    const navLinks = document.querySelectorAll(".sidebar a");
    const currentPath = window.location.pathname;

    navLinks.forEach(link => {
        if (link.getAttribute("href") && currentPath.includes(link.getAttribute("href"))) {
            link.classList.add("active");
        }
    });
});

// Điều hướng sang Dashboard theo yêu cầu reviewer
function redirectToDashboard() {
    window.location.href = "../dashboard/index.html";
}