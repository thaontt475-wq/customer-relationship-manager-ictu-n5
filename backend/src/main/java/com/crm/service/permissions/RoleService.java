package com.crm.service.permissions;

import com.crm.dao.permissions.PermissionDAO;
import java.sql.SQLException;
import java.util.*;

public class RoleService {
    public List<Map<String, Object>> getRoles() throws SQLException {
        return new PermissionDAO().findRoles();
    }
}
