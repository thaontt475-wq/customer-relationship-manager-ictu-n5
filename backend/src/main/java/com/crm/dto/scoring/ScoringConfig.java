package com.crm.dto.scoring;

import java.util.List;

/** No implicit business defaults: migration starts unconfigured. */
public record ScoringConfig(long version, Integer hot, Integer warm, String aggregation,
                            Integer minScore, Integer maxScore, List<ScoringRule> rules) {
    public ScoringConfig { rules = List.copyOf(rules); }
    public boolean configured() { return hot != null && warm != null && aggregation != null; }
}
