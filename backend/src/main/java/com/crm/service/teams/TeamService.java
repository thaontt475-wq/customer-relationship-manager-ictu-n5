package com.crm.service.teams;

import com.crm.dao.teams.TeamDAO;
import com.crm.dao.users.UserDAO;
import com.crm.dao.users.UserManagementDAO;
import com.crm.dao.permissions.PermissionDAO;

import java.util.List;
import java.util.Map;

public class TeamService {

    private final TeamDAO teamDAO = new TeamDAO();
    private final UserDAO userDAO = new UserDAO();
    private final UserManagementDAO userManagementDAO = new UserManagementDAO();
    private final PermissionDAO permissionDAO = new PermissionDAO();

    public List<Map<String, Object>> getTeams(
            String keyword,
            Boolean active
    ) throws Exception {
        return getTeams(keyword, active, null);
    }

    public List<Map<String, Object>> getTeams(
            String keyword,
            Boolean active,
            Long regionId
    ) throws Exception {
        return teamDAO.findAll(keyword, active, regionId);
    }

    public Map<String, Object> getTeamById(long id) throws Exception {
        Map<String, Object> team = teamDAO.findById(id);
        if (team == null) {
            throw new IllegalArgumentException("Nhóm kinh doanh không tồn tại");
        }
        return team;
    }

    public List<Map<String, Object>> getTeamMembers(long teamId) throws Exception {
        if (teamDAO.findById(teamId) == null) {
            throw new IllegalArgumentException("Nhóm kinh doanh không tồn tại");
        }
        return teamDAO.getMembers(teamId);
    }

    public Map<String, Object> getLookups() throws Exception {
        return teamDAO.getLookups();
    }

    public Map<String, Object> createTeam(Map<String, Object> body) throws Exception {
        String name = getString(body, "name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên nhóm kinh doanh là bắt buộc");
        }
        name = name.trim();

        if (teamDAO.existsName(name, null)) {
            throw new IllegalArgumentException("Tên nhóm kinh doanh đã tồn tại");
        }

        Long parentId = getLong(body, "parentId");
        if (parentId != null && teamDAO.findById(parentId) == null) {
            throw new IllegalArgumentException("Nhóm cha không tồn tại");
        }

        Long leaderId = getLong(body, "leaderId");
        if (leaderId != null && !userDAO.existsById(leaderId)) {
            throw new IllegalArgumentException("Trưởng nhóm không tồn tại");
        }

        Long regionId = getLong(body, "regionId");
        boolean active = body.containsKey("active") ? Boolean.TRUE.equals(body.get("active")) : true;

        long id = teamDAO.create(name, parentId, leaderId, regionId, active);

        // If leader is specified, assign leader to this team as well
        if (leaderId != null) {
            userManagementDAO.assignTeam(leaderId, id);
        }

        return teamDAO.findById(id);
    }

    public Map<String, Object> updateTeam(long id, Map<String, Object> body) throws Exception {
        Map<String, Object> existing = teamDAO.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Nhóm kinh doanh không tồn tại");
        }

        String name = getString(body, "name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên nhóm kinh doanh là bắt buộc");
        }
        name = name.trim();

        if (teamDAO.existsName(name, id)) {
            throw new IllegalArgumentException("Tên nhóm kinh doanh đã tồn tại");
        }

        Long parentId = getLong(body, "parentId");
        if (parentId != null) {
            if (parentId == id) {
                throw new IllegalArgumentException("Nhóm không thể là cha của chính nó");
            }
            if (teamDAO.findById(parentId) == null) {
                throw new IllegalArgumentException("Nhóm cha không tồn tại");
            }
            if (teamDAO.createsCycle(id, parentId)) {
                throw new IllegalArgumentException("Không thể tạo vòng lặp cây tổ chức nhóm");
            }
        }

        Long leaderId = getLong(body, "leaderId");
        if (leaderId != null && !userDAO.existsById(leaderId)) {
            throw new IllegalArgumentException("Trưởng nhóm không tồn tại");
        }

        Long regionId = getLong(body, "regionId");
        boolean active = body.containsKey("active") ? Boolean.TRUE.equals(body.get("active")) : true;

        teamDAO.update(id, name, parentId, leaderId, regionId, active);

        if (leaderId != null) {
            userManagementDAO.assignTeam(leaderId, id);
        }

        return teamDAO.findById(id);
    }

    public void deleteTeam(long id) throws Exception {
        Map<String, Object> existing = teamDAO.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Nhóm kinh doanh không tồn tại");
        }

        if (teamDAO.hasChildren(id)) {
            throw new IllegalArgumentException("Không thể xóa nhóm đang có nhóm con");
        }

        if (teamDAO.hasMembers(id)) {
            throw new IllegalArgumentException("Không thể xóa nhóm đang có nhân viên trực thuộc");
        }

        teamDAO.delete(id);
    }

    public Map<String, Object> assignTeam(
            long userId,
            Long teamId
    ) throws Exception {

        if (!userDAO.existsById(userId)) {
            throw new IllegalArgumentException(
                    "User không tồn tại"
            );
        }

        if (
                teamId != null &&
                !teamDAO.existsActive(teamId)
        ) {
            throw new IllegalArgumentException(
                    "Team không tồn tại hoặc đã bị khóa"
            );
        }

        if (teamId == null && permissionDAO.hasRole(userId, "TEAM_LEAD")) {
            throw new IllegalArgumentException("Trưởng nhóm kinh doanh phải được gán vào một nhóm.");
        }

        userManagementDAO.assignTeam(
                userId,
                teamId
        );

        return userManagementDAO.findById(
                userId
        );
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try {
            String s = String.valueOf(val).trim();
            return s.isEmpty() ? null : Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
