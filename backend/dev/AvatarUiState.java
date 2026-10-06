import com.crm.config.DatabaseConfig;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Local avatar UI test cleanup only; excluded from Maven's production source roots. */
public final class AvatarUiState {
    private static final Duration MAX_AGE = Duration.ofMinutes(45);
    private static final int MAX_STATE_BYTES = 16_384;
    private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final Pattern AVATAR = Pattern.compile("^/crm/uploads/avatars/(" + UUID + ")\\.(jpg|png)$");
    private static final Pattern THUMBNAIL = Pattern.compile("^/crm/uploads/avatars/(" + UUID + ")_thumb\\.(jpg|png)$");
    private static final Set<String> STATE_KEYS = Set.of("id", "email", "avatarUrl", "avatarThumbnailUrl", "createdAt");

    private record Snapshot(long id, String email, String avatarUrl, String avatarThumbnailUrl, String createdAt) {}
    private record AvatarPair(String avatarUrl, String thumbnailUrl) {}
    private record StateFile(Snapshot snapshot, byte[] bytes) {}
    private static final class DevException extends Exception {
        DevException(String message) { super(message); }
    }

    public static void main(String[] args) {
        try {
            checkEnvironment();
            if (args.length == 3 && "snapshot".equals(args[0])) {
                snapshot(args[1], statePath(args[2]));
            } else if (args.length == 4 && "restore".equals(args[0])) {
                AvatarPair expected = expectedPair(args[2], args[3]);
                restore(statePath(args[1]), expected);
            } else {
                throw new DevException("Use snapshot <email> <stateFile> or restore <stateFile> <expectedAvatar> <expectedThumbnail>.");
            }
        } catch (DevException e) {
            System.err.println("FAIL " + e.getMessage());
            System.exit(1);
        } catch (Exception | LinkageError e) {
            // Connection, file and parser errors can contain private values. Never print them.
            System.err.println("FAIL DEV avatar state helper could not complete; no private values logged.");
            System.exit(1);
        }
    }

    private static void checkEnvironment() throws DevException {
        String environment = System.getenv("CRM_ENV");
        if (environment != null && !Set.of("local", "dev", "test").contains(environment.trim().toLowerCase(Locale.ROOT))) {
            throw new DevException("CRM_ENV, when set, must be local, dev or test.");
        }
    }

    private static Path statePath(String value) throws Exception {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path repository;
        if (Files.isRegularFile(cwd.resolve("backend/pom.xml")) && Files.isDirectory(cwd.resolve("backend/dev"))) {
            repository = cwd.toRealPath();
        } else if (cwd.getFileName() != null && "backend".equals(cwd.getFileName().toString())
                && Files.isRegularFile(cwd.resolve("pom.xml")) && Files.isDirectory(cwd.resolve("dev"))) {
            repository = cwd.getParent().toRealPath();
        } else {
            throw new DevException("Run this helper from the repository root or its backend directory.");
        }
        Path target = repository.resolve("backend/target").normalize();
        if (!Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(target)
                || !target.toRealPath().startsWith(repository.resolve("backend").toRealPath())) {
            throw new DevException("The allowed local backend target directory is unavailable.");
        }
        Path requested = Path.of(value);
        Path resolved = (requested.isAbsolute() ? requested : cwd.resolve(requested)).normalize();
        if (resolved.getParent() == null || !resolved.getParent().toRealPath().equals(target.toRealPath())
                || !resolved.getFileName().toString().matches("avatar-ui-state[a-zA-Z0-9_-]*\\.json")
                || Files.isSymbolicLink(resolved)) {
            throw new DevException("State files must use the avatar-ui-state prefix in the local backend target directory.");
        }
        return resolved;
    }

    private static Connection localConnection() throws Exception {
        Connection connection = DatabaseConfig.getConnection();
        try {
            URI database = URI.create(connection.getMetaData().getURL().replaceFirst("^jdbc:", ""));
            if (!Set.of("localhost", "127.0.0.1", "::1", "[::1]").contains(database.getHost())) {
                throw new DevException("This helper only permits a loopback database.");
            }
            return connection;
        } catch (Exception e) {
            connection.close();
            throw e;
        }
    }

    private static void snapshot(String email, Path file) throws Exception {
        if (email == null || email.isBlank() || email.length() > 255) {
            throw new DevException("A valid account email argument is required.");
        }
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new DevException("The state file already exists; no file was overwritten.");
        }
        Snapshot snapshot;
        try (Connection connection = localConnection(); PreparedStatement select = connection.prepareStatement("""
                SELECT id, email, avatar_url, avatar_thumbnail_url
                FROM users WHERE LOWER(email) = ?
                """)) {
            select.setString(1, email.trim().toLowerCase(Locale.ROOT));
            try (ResultSet rows = select.executeQuery()) {
                if (!rows.next()) throw new DevException("The designated account does not exist.");
                snapshot = new Snapshot(rows.getLong("id"), rows.getString("email"), rows.getString("avatar_url"),
                        rows.getString("avatar_thumbnail_url"), Instant.now().toString());
                if (rows.next()) throw new DevException("The designated account is ambiguous.");
            }
        }
        Files.writeString(file, JsonUtil.getGson().toJson(snapshot), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.println("PASS DEV avatar snapshot created: 1 account.");
    }

    private static AvatarPair expectedPair(String avatarValue, String thumbnailValue) throws Exception {
        String avatar = relativeAvatarPath(avatarValue);
        String thumbnail = relativeAvatarPath(thumbnailValue);
        var avatarMatch = AVATAR.matcher(avatar);
        var thumbnailMatch = THUMBNAIL.matcher(thumbnail);
        if (!avatarMatch.matches() || !thumbnailMatch.matches()
                || !avatarMatch.group(1).equals(thumbnailMatch.group(1))
                || !avatarMatch.group(2).equals(thumbnailMatch.group(2))) {
            throw new DevException("Expected avatar paths must be a matching generated avatar and thumbnail pair.");
        }
        return new AvatarPair(avatar, thumbnail);
    }

    private static String relativeAvatarPath(String value) throws Exception {
        URI url = URI.create(value);
        if (url.getQuery() != null || url.getFragment() != null || url.getUserInfo() != null) {
            throw new DevException("Expected avatar paths must not contain URL credentials, queries or fragments.");
        }
        if (url.isAbsolute()) {
            if (!Set.of("http", "https").contains(url.getScheme())
                    || !Set.of("localhost", "127.0.0.1", "::1", "[::1]").contains(url.getHost())) {
                throw new DevException("Expected absolute avatar URLs must use a loopback API origin.");
            }
        } else if (url.getAuthority() != null) {
            throw new DevException("Expected avatar paths must use the local application context.");
        }
        return url.getPath();
    }

    private static StateFile readState(Path file) throws Exception {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > MAX_STATE_BYTES) {
            throw new DevException("The state file is missing or invalid.");
        }
        byte[] bytes = Files.readAllBytes(file);
        JsonObject json = JsonUtil.getGson().fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
        if (json == null || !json.keySet().equals(STATE_KEYS)) throw new DevException("The state file schema is invalid.");
        Snapshot snapshot = JsonUtil.getGson().fromJson(json, Snapshot.class);
        if (snapshot.id() <= 0 || snapshot.email() == null || snapshot.email().isBlank()
                || snapshot.email().length() > 255 || snapshot.createdAt() == null) {
            throw new DevException("The state file account reference is invalid.");
        }
        Duration age = Duration.between(Instant.parse(snapshot.createdAt()), Instant.now());
        if (age.isNegative() || age.compareTo(MAX_AGE) > 0) {
            throw new DevException("The state file is expired or has an invalid creation time.");
        }
        return new StateFile(snapshot, bytes);
    }

    private static AvatarPair currentPair(Connection connection, Snapshot snapshot, boolean lock) throws Exception {
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT avatar_url, avatar_thumbnail_url
                FROM users WHERE id = ? AND email = ?
                """ + (lock ? " FOR UPDATE" : ""))) {
            select.setLong(1, snapshot.id()); select.setString(2, snapshot.email());
            try (ResultSet rows = select.executeQuery()) {
                if (!rows.next()) throw new DevException("The snapshot account reference no longer exists.");
                return new AvatarPair(rows.getString("avatar_url"), rows.getString("avatar_thumbnail_url"));
            }
        }
    }

    private static void restore(Path file, AvatarPair expected) throws Exception {
        StateFile state = readState(file);
        Snapshot snapshot = state.snapshot();
        AvatarPair original = new AvatarPair(snapshot.avatarUrl(), snapshot.avatarThumbnailUrl());
        boolean alreadyOriginal;
        try (Connection connection = localConnection()) {
            connection.setAutoCommit(false);
            try {
                AvatarPair current = currentPair(connection, snapshot, true);
                alreadyOriginal = Objects.equals(current, original);
                if (!alreadyOriginal) {
                    if (!Objects.equals(current, expected)) {
                        throw new DevException("The avatar changed outside this UI test; no account data was overwritten.");
                    }
                    try (PreparedStatement update = connection.prepareStatement("""
                            UPDATE users SET avatar_url = ?, avatar_thumbnail_url = ?
                            WHERE id = ? AND email = ? AND avatar_url <=> ? AND avatar_thumbnail_url <=> ?
                            """)) {
                        update.setString(1, original.avatarUrl()); update.setString(2, original.thumbnailUrl());
                        update.setLong(3, snapshot.id()); update.setString(4, snapshot.email());
                        update.setString(5, expected.avatarUrl()); update.setString(6, expected.thumbnailUrl());
                        if (update.executeUpdate() != 1) throw new DevException("The guarded avatar restoration did not update exactly one account.");
                    }
                }
                if (!Objects.equals(currentPair(connection, snapshot, false), original)) {
                    throw new DevException("The original avatar state could not be verified.");
                }
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || Files.size(file) > MAX_STATE_BYTES || !Arrays.equals(Files.readAllBytes(file), state.bytes())) {
            throw new DevException("The account avatar was verified but the state file changed; no state file was deleted.");
        }
        Files.delete(file);
        System.out.println(alreadyOriginal
                ? "PASS DEV original avatar state already verified: 1 account."
                : "PASS DEV avatar restoration verified: 1 account.");
    }
}
