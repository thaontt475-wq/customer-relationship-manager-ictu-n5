import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { webcrypto } from 'node:crypto';
import vm from 'node:vm';

// Execute the real form script with a small DOM adapter; these are client tests,
// not evidence of persistence (which is covered by LeadCaptureJdbcTest).
const html = readFileSync(new URL('../lead-capture-form.html', import.meta.url), 'utf8');
const script = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].at(-1)[1];
function formHarness(fetchImpl, search = '?formId=00000000-0000-0000-0000-000000000001') {
    const nodes = new Map();
    const element = id => {
        if (!nodes.has(id)) nodes.set(id, {
            value: '', style: {}, handlers: {}, classList: { add() {}, remove() {} },
            addEventListener(type, handler) { this.handlers[type] = handler; }, reset() {},
            closest(selector) { return selector === '.form-group' ? { querySelector: () => null } : null; }
        });
        return nodes.get(id);
    };
    const document = { getElementById: element, querySelector: () => null,
        documentElement: { setAttribute() {}, style: { setProperty() {} } }, body: element('body') };
    const window = { location: { search, pathname: '/crm/lead-capture-form.html', origin: 'http://localhost:18080', protocol: 'http:', hostname: 'localhost' },
        addEventListener() {}, parent: { postMessage() {} } };
    vm.runInNewContext(script, { document, window, URLSearchParams, crypto: webcrypto, fetch: fetchImpl, setTimeout() {} });
    element('leadFullName').value = 'Lead QA'; element('leadPhone').value = '0901234567';
    element('leadEmail').value = 'qa@example.invalid';
    return { element, submit: () => element('leadCaptureForm').handlers.submit({ preventDefault() {} }) };
}
const success = () => ({ ok: true, json: async () => ({ success: true, data: { id: 12 } }) });
test('public form uses canonical source, actual server origin and no session cookies', async () => {
    let request;
    const h = formHarness(async (url, options) => { request = { url, ...options }; return success(); }, '?formId=00000000-0000-0000-0000-000000000001&source=EVENT');
    await h.submit();
    assert.equal(request.url, 'http://localhost:18080/crm/api/leads/web-form');
    assert.equal(request.credentials, 'omit');
    assert.equal(JSON.parse(request.body).source, 'WEB_FORM');
    assert.match(JSON.parse(request.body).requestId, /^[a-f0-9]{32}$/);
    assert.equal(h.element('successView').style.display, 'block');
});
test('double submit while awaiting the server sends one request', async () => {
    let finish, count = 0;
    const h = formHarness(() => { count++; return new Promise(resolve => { finish = resolve; }); });
    const pending = h.submit(); await h.submit();
    assert.equal(count, 1); assert.equal(h.element('btnSubmitLead').disabled, true);
    finish(success()); await pending;
    assert.equal(h.element('btnSubmitLead').disabled, false);
});
test('a failed response keeps the form visible and preserves retry identity', async () => {
    const ids = [];
    const h = formHarness(async (url, options) => {
        ids.push(JSON.parse(options.body).requestId);
        return { ok: false, json: async () => ({ success: false, message: 'Database unavailable' }) };
    });
    await h.submit(); await h.submit();
    assert.equal(ids[0], ids[1]);
    assert.notEqual(h.element('successView').style.display, 'block');
    assert.equal(h.element('toastMsg').textContent, 'Database unavailable');
});
test('unregistered form and invalid contacts never send a request', async () => {
    let count = 0;
    const h = formHarness(async () => { count++; return success(); }, ''); await h.submit();
    const other = formHarness(async () => { count++; return success(); });
    other.element('leadEmail').value = 'bad'; await other.submit();
    assert.equal(count, 0);
});
test('honeypot does not display a fake success', async () => {
    let count = 0; const h = formHarness(async () => { count++; return success(); });
    h.element('website_hp').value = 'spam'; await h.submit();
    assert.equal(count, 0); assert.notEqual(h.element('successView').style.display, 'block');
});
