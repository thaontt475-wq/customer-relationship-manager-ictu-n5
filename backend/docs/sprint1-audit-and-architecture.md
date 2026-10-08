# Sprint 1: Phase 0 audit and Phase 1 architecture

Audited on 2026-10-06 against the checked-out source and the local `crm_db` database. This document is a plan and evidence record, not a claim that Sprint 1 is complete.

## Phase 0 — source and running database

The active backend path is `AuthenticationFilter -> Servlet -> Service -> DAO -> JDBC -> MySQL`. `web.xml` maps the filter to `/api/*`; servlets are annotated with `@WebServlet`. The frontend is a static multi-page app. `shared-shell.js` fetches `/api/auth/session` and `/api/navigation/menu`, then renders desktop and mobile navigation from the same returned menu. `MenuService` maps permission codes to links.

The running `crm_db` has 18 tables, 5 users, 3 roles, 31 permissions and 4 user-role assignments. Role codes are `ADMIN`, `MANAGER`, `SALE`; `ADMIN` has 31 grants, `MANAGER` and `SALE` have 3 each. The schema has `users`, `roles`, `user_roles`, `permissions`, `role_permissions`, `teams`, `organization_units`, `audit_logs`, `products` and the other Sprint 2 catalog tables. It has **no** `customers`, `opportunities`, `activities` or `quotes` tables. The source migrations `001`–`006` match that absence. `teams` is flat and has no parent/lead relation. `users.data_scope` is global. The existing `MANAGER` assignment has a team; no `SALE` assignment is present. The source has no `backend/src/test` tests.

### Acceptance status

| Story | Status | Evidence and gap |
| --- | --- | --- |
| S1-05 role/data scope | FAIL | No four business tables or DAOs to scope; no shared scope service. `users.data_scope` is stored but not enforced on business queries. |
| S1-06 permission menu/user shell | PARTIAL | `/api/navigation/menu` and session payload exist. `shared-shell.js` contains older hard-coded bottom-nav and newer top-shortcut code; `ui-core.js` and shell both contain drawer/dropdown handlers, with runtime guards. `theme.js` and shell both contain theme code. Static business pages are still reachable by URL and their localStorage data is not protected by backend authorization. |
| S1-07 common error UI | PARTIAL | `/api/error` provides JSON 404/500; `AuthenticationFilter` returns 401 and servlets often return 403. There is no common 401/403/404/500 frontend page or shared page-level fetch error handler. |
| S1-08 user management | PARTIAL | Backend create/edit and duplicate email conflict exist; list uses server keyword/status pagination, default size 20. Team and role filters are not applied before pagination: the frontend filters roles after receiving a page. No onboarding email is sent by `UserManagementService.create`. |
| S1-09 multiple roles/team | PARTIAL | `user_roles` many-to-many and role assignment endpoint exist. Seven required role codes are not seeded. No backend rule requires team for TEAM_LEAD or protects self-removal/last ADMIN. |
| S1-10 lock/transfer | PARTIAL | Lock changes status/session version, and the filter rejects revoked sessions. Transfer currently updates `organization_units.manager_id`, not customer/opportunity owners. Transfer and lock are separate operations; ownership counts are absent from audit. |
| UI, migration, tests | PARTIAL/FAIL | Existing responsive/theme work exists but duplicated shell/theme logic remains. UTF-8 is declared in Maven and MySQL schema. Full dark/mobile/encoding QA is outstanding. Fresh migrations do not create four business tables; automated acceptance tests are absent. |

### Main conflicts and files to change

- Frontend shell ownership: `frontend/js/shared-shell.js` should own server menu and user rendering; `frontend/js/ui-core.js` should own dropdown/drawer/search interactions; `frontend/js/theme.js` should own theme; `frontend/js/ui-effects.js` should own only display effects. Remove duplicate listeners and dead navigation paths, then update `frontend/css/app-shell.css` only where behavior requires it. The existing `frontend/users.html` and `frontend/js/pages/users.js` need server-side team/role search/filter wiring.
- Authorization: `backend/src/main/java/com/crm/filter/AuthenticationFilter.java`, `service/permissions/{PermissionService,MenuService}.java`, `dao/permissions/PermissionDAO.java`, `service/teams/TeamService.java` and `dao/teams/TeamDAO.java` need coordinated changes. Every new business servlet must check action permission and call a shared scope service before its DAO reads/writes.
- User lifecycle: `controller/users/UserServlet.java`, `service/users/{UserManagementService,UserStatusService}.java`, `dao/users/{UserManagementDAO,UserDAO}.java`, `service/mail/MailService.java`, and `dao/auth/SessionDAO.java` are the current paths. The transfer behavior in `UserStatusService` must be replaced, not treated as customer ownership transfer.
- Database: add forward-only migrations after `006_sprint2_catalogs.sql` for the role/permission matrix, hierarchy, business ownership tables and audit detail. Keep existing data and IDs; do not assume all deployed databases equal the source baseline.
- Errors: one common frontend error view/helper and consistent server response codes; existing servlet URLs and response envelope should remain compatible.

## Phase 1 — permission matrix and scope architecture

### Role normalization

Keep `ADMIN`. Migrate legacy `SALE` to `SALES_REP` and `MANAGER` to `TEAM_LEAD` **by updating existing role rows**, preserving role IDs and `user_roles` links. Seed `DIRECTOR`, `MARKETING`, `CUSTOMER_SUCCESS`, `ACCOUNTANT` for a total of seven role codes. Before the migration, assert there is no conflicting target code and that assigned TEAM_LEAD users have a valid team. Do not silently attach users to a default team. The later migration must be idempotent and avoid dropping user-role links.

Use existing `<module>.<action>` permission names. Add missing codes without renaming the 31 deployed codes; retire aliases only after references and role grants are migrated. `PermissionDAO.findByUserId` already unions grants across roles. For multiple roles, grant an action when any role grants it; apply the widest permitted scope for that **module and action**. A global `users.data_scope` must not broaden a module beyond its role policy. ADMIN has every permission, including any new permission inserted by a migration.

### Module matrix

F = full action set in the stated scope; W = read and permitted write in scope; R = read only; — = no grant. This is the default seed policy; a servlet still checks its exact action permission.

| Module | SALES_REP | TEAM_LEAD | DIRECTOR | MARKETING | CUSTOMER_SUCCESS | ACCOUNTANT | ADMIN |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Sales catalogs/config | R | R | F | R | R | R | F |
| Customer/contact | W SELF | F TEAM | F ALL | W policy scope | W assigned | R policy scope | F ALL |
| Lead/assignment | W SELF | F TEAM | F ALL | F policy scope | — | — | F ALL |
| Opportunity/pipeline | W SELF | F TEAM | F ALL | R policy scope | R assigned | R policy scope | F ALL |
| Activity/calendar | W SELF | F TEAM | F ALL | W policy scope | W assigned | — | F ALL |
| Quote/contract | W SELF | W TEAM | F ALL | — | R assigned | W policy scope | F ALL |
| KPI | R SELF | W TEAM | F ALL | — | — | R policy scope | F ALL |
| Dashboard/report | R SELF | R TEAM | F ALL | R policy scope | R assigned | R policy scope | F ALL |
| Automation/notification | R | R | F | W | R | — | F |
| User/audit | — | — | R | — | — | — | F |

“Policy scope” and “assigned” are deliberately not treated as ALL. They require an explicit module rule and persisted assignment/ownership model before grants become active. No business permission should be seeded for an absent API solely to make a menu appear.

### Scope data model and enforcement

Add a role/module/action scope policy (e.g. `role_module_scopes`) instead of relying on `users.data_scope` alone. Scope values are `SELF`, `TEAM`, `ALL`; assigned-customer access is a separate explicit assignment rule, not an implicit global scope. Extend the real `teams` hierarchy with a parent and lead relation or normalize the existing organization hierarchy into team membership after verifying deployed data. Do not infer team membership from `organization_units.manager_id` transfer. A recursive CTE can resolve descendant teams; cycles must be rejected when editing hierarchy.

`DataScopeService` resolves the authenticated user, action permission, effective scope and allowed owner IDs. Its DAO helper supplies bound predicates: SELF `owner_id = ?`; TEAM `owner_id IN (self + active users in managed team subtree)`; ALL no owner predicate. Use the same resolver in list, detail, search, filter and export queries for Customer, Opportunity, Activity and Quote. For writes and ownership transfer, check both action permission and target record/owner scope in the transaction. Never accept caller-supplied role, scope or actor ID as authority. Out-of-scope detail must return a consistent 403 or privacy-preserving 404 with a Vietnamese message and navigation action in the frontend.

Before Phase 2 can pass, real customer/opportunity/activity/quote schema and endpoints need explicit owner columns and DAO integration. The existing repository does not provide those tables or contracts, so Phase 1 does not assert S1-05 is implemented.

### Safe implementation order after Phase 1

1. Forward-only schema and scope service with ownership tests, then business DAO/servlet enforcement (Phase 2).
2. Permission-derived shell and single mobile navigation/event owner (Phase 3).
3. Common API/error handling (Phase 4).
4. Server-filtered user management and onboarding mail (Phase 5).
5. Seven-role assignment safeguards and team rules (Phase 6).
6. Transactional ownership transfer plus lock, revoke and audit (Phase 7).
7. Sprint 1 UI/mobile/dark/encoding QA (Phase 8), migration replay (Phase 9), automated suite (Phase 10).

## Verification commands

PowerShell backend build (this host's JVM otherwise resolves `user.home` to `C:\`):

```powershell
$env:JAVA_TOOL_OPTIONS = "-Duser.home=$env:USERPROFILE"
cd C:\Users\thang\Projects\customer-relationship-manager-ictu-n5\backend
mvn package -DskipTests
```

JavaScript syntax, after frontend changes:

```powershell
Get-ChildItem C:\Users\thang\Projects\customer-relationship-manager-ictu-n5\frontend\js -Recurse -Filter *.js |
    ForEach-Object { node --check $_.FullName }
```

Database verification before any migration:

```text
mysql -u root -p
USE crm_db;
SELECT code FROM roles ORDER BY code;
SELECT code FROM permissions ORDER BY code;
SELECT table_name FROM information_schema.tables
 WHERE table_schema = 'crm_db' ORDER BY table_name;
exit;
```

Phase 0 baseline build passed with the `JAVA_TOOL_OPTIONS` command above. No migration was applied during the architecture phase; the live database was inspected read-only. Phase 1 is an architecture decision and is not a functional acceptance pass.
