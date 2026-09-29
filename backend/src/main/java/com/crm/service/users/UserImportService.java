package com.crm.service.users;

import com.crm.dao.permissions.PermissionDAO;
import com.crm.dao.teams.TeamDAO;
import com.crm.dao.users.UserDAO;
import com.crm.dto.users.UserImportResult;
import com.crm.dto.users.UserImportRow;
import com.crm.model.Role;
import com.crm.model.Team;
import com.crm.model.User;
import com.crm.util.DBConnection;
import com.crm.util.PasswordUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class UserImportService {

    private final UserDAO userDAO;
    private final TeamDAO teamDAO;
    private final PermissionDAO permissionDAO;
    private final UserExcelParser excelParser;

    public UserImportService() {
        this(new UserDAO(), new TeamDAO(), new PermissionDAO(), new UserExcelParser());
    }

    public UserImportService(
            UserDAO userDAO,
            TeamDAO teamDAO,
            PermissionDAO permissionDAO,
            UserExcelParser excelParser) {
        this.userDAO = userDAO;
        this.teamDAO = teamDAO;
        this.permissionDAO = permissionDAO;
        this.excelParser = excelParser;
    }

    public void generateTemplateXlsx(OutputStream out) throws IOException {
        excelParser.writeTemplateXlsx(out);
    }

    public void generateTemplateCsv(Writer writer) throws IOException {
        excelParser.writeTemplateCsv(writer);
    }

    public UserImportResult preview(InputStream in, String filename) throws IOException, SQLException {
        List<UserImportRow> rawRows = excelParser.parse(in, filename);
        UserImportResult result = new UserImportResult();
        result.setTotalRows(rawRows.size());

        try (Connection conn = DBConnection.getConnection()) {
            validateRows(conn, rawRows);
        }

        int validCount = 0;
        int errorCount = 0;
        for (UserImportRow r : rawRows) {
            if (r.isValid()) {
                validCount++;
            } else {
                errorCount++;
            }
        }

        result.setValidRows(validCount);
        result.setErrorRows(errorCount);
        result.setRows(rawRows);
        return result;
    }

    public void validateRows(Connection conn, List<UserImportRow> rows) throws SQLException {
        // Load danh mục Roles từ DB
        List<Role> dbRoles = permissionDAO.findAllRoles(conn);
        Map<String, Long> roleMap = new HashMap<>();
        Long defaultRoleId = null;
        for (Role r : dbRoles) {
            roleMap.put(r.getName().trim().toLowerCase(Locale.ROOT), r.getId());
            if ("sales rep".equalsIgnoreCase(r.getName().trim())) {
                defaultRoleId = r.getId();
            }
        }
        if (defaultRoleId == null && !dbRoles.isEmpty()) {
            defaultRoleId = dbRoles.get(0).getId();
        }

        // Load danh mục Teams từ DB
        List<Team> dbTeams = teamDAO.findAll(conn);
        Map<String, Long> teamMap = new HashMap<>();
        for (Team t : dbTeams) {
            teamMap.put(t.getName().trim().toLowerCase(Locale.ROOT), t.getId());
        }

        // Load Users hiện có trong DB để check trùng
        List<User> existingUsers = userDAO.findAll(conn);
        Set<String> dbEmails = new HashSet<>();
        Set<String> dbUsernames = new HashSet<>();
        for (User u : existingUsers) {
            if (u.getEmail() != null) dbEmails.add(u.getEmail().trim().toLowerCase(Locale.ROOT));
            if (u.getUsername() != null) dbUsernames.add(u.getUsername().trim().toLowerCase(Locale.ROOT));
        }

        Set<String> fileEmails = new HashSet<>();
        Set<String> fileUsernames = new HashSet<>();

        for (UserImportRow row : rows) {
            String email = row.getEmail() != null ? row.getEmail().trim() : "";
            String fullName = row.getFullName() != null ? row.getFullName().trim() : "";
            String username = row.getUsername() != null ? row.getUsername().trim() : "";
            String roleName = row.getRoleName() != null ? row.getRoleName().trim() : "";
            String teamName = row.getTeamName() != null ? row.getTeamName().trim() : "";
            String dataScope = row.getDataScope() != null ? row.getDataScope().trim() : "";

            // 1. Kiểm tra Email
            if (email.isEmpty()) {
                row.addError("Email không được để trống");
            } else if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                row.addError("Định dạng email '" + email + "' không hợp lệ");
            } else {
                String normEmail = email.toLowerCase(Locale.ROOT);
                if (dbEmails.contains(normEmail)) {
                    row.addError("Email '" + email + "' đã tồn tại trong hệ thống");
                } else if (fileEmails.contains(normEmail)) {
                    row.addError("Email '" + email + "' bị trùng lặp trong tệp Excel");
                } else {
                    fileEmails.add(normEmail);
                }
            }

            // 2. Kiểm tra Họ và tên
            if (fullName.isEmpty()) {
                row.addError("Họ và tên không được để trống");
            }

            // 3. Kiểm tra Tên đăng nhập (Username)
            if (username.isEmpty()) {
                if (!email.isEmpty() && email.contains("@")) {
                    username = email.split("@")[0].replaceAll("[^a-zA-Z0-9_.-]", "_");
                    row.setUsername(username);
                }
            }

            if (username.isEmpty()) {
                row.addError("Tên đăng nhập không được để trống");
            } else if (username.contains(" ")) {
                row.addError("Tên đăng nhập không được chứa khoảng trắng");
            } else {
                String normUsername = username.toLowerCase(Locale.ROOT);
                if (dbUsernames.contains(normUsername)) {
                    row.addError("Tên đăng nhập '" + username + "' đã tồn tại trong hệ thống");
                } else if (fileUsernames.contains(normUsername)) {
                    row.addError("Tên đăng nhập '" + username + "' bị trùng lặp trong tệp Excel");
                } else {
                    fileUsernames.add(normUsername);
                }
            }

            // 4. Kiểm tra Vai trò (Role)
            if (!roleName.isEmpty()) {
                String normRole = roleName.toLowerCase(Locale.ROOT);
                Long rId = roleMap.get(normRole);
                if (rId == null) {
                    row.addError("Vai trò '" + roleName + "' không tồn tại trong hệ thống");
                } else {
                    row.setResolvedRoleId(rId);
                }
            } else {
                row.setResolvedRoleId(defaultRoleId);
                row.setRoleName("Sales Rep");
            }

            // 5. Kiểm tra Nhóm kinh doanh (Team)
            if (!teamName.isEmpty()) {
                String normTeam = teamName.toLowerCase(Locale.ROOT);
                Long tId = teamMap.get(normTeam);
                if (tId == null) {
                    row.addError("Nhóm kinh doanh '" + teamName + "' không tồn tại trong hệ thống");
                } else {
                    row.setResolvedTeamId(tId);
                }
            }

            // 6. Kiểm tra Phạm vi dữ liệu (Data Scope)
            if (!dataScope.isEmpty()) {
                String upperScope = dataScope.toUpperCase(Locale.ROOT);
                if ("CÁ NHÂN".equalsIgnoreCase(dataScope) || "CA NHAN".equalsIgnoreCase(dataScope)) {
                    upperScope = "SELF";
                } else if ("NHÓM".equalsIgnoreCase(dataScope) || "NHOM".equalsIgnoreCase(dataScope) || "CỦA NHÓM TÔI".equalsIgnoreCase(dataScope)) {
                    upperScope = "TEAM";
                } else if ("TẤT CẢ".equalsIgnoreCase(dataScope) || "TAT CA".equalsIgnoreCase(dataScope)) {
                    upperScope = "ALL";
                }

                if (!"SELF".equals(upperScope) && !"TEAM".equals(upperScope) && !"ALL".equals(upperScope)) {
                    row.addError("Phạm vi dữ liệu '" + dataScope + "' không hợp lệ (hỗ trợ: SELF, TEAM, ALL)");
                } else {
                    row.setDataScope(upperScope);
                }
            } else {
                // Tự suy luận mặc định
                if ("Director".equalsIgnoreCase(row.getRoleName())) {
                    row.setDataScope("ALL");
                } else if ("Team Lead".equalsIgnoreCase(row.getRoleName())) {
                    row.setDataScope("TEAM");
                } else {
                    row.setDataScope("SELF");
                }
            }
        }
    }

    public UserImportResult executeImport(UserImportResult previewResult) throws SQLException {
        if (previewResult == null || previewResult.getRows() == null) {
            return new UserImportResult();
        }

        int importedCount = 0;
        int skippedCount = 0;

        try (Connection conn = DBConnection.getConnection()) {
            for (UserImportRow row : previewResult.getRows()) {
                if (!row.isValid()) {
                    skippedCount++;
                    continue;
                }

                conn.setAutoCommit(false);
                try {
                    String password = row.getPassword();
                    if (password == null || password.trim().isEmpty()) {
                        password = "P@ssword123";
                    }

                    User user = new User();
                    user.setUsername(row.getUsername().trim());
                    user.setEmail(row.getEmail().trim());
                    user.setFullName(row.getFullName().trim());
                    user.setPhone(row.getPhone() != null && !row.getPhone().trim().isEmpty() ? row.getPhone().trim() : null);
                    user.setTeamId(row.getResolvedTeamId());
                    user.setDataScope(row.getDataScope() != null ? row.getDataScope() : "SELF");
                    user.setPasswordHash(PasswordUtil.hashPassword(password));
                    user.setStatus("ACTIVE");
                    user.setActive(true);

                    long newUserId = userDAO.create(conn, user);

                    // Gán vai trò vào user_roles
                    if (row.getResolvedRoleId() != null) {
                        permissionDAO.replaceUserRoles(conn, newUserId, List.of(row.getResolvedRoleId()));
                    }

                    conn.commit();
                    importedCount++;

                } catch (Exception ex) {
                    conn.rollback();
                    row.addError("Lỗi CSDL khi tạo tài khoản: " + ex.getMessage());
                    skippedCount++;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        }

        previewResult.setImportedCount(importedCount);
        previewResult.setSkippedCount(skippedCount);
        previewResult.setExecuted(true);
        return previewResult;
    }
}
