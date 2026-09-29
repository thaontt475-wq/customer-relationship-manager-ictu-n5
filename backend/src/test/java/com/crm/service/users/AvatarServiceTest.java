package com.crm.service.users;

import com.crm.dao.users.AvatarDAO;
import com.crm.model.UserAvatar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class AvatarServiceTest {
    @TempDir Path root;
    static class FakeDAO extends AvatarDAO {
        UserAvatar value; boolean fail; boolean active = true;
        @Override public boolean lockActiveUser(Connection c, long id) { return active; }
        @Override public UserAvatar find(Connection c, long id) { return value; }
        @Override public void save(Connection c, long id, UserAvatar next) throws SQLException {
            if (fail) throw new SQLException("simulated DB failure");
            value = next;
        }
    }
    private Connection connection(boolean commitFails) {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Connection.class}, (p,m,a) -> {
            if (m.getName().equals("commit") && commitFails) throw new SQLException("lost commit acknowledgement");
            return null;
        });
    }
    private UserAvatar upload(AvatarService service) throws Exception {
        byte[] image = AvatarImageProcessorTest.image("png", 80,40);
        return service.upload(42, new ByteArrayInputStream(image), "../../unsafe.png", image.length);
    }
    private long count() throws Exception { try (var files = Files.list(root)) { return files.count(); } }
    @Test void savesSafeKeysAndCleansReplacedFiles() throws Exception {
        FakeDAO dao = new FakeDAO();
        AvatarService service = new AvatarService(root, dao, () -> connection(false));
        UserAvatar first = upload(service);
        assertTrue(first.imagePath().matches("[a-f0-9-]+\\.png"));
        assertNotNull(service.read(42, false)); assertNotNull(service.read(42, true));
        UserAvatar next = upload(service);
        assertNotEquals(first, next); assertEquals(2, count());
        assertFalse(Files.exists(root.resolve(first.imagePath())));
    }
    @Test void databaseFailureKeepsPreviousAvatarAndRemovesNewPair() throws Exception {
        FakeDAO dao = new FakeDAO();
        AvatarService service = new AvatarService(root, dao, () -> connection(false));
        UserAvatar first = upload(service); dao.fail = true;
        assertThrows(SQLException.class, () -> upload(service));
        assertEquals(first, dao.value); assertEquals(2, count());
        assertTrue(Files.exists(root.resolve(first.imagePath())));
    }
    @Test void unavailableDatabaseAndInactiveUserLeaveNoFiles() throws Exception {
        assertThrows(SQLException.class, () -> upload(new AvatarService(root, new FakeDAO(), () -> { throw new SQLException("offline"); })));
        assertEquals(0, count());
        FakeDAO dao = new FakeDAO(); dao.active = false;
        assertEquals(403, assertThrows(AvatarException.class, () -> upload(new AvatarService(root, dao, () -> connection(false)))).getStatus());
        assertEquals(0, count());
    }
    @Test void ambiguousCommitRetainsFilesForDatabaseReference() throws Exception {
        FakeDAO dao = new FakeDAO();
        assertThrows(SQLException.class, () -> upload(new AvatarService(root, dao, () -> connection(true))));
        assertEquals(2, count());
    }
    @Test void storageFailureDoesNotChangeDatabase() throws Exception {
        Path file = root.resolve("not-directory"); Files.writeString(file, "x");
        FakeDAO dao = new FakeDAO();
        assertThrows(java.io.IOException.class, () -> upload(new AvatarService(file, dao, () -> connection(false))));
        assertNull(dao.value);
    }
    @Test void rejectsTamperedDatabasePath() {
        FakeDAO dao = new FakeDAO(); dao.value = new UserAvatar("../../secret", "../../secret");
        assertThrows(java.io.IOException.class, () -> new AvatarService(root, dao, () -> connection(false)).read(42, false));
    }
}
