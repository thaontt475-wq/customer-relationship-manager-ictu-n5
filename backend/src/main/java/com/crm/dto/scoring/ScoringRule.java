package com.crm.dto.scoring;

public record ScoringRule(String id, String group, String name, String field,
                          String operator, String value, int points, String description, boolean isActive) {}
