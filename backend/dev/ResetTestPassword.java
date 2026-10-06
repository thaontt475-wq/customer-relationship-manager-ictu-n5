import com.crm.config.DatabaseConfig;
import com.crm.util.PasswordUtil;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Locale;
import java.util.Set;

/** Local development CLI only. This directory is outside Maven's production source roots. */
public final class ResetTestPassword {
    private static final class SetupException extends Exception {
        SetupException(String message) { super(message); }
    }

    public static void main(String[] args) {
        try {
            if (args.length > 1 || (args.length == 1 && !"--verify".equals(args[0]))) {
                throw new SetupException("Use no argument to synchronize or --verify for a read-only check.");
            }
            run(args.length == 1 && "--verify".equals(args[0]));
        } catch (SetupException e) {
            System.err.println("FAIL " + e.getMessage());
            System.exit(1);
        } catch (Exception | LinkageError e) {
            // SQL/driver exceptions can contain connection details. Never print their message or stack trace.
            System.err.println("FAIL Local dev credential helper could not complete; no secret values logged.");
            System.exit(1);
        }
    }

    private static void run(boolean verifyOnly) throws Exception {
        if (!Set.of("local", "dev", "test").contains(System.getenv("CRM_ENV") == null
                ? "" : System.getenv("CRM_ENV").toLowerCase(Locale.ROOT))) {
            throw new SetupException("CRM_ENV must be local, dev or test.");
        }
        String email = System.getenv("CRM_TEST_EMAIL");
        String password = System.getenv("CRM_TEST_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new SetupException("CRM_TEST_EMAIL and CRM_TEST_PASSWORD must be present in this process.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new SetupException("CRM_TEST_PASSWORD exceeds the BCrypt 72-byte limit.");
        }
        email = email.trim().toLowerCase(Locale.ROOT);

        try (Connection connection = DatabaseConfig.getConnection()) {
            URI database = URI.create(connection.getMetaData().getURL().replaceFirst("^jdbc:", ""));
            if (!Set.of("localhost", "127.0.0.1", "::1", "[::1]").contains(database.getHost())) {
                throw new SetupException("This helper only permits a loopback MySQL database.");
            }
            if (verifyOnly) {
                verify(connection, email, password);
                System.out.println("PASS DEV account verification: password matches ENV, ACTIVE, attempts=0, unlocked.");
                return;
            }

            connection.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT id FROM users WHERE LOWER(email) = ? FOR UPDATE")) {
                    select.setString(1, email);
                    try (ResultSet rows = select.executeQuery()) {
                        if (!rows.next()) throw new SetupException("The designated test account does not exist; no account created.");
                        userId = rows.getLong(1);
                        if (rows.next()) throw new SetupException("The designated email is ambiguous; no account modified.");
                    }
                }
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE users SET password_hash = ?, failed_login_attempts = 0,
                            locked_until = NULL, status = 'ACTIVE', session_version = session_version + 1
                        WHERE id = ?
                        """)) {
                    update.setString(1, PasswordUtil.hash(password));
                    update.setLong(2, userId);
                    if (update.executeUpdate() != 1) throw new SetupException("The test account was not updated.");
                }
                try (PreparedStatement revoke = connection.prepareStatement(
                        "UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP WHERE user_id = ? AND used_at IS NULL")) {
                    revoke.setLong(1, userId);
                    revoke.executeUpdate();
                }
                verify(connection, email, password);
                connection.commit();
                System.out.println("PASS DEV test password synchronized from ENV; account ACTIVE and unlocked; old sessions revoked.");
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static void verify(Connection connection, String email, String password) throws Exception {
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT password_hash, status, failed_login_attempts, locked_until
                FROM users WHERE LOWER(email) = ?
                """)) {
            query.setString(1, email);
            try (ResultSet rows = query.executeQuery()) {
                if (!rows.next() || !PasswordUtil.matches(password, rows.getString("password_hash"))
                        || !"ACTIVE".equals(rows.getString("status"))
                        || rows.getInt("failed_login_attempts") != 0 || rows.getTimestamp("locked_until") != null) {
                    throw new SetupException("The test account password/status/attempt state does not match the expected dev state.");
                }
            }
        }
    }
}
