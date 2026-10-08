package com.crm.service.teams;

import com.crm.dao.teams.TeamDAO;
import com.crm.dao.users.UserDAO;
import com.crm.dao.users.UserManagementDAO;
import com.crm.dao.permissions.PermissionDAO;

import java.util.List;
import java.util.Map;

public class TeamService {

    private final TeamDAO teamDAO =
            new TeamDAO();

    private final UserDAO userDAO =
            new UserDAO();

    private final UserManagementDAO userManagementDAO =
            new UserManagementDAO();
    private final PermissionDAO permissionDAO = new PermissionDAO();

    public List<Map<String, Object>> getTeams(
            String keyword,
            Boolean active
    ) throws Exception {

        return teamDAO.findAll(
                keyword,
                active
        );
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
}
