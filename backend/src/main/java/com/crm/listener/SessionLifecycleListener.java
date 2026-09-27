package com.crm.listener;

import com.crm.util.SessionRegistry;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@WebListener
public class SessionLifecycleListener implements HttpSessionListener {
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        SessionRegistry.unregister(event.getSession());
    }
}
