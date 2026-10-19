# S4-01 — Lead Capture: implementation and verification

Verified on 2026-10-17 (Asia/Bangkok). Branch: `fix/S4-01-lead-capture-full`,
based on `origin/develop` at `5f1711e`. The original checkout and its two
uncommitted SLA/notification files were preserved in place; work uses a separate worktree.

## Scope and source of truth

The existing `lead-capture-manager.html`, `lead-capture-form.html`, `LeadServlet`,
`WebFormLeadRequest`, `generateEmbedCode()` and migration
`024_sprint4_web_form_leads.sql` explicitly implement CRM-113 / S4-01 as embedded
website lead capture. Embedding is therefore in scope. No separate S4-01 acceptance
document was found in tracked Markdown, and the repository issue search for S4-01
returned no issue. The existing form designer customizes the URL and generates
embed snippets; there was no persisted form-template CRUD API. This change adds
the ownership binding needed for secure capture, without introducing a template editor CRUD module.

## Root causes

- The manager's old random-Lead button used `POST /api/leads` without credentials.
  From port 5500 this omitted the session cookie and the servlet returned 401.
  That route is authenticated manual creation, not anonymous web capture. The
  filter's obsolete public-root-POST exception also skipped session-version checks;
  it now protects the root route and exempts only exact public submission paths.
- The original GET already used `credentials: include`. The supplied historical
  GET 401 cannot be conclusively attributed without that browser's cookie/session
  trace. Missing/expired sessions must still produce 401. Login, shell and shared
  API code previously pinned the backend to 8080 even when the WAR was served on
  another port; they now use the current origin under `/crm/`. The manager checks
  `/api/auth/session` first, reuses `CrmApi`, and distinguishes authentication
  failure from permission denial. No session or authorization filter was disabled.
- The same button sent JSON `source: "Website Form"` to manual creation.
  `LeadWriteRequest.fromJson` strictly accepts `sourceId`, not `source`, so an
  authenticated request failed with `Field không hỗ trợ: source`. This was a POST
  body mismatch, not the supported GET search/filter parameter `source`.
- The public form previously accepted arbitrary source labels, defaulted missing
  sources, had no trusted form owner, and used an insert helper that swallowed
  SQL failures and returned `-1`. Captured unassigned Leads also disappeared from
  Marketing's SELF list because the list used only `assigned_to`.
- There was no server-side retry identity; rapid or retried submissions could
  create additional rows. The frontend honeypot even displayed a fake success.

## Final API contract

All paths below are relative to `/crm` and use the existing
`{success,message,data}` JSON envelope.

- `GET /api/leads`: authenticated, requires `lead.read`, honors data scope and
  existing search, source, paging and filter parameters. 200 / 401 / 403.
- `POST /api/leads`: authenticated manual creation, requires `lead.create`.
  Body: `fullName`, `companyName?`, `email?`, `phone?`, `sourceId`, `notes?`.
  Source ID must refer to active `master_data` of type `lead-source`.
  At least one contact method is required. Existing S4-02 contract is retained.
- `GET /api/leads/sources`: authenticated catalog, requires `lead.read`.
- `GET /api/leads/embed-code?formId=crm-web-lead-form&theme=light&title=...`:
  requires `lead.create` and `lead.read`. The input `formId` is a 1–100 character
  form name (`A-Z`, `a-z`, digits, `_`, `-`). Returns the existing embed response
  fields: `formId`, `formUrl`, `iframeSnippet`, `scriptSnippet`, `htmlSnippet`,
  `previewUrl`. The returned `formId` is a persisted opaque UUID owned by the
  current user; the same owner/name returns the same ID.
- `POST /api/leads/web-form` and `/api/leads/web-form/submit`: public, no login
  cookie required. JSON or `application/x-www-form-urlencoded`. Required fields:
  `fullName`, `source: "WEB_FORM"`, returned UUID `formId`, `requestId`
  (16–64 letters, digits, `_`, `-`), plus at least one valid `email` or `phone`.
  Existing company/notes aliases and UTM metadata remain supported. The public
  UI continues requiring both email and Vietnamese mobile phone, as before.
  Owner/status/source ID are chosen by the server. Form must be active, owned
  by an active account retaining `lead.create`, and Website source must be active.
  Returns 201 with only `{id,fullName,status:"NEW"}` after commit. Identical retries
  reuse the Lead ID; reuse of a request ID with different normalized input is 409.
  Validation is 400, oversized body 413, IP limit 429, database failure 500.
- `GET /api/leads/health`: public health response, 200.

The public JSON body is bounded at 16,384 characters; URL-encoded bodies require a
known Content-Length of at most 16,384 bytes. Limits remain five public requests
per IP per minute per application instance, based on the container remote address.
They are not a distributed rate limiter.

The manager no longer manufactures random contact data. Its create action opens
the registered form for real entry. It reads every server page, refreshes on focus
and on verified iframe messages, and uses no local-storage Lead fallback. Form
submission is locked while in flight and keeps its request ID after an error.

## Migration and deployment

Apply `backend/database/028_s4_01_lead_capture.sql` to the explicitly selected
database **after** existing Sprint 4 migrations. It creates `lead_capture_forms`
and `lead_capture_submissions`, seeds the canonical Website source if absent,
fills missing Marketing/Admin Lead read/create permissions and missing scopes,
and explicitly adds legacy Lead columns formerly created inside `LeadDAO`.
Existing scope policies and inactive Website catalog entries are retained.
The script intentionally contains no `USE crm_db` and no DELETE/DROP statements.
`LeadDAO` no longer creates or alters schema in its constructor.

Regenerate old embeds in CRM after applying this migration: legacy arbitrary
form IDs without a registered owner and request ID now fail validation. Appearance
options remain in the embed URL. A form's public ID is intended to be visible;
it does not grant access to management APIs.

Use the same hostname for login and CRM when developing on 5500/8080. Browser
cookies for `localhost` do not authenticate `127.0.0.1`, or conversely. Both local
5500 origins are credentialed CORS development origins. Other origins get only
public POST/OPTIONS and Content-Type, without a credential grant. Root Lead
management is not opened to external origins. No SameSite security setting was
relaxed. `CRM_BACKEND_BASE` remains an explicit frontend override;
`CRM_BACKEND_PUBLIC_URL` configures the publicly reachable embed host behind a proxy.

WAR packaging includes the actual frontend and backend, excludes frontend tests
and node_modules, and contains no new credential file. Runtime credentials are
read from existing environment variables. No migration or test data was applied
to `crm_db` during this task.

## Changed files

Frontend: `lead-capture-manager.html`, `lead-capture-form.html`,
`js/pages/lead-capture.js`, `js/crm-api.js`, `js/login.js`, `js/shared-shell.js`.
The last three only change backend origin resolution shared by session/login.

Backend: `LeadServlet.java`, `LeadService.java`, `LeadDAO.java`, new
`LeadCaptureDAO.java`, `WebFormLeadRequest.java`, `AuthenticationFilter.java`,
`CorsFilter.java`, `DataScopeContext.java`, `pom.xml`, and migration 028.
The scope helper adds a validated COALESCE predicate for captured Leads awaiting
assignment; it does not widen Marketing to ALL. Lead detail uses the same owner rule.

Tests: `LeadCaptureJdbcTest`, `LeadCaptureCorsTest`, `LeadCaptureValidationTest`,
updated `WebFormSecurityTest`, `frontend/tests/lead-capture.test.mjs`, and
`frontend/tests/lead-capture-http-smoke.mjs`.

## Verification and limits

Final executed results:

- Focused Java suite: **67 PASS, 0 FAIL, 0 ERROR, 0 SKIP**.
- Full regression excluding the schema-name-restricted scoring fixture:
  **327 PASS, 0 FAIL, 0 ERROR, 0 SKIP** on `crm_sprint1_test_s401_20261017`.
- `LeadScoringJdbcTest`: **9 PASS, 0 FAIL, 0 ERROR, 0 SKIP**, run separately
  on exactly `crm_sprint1_test`, using connection-local temporary tables.
  Together these cover **336 distinct Java tests**. The earlier unsplit regression
  had 9 setup failures because that suite rejects suffixed database names; these
  are recorded as an initial failed run, not concealed or counted as successful.
- Node frontend behavioral tests: **5 PASS, 0 FAIL**. These use a DOM adapter and
  injected HTTP responses; they are client unit tests, not database persistence evidence.
- Real Tomcat 10.1.60 HTTP smoke: **14 PASS, 0 FAIL** after redeploying the fixed WAR.
  This includes absent-Origin requests, public/management CORS, anonymous 401,
  source validation, malformed and oversized JSON, native form encoding,
  both public aliases and the actual 429/Retry-After response.
- Form page opened in the in-app browser without authentication and rendered again
  after reload. No successful browser submission is claimed.
- Maven clean WAR build and the final regression/package build: **BUILD SUCCESS**.
  The two HTML pages and capture/shared API JavaScript in `crm.war` were compared
  byte-for-byte with source. Frontend tests/node_modules are absent from the WAR.
- Initial verified WAR SHA-256:
  `3da73501a4918da00e2ad5297b357a27dfbeb7e7886040891a482b513c4c4536`.
- Before startup port 8080 was free. During runtime checks it had one listener,
  PID 23888, belonging to the isolated QA Tomcat. No existing Tomcat was stopped.
  The task-owned QA instance was stopped after verification to release port 8080.

Commands (database credentials are supplied through existing environment variables):

```powershell
# CRM_DB_URL points to crm_sprint1_test_s401_20261017 for these tests.
mvn -B -f backend/pom.xml '-Dtest=LeadCapture*,WebForm*,LeadServletTest,LeadValidationTest,LeadImportJdbcTest,LeadExcelReaderTest,LeadFilterJdbcTest' package
mvn -B -f backend/pom.xml '-Dtest=*,!LeadScoringJdbcTest' package
# With CRM_DB_URL changed to the exact crm_sprint1_test schema:
mvn -B -f backend/pom.xml '-Dtest=LeadScoringJdbcTest' test
node --test frontend/tests/lead-capture.test.mjs
# Requires the QA WAR running locally and a fresh rate-limit window:
node frontend/tests/lead-capture-http-smoke.mjs
```

Local logs (ignored by Git) are `tests-s401-final.log`, `tests-regression-final.log`,
`tests-scoring.log`, `tests-frontend.log`, `tests-http-smoke.log`, and
`build-war-clean.log`. Test schema was copied from the existing test database,
not from development customer data. Migration 028 and existing scoring migration
025 were applied only to the new test schema. Runtime background assignment logs
also reported an existing legacy fixture value `assigned_to='USR-2'`; that
S4-06 fixture compatibility issue is outside this fix and is not reported as PASS.

The user explicitly chose to skip browser testing requiring sign-in. No new
reusable QA login account was created, and no password was reset. Consequently,
browser login/session-cookie behavior, successful browser embed submission and
visual appearance of the saved Lead in the authenticated CRM are **not verified**.
Real MySQL persistence, authenticated Marketing requests, 401/403, source validation,
scoped readback, and simultaneous retry deduplication are covered by JDBC tests
through the authentication filter, servlet, service and DAO.


## Follow-up scope and migration verification (2026-10-17)

The complete PR diff was checked against the freshly fetched `origin/develop`.
The original checkout still has only its pre-existing uncommitted changes in
`NotificationDAO.java` and `LeadSlaMonitorService.java`; neither path is in this PR.
No S4-07 or notification behavior was added to the follow-up. The empty-state
capture button now also says “Nhập Lead mới”, matching the real-entry action.

Migration 028 is the only tracked migration with prefix 028 and the only migration
creating the capture tables. It does not replace an existing migration or change
existing column definitions. `backend/dev/LeadCaptureMigrationChecks.java` was
executed on `crm_sprint1_test_s401_migration_20261017`, copied only from the existing
**test** schema. Results: **3 PASS, 0 FAIL**:

- Every original row (hashed cell-by-cell, including NULL values) and every original
  column definition was preserved across all **29 existing tables**. Only the
  documented catalog/permission/scope seed tables are allowed additional rows.
- Applying 028 a second time left all table rows and column definitions identical
  to the first result, including the new capture tables.
- All three capture foreign keys exist without naming conflicts.

This verifies the existing MySQL migration baseline and repeat execution. It does
not claim compatibility with arbitrary manually modified schemas. The helper
refuses any catalog outside `crm_sprint1_test_s401_migration_*`, has no hardcoded
credentials, prints no row contents, and stays outside production sources/WAR.
Neither this verification nor its fixture setup writes to `crm_db`.

To repeat after preparing a dedicated copy of the existing test schema:

```powershell
# Set CRM_DB_URL to the dedicated crm_sprint1_test_s401_migration_* schema.
java -cp 'backend/target/classes;backend/target/crm/WEB-INF/lib/*' backend/dev/LeadCaptureMigrationChecks.java backend/database/028_s4_01_lead_capture.sql
```


Follow-up validation completed: **67 Java tests PASS**, **5 frontend tests PASS**,
**3 migration checks PASS**, and Maven `package` returned **BUILD SUCCESS**.
The rebuilt WAR contains the updated real-entry button label and excludes the
standalone migration checker, frontend tests and node_modules.
Latest WAR SHA-256:
`4d94bef8e22bb5627a0097e5e34bb845e662a3226c350a23237ad5f8d7674611`.
Local follow-up evidence: `tests-s401-followup.log`,
`tests-frontend-followup.log`, and `tests-migration-028.log` (all ignored by Git).

## Complete PR file list

```text
backend/database/028_s4_01_lead_capture.sql
backend/dev/LeadCaptureMigrationChecks.java
backend/docs/S4-01-lead-capture.md
backend/pom.xml
backend/src/main/java/com/crm/controller/leads/LeadServlet.java
backend/src/main/java/com/crm/dao/leads/LeadCaptureDAO.java
backend/src/main/java/com/crm/dao/leads/LeadDAO.java
backend/src/main/java/com/crm/dto/leads/WebFormLeadRequest.java
backend/src/main/java/com/crm/filter/AuthenticationFilter.java
backend/src/main/java/com/crm/filter/CorsFilter.java
backend/src/main/java/com/crm/service/leads/LeadService.java
backend/src/main/java/com/crm/service/permissions/DataScopeContext.java
backend/src/test/java/com/crm/controller/leads/LeadCaptureCorsTest.java
backend/src/test/java/com/crm/controller/leads/LeadCaptureJdbcTest.java
backend/src/test/java/com/crm/controller/leads/WebFormSecurityTest.java
backend/src/test/java/com/crm/service/leads/LeadCaptureValidationTest.java
frontend/js/crm-api.js
frontend/js/login.js
frontend/js/pages/lead-capture.js
frontend/js/shared-shell.js
frontend/lead-capture-form.html
frontend/lead-capture-manager.html
frontend/tests/lead-capture-http-smoke.mjs
frontend/tests/lead-capture.test.mjs
```
