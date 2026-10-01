package com.crm.service.winloss;

import com.crm.dao.winloss.WinLossDAO;
import com.crm.model.Competitor;
import com.crm.model.WinLossReason;
import com.crm.util.DBConnection;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WinLossService {
    private static final Set<String> REASON_TYPES = Set.of("WIN", "LOSS");
    private final WinLossDAO dao;
    private final ConnectionProvider connectionProvider;

    @FunctionalInterface
    public interface ConnectionProvider { Connection getConnection() throws SQLException; }
    public enum DeleteOutcome { DELETED, DEACTIVATED }
    public record ReasonsResult(List<WinLossReason> winReasons, List<WinLossReason> lossReasons) { }

    public WinLossService() { this(new WinLossDAO(), DBConnection::getConnection); }

    public WinLossService(WinLossDAO dao, ConnectionProvider connectionProvider) {
        this.dao = dao;
        this.connectionProvider = connectionProvider;
    }

    public ReasonsResult getReasons(boolean includeInactive) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return new ReasonsResult(
                    dao.findReasons(connection, "WIN", !includeInactive),
                    dao.findReasons(connection, "LOSS", !includeInactive));
        }
    }

    public WinLossReason createReason(WinLossReason reason) throws SQLException {
        normalizeReason(reason);
        try (Connection connection = connectionProvider.getConnection()) {
            return transaction(connection, () -> {
                ensureReasonUnique(connection, reason, null);
                if (reason.getDisplayOrder() <= 0) {
                    reason.setDisplayOrder(dao.nextReasonOrder(connection, reason.getType()));
                }
                long id;
                try {
                    id = dao.insertReason(connection, reason);
                } catch (SQLException e) {
                    if (isIntegrityConstraint(e)) throw new DuplicateException("Lý do đã tồn tại trong cùng nhóm");
                    throw e;
                }
                reason.setId(id);
                return dao.findReasonById(connection, id);
            });
        }
    }

    public WinLossReason updateReason(WinLossReason reason) throws SQLException {
        requireId(reason == null ? null : reason.getId(), "ID lý do");
        normalizeReason(reason);
        try (Connection connection = connectionProvider.getConnection()) {
            return transaction(connection, () -> {
                WinLossReason existing = dao.findReasonById(connection, reason.getId());
                if (existing == null) throw new NotFoundException("Không tìm thấy lý do cần cập nhật");
                if (!existing.getType().equals(reason.getType())) {
                    throw new IllegalArgumentException("Không được chuyển lý do giữa nhóm WIN và LOSS");
                }
                ensureReasonUnique(connection, reason, reason.getId());
                if (reason.getDisplayOrder() <= 0) reason.setDisplayOrder(existing.getDisplayOrder());
                try {
                    dao.updateReason(connection, reason);
                } catch (SQLException e) {
                    if (isIntegrityConstraint(e)) throw new DuplicateException("Lý do đã tồn tại trong cùng nhóm");
                    throw e;
                }
                return dao.findReasonById(connection, reason.getId());
            });
        }
    }

    public DeleteOutcome deleteReason(long id) throws SQLException {
        requireId(id, "ID lý do");
        try (Connection connection = connectionProvider.getConnection()) {
            return transaction(connection, () -> {
                if (dao.findReasonById(connection, id) == null) {
                    throw new NotFoundException("Không tìm thấy lý do cần xóa");
                }
                try {
                    dao.deleteReason(connection, id);
                    return DeleteOutcome.DELETED;
                } catch (SQLException e) {
                    if (!isIntegrityConstraint(e)) throw e;
                    dao.deactivateReason(connection, id);
                    return DeleteOutcome.DEACTIVATED;
                }
            });
        }
    }

    public List<Competitor> getCompetitors(boolean includeInactive) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return dao.findCompetitors(connection, !includeInactive);
        }
    }

    public Competitor createCompetitor(Competitor competitor) throws SQLException {
        normalizeCompetitor(competitor);
        try (Connection connection = connectionProvider.getConnection()) {
            return transaction(connection, () -> {
                ensureCompetitorUnique(connection, competitor, null);
                if (competitor.getDisplayOrder() <= 0) {
                    competitor.setDisplayOrder(dao.nextCompetitorOrder(connection));
                }
                long id;
                try {
                    id = dao.insertCompetitor(connection, competitor);
                } catch (SQLException e) {
                    if (isIntegrityConstraint(e)) throw new DuplicateException("Tên đối thủ đã tồn tại");
                    throw e;
                }
                competitor.setId(id);
                return dao.findCompetitorById(connection, id);
            });
        }
    }

    public Competitor updateCompetitor(Competitor competitor) throws SQLException {
        requireId(competitor == null ? null : competitor.getId(), "ID đối thủ");
        normalizeCompetitor(competitor);
        try (Connection connection = connectionProvider.getConnection()) {
            return transaction(connection, () -> {
                Competitor existing = dao.findCompetitorById(connection, competitor.getId());
                if (existing == null) throw new NotFoundException("Không tìm thấy đối thủ cần cập nhật");
                ensureCompetitorUnique(connection, competitor, competitor.getId());
                if (competitor.getDisplayOrder() <= 0) competitor.setDisplayOrder(existing.getDisplayOrder());
                try {
                    dao.updateCompetitor(connection, competitor);
                } catch (SQLException e) {
                    if (isIntegrityConstraint(e)) throw new DuplicateException("Tên đối thủ đã tồn tại");
                    throw e;
                }
                return dao.findCompetitorById(connection, competitor.getId());
            });
        }
    }

    public DeleteOutcome deleteCompetitor(long id) throws SQLException {
        requireId(id, "ID đối thủ");
        try (Connection connection = connectionProvider.getConnection()) {
            return transaction(connection, () -> {
                if (dao.findCompetitorById(connection, id) == null) {
                    throw new NotFoundException("Không tìm thấy đối thủ cần xóa");
                }
                try {
                    dao.deleteCompetitor(connection, id);
                    return DeleteOutcome.DELETED;
                } catch (SQLException e) {
                    if (!isIntegrityConstraint(e)) throw e;
                    dao.deactivateCompetitor(connection, id);
                    return DeleteOutcome.DEACTIVATED;
                }
            });
        }
    }

    private void normalizeReason(WinLossReason reason) {
        if (reason == null) throw new IllegalArgumentException("Dữ liệu lý do không hợp lệ");
        String type = clean(reason.getType());
        type = type == null ? null : type.toUpperCase(Locale.ROOT);
        if (!REASON_TYPES.contains(type)) throw new IllegalArgumentException("Phân loại lý do chỉ nhận WIN hoặc LOSS");
        reason.setType(type);
        String text = clean(reason.getReasonText());
        if (text == null || text.length() > 255) {
            throw new IllegalArgumentException("Nội dung lý do là bắt buộc và không quá 255 ký tự");
        }
        reason.setReasonText(text);
        reason.setDescription(optional(reason.getDescription(), 1000, "Mô tả"));
        if (reason.getDisplayOrder() < 0) throw new IllegalArgumentException("Thứ tự hiển thị không hợp lệ");
    }

    private void normalizeCompetitor(Competitor competitor) {
        if (competitor == null) throw new IllegalArgumentException("Dữ liệu đối thủ không hợp lệ");
        String name = clean(competitor.getName());
        if (name == null || name.length() > 255) {
            throw new IllegalArgumentException("Tên đối thủ là bắt buộc và không quá 255 ký tự");
        }
        competitor.setName(name);
        competitor.setStrengths(optional(competitor.getStrengths(), 1000, "Điểm mạnh"));
        competitor.setWeaknesses(optional(competitor.getWeaknesses(), 1000, "Điểm yếu"));
        competitor.setWebsite(validateWebsite(competitor.getWebsite()));
        if (competitor.getDisplayOrder() < 0) throw new IllegalArgumentException("Thứ tự hiển thị không hợp lệ");
    }

    private String validateWebsite(String value) {
        String website = clean(value);
        if (website == null) return null;
        if (website.length() > 500) throw new IllegalArgumentException("Website không được vượt quá 500 ký tự");
        try {
            URI uri = new URI(website);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("Website phải là URL http/https hợp lệ");
            }
            return website;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Website phải là URL http/https hợp lệ");
        }
    }

    private void ensureReasonUnique(Connection connection, WinLossReason reason, Long excludeId)
            throws SQLException {
        if (dao.reasonExists(connection, reason.getType(), reason.getReasonText(), excludeId)) {
            throw new DuplicateException("Lý do đã tồn tại trong cùng nhóm " + reason.getType());
        }
    }

    private void ensureCompetitorUnique(Connection connection, Competitor competitor, Long excludeId)
            throws SQLException {
        if (dao.competitorExists(connection, competitor.getName(), excludeId)) {
            throw new DuplicateException("Tên đối thủ đã tồn tại");
        }
    }

    private String optional(String value, int maxLength, String label) {
        String result = clean(value);
        if (result != null && result.length() > maxLength) {
            throw new IllegalArgumentException(label + " không được vượt quá " + maxLength + " ký tự");
        }
        return result;
    }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static void requireId(Long id, String label) {
        if (id == null || id <= 0) throw new IllegalArgumentException(label + " không hợp lệ");
    }

    private static boolean isIntegrityConstraint(SQLException error) {
        return error.getSQLState() != null && error.getSQLState().startsWith("23");
    }

    private <T> T transaction(Connection connection, SqlWork<T> work) throws SQLException {
        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.run();
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            try { connection.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
            throw e;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    @FunctionalInterface private interface SqlWork<T> { T run() throws SQLException; }

    public static class NotFoundException extends IllegalArgumentException {
        public NotFoundException(String message) { super(message); }
    }

    public static class DuplicateException extends IllegalArgumentException {
        public DuplicateException(String message) { super(message); }
    }

}
