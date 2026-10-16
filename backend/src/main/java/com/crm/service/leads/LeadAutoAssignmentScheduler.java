package com.crm.service.leads;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background worker task for automated lead distribution (AC 4).
 * Periodically scans the Pending Assignment Queue every 60 seconds (under 5 minutes)
 * and distributes matching leads through the Priority Rules Engine.
 */
public class LeadAutoAssignmentScheduler {

    private static final LeadAutoAssignmentScheduler INSTANCE = new LeadAutoAssignmentScheduler();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "crm-lead-auto-assignment-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final LeadAutoAssignmentService assignmentService = new LeadAutoAssignmentService();
    private boolean isRunning = false;

    private LeadAutoAssignmentScheduler() {
    }

    public static LeadAutoAssignmentScheduler getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;

        // Schedule periodic queue processing every 60 seconds
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                int assignedCount = assignmentService.processPendingQueue();
                if (assignedCount > 0) {
                    System.out.println("[LeadAutoAssignmentWorker] Background scan auto-assigned " + assignedCount + " pending leads.");
                }
            } catch (Exception e) {
                System.err.println("[LeadAutoAssignmentWorker] Error during background lead auto assignment: " + e.getMessage());
            }
        }, 15, 60, TimeUnit.SECONDS);

        System.out.println("[LeadAutoAssignmentScheduler] Background worker started (runs every 60s).");
    }

    public synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            System.out.println("[LeadAutoAssignmentScheduler] Background worker stopped.");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
