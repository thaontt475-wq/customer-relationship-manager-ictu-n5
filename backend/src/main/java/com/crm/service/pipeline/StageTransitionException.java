package com.crm.service.pipeline;

import java.util.List;

/**
 * Exception thrown when an opportunity cannot transition to a target stage
 * because mandatory exit criteria / stage requirements are unmet.
 * Mapped to HTTP 400 Bad Request.
 */
public class StageTransitionException extends Exception {
    private static final long serialVersionUID = 1L;

    private final List<String> unmetRequirements;

    public StageTransitionException(String message, List<String> unmetRequirements) {
        super(message);
        this.unmetRequirements = unmetRequirements != null ? unmetRequirements : List.of();
    }

    public List<String> getUnmetRequirements() {
        return unmetRequirements;
    }
}
