package com.crm.service.pipeline;

/**
 * Exception thrown when a pipeline stage cannot be deleted
 * because it still has active opportunities assigned to it.
 * Mapped to HTTP 409 Conflict / 400 Bad Request.
 */
public class StageInUseException extends Exception {
    private static final long serialVersionUID = 1L;

    private final long stageId;
    private final long opportunityCount;

    public StageInUseException(long stageId, long opportunityCount, String message) {
        super(message);
        this.stageId = stageId;
        this.opportunityCount = opportunityCount;
    }

    public long getStageId() {
        return stageId;
    }

    public long getOpportunityCount() {
        return opportunityCount;
    }
}
