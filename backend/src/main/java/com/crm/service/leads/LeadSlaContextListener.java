package com.crm.service.leads;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background scheduler context listener that automatically checks for overdue Lead SLAs
 * and notifies direct team leads.
 */
@WebListener
public class LeadSlaContextListener implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(LeadSlaContextListener.class.getName());
    private ScheduledExecutorService scheduler;
    private final LeadSlaMonitorService monitorService = new LeadSlaMonitorService();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "crm-lead-sla-monitor-thread");
            t.setDaemon(true);
            return t;
        });

        // Run after 5 seconds, then repeat every 30 seconds
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                int sent = monitorService.checkAndNotifyOverdueLeads();
                if (sent > 0) {
                    LOGGER.info("Lead SLA background check dispatched " + sent + " alert(s).");
                }
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Lỗi kiểm tra Lead SLA định kỳ", t);
            }
        }, 5, 30, TimeUnit.SECONDS);

        LOGGER.info("Khởi động LeadSlaContextListener thành công (chu kỳ 30s).");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            LOGGER.info("Dừng LeadSlaContextListener background scheduler.");
        }
    }
}
