package com.crm.service.users;

import com.crm.dao.users.AvatarDAO;
import com.crm.model.UserAvatar;
import com.crm.util.DBConnection;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class AvatarService {
    private static final Logger LOG = Logger.getLogger(AvatarService.class.getName());
    @FunctionalInterface interface Connections { Connection open() throws SQLException; }
    private final Path root;
    private final AvatarDAO dao;
    private final Connections connections;

    public AvatarService(Path root) { this(root, new AvatarDAO(), DBConnection::getConnection); }
    AvatarService(Path root, AvatarDAO dao, Connections connections) {
        this.root = root.toAbsolutePath().normalize();
        this.dao = dao;
        this.connections = connections;
    }

    public UserAvatar find(long userId) throws SQLException {
        try (Connection conn = connections.open()) { return dao.find(conn, userId); }
    }

    public UserAvatar upload(long userId, InputStream input, String filename, long size)
            throws IOException, SQLException, AvatarException {
        if (userId <= 0) throw new AvatarException(401, "Yêu cầu đăng nhập.");
        var images = new AvatarImageProcessor().process(input, filename, size);
        Files.createDirectories(root);
        String key = UUID.randomUUID().toString();
        UserAvatar next = new UserAvatar(key + ".png", key + "-thumb.png");
        boolean commitAttempted = false;
        UserAvatar previous = null;
        try {
            write(next.imagePath(), images.image());
            write(next.thumbnailPath(), images.thumbnail());
            try (Connection conn = connections.open()) {
                conn.setAutoCommit(false);
                try {
                    // Lock the user row, including first upload, to serialize concurrent replacements.
                    if (!dao.lockActiveUser(conn, userId)) throw new AvatarException(403, "Tài khoản không còn hoạt động.");
                    previous = dao.find(conn, userId);
                    dao.save(conn, userId, next);
                    commitAttempted = true;
                    conn.commit();
                } catch (SQLException | AvatarException | RuntimeException e) {
                    try { conn.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                    throw e;
                }
            }
        } catch (IOException | SQLException | AvatarException | RuntimeException e) {
            // A lost commit acknowledgement is ambiguous: preserve files in case DB committed.
            if (!commitAttempted) cleanup(next);
            throw e;
        }
        if (previous != null) cleanup(previous);
        return next;
    }

    public byte[] read(long userId, boolean thumbnail) throws SQLException, IOException {
        UserAvatar avatar = find(userId);
        if (avatar == null) return null;
        try { return Files.readAllBytes(resolve(thumbnail ? avatar.thumbnailPath() : avatar.imagePath())); }
        catch (java.nio.file.NoSuchFileException e) { return null; }
    }

    private Path resolve(String key) throws IOException {
        if (key == null || !key.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}(-thumb)?\\.png"))
            throw new IOException("Invalid stored avatar key");
        return root.resolve(key);
    }
    private void write(String key, java.awt.image.BufferedImage image) throws IOException {
        try (var out = Files.newOutputStream(resolve(key), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            if (!ImageIO.write(image, "png", out)) throw new IOException("PNG writer unavailable");
        }
    }
    private void cleanup(UserAvatar avatar) {
        for (String key : new String[]{avatar.imagePath(), avatar.thumbnailPath()}) {
            try { Files.deleteIfExists(resolve(key)); }
            catch (IOException e) { LOG.log(Level.WARNING, "Unable to remove unused avatar file " + key, e); }
        }
    }
}
