# CRM-21 login

## Database setup

- Fresh MySQL 8 database: run `database/schema.sql`, then `database/data.sql`.
- Existing database from before CRM-21: run `database/migrations/CRM-21-login.sql` ONCE, then `database/data.sql`. Do not also run the migration on a fresh schema.
- Existing users and password-reset/handover tables are preserved. Existing `full_name` is the display-name fallback. Login requires BOTH `active = TRUE` and `status = 'ACTIVE'` so CRM-30 locked users remain blocked.
- The seed creates Admin, Sales Rep and Accountant roles only. It creates no accounts. For local testing, generate a hash using the existing `PasswordUtil.hashPassword`, insert a synthetic user (including the existing required `username`), and associate its ID with roles through `user_roles`. Never store plaintext in `password_hash`.
- Configure the classpath `db.properties`; environment overrides are `CRM_DB_URL`, `CRM_DB_USERNAME`, `CRM_DB_PASSWORD`. No personal credentials were added by this change.

## Behavior and compatibility

GET `/login` forwards to `/jsp/auth/login.jsp`. POST validates fields, normalizes email using trim + Locale.ROOT lowercase, reads the user and all roles with a parameterized JDBC query and verifies BCrypt. Password is not trimmed. Unknown users, incorrect passwords, inactive/locked users receive the same error. Database failures return HTTP 503 with a generic message.

Successful login invalidates the old session before creating a new one. Session attributes are `userId` (Long), `roles` (List<String>), optional `displayName`, and a safe `currentUser` map containing only `id` for the existing session/user endpoints. Neither the User object nor password/hash is stored in the session.

There is no implemented dashboard route. Success redirects to the existing `/html/index.html` under the deployed context path. This page is currently a minimal CRM heading.

Existing password-reset methods in AuthService retain their prior JDBC implementation; the new login method contains no JDBC or Servlet code. No CRM-26, sidebar, authorization filter, logout or password-reset behavior was changed.

## Verification

From `backend`, run `mvn clean package` with JDK 21. This project currently has no Maven-managed unit tests. The standalone regression harness adds no dependencies and runs separately after building.

PowerShell (set `$crmRepo` to your Maven local repository):

```powershell
$crmRepo = "$env:USERPROFILE/.m2/repository"
$crmRoot = (Get-Location).Path
$crmClasspath = "$crmRoot/target/classes;$crmRoot/target/ROOT/WEB-INF/lib/*;$crmRepo/jakarta/servlet/jakarta.servlet-api/6.0.0/jakarta.servlet-api-6.0.0.jar"
javac -encoding UTF-8 -cp $crmClasspath -d target/crm21-tests tests/CRM21LoginCheck.java
if ($LASTEXITCODE -eq 0) {
    java -cp "$crmRoot/target/crm21-tests;$crmClasspath" CRM21LoginCheck
}
```

The harness uses a fake DAO and Servlet API proxies; it checks BCrypt, normalized email, missing fields, incorrect credentials, inactive/locked users, malformed hash, database-error propagation, GET forwarding, failure messages, session renewal, session types, credential exclusion and context-relative redirect. It does not validate live MySQL queries or Tomcat deployment.

For integration testing on a local MySQL/Tomcat instance, apply the appropriate SQL setup, create synthetic BCrypt users with zero/one/multiple roles, deploy `target/ROOT.war`, and repeat the successful/failed/inactive cases. After successful login verify `/api/navigation/menu` accepts the new session and returns the expected menu; confirm the session cookie changes from the pre-login session. Test `active = FALSE` and `status = 'LOCKED'` separately.
