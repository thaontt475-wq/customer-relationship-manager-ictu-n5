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

    const symbols = {
        success: '<path d="m5 12 4 4L19 6"/>',
        error: '<circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/>',
        warning: '<path d="m12 3 10 18H2L12 3Z"/><path d="M12 9v5m0 3v1"/>',
        info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6m0-10v1"/>'
    };

    // Display-only API: it never writes storage, submits forms or assumes success.
    window.crmToast = (message, type = "info", options = {}) => {
        if (!String(message ?? "").trim()) return null;
        if (!Object.hasOwn(symbols, type)) type = "info";
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
        icon.innerHTML = `<svg viewBox="0 0 24 24" aria-hidden="true">${symbols[type]}</svg>`;
        const copy = document.createElement("span");
        copy.className = "crm-toast-copy";
        copy.textContent = String(message);
        const close = document.createElement("button");
        close.type = "button";
        close.className = "crm-toast-close";
        close.setAttribute("aria-label", "Đóng thông báo");
        close.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18"/></svg>';
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
    const paths = {
    "▦": "<rect x=\"3\" y=\"3\" width=\"7\" height=\"7\" rx=\"1\"/><rect x=\"14\" y=\"3\" width=\"7\" height=\"7\" rx=\"1\"/><rect x=\"3\" y=\"14\" width=\"7\" height=\"7\" rx=\"1\"/><rect x=\"14\" y=\"14\" width=\"7\" height=\"7\" rx=\"1\"/>",
    "♙": "<circle cx=\"12\" cy=\"8\" r=\"4\"/><path d=\"M4 21c0-4 3.6-7 8-7s8 3 8 7\"/>",
    "◇": "<path d=\"m12 3 8 9-8 9-8-9 8-9Z\"/>",
    "☑": "<rect x=\"3\" y=\"3\" width=\"18\" height=\"18\" rx=\"3\"/><path d=\"m7 12 3 3 7-7\"/>",
    "✓": "<path d=\"m5 12 4 4L19 6\"/>",
    "□": "<rect x=\"3\" y=\"5\" width=\"18\" height=\"16\" rx=\"2\"/><path d=\"M8 3v4m8-4v4M3 10h18\"/>",
    "✉": "<rect x=\"3\" y=\"5\" width=\"18\" height=\"14\" rx=\"2\"/><path d=\"m3 6 9 7 9-7\"/>",
    "☎": "<path d=\"M5 3h4l2 5-3 2c2 3 3 4 6 6l2-3 5 2v4c0 2-2 3-4 2C9 19 5 15 3 7 2 5 3 3 5 3Z\"/>",
    "✎": "<path d=\"m16 3 5 5-12 12-6 1 1-6L16 3ZM14 5l5 5\"/>",
    "☰": "<path d=\"M4 6h16M4 12h16M4 18h16\"/>",
    "🔒": "<rect x=\"5\" y=\"10\" width=\"14\" height=\"11\" rx=\"2\"/><path d=\"M8 10V7a4 4 0 0 1 8 0v3m-4 4v3\"/>",
    "⚠": "<path d=\"m12 3 10 18H2L12 3Z\"/><path d=\"M12 9v5m0 3v1\"/>",
    "🎉": "<path d=\"m4 20 4-12 8 8L4 20Zm7-15 1-2m5 7 4-1m-6-3 3-3\"/>",
    "😞": "<circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M8 9h1m6 0h1m-8 7c2-3 6-3 8 0\"/>",
    "◉": "<path d=\"M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z\"/><circle cx=\"12\" cy=\"12\" r=\"3\"/>",
    "⊘": "<path d=\"M3 3l18 18M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z\"/>",
    "○": "<circle cx=\"12\" cy=\"12\" r=\"8\"/>",
    "⌂": "<path d=\"m3 10 9-7 9 7v11H3V10Zm6 11v-7h6v7\"/>",
    "◈": "<path d=\"m12 3 8 4-8 4-8-4 8-4ZM4 7v10l8 4 8-4V7M12 11v10\"/>",
    "⇧": "<path d=\"M12 16V3m-5 5 5-5 5 5M4 16v5h16v-5\"/>",
    "☷": "<path d=\"M8 6h13M8 12h13M8 18h13M3 6h1M3 12h1M3 18h1\"/>",
    "⊞": "<rect x=\"3\" y=\"3\" width=\"18\" height=\"18\" rx=\"2\"/><path d=\"M12 3v18M3 12h18\"/>",
    "◎": "<circle cx=\"12\" cy=\"12\" r=\"9\"/><circle cx=\"12\" cy=\"12\" r=\"4\"/>",
    "⌘": "<rect x=\"9\" y=\"3\" width=\"6\" height=\"5\"/><rect x=\"3\" y=\"16\" width=\"6\" height=\"5\"/><rect x=\"15\" y=\"16\" width=\"6\" height=\"5\"/><path d=\"M12 8v4M6 16v-4h12v4\"/>",
    "⚙": "<circle cx=\"12\" cy=\"12\" r=\"3\"/><path d=\"m9 3 6 0 1 4 4 1 1 6-4 2-1 4-6 1-2-4-4-1-1-6 4-2 1-4Z\"/>",
    "×": "<path d=\"m6 6 12 12M18 6 6 18\"/>",
    "‹": "<path d=\"m15 5-7 7 7 7\"/>",
    "›": "<path d=\"m9 5 7 7-7 7\"/>",
    "⌄": "<path d=\"m6 9 6 6 6-6\"/>",
    "↻": "<path d=\"M20 7v5h-5M20 12a8 8 0 1 0-2 5\"/>",
    "♧": "<path d=\"M18 8a6 6 0 1 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4\"/>"
};
    paths["☹️"] = paths["😞"];
    paths["▣"] = paths["□"];
    paths["⋮"] = '<circle cx="12" cy="5" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="12" cy="19" r="1"/>';
    const slots = ".crm-mobile-menu, .crm-user-arrow, .recent-icon, .activity-icon, .org-node-icon, .task-check, .deal-emoji, .password-toggle, .password-rule, .mobile-card-contact > span, .timeline-type, .row-action-button, .card-menu-btn, .card-menu button";
    function normalize(scope) {
        const elements = [...scope.querySelectorAll(slots)];
        if (scope.matches?.(slots)) elements.unshift(scope);
        for (const element of elements) {
            for (const text of [...element.childNodes]) {
                if (text.nodeType !== Node.TEXT_NODE) continue;
                const value = text.textContent.trim();
                const glyph = Object.keys(paths).find(key => value.startsWith(key));
                if (!glyph) continue;
                const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svg.setAttribute("viewBox", "0 0 24 24");
                svg.setAttribute("class", "crm-inline-icon");
                svg.setAttribute("aria-hidden", "true");
                svg.innerHTML = paths[glyph];
                text.replaceWith(svg, document.createTextNode(value.slice(glyph.length) ? " " + value.slice(glyph.length) : ""));
            }
        }
    }
    normalize(document);
    new MutationObserver(records => {
        const scopes = new Set();
        for (const record of records) {
            if (record.target instanceof Element && record.target.matches(slots)) scopes.add(record.target);
            for (const node of record.addedNodes) if (node instanceof Element && !node.closest("svg")) scopes.add(node);
        }
        for (const scope of scopes) normalize(scope);
    }).observe(document.body, {childList: true, subtree: true});
})();
