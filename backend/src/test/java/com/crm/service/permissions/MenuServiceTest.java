package com.crm.service.permissions;

import com.crm.dto.permissions.MenuItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuServiceTest {
    private final MenuService menuService = new MenuService();

    @Test
    void adminReceivesCompleteSprintOneNavigation() {
        List<String> codes = codes(menuService.getSprint1MenuItems(List.of("Admin")));

        assertEquals(List.of("DASHBOARD", "USERS", "PERMISSIONS", "CHANGE_PASSWORD", "LOGOUT"),
                codes);
    }

    @Test
    void regularUserDoesNotReceiveAdministrativeNavigation() {
        List<String> codes = codes(menuService.getSprint1MenuItems(List.of("Sales Rep")));

        assertTrue(codes.contains("DASHBOARD"));
        assertTrue(codes.contains("CHANGE_PASSWORD"));
        assertTrue(codes.contains("LOGOUT"));
        assertFalse(codes.contains("USERS"));
        assertFalse(codes.contains("PERMISSIONS"));
    }

    private List<String> codes(List<MenuItem> items) {
        return items.stream().map(MenuItem::getCode).toList();
    }
}
