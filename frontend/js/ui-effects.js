"use strict";

(() => {
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    const toastStates = new Map();
    let region;

    function dismiss(toast) {
        const state = toastStates.get(toast);
        if (!state) return;
        clearTimeout(state.timer);
        toastStates.delete(toast);
        toast.classList.add("closing");
        setTimeout(() => toast.remove(), reducedMotion.matches ? 0 : 200);
    }

    function pause(toast) {
        const state = toastStates.get(toast);
        if (!state || state.paused) return;
        clearTimeout(state.timer);
        state.remaining = Math.max(0, state.remaining - (performance.now() - state.started));
        state.paused = true;
        toast.classList.add("paused");
    }

    function resume(toast) {
        const state = toastStates.get(toast);
        if (!state || document.hidden || toast.matches(":hover, :focus-within")) return;
        state.paused = false;
        state.started = performance.now();
        clearTimeout(state.timer);
        state.timer = setTimeout(() => dismiss(toast), state.remaining);
        toast.classList.remove("paused");
    }

    const iconClasses = {
        success: 'fa-solid fa-circle-check',
        error: 'fa-solid fa-circle-xmark',
        warning: 'fa-solid fa-triangle-exclamation',
        info: 'fa-solid fa-circle-info'
    };

    // Display-only API: it never writes storage, submits forms or assumes success.
    window.crmToast = (message, type = "info", options = {}) => {
        if (!String(message ?? "").trim()) return null;
        if (!Object.hasOwn(iconClasses, type)) type = "info";
        if (!region) {
            region = document.createElement("section");
            region.className = "crm-toast-region";
            region.setAttribute("aria-label", "Thông báo thao tác");
            document.body.appendChild(region);
        }
        const toast = document.createElement("div");
        toast.className = `crm-toast crm-toast-${type}`;
        toast.setAttribute("role", type === "error" ? "alert" : "status");
        toast.setAttribute("aria-atomic", "true");
        const icon = document.createElement("span");
        icon.className = "crm-toast-icon";
        icon.innerHTML = `<i class="${iconClasses[type]}"></i>`;
        const copy = document.createElement("span");
        copy.className = "crm-toast-copy";
        copy.textContent = String(message);
        const close = document.createElement("button");
        close.type = "button";
        close.className = "crm-toast-close";
        close.setAttribute("aria-label", "Đóng thông báo");
        close.innerHTML = '<i class="fa-solid fa-xmark"></i>';
        const progress = document.createElement("span");
        progress.className = "crm-toast-progress";
        progress.setAttribute("aria-hidden", "true");
        const duration = Math.max(3000, Number(options.duration) || 4500);
        toast.style.setProperty("--crm-toast-duration", `${duration}ms`);
        toast.append(icon, copy, close, progress);
        region.appendChild(toast);
        toastStates.set(toast, {remaining: duration, started: performance.now(), paused: false});
        resume(toast);
        if (document.hidden) pause(toast);
        return {close: () => dismiss(toast)};
    };

    document.addEventListener("click", event => {
        const close = event.target.closest?.(".crm-toast-close");
        if (close) dismiss(close.closest(".crm-toast"));
    });
    for (const type of ["pointerover", "focusin"]) {
        document.addEventListener(type, event => {
            const toast = event.target.closest?.(".crm-toast");
            if (toast) pause(toast);
        });
    }
    for (const type of ["pointerout", "focusout"]) {
        document.addEventListener(type, event => {
            const toast = event.target.closest?.(".crm-toast");
            if (toast) requestAnimationFrame(() => resume(toast));
        });
    }
    document.addEventListener("visibilitychange", () => {
        for (const toast of toastStates.keys()) document.hidden ? pause(toast) : resume(toast);
    });

    // A fixed, pointer-transparent highlight avoids changing button layout/overflow.
    document.addEventListener("pointerdown", event => {
        if (reducedMotion.matches || event.button !== 0 || !event.isPrimary) return;
        const button = event.target.closest?.("button, a.crm-btn");
        if (!button || button.disabled || button.getAttribute("aria-disabled") === "true") return;
        const box = button.getBoundingClientRect();
        const effect = document.createElement("span");
        effect.className = "crm-click-highlight";
        Object.assign(effect.style, {
            left: `${box.left}px`, top: `${box.top}px`, width: `${box.width}px`, height: `${box.height}px`,
            borderRadius: getComputedStyle(button).borderRadius
        });
        const wave = document.createElement("span");
        const size = Math.hypot(box.width, box.height) * 2;
        Object.assign(wave.style, {
            left: `${event.clientX - box.left}px`, top: `${event.clientY - box.top}px`,
            width: `${size}px`, height: `${size}px`
        });
        effect.appendChild(wave);
        document.body.appendChild(effect);
        setTimeout(() => effect.remove(), 420);
    }, {capture: true, passive: true});

    // This existing message is assigned only after localStorage.setItem succeeds.
    // Observe the UI result; do not patch Storage or attach another submit handler.
    const savedMessage = document.getElementById("profileMessage");
    if (savedMessage) {
        new MutationObserver(() => {
            const message = savedMessage.textContent.trim();
            if (message) window.crmToast(message, "success");
        }).observe(savedMessage, {childList: true, characterData: true, subtree: true});
    }
})();

// Normalize dynamic icon slots only; never inspect customer/user content.
(() => {
    const glyphIcons = {
        "▦": "fa-solid fa-chart-pie",
        "♙": "fa-solid fa-users",
        "◇": "fa-solid fa-chart-line",
        "▤": "fa-solid fa-file-signature",
        "📄": "fa-solid fa-file-signature",
        "☑": "fa-solid fa-circle-check",
        "✓": "fa-solid fa-check",
        "□": "fa-regular fa-square",
        "▣": "fa-solid fa-square-check",
        "✉": "fa-solid fa-envelope",
        "☎": "fa-solid fa-phone",
        "✎": "fa-solid fa-pen-to-square",
        "☰": "fa-solid fa-bars",
        "🔒": "fa-solid fa-lock",
        "⚠": "fa-solid fa-triangle-exclamation",
        "🎉": "fa-solid fa-trophy",
        "😞": "fa-regular fa-face-frown",
        "☹️": "fa-regular fa-face-frown",
        "◉": "fa-solid fa-eye",
        "⊘": "fa-solid fa-ban",
        "○": "fa-regular fa-circle",
        "⌂": "fa-solid fa-house",
        "◈": "fa-solid fa-box",
        "⇧": "fa-solid fa-arrow-up-from-bracket",
        "☷": "fa-solid fa-list-check",
        "⊞": "fa-solid fa-border-all",
        "◎": "fa-solid fa-bullseye",
        "⌘": "fa-solid fa-building",
        "⚙": "fa-solid fa-gear",
        "×": "fa-solid fa-xmark",
        "‹": "fa-solid fa-angle-left",
        "›": "fa-solid fa-angle-right",
        "⌄": "fa-solid fa-angle-down",
        "↻": "fa-solid fa-rotate-right",
        "♧": "fa-solid fa-bell",
        "⋮": "fa-solid fa-ellipsis-vertical",
        "✍️": "fa-solid fa-signature",
        "✨": "fa-solid fa-bullseye",
        "⭐": "fa-solid fa-id-badge",
        "👤": "fa-solid fa-user",
        "💼": "fa-solid fa-briefcase",
        "🏢": "fa-solid fa-building",
        "✕": "fa-solid fa-xmark"
    };

    const slots = ".crm-nav-icon, .crm-mobile-menu, .crm-user-arrow, .recent-icon, .activity-icon, .org-node-icon, .task-check, .deal-emoji, .password-toggle, .password-rule, .mobile-card-contact > span, .timeline-type, .row-action-button, .card-menu-btn, .card-menu button";
    function normalize(scope) {
        const elements = [...scope.querySelectorAll(slots)];
        if (scope.matches?.(slots)) elements.unshift(scope);
        for (const element of elements) {
            for (const text of [...element.childNodes]) {
                if (text.nodeType !== Node.TEXT_NODE) continue;
                const value = text.textContent.trim();
                const glyph = Object.keys(glyphIcons).find(key => value.startsWith(key));
                if (!glyph) continue;
                const iconElem = document.createElement("i");
                iconElem.className = glyphIcons[glyph];
                iconElem.setAttribute("aria-hidden", "true");
                const remaining = value.slice(glyph.length).trim();
                text.replaceWith(iconElem, remaining ? document.createTextNode(" " + remaining) : document.createTextNode(""));
            }
        }
    }
    normalize(document);
    new MutationObserver(records => {
        const scopes = new Set();
        for (const record of records) {
            if (record.target instanceof Element && record.target.matches(slots)) scopes.add(record.target);
            for (const node of record.addedNodes) if (node instanceof Element && !node.closest("i")) scopes.add(node);
        }
        for (const scope of scopes) normalize(scope);
    }).observe(document.body, {childList: true, subtree: true});
})();
