package com.crm.dao.teams;

import com.crm.model.Team;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO handling User - Team relationship operations (CRM-29).
 * Manages team assignment and user team lookup via JDBC.
 */
public class UserTeamDAO {
    private static final Logger LOGGER = Logger.getLogger(UserTeamDAO.class.getName());

    /**
     * Get the team ID currently assigned to a user.
     *
     * @param conn   active JDBC connection
     * @param userId target user ID
     * @return team ID or null if user has no team
     */
    public Long findTeamIdByUserId(Connection conn, long userId) throws SQLException {
        if (conn == null || userId <= 0) {
            return null;
        }

        String sql = "SELECT team_id FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long teamId = rs.getLong("team_id");
                    return rs.wasNull() ? null : teamId;
                }
            }
        }
        return null;
    }

    public Long findTeamIdByUserId(long userId) {
        if (userId <= 0) {
            return null;
        }
        try (Connection conn = DBConnection.getConnection()) {
            return findTeamIdByUserId(conn, userId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding team ID for userId: " + userId, e);
            return null;
        }
    }

    /**
     * Find and lock the team led by a user.
     *
     * @param conn   active JDBC connection
     * @param userId target user ID
     * @return led team ID or null if the user is not a team leader
     */
    public Long findLedTeamIdByUserId(Connection conn, long userId) throws SQLException {
        if (conn == null || userId <= 0) {
            return null;
        }

        String sql = "SELECT id FROM teams WHERE leader_user_id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    /**
     * Assign (or reassign) a user to a team by updating the team_id field on the users table.
     *
     * @param conn   active JDBC connection
     * @param userId target user ID
     * @param teamId target team ID (must be > 0)
     * @return number of rows updated (should be 1)
     */
    public int assignUserToTeam(Connection conn, long userId, long teamId) throws SQLException {
        if (conn == null || userId <= 0 || teamId <= 0) {
            return 0;
        }

        String sql = "UPDATE users u "
                + "LEFT JOIN teams managed ON managed.leader_user_id = u.id "
                + "SET u.team_id = ? "
                + "WHERE u.id = ? AND (managed.id IS NULL OR managed.id = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, teamId);
            stmt.setLong(2, userId);
            stmt.setLong(3, teamId);
            return stmt.executeUpdate();
        }
    }

    public int assignUserToTeam(long userId, long teamId) {
        try (Connection conn = DBConnection.getConnection()) {
            return assignUserToTeam(conn, userId, teamId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error assigning user " + userId + " to team " + teamId, e);
            return 0;
        }
    }

    /**
     * Remove a user from their current team (set team_id = NULL).
     *
     * @param conn   active JDBC connection
     * @param userId target user ID
     * @return number of rows updated
     */
    public int removeUserFromTeam(Connection conn, long userId) throws SQLException {
        if (conn == null || userId <= 0) {
            return 0;
        }

        String sql = "UPDATE users u "
                + "LEFT JOIN teams managed ON managed.leader_user_id = u.id "
                + "SET u.team_id = NULL "
                + "WHERE u.id = ? AND managed.id IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return stmt.executeUpdate();
        }
    }

    public int removeUserFromTeam(long userId) {
        try (Connection conn = DBConnection.getConnection()) {
            return removeUserFromTeam(conn, userId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error removing user from team: " + userId, e);
            return 0;
        }
    }

    /**
     * Check if a team exists by its ID.
     */
    public boolean teamExists(Connection conn, long teamId) throws SQLException {
        if (conn == null || teamId <= 0) {
            return false;
        }

        String sql = "SELECT 1 FROM teams WHERE id = ? AND active = TRUE LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean teamExists(long teamId) {
        try (Connection conn = DBConnection.getConnection()) {
            return teamExists(conn, teamId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if team exists: " + teamId, e);
            return false;
        }
    }

    /**
     * Find a team by ID.
     */
    public Team findTeamById(Connection conn, long teamId) throws SQLException {
        if (conn == null || teamId <= 0) {
            return null;
        }

        String sql = "SELECT id, name FROM teams WHERE id = ? AND active = TRUE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Team(rs.getLong("id"), rs.getString("name"));
                }
            }
        }
        return null;
    }

    public Team findTeamById(long teamId) {
        if (teamId <= 0) {
            return null;
        }
        try (Connection conn = DBConnection.getConnection()) {
            return findTeamById(conn, teamId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding team by id: " + teamId, e);
            return null;
        }
    }

    /**
     * Find all teams.
     */
    public List<Team> findAllTeams(Connection conn) throws SQLException {
        if (conn == null) {
            return List.of();
        }

        String sql = "SELECT id, name FROM teams WHERE active = TRUE ORDER BY name";
        List<Team> teams = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                teams.add(new Team(rs.getLong("id"), rs.getString("name")));
            }
        }
        return teams;
    }

    public List<Team> findAllTeams() {
        try (Connection conn = DBConnection.getConnection()) {
            return findAllTeams(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all teams", e);
            return List.of();
        }
    }

    /**
     * Find all user IDs belonging to a given team.
     */
    public List<Long> findUserIdsByTeamId(Connection conn, long teamId) throws SQLException {
        if (conn == null || teamId <= 0) {
            return List.of();
        }

        String sql = "SELECT id FROM users WHERE team_id = ? ORDER BY id";
        List<Long> userIds = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getLong("id"));
                }
            }
        }
        return userIds;
    }

    public List<Long> findUserIdsByTeamId(long teamId) {
        if (teamId <= 0) {
            return List.of();
        }
        try (Connection conn = DBConnection.getConnection()) {
            return findUserIdsByTeamId(conn, teamId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user IDs for teamId: " + teamId, e);
            return List.of();
        }
    }
}
