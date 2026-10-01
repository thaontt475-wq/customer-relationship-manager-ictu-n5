/* Keyboard and focus management only; no API or business validation changes. */
document.addEventListener('DOMContentLoaded', function () {
    var dialogs = Array.from(document.querySelectorAll('.users-admin .user-modal-overlay'));
    var current = null;
    var previousFocus = null;
    var background = [];
    function synchronize() {
        var next = dialogs.find(function (dialog) { return dialog.classList.contains('is-open'); });
        dialogs.forEach(function (dialog) { dialog.setAttribute('aria-hidden', dialog === next ? 'false' : 'true'); });
        if (next === current) return;
        if (next) {
            if (!current) {
                previousFocus = document.activeElement;
                if (next.contains(previousFocus)) previousFocus = window.usersAdminDialogTrigger || document.getElementById('btnOpenCreateModal');
                background = Array.from(document.querySelectorAll('.crm-header, .crm-main-layout')).map(function (node) { var item = { node: node, inert: node.inert }; node.inert = true; return item; });
                document.body.classList.add('users-dialog-open');
            }
            current = next;
            var focus = next.querySelector('input:not([type="hidden"]):not(:disabled), button:not(:disabled)');
            if (focus && !next.contains(document.activeElement)) focus.focus();
        } else {
            current = null;
            background.forEach(function (item) { item.node.inert = item.inert; });
            background = [];
            document.body.classList.remove('users-dialog-open');
            if (previousFocus && previousFocus.isConnected) previousFocus.focus();
        }
    }
    document.addEventListener('click', function (event) {
        var trigger = event.target.closest('#btnOpenCreateModal, .btn-edit-user, .btn-assign-roles, .btn-delete-user');
        if (trigger) window.usersAdminDialogTrigger = trigger;
    }, true);
    dialogs.forEach(function (dialog) { new MutationObserver(synchronize).observe(dialog, { attributes: true, attributeFilter: ['class'] }); });
    document.addEventListener('keydown', function (event) {
        if (!current) return;
        var candidates = Array.from(current.querySelectorAll('a[href], button:not(:disabled), input:not([type="hidden"]):not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex="0"]')).filter(function (node) { return node.getClientRects().length > 0; });
        if (event.key === 'Escape') {
            var busy = current.querySelector('button[type="submit"]:disabled, #btnConfirmDelete:disabled');
            if (!busy) current.classList.remove('is-open');
            event.preventDefault();
        } else if (event.key === 'Tab' && candidates.length) {
            var first = candidates[0], last = candidates[candidates.length - 1];
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
            else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
        }
    });
    synchronize();
});
