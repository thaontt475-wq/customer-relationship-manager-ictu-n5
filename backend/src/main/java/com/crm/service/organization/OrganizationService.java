package com.crm.service.organization;

import com.crm.dao.organization.OrganizationDAO;
import com.crm.dao.teams.UserTeamDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.Organization;
import com.crm.model.User;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class OrganizationService {

    private static final int MAX_NAME_LENGTH = 150;
    private static final Set<String> VALID_REGIONS = Set.of(
            "NORTH", "CENTRAL", "SOUTH", "NATIONAL", "OVERSEAS"
    );

    private final OrganizationDAO organizationDAO;
    private final UserDAO userDAO;
    private final UserTeamDAO userTeamDAO;
    private final ConnectionProvider connectionProvider;

    public OrganizationService() {
        this(new OrganizationDAO(), new UserDAO(), new UserTeamDAO(), DBConnection::getConnection);
    }

    public OrganizationService(
            OrganizationDAO organizationDAO,
            UserDAO userDAO,
            UserTeamDAO userTeamDAO,
            ConnectionProvider connectionProvider) {
        this.organizationDAO = organizationDAO != null ? organizationDAO : new OrganizationDAO();
        this.userDAO = userDAO != null ? userDAO : new UserDAO();
        this.userTeamDAO = userTeamDAO != null ? userTeamDAO : new UserTeamDAO();
        this.connectionProvider = connectionProvider != null
                ? connectionProvider
                : DBConnection::getConnection;
    }

    public List<Organization> getUnits() throws SQLException {
        try (Connection conn = connectionProvider.getConnection()) {
            return organizationDAO.findAll(conn);
        }
    }

    public Organization createUnit(UnitInput input) throws SQLException, OrganizationException {
        NormalizedInput normalized = validateAndNormalize(input);

        try (Connection conn = connectionProvider.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                if (normalized.parentId() != null
                        && organizationDAO.findByIdForUpdate(conn, normalized.parentId()) == null) {
                    throw error(ErrorCode.PARENT_NOT_FOUND, "Đơn vị cha không tồn tại");
                }

                ensureNameAvailable(conn, normalized.name(), null);
                User leader = lockAndValidateLeader(conn, normalized.managerId());
                ensureLeaderCanManage(conn, leader, null);

                long unitId = organizationDAO.insert(
                        conn,
                        normalized.name(),
                        normalized.parentId(),
                        normalized.managerId(),
                        normalized.region(),
                        normalized.active()
                );
                if (unitId <= 0) {
                    throw error(ErrorCode.UPDATE_CONFLICT, "Không thể tạo đơn vị");
                }

                int assigned = userTeamDAO.assignUserToTeam(conn, leader.getId(), unitId);
                if (assigned != 1) {
                    throw error(ErrorCode.UPDATE_CONFLICT, "Không thể gán trưởng đơn vị vào đơn vị mới");
                }

                Organization created = organizationDAO.findById(conn, unitId);
                if (created == null) {
                    throw error(ErrorCode.UPDATE_CONFLICT, "Không thể đọc lại đơn vị vừa tạo");
                }

                conn.commit();
                return created;
            } catch (SQLException | OrganizationException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public Organization updateUnit(long unitId, UnitInput input)
            throws SQLException, OrganizationException {
        if (unitId <= 0) {
            throw error(ErrorCode.VALIDATION, "ID đơn vị không hợp lệ");
        }
        NormalizedInput normalized = validateAndNormalize(input);

        try (Connection conn = connectionProvider.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                Organization existing = organizationDAO.findByIdForUpdate(conn, unitId);
                if (existing == null) {
                    throw error(ErrorCode.UNIT_NOT_FOUND, "Không tìm thấy đơn vị");
                }

                validateAndLockParentPath(conn, unitId, normalized.parentId());
                ensureNameAvailable(conn, normalized.name(), unitId);

                User leader = lockAndValidateLeader(conn, normalized.managerId());
                ensureLeaderCanManage(conn, leader, unitId);

                int updated = organizationDAO.update(
                        conn,
                        unitId,
                        normalized.name(),
                        normalized.parentId(),
                        normalized.managerId(),
                        normalized.region(),
                        normalized.active()
                );
                if (updated != 1) {
                    throw error(ErrorCode.UPDATE_CONFLICT, "Đơn vị đã thay đổi, vui lòng thử lại");
                }

                if (leader.getTeamId() == null) {
                    int assigned = userTeamDAO.assignUserToTeam(conn, leader.getId(), unitId);
                    if (assigned != 1) {
                        throw error(ErrorCode.UPDATE_CONFLICT, "Không thể gán trưởng đơn vị vào đơn vị");
                    }
                }

                Organization updatedUnit = organizationDAO.findById(conn, unitId);
                if (updatedUnit == null) {
                    throw error(ErrorCode.UPDATE_CONFLICT, "Không thể đọc lại đơn vị vừa cập nhật");
                }

                conn.commit();
                return updatedUnit;
            } catch (SQLException | OrganizationException | RuntimeException e) {
                rollback(conn, e);
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private NormalizedInput validateAndNormalize(UnitInput input) throws OrganizationException {
        if (input == null) {
            throw error(ErrorCode.VALIDATION, "Dữ liệu đơn vị không được để trống");
        }

        String name = input.name() == null ? "" : input.name().trim();
        if (name.isEmpty()) {
            throw error(ErrorCode.VALIDATION, "Tên đơn vị không được để trống");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw error(ErrorCode.VALIDATION, "Tên đơn vị không được vượt quá 150 ký tự");
        }

        Long parentId = input.parentId();
        if (parentId != null && parentId <= 0) {
            throw error(ErrorCode.VALIDATION, "ID đơn vị cha không hợp lệ");
        }

        Long managerId = input.managerId();
        if (managerId == null || managerId <= 0) {
            throw error(ErrorCode.VALIDATION, "Trưởng đơn vị là bắt buộc");
        }

        String region = input.region() == null
                ? ""
                : input.region().trim().toUpperCase(Locale.ROOT);
        if (!VALID_REGIONS.contains(region)) {
            throw error(ErrorCode.INVALID_REGION,
                    "Khu vực phải là NORTH, CENTRAL, SOUTH, NATIONAL hoặc OVERSEAS");
        }

        return new NormalizedInput(
                name,
                parentId,
                managerId,
                region,
                input.active() == null || input.active()
        );
    }

    private void validateAndLockParentPath(Connection conn, long unitId, Long parentId)
            throws SQLException, OrganizationException {
        if (parentId == null) {
            return;
        }
        if (parentId == unitId) {
            throw error(ErrorCode.SELF_PARENT, "Đơn vị không thể là cha của chính nó");
        }

        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        boolean first = true;
        while (cursor != null) {
            if (cursor == unitId || !visited.add(cursor)) {
                throw error(ErrorCode.CYCLE, "Cập nhật đơn vị cha sẽ tạo chu trình");
            }

            Organization node = organizationDAO.findByIdForUpdate(conn, cursor);
            if (node == null) {
                if (first) {
                    throw error(ErrorCode.PARENT_NOT_FOUND, "Đơn vị cha không tồn tại");
                }
                throw error(ErrorCode.CYCLE, "Cây tổ chức hiện tại không nhất quán");
            }
            cursor = node.getParentId();
            first = false;
        }
    }

    private void ensureNameAvailable(Connection conn, String name, Long excludedId)
            throws SQLException, OrganizationException {
        if (organizationDAO.existsByNameExcluding(conn, name, excludedId)) {
            throw error(ErrorCode.DUPLICATE_NAME, "Tên đơn vị đã tồn tại");
        }
    }

    private User lockAndValidateLeader(Connection conn, long managerId)
            throws SQLException, OrganizationException {
        User leader = userDAO.findByIdForUpdate(conn, managerId);
        if (leader == null) {
            throw error(ErrorCode.LEADER_NOT_FOUND, "Người được chọn làm trưởng đơn vị không tồn tại");
        }
        if (!leader.isActive()
                || leader.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(leader.getStatus().trim())) {
            throw error(ErrorCode.INVALID_LEADER, "Trưởng đơn vị phải là người dùng đang hoạt động");
        }
        return leader;
    }

    private void ensureLeaderCanManage(Connection conn, User leader, Long targetUnitId)
            throws SQLException, OrganizationException {
        Long leaderUnitId = organizationDAO.findUnitIdByLeader(conn, leader.getId());
        if (leaderUnitId != null && !leaderUnitId.equals(targetUnitId)) {
            throw error(ErrorCode.LEADER_ALREADY_ASSIGNED,
                    "Người dùng đã là trưởng của một đơn vị khác");
        }

        Long currentTeamId = leader.getTeamId();
        if (currentTeamId != null && !currentTeamId.equals(targetUnitId)) {
            throw error(ErrorCode.USER_ALREADY_IN_TEAM,
                    "Người dùng đã thuộc một đơn vị khác");
        }
    }

    private void rollback(Connection conn, Exception cause) {
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            cause.addSuppressed(rollbackError);
        }
    }

    private OrganizationException error(ErrorCode code, String message) {
        return new OrganizationException(code, message);
    }

    public record UnitInput(
            String name,
            Long parentId,
            Long managerId,
            String region,
            Boolean active) {
    }

    private record NormalizedInput(
            String name,
            Long parentId,
            long managerId,
            String region,
            boolean active) {
    }

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    public enum ErrorCode {
        VALIDATION,
        INVALID_REGION,
        UNIT_NOT_FOUND,
        PARENT_NOT_FOUND,
        LEADER_NOT_FOUND,
        INVALID_LEADER,
        SELF_PARENT,
        CYCLE,
        DUPLICATE_NAME,
        USER_ALREADY_IN_TEAM,
        LEADER_ALREADY_ASSIGNED,
        UPDATE_CONFLICT
    }

    public static class OrganizationException extends Exception {
        private final ErrorCode code;

        public OrganizationException(ErrorCode code, String message) {
            super(message);
            this.code = code;
        }

        public ErrorCode getCode() {
            return code;
        }
    }
}
