package com.crm.config;

import com.crm.service.leads.LeadAutoAssignmentScheduler;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class LeadAssignmentContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LeadAutoAssignmentScheduler.getInstance().start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LeadAutoAssignmentScheduler.getInstance().stop();
    }
}
