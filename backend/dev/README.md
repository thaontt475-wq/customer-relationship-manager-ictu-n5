# Local Sprint 1 credential synchronization

`ResetTestPassword.java` is a standalone Java 21 CLI outside `src/main/java`.
Maven does not compile or package it in the production WAR. There is no HTTP reset endpoint.

The helper requires `CRM_ENV=local|dev|test`, `CRM_TEST_EMAIL` and
`CRM_TEST_PASSWORD` in its own process. It refuses a non-loopback MySQL URL,
updates only the existing account matching the designated email, uses
`com.crm.util.PasswordUtil.hash`, clears failed attempts/temporary lock, sets
ACTIVE and revokes old sessions/reset tokens in a transaction. It does not
create users, alter roles or print passwords/hashes/tokens.

After a backend build, run this workflow in the terminal containing the test ENV:

```powershell
node C:\Users\thang\Projects\customer-relationship-manager-ictu-n5\frontend\tests\dev-sprint1.mjs
```

It performs the password sync first, requires a direct login 200 with a real
cookie session, tests logout, then runs the full Sprint 1 API E2E runner and
verifies that the main test password/status/attempt state is preserved even if
E2E fails. It refuses an explicitly non-development CRM_ENV and a non-loopback
CRM_TEST_API_BASE; bootstrap and E2E use the same validated local API base.
Change/reset-password and lock/transfer tests use disposable API-created users.
The helpers and reports contain no credential values.

## Sprint 2 import checks

`ImportContractChecks.java` runs ten service/DB-read checks without creating users.
It is outside production sources and writes safe counts to target/import-contract-results.json.
`ImportTestWorkbook.java` supplies XLSX fixtures to the Node story runner in memory;
its temporary passwords are random and unrelated to the main account.

After the S2-01 backend WAR is deployed, run with the existing test ENV:

```powershell
node C:\Users\thang\Projects\customer-relationship-manager-ictu-n5\frontend\tests\sprint2-e2e.mjs --story=S2-01
```

This runner does not synchronize or reset the main test credential. It checks actual
template/preview/confirm, invalid files/headers/duplicate email, batch replay,
permission403, navigation and cleanup; report frontend/tests/s2-01-e2e-results.json.

## Sprint 2 profile checks

`ProfileValidationChecks.java` exercises invalid name/phone and malformed JSON
without calling the DAO. It is outside production sources and the WAR.

With the existing credential ENV and the Profile WAR deployed:

```powershell
node C:\Users\thang\Projects\customer-relationship-manager-ictu-n5\frontend\tests\sprint2-e2e.mjs --story=S2-02
```

The runner prefers an API-created disposable user with its own session, leaving
the administrator profile unchanged. It checks GET/PUT, validation boundaries,
readonly fields and session identity, restores the original profile before
logout, and soft-deletes its fixture. If CRUD permissions are unavailable it
reports the designated account fallback and refuses mutations when the original
profile cannot be restored exactly. It never resets passwords or submits an
incorrect administrator password. Report: frontend/tests/s2-02-e2e-results.json.
