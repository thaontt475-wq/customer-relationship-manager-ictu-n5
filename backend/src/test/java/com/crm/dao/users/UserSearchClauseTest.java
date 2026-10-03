package com.crm.dao.users;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSearchClauseTest {
    @Test
    void teamFilterIsParameterizedAlongsideNameEmailRoleAndStatus() throws Exception {
        Object clause = build("alex", "42", "Admin", "ACTIVE");
        Method sqlAccessor = clause.getClass().getDeclaredMethod("sql");
        Method parametersAccessor = clause.getClass().getDeclaredMethod("parameters");
        sqlAccessor.setAccessible(true);
        parametersAccessor.setAccessible(true);

        String sql = (String) sqlAccessor.invoke(clause);
        List<?> parameters = (List<?>) parametersAccessor.invoke(clause);
        assertTrue(sql.contains("u.team_id = ?"));
        assertTrue(sql.contains("u.email) LIKE ?"));
        assertTrue(sql.contains("LOWER(r2.name) = LOWER(?)"));
        assertTrue(sql.contains("UPPER(u.status) = UPPER(?)"));
        assertEquals(List.of("%alex%", "%alex%", "%alex%", 42L, "Admin", "ACTIVE"), parameters);
    }

    @Test
    void invalidTeamFilterMatchesNoRowsWithoutUnboundParameter() throws Exception {
        Object clause = build(null, "42 OR 1=1", null, null);
        Method sqlAccessor = clause.getClass().getDeclaredMethod("sql");
        Method parametersAccessor = clause.getClass().getDeclaredMethod("parameters");
        sqlAccessor.setAccessible(true);
        parametersAccessor.setAccessible(true);

        assertTrue(((String) sqlAccessor.invoke(clause)).contains("AND 1 = 0"));
        assertEquals(List.of(), parametersAccessor.invoke(clause));
    }

    private Object build(String keyword, String team, String role, String status) throws Exception {
        Method method = UserDAO.class.getDeclaredMethod("buildSearchClause", String.class,
                String.class, String.class, String.class);
        method.setAccessible(true);
        return method.invoke(new UserDAO(), keyword, team, role, status);
    }
}
