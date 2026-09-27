package com.crm.util;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Thread-safe registry for all authenticated sessions in one Tomcat instance. */
public final class SessionRegistry {
    private static final ConcurrentMap<Long, ConcurrentMap<String, HttpSession>> SESSIONS_BY_USER =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> USER_BY_SESSION = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, Object> USER_LOCKS = new ConcurrentHashMap<>();
    private static final Set<Long> REVOKED_USERS = ConcurrentHashMap.newKeySet();

    private SessionRegistry() {
    }

    public static boolean register(long userId, HttpSession session) {
        if (userId <= 0 || session == null) {
            return false;
        }

        String sessionId = session.getId();
        Object userLock = USER_LOCKS.computeIfAbsent(userId, ignored -> new Object());
        synchronized (userLock) {
            if (REVOKED_USERS.contains(userId)) {
                invalidateQuietly(session);
                return false;
            }

            Long previousUserId = USER_BY_SESSION.put(sessionId, userId);
            if (previousUserId != null && previousUserId != userId) {
                removeSession(previousUserId, sessionId);
            }
            SESSIONS_BY_USER.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
                    .put(sessionId, session);
            return true;
        }
    }

    public static void unregister(HttpSession session) {
        if (session == null) {
            return;
        }
        unregister(session.getId());
    }

    public static int revokeAll(long userId) {
        Object userLock = USER_LOCKS.computeIfAbsent(userId, ignored -> new Object());
        ArrayList<HttpSession> sessions;
        synchronized (userLock) {
            REVOKED_USERS.add(userId);
            ConcurrentMap<String, HttpSession> removed = SESSIONS_BY_USER.remove(userId);
            if (removed == null) {
                return 0;
            }
            removed.keySet().forEach(sessionId -> USER_BY_SESSION.remove(sessionId, userId));
            sessions = new ArrayList<>(removed.values());
        }

        sessions.forEach(SessionRegistry::invalidateQuietly);
        return sessions.size();
    }

    public static void allowUser(long userId) {
        Object userLock = USER_LOCKS.computeIfAbsent(userId, ignored -> new Object());
        synchronized (userLock) {
            REVOKED_USERS.remove(userId);
        }
    }

    private static void unregister(String sessionId) {
        Long userId = USER_BY_SESSION.remove(sessionId);
        if (userId != null) {
            removeSession(userId, sessionId);
        }
    }

    private static void removeSession(long userId, String sessionId) {
        ConcurrentMap<String, HttpSession> sessions = SESSIONS_BY_USER.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                SESSIONS_BY_USER.remove(userId, sessions);
            }
        }
    }

    private static void invalidateQuietly(HttpSession session) {
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // Already expired, logged out, or invalidated concurrently.
        }
    }
}
