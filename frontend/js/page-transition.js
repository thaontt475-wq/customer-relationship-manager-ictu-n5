"use strict";

/**
 * ===================================================================
 * CRM SPA ENGINE & TRANSITION SHIELD
 * - Chuyển chức năng tức thì, KHÔNG load lại trang web
 * - Triệt tiêu 100% hiện tượng "giật bóng" / FOUC / bóng ma của các
 *   chức năng khác (drawer, modal, backdrop unstyled)
 * - Tự động dọn dẹp event listeners & styles của trang cũ
 * - Tải trước CSS & Script trước khi hoán đổi giao diện
 * ===================================================================
 */
(function () {

    let isNavigating = false;
    let pageCleanups = [];

    // 1. Kích hoạt khiên bảo vệ CSS ngay lập tức để tránh lộ drawer/modal
    injectSpaShield();

    // 2. Đánh dấu stylesheet ban đầu của trang hiện tại
    tagInitialPageStyles();

    // 3. Khởi tạo điều hướng SPA không reload
    setupSpaNavigation();

    // 4. Đánh dấu trạng thái sẵn sàng
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", () => {
            document.body.classList.add("crm-page-ready");
        });
    } else {
        document.body.classList.add("crm-page-ready");
    }

    // 5. Xử lý nút Back / Forward trên trình duyệt
    window.addEventListener("popstate", () => {
        loadPage(window.location.href, false);
    });


    /* =========================================================
       1. KHIÊN BẢO VỆ GIAO DIỆN (SHIELD)
       Đảm bảo drawer, modal, overlay của các chức năng KHÔNG BAO GIỜ
       bị giật bóng hay lộ ra trước khi mở
    ========================================================= */
    function injectSpaShield() {
        if (document.getElementById("crm-spa-shield")) return;

        const style = document.createElement("style");
        style.id = "crm-spa-shield";
        style.textContent = `
            /* Triệt tiêu 100% bóng ma / FOUC của drawer, modal, backdrop khi chuyển trang */
            .customer-drawer:not(.open),
            .opportunity-drawer:not(.open),
            .user-drawer:not(.open),
            .activity-drawer:not(.open),
            .crm-drawer:not(.open),
            .crm-custom-drawer:not(.open) {
                position: fixed !important;
                top: 0 !important;
                right: 0 !important;
                bottom: 0 !important;
                transform: translateX(105%) !important;
                visibility: hidden !important;
                pointer-events: none !important;
            }

            .customer-drawer-overlay:not(.open),
            .close-deal-overlay:not(.open),
            .opportunity-overlay:not(.open),
            .deal-backdrop:not(.open),
            .lock-overlay:not(.open),
            .task-overlay:not(.open),
            .calendar-overlay:not(.open),
            .crm-modal-backdrop:not(.open) {
                position: fixed !important;
                inset: 0 !important;
                opacity: 0 !important;
                visibility: hidden !important;
                pointer-events: none !important;
            }

            .close-deal-modal:not(.open),
            .deal-modal:not(.open),
            .lock-modal:not(.open),
            .task-modal:not(.open),
            .calendar-modal:not(.open),
            .item-modal:not(.open),
            .company-modal:not(.open),
            .crm-modal:not(.open),
            [data-drawer]:not(.open),
            [data-modal]:not(.open) {
                position: fixed !important;
                top: 50% !important;
                left: 50% !important;
                transform: translate(-50%, -50%) scale(0.95) !important;
                opacity: 0 !important;
                visibility: hidden !important;
                pointer-events: none !important;
            }

            /* Hiệu ứng chuyển cảnh vi mô (micro cross-fade) cực mượt */
            .crm-main {
                transition: opacity 100ms cubic-bezier(0.4, 0, 0.2, 1);
                will-change: opacity;
            }
            .crm-main.spa-swapping {
                opacity: 0 !important;
                pointer-events: none !important;
            }
        `;
        document.head.appendChild(style);
    }


    /* =========================================================
       2. ĐÁNH DẤU STYLESHEET KHỞI ĐẦU
    ========================================================= */
    function tagInitialPageStyles() {
        document.querySelectorAll('link[rel="stylesheet"]').forEach(link => {
            const href = link.getAttribute("href") || "";
            if (href.includes("pages/")) {
                link.setAttribute("data-page-css", "active");
            }
        });
    }


    /* =========================================================
       3. BẮT SỰ KIỆN CLICK ĐIỀU HƯỚNG SPA
    ========================================================= */
    function setupSpaNavigation() {
        document.addEventListener(
            "click",
            event => {
                const link = event.target.closest("a[href]");
                if (
                    !link
                    || event.button !== 0
                    || link.hasAttribute("download")
                ) {
                    return;
                }

                const href = link.getAttribute("href");
                if (
                    !href
                    || href === "#"
                    || href.startsWith("javascript:")
                    || href.startsWith("mailto:")
                    || href.startsWith("tel:")
                    || (link.target && link.target !== "_self")
                    || event.ctrlKey
                    || event.metaKey
                    || event.shiftKey
                    || event.altKey
                ) {
                    return;
                }

                let url;
                try {
                    url = new URL(href, window.location.href);
                } catch (_) {
                    return;
                }

                // Chỉ xử lý link nội bộ cùng host
                if (url.origin !== window.location.origin) {
                    return;
                }

                // Nếu là anchor link trên cùng trang -> để trình duyệt cuộn
                if (
                    url.pathname === window.location.pathname
                    && url.search === window.location.search
                    && url.hash
                ) {
                    return;
                }

                // Không chặn các trang xác thực (login, forgot, reset)
                if (
                    url.pathname.endsWith("login.html")
                    || url.pathname.endsWith("forgot-password.html")
                    || url.pathname.endsWith("reset-password.html")
                ) {
                    return;
                }

                // Bấm vào chính trang đang đứng: không cần tải lại
                if (
                    url.pathname === window.location.pathname
                    && url.search === window.location.search
                ) {
                    event.preventDefault();
                    event.stopPropagation();
                    return;
                }

                // CHẶN HOÀN TOÀN TẢI LẠI TRANG
                event.preventDefault();
                event.stopPropagation();

                // Thực hiện hoán đổi nội dung mượt mà
                loadPage(url.href, true);
            },
            true
        );
    }


    /* =========================================================
       4. TIẾN TRÌNH TẢI VÀ CHUYỂN ĐỔI CHỨC NĂNG
    ========================================================= */
    async function loadPage(targetUrl, pushState = true) {
        if (isNavigating) return;
        isNavigating = true;

        showProgressBar();

        try {
            // Tải trước mã HTML của trang mục tiêu
            const response = await fetch(targetUrl, { cache: "no-cache" });
            if (!response.ok) {
                console.warn("Không thể tải trang:", targetUrl, response.status);
                window.location.href = targetUrl;
                return;
            }

            const html = await response.text();
            const parser = new DOMParser();
            const newDoc = parser.parseFromString(html, "text/html");

            // Tìm script chức năng tương ứng
            const pageScriptTag = Array.from(newDoc.querySelectorAll("script[src]")).find(s => {
                const src = s.getAttribute("src") || "";
                return src.includes("pages/");
            });

            let scriptCodePromise = Promise.resolve(null);
            if (pageScriptTag) {
                const scriptUrl = new URL(pageScriptTag.getAttribute("src"), targetUrl).href;
                scriptCodePromise = fetch(scriptUrl, { cache: "no-cache" })
                    .then(r => r.ok ? r.text() : null)
                    .catch(err => {
                        console.warn("Không thể tải script:", scriptUrl, err);
                        return null;
                    });
            }

            // Tải trước CSS và Script ĐỒNG THỜI trước khi đụng vào DOM
            const [commitStyles, scriptCode] = await Promise.all([
                preparePageStyles(newDoc),
                scriptCodePromise
            ]);

            // Mờ nhẹ vùng làm việc hiện tại
            const currentMain = document.querySelector("main.crm-main");
            if (currentMain) {
                currentMain.classList.add("spa-swapping");
            }

            // Chờ 1 frame ngắn cho class mờ áp dụng
            await new Promise(r => requestAnimationFrame(r));

            // A. DỌN DẸP TRANG CŨ (loại bỏ event listeners & đóng các drawer đang mở)
            runPageCleanups();

            // B. KÍCH HOẠT CSS MỚI, GỠ BỎ CSS CŨ
            commitStyles();

            // C. CẬP NHẬT TIÊU ĐỀ VÀ BODY CLASS
            document.title = newDoc.title;
            if (newDoc.body && newDoc.body.className) {
                document.body.className = newDoc.body.className;
            }

            // D. HOÁN ĐỔI NỘI DUNG (chỉ đổi main và drawer trang, giữ nguyên sidebar/topbar)
            replacePageContent(newDoc);

            // E. CẬP NHẬT URL TRÌNH DUYỆT (không reload)
            if (pushState) {
                history.pushState({ path: targetUrl }, newDoc.title, targetUrl);
            }

            // F. CẬP NHẬT ACTIVE MENU
            updateActiveNavigation(targetUrl);

            // G. CUỘN LÊN ĐẦU
            window.scrollTo(0, 0);

            // H. THỰC THI SCRIPT CHỨC NĂNG VỚI HỆ THỐNG GHI NHẬN CLEANUP
            if (scriptCode) {
                executePageScriptCode(scriptCode);
            }

            // I. ĐỒNG BỘ AVATAR & MOBILE DRAWER NẾU CÓ
            if (typeof window.crmUpdateGlobalAvatar === "function") {
                try { window.crmUpdateGlobalAvatar(); } catch (_) {}
            }
            if (typeof setupMobileDrawer === "function") {
                try { setupMobileDrawer(); } catch (_) {}
            }

            // J. HIỆN NỘI DUNG MỚI MƯỢT MÀ (kết thúc mờ)
            const newMain = document.querySelector("main.crm-main");
            if (newMain) {
                requestAnimationFrame(() => {
                    requestAnimationFrame(() => {
                        newMain.classList.remove("spa-swapping");
                    });
                });
            }

        } catch (error) {
            console.error("Lỗi khi chuyển chức năng:", error);
            window.location.href = targetUrl;
        } finally {
            hideProgressBar();
            isNavigating = false;
        }
    }


    /* =========================================================
       5. CHUẨN BỊ VÀ TẢI TRƯỚC CSS TRANG MỤC TIÊU
    ========================================================= */
    async function preparePageStyles(newDoc) {
        const targetLinks = Array.from(newDoc.querySelectorAll('link[rel="stylesheet"]')).filter(link => {
            const href = link.getAttribute("href") || "";
            return href.includes("pages/");
        });

        const targetHrefs = targetLinks.map(l => l.href);
        const loadPromises = [];

        targetLinks.forEach(link => {
            const existing = Array.from(document.querySelectorAll('link[rel="stylesheet"]')).find(
                el => el.href === link.href
            );

            if (!existing) {
                const newLink = document.createElement("link");
                newLink.rel = "stylesheet";
                newLink.href = link.href;
                newLink.setAttribute("data-page-css", "pending");

                const promise = new Promise(resolve => {
                    newLink.onload = resolve;
                    newLink.onerror = resolve;
                    // Timeout dự phòng
                    setTimeout(resolve, 350);
                });

                loadPromises.push(promise);
                document.head.appendChild(newLink);
            }
        });

        if (loadPromises.length > 0) {
            await Promise.all(loadPromises);
        }

        // Trả về hàm commit để hoán đổi CSS nguyên tử
        return function commitStyles() {
            // Gỡ tất cả stylesheet của các chức năng khác (bao gồm cả trang khởi đầu)
            document.querySelectorAll('link[rel="stylesheet"]').forEach(el => {
                const href = el.getAttribute("href") || "";
                if (href.includes("pages/") && !targetHrefs.includes(el.href)) {
                    el.remove();
                }
            });

            // Kích hoạt stylesheet trang mới
            document.querySelectorAll('link[rel="stylesheet"][data-page-css="pending"]').forEach(el => {
                el.setAttribute("data-page-css", "active");
            });
        };
    }


    /* =========================================================
       6. DỌN DẸP SỰ KIỆN VÀ GIAO DIỆN TRANG CŨ
    ========================================================= */
    function addPageCleanup(fn) {
        pageCleanups.push(fn);
    }

    function runPageCleanups() {
        // Hủy đăng ký tất cả sự kiện đã ghi nhận
        while (pageCleanups.length > 0) {
            const fn = pageCleanups.pop();
            try { fn(); } catch (_) {}
        }

        // Đóng toàn bộ drawer, modal, dropdown đang mở
        document.querySelectorAll(".open").forEach(el => el.classList.remove("open"));
        document.body.classList.remove("drawer-open", "modal-open", "sidebar-open");
    }


    /* =========================================================
       7. PHÂN LOẠI PHẦN TỬ KHUNG HỆ THỐNG (SHELL)
    ========================================================= */
    function isShellElement(el) {
        if (!el || el.nodeType !== 1) return false;
        return el.matches(
            "aside.crm-sidebar, " +
            "header.crm-topbar, " +
            "header.crm-mobile-header, " +
            "#drawerOverlay, .crm-drawer-overlay, " +
            "#mobileDrawer, aside.crm-mobile-drawer, " +
            "nav.crm-bottom-nav, " +
            ".crm-toast-region, " +
            "#spa-progress-bar, " +
            "#crm-spa-shield, " +
            "script"
        );
    }


    /* =========================================================
       8. HOÁN ĐỔI NỘI DUNG DOM CHỨC NĂNG
    ========================================================= */
    function replacePageContent(newDoc) {
        // Xóa toàn bộ phần tử thuộc chức năng cũ (main, drawer, modal, overlay...)
        Array.from(document.body.children).forEach(child => {
            if (!isShellElement(child)) {
                child.remove();
            }
        });

        // Lấy các phần tử thuộc chức năng mới từ tài liệu tải về
        const newPageElements = [];
        Array.from(newDoc.body.children).forEach(child => {
            if (!isShellElement(child)) {
                newPageElements.push(document.importNode(child, true));
            }
        });

        // Tìm vị trí chèn chính xác: ngay trước mobile-header hoặc bottom-nav
        const anchor =
            document.querySelector("header.crm-mobile-header") ||
            document.querySelector("nav.crm-bottom-nav") ||
            Array.from(document.body.children).find(el => el.tagName === "SCRIPT");

        newPageElements.forEach(el => {
            if (el.tagName === "MAIN") {
                el.classList.add("spa-swapping");
            }
            try {
                if (anchor && anchor.parentNode === document.body) {
                    document.body.insertBefore(el, anchor);
                } else {
                    document.body.appendChild(el);
                }
            } catch (_) {
                document.body.appendChild(el);
            }
        });
    }


    /* =========================================================
       9. THỰC THI SCRIPT CHỨC NĂNG VÀ THEO DÕI SỰ KIỆN
    ========================================================= */
    function executePageScriptCode(code) {
        if (!code) return;

        const originalDocAdd = document.addEventListener;
        const originalDocRemove = document.removeEventListener;
        const originalWinAdd = window.addEventListener;
        const originalWinRemove = window.removeEventListener;
        const originalSetInterval = window.setInterval;
        const originalClearInterval = window.clearInterval;

        // Ghi nhận và theo dõi tất cả event listener gắn vào document
        document.addEventListener = function (type, listener, options) {
            if (type === "DOMContentLoaded" || type === "load") {
                try { listener(); } catch (e) { console.error("DOM ready callback error:", e); }
                return;
            }
            originalDocAdd.call(document, type, listener, options);
            addPageCleanup(() => {
                try { originalDocRemove.call(document, type, listener, options); } catch (_) {}
            });
        };

        // Ghi nhận và theo dõi tất cả event listener gắn vào window
        window.addEventListener = function (type, listener, options) {
            if (type === "DOMContentLoaded" || type === "load") {
                try { listener(); } catch (e) { console.error("Window ready callback error:", e); }
                return;
            }
            originalWinAdd.call(window, type, listener, options);
            addPageCleanup(() => {
                try { originalWinRemove.call(window, type, listener, options); } catch (_) {}
            });
        };

        // Ghi nhận timers
        window.setInterval = function (handler, timeout, ...args) {
            const id = originalSetInterval.call(window, handler, timeout, ...args);
            addPageCleanup(() => {
                originalClearInterval.call(window, id);
            });
            return id;
        };

        try {
            const runner = new Function(code);
            runner();
        } catch (err) {
            console.error("Lỗi khi khởi chạy script chức năng:", err);
        } finally {
            // Khôi phục lại native listeners
            document.addEventListener = originalDocAdd;
            window.addEventListener = originalWinAdd;
            window.setInterval = originalSetInterval;
        }
    }


    /* =========================================================
       10. CẬP NHẬT TRẠNG THÁI ACTIVE TRÊN MENU
    ========================================================= */
    function updateActiveNavigation(targetUrl) {
        if (typeof setupActiveNavigation === "function") {
            try { setupActiveNavigation(); } catch (_) {}
        }

        const url = new URL(targetUrl, window.location.href);
        const filename = url.pathname.split("/").pop() || "dashboard.html";

        document.querySelectorAll(
            "aside.crm-sidebar nav a, aside.crm-mobile-drawer nav a, nav.crm-bottom-nav a, nav a[href]"
        ).forEach(item => {
            const href = item.getAttribute("href") || "";
            if (!href || href === "#") return;
            const itemFile = href.split("?")[0].split("/").pop();
            if (itemFile === filename) {
                item.classList.add("active");
            } else {
                item.classList.remove("active");
            }
        });
    }


    /* =========================================================
       11. THANH TIẾN TRÌNH TRÊN ĐẦU TRANG (PROGRESS BAR)
    ========================================================= */
    function showProgressBar() {
        let bar = document.getElementById("spa-progress-bar");
        if (!bar) {
            bar = document.createElement("div");
            bar.id = "spa-progress-bar";
            bar.style.cssText =
                "position:fixed;top:0;left:0;height:2.5px;width:0%;background:linear-gradient(90deg,#2563eb,#38bdf8);z-index:999999;transition:width 0.2s cubic-bezier(0.4,0,0.2,1),opacity 0.2s ease;pointer-events:none;box-shadow:0 0 10px rgba(56,189,248,0.7);";
            document.body.appendChild(bar);
        }

        bar.style.opacity = "1";
        bar.style.width = "40%";

        setTimeout(() => {
            if (isNavigating) {
                bar.style.width = "80%";
            }
        }, 120);
    }

    function hideProgressBar() {
        const bar = document.getElementById("spa-progress-bar");
        if (bar) {
            bar.style.width = "100%";
            setTimeout(() => {
                bar.style.opacity = "0";
                setTimeout(() => {
                    bar.style.width = "0%";
                }, 200);
            }, 120);
        }
    }

})();
