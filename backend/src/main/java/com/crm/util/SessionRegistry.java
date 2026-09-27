package com.crm.util;

import jakarta.servlet.http.HttpSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionRegistry {

    private static final ConcurrentHashMap<Long, Set<HttpSession>> SESSIONS =
            new ConcurrentHashMap<>();

    private SessionRegistry() {
    }

    public static void register(long userId, HttpSession session) {
        if (userId <= 0 || session == null) {
            return;
        }
        SESSIONS.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    public static void unregister(long userId, HttpSession session) {
        if (userId <= 0 || session == null) {
            return;
        }

        Set<HttpSession> userSessions = SESSIONS.get(userId);
        if (userSessions == null) {
            return;
        }

        userSessions.remove(session);

        if (userSessions.isEmpty()) {
            SESSIONS.remove(userId, userSessions);
        }
    }

    public static int invalidateOtherSessions(long userId, HttpSession currentSession) {
        Set<HttpSession> userSessions = SESSIONS.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return 0;
        }

        int invalidated = 0;

        for (HttpSession session : Set.copyOf(userSessions)) {
            if (session == currentSession) {
                continue;
            }

            try {
                session.invalidate();
                invalidated++;
            } catch (IllegalStateException ignored) {
                // Session đã hết hạn hoặc đã bị thu hồi trước đó.
            } finally {
                userSessions.remove(session);
            }
        }

        if (userSessions.isEmpty()) {
            SESSIONS.remove(userId, userSessions);
        }

        return invalidated;
    }
}