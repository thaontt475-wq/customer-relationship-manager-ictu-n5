package com.crm.service.teams;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TeamServiceTest {

    @Test
    void createTeamRequiresName() {
        TeamService service = new TeamService();
        Map<String, Object> body = new HashMap<>();
        body.put("name", "");

        assertThrows(IllegalArgumentException.class, () -> service.createTeam(body));
    }

    @Test
    void updateTeamRequiresExisting() {
        TeamService service = new TeamService();
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Test");

        assertThrows(Exception.class, () -> service.updateTeam(-9999L, body));
    }

    @Test
    void deleteTeamRequiresExisting() {
        TeamService service = new TeamService();
        assertThrows(Exception.class, () -> service.deleteTeam(-9999L));
    }
}
