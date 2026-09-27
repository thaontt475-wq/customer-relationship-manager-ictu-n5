package com.crm.util;

import com.crm.listener.SessionLifecycleListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionRegistryTest {

    @Test
    void revokeAllInvalidatesExistingSessionsAndBlocksLateRegistration() {
        long userId = 900_001L;
        SessionRegistry.allowUser(userId);
        SessionStub first = session("session-1");
        SessionStub late = session("session-2");

        try {
            assertTrue(SessionRegistry.register(userId, first.session()));
            assertEquals(1, SessionRegistry.revokeAll(userId));
            assertTrue(first.invalidated().get());

            assertFalse(SessionRegistry.register(userId, late.session()));
            assertTrue(late.invalidated().get());
        } finally {
            SessionRegistry.allowUser(userId);
        }
    }

    @Test
    void destroyedSessionIsUnregistered() {
        long userId = 900_002L;
        SessionRegistry.allowUser(userId);
        SessionStub stub = session("session-3");

        try {
            assertTrue(SessionRegistry.register(userId, stub.session()));
            new SessionLifecycleListener().sessionDestroyed(new HttpSessionEvent(stub.session()));
            assertEquals(0, SessionRegistry.revokeAll(userId));
            assertFalse(stub.invalidated().get());
        } finally {
            SessionRegistry.allowUser(userId);
        }
    }

    private SessionStub session(String id) {
        AtomicBoolean invalidated = new AtomicBoolean();
        HttpSession session = (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> id;
                    case "invalidate" -> {
                        if (!invalidated.compareAndSet(false, true)) {
                            throw new IllegalStateException("already invalidated");
                        }
                        yield null;
                    }
                    case "toString" -> id;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
        return new SessionStub(session, invalidated);
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }

    private record SessionStub(HttpSession session, AtomicBoolean invalidated) {
    }
}
