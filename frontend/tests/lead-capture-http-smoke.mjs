// Run against a fresh local Tomcat (or wait one rate-limit window between runs).
// No authentication credentials, inserts, updates or deletes are used.
import assert from 'node:assert/strict';
const base = (process.env.CRM_TEST_API_BASE || 'http://localhost:8080/crm').replace(/\/$/, '');
if (!/^http:\/\/(localhost|127\.0\.0\.1):\d+\/crm$/.test(base)) throw Error('Local QA URL required');
let passed = 0;
async function check(name, path, options, status, verify = () => {}) {
    const response = await fetch(base + path, options);
    const body = await response.text();
    assert.equal(response.status, status, `${name}: ${body}`);
    verify(response, body); passed++; console.log(`PASS ${name}`);
}
const json = body => ({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body });
await check('public health', '/api/leads/health', {}, 200);
await check('anonymous Lead list blocked', '/api/leads', {}, 401);
await check('anonymous manual create blocked', '/api/leads', json('{}'), 401);
await check('anonymous embed generation blocked', '/api/leads/embed-code', {}, 401);
await check('public form served from WAR', '/lead-capture-form.html', {}, 200, (_, body) => assert.ok(body.includes('credentials: "omit"')));
await check('manager JS served from WAR', '/js/pages/lead-capture.js', {}, 200, (_, body) => assert.ok(body.includes('CrmApi.session()')));
await check('external public POST preflight', '/api/leads/web-form', { method: 'OPTIONS', headers: { Origin: 'https://outside.example', 'Access-Control-Request-Method': 'POST', 'Access-Control-Request-Headers': 'Content-Type' } }, 200,
    r => { assert.equal(r.headers.get('access-control-allow-origin'), 'https://outside.example'); assert.notEqual(r.headers.get('access-control-allow-credentials'), 'true'); assert.equal(r.headers.get('access-control-allow-methods'), 'POST, OPTIONS'); });
await check('external admin preflight has no origin grant', '/api/leads', { method: 'OPTIONS', headers: { Origin: 'https://outside.example', 'Access-Control-Request-Method': 'POST' } }, 200,
    r => assert.equal(r.headers.get('access-control-allow-origin'), null));
await check('missing source rejected', '/api/leads/web-form', json('{"fullName":"S401 QA","email":"qa@example.invalid"}'), 400,
    (_, body) => assert.ok(JSON.parse(body).message.includes('Nguồn Lead')));
await check('malformed JSON rejected', '/api/leads/web-form', json('{broken'), 400);
await check('public alias reaches validation without cookies', '/api/leads/web-form/submit', json('{}'), 400);
await check('oversized public JSON rejected', '/api/leads/web-form', json(' '.repeat(16385)), 413);
await check('native HTML form encoding reaches validation', '/api/leads/web-form', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: 'fullName=S401+QA&email=qa%40example.invalid' }, 400);
await check('public IP rate limit enforced', '/api/leads/web-form', json('{}'), 429, r => assert.equal(r.headers.get('retry-after'), '60'));
console.log(`HTTP smoke: ${passed} PASS, 0 FAIL`);
