package com.crm.util;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
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
        Object userLock = userLock(userId);
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

        String sessionId = session.getId();
        Long userId = USER_BY_SESSION.get(sessionId);
        if (userId != null) {
            unregister(userId, session);
        }
    }

    /** Compatibility overload used by CRM-24 logout/password flows. */
    public static void unregister(long userId, HttpSession session) {
        if (userId <= 0 || session == null) {
            return;
        }

        String sessionId = session.getId();
        synchronized (userLock(userId)) {
            USER_BY_SESSION.remove(sessionId, userId);
            removeSession(userId, sessionId);
        }
    }

    public static int invalidateOtherSessions(long userId, HttpSession currentSession) {
        if (userId <= 0) {
            return 0;
        }

        String currentSessionId = currentSession == null ? null : currentSession.getId();
        List<HttpSession> sessionsToInvalidate = new ArrayList<>();
        synchronized (userLock(userId)) {
            ConcurrentMap<String, HttpSession> sessions = SESSIONS_BY_USER.get(userId);
            if (sessions == null || sessions.isEmpty()) {
                return 0;
            }

            for (var entry : sessions.entrySet()) {
                if (entry.getKey().equals(currentSessionId)) {
                    continue;
                }
                if (sessions.remove(entry.getKey(), entry.getValue())) {
                    USER_BY_SESSION.remove(entry.getKey(), userId);
                    sessionsToInvalidate.add(entry.getValue());
                }
            }

            if (sessions.isEmpty()) {
                SESSIONS_BY_USER.remove(userId, sessions);
            }
        }

        int invalidated = 0;
        for (HttpSession session : sessionsToInvalidate) {
            if (invalidateQuietly(session)) {
                invalidated++;
            }
        }
        return invalidated;
    }

    public static int revokeAll(long userId) {
        if (userId <= 0) {
            return 0;
        }

        List<HttpSession> sessions;
        synchronized (userLock(userId)) {
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
        if (userId <= 0) {
            return;
        }

        synchronized (userLock(userId)) {
            REVOKED_USERS.remove(userId);
        }
    }

    private static Object userLock(long userId) {
        return USER_LOCKS.computeIfAbsent(userId, ignored -> new Object());
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

    private static boolean invalidateQuietly(HttpSession session) {
        try {
            session.invalidate();
            return true;
        } catch (IllegalStateException ignored) {
            // Already expired, logged out, or invalidated concurrently.
            return false;
        }
    }
}
