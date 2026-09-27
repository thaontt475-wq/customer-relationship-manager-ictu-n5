package com.crm.service.teams;

import com.crm.dao.teams.TeamDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.Team;
import com.crm.model.User;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class TeamService {

    private final TeamDAO teamDAO = new TeamDAO();
    private final UserDAO userDAO = new UserDAO();

    public List<Team> findAllTeams() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return teamDAO.findAll(conn);
        }
    }

    public AssignmentResult assignUserToTeam(
            long userId,
            long teamId) throws SQLException {

        if (userId <= 0) {
            return AssignmentResult.INVALID_USER;
        }

        if (teamId <= 0) {
            return AssignmentResult.INVALID_TEAM;
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();

            try {
                conn.setAutoCommit(false);

                User user = userDAO.findByIdForUpdate(conn, userId);
                if (user == null) {
                    conn.rollback();
                    return AssignmentResult.USER_NOT_FOUND;
                }

                Team team = teamDAO.findById(conn, teamId);
                if (team == null) {
                    conn.rollback();
                    return AssignmentResult.TEAM_NOT_FOUND;
                }

                int updated =
                        teamDAO.assignUserToTeam(
                                conn,
                                userId,
                                teamId
                        );

                if (updated != 1) {
                    conn.rollback();
                    return AssignmentResult.UPDATE_CONFLICT;
                }

                conn.commit();
                return AssignmentResult.SUCCESS;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public enum AssignmentResult {
        SUCCESS,
        INVALID_USER,
        INVALID_TEAM,
        USER_NOT_FOUND,
        TEAM_NOT_FOUND,
        UPDATE_CONFLICT
    }
}