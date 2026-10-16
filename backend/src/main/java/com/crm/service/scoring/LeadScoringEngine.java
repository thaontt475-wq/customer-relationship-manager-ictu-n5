package com.crm.service.scoring;

import com.crm.dto.scoring.*;
import java.math.BigDecimal;
import java.util.*;

/** Pure deterministic engine; each calculation starts at zero. */
public final class LeadScoringEngine {
    public record Breakdown(String ruleId, String ruleName, String group, int points,
                            Object actualValue, String description) {}
    public record Result(int rawScore, int score, String classification, List<Breakdown> breakdown) {}
    public static final Set<String> FIELDS = Set.of("industry", "employeeCount", "source", "recentActivity", "title");
    public static final Set<String> GROUPS = Set.of("INDUSTRY", "COMPANY_SIZE", "SOURCE", "INTENT", "CUSTOM");
    public static final Set<String> OPERATORS = Set.of("EQUALS", "CONTAINS", "GREATER_THAN", "LESS_THAN");

    public void validate(ScoringConfig config) {
        if (!config.configured()) throw new IllegalStateException("Quy tắc nghiệp vụ chưa được xác nhận và cấu hình");
        if (config.warm() < 0 || config.hot() <= config.warm() || config.hot() > 100000)
            throw new IllegalArgumentException("Ngưỡng phải thỏa 0 <= warm < hot <= 100000");
        if (!Set.of("SUM_ALL", "MAX_PER_GROUP").contains(config.aggregation()))
            throw new IllegalArgumentException("aggregation phải là SUM_ALL hoặc MAX_PER_GROUP");
        if ((config.minScore() != null && Math.abs((long)config.minScore()) > 100000)
                || (config.maxScore() != null && Math.abs((long)config.maxScore()) > 100000)
                || (config.minScore() != null && config.maxScore() != null && config.minScore() > config.maxScore())
                || (config.maxScore() != null && config.maxScore() < config.hot()))
            throw new IllegalArgumentException("Giới hạn điểm không hợp lệ");
        if (config.rules().size() > 200) throw new IllegalArgumentException("Tối đa 200 tiêu chí");
        Set<String> ids = new HashSet<>();
        for (ScoringRule rule : config.rules()) {
            if (rule == null || rule.id() == null || !rule.id().matches("[A-Za-z0-9_-]{1,64}") || !ids.add(rule.id()))
                throw new IllegalArgumentException("ID tiêu chí không hợp lệ hoặc trùng lặp");
            if (!GROUPS.contains(String.valueOf(rule.group())) || !FIELDS.contains(String.valueOf(rule.field()))
                    || !OPERATORS.contains(String.valueOf(rule.operator())))
                throw new IllegalArgumentException("Nhóm, trường hoặc phép so sánh không được hỗ trợ");
            if (rule.name() == null || rule.name().isBlank() || rule.name().length() > 150
                    || rule.value() == null || rule.value().isBlank() || rule.value().length() > 255
                    || Math.abs((long)rule.points()) > 1000 || (rule.description() != null && rule.description().length() > 1000))
                throw new IllegalArgumentException("Tên, giá trị hoặc điểm tiêu chí không hợp lệ");
            if (Set.of("GREATER_THAN", "LESS_THAN").contains(rule.operator())) {
                if (!rule.field().equals("employeeCount")) throw new IllegalArgumentException("So sánh số chỉ hỗ trợ employeeCount");
                try { new BigDecimal(rule.value().trim()); }
                catch (NumberFormatException error) { throw new IllegalArgumentException("Giá trị so sánh phải là số"); }
            }
        }
    }

    public Result calculate(Map<String,Object> lead, ScoringConfig config) {
        validate(config);
        List<Breakdown> matches = new ArrayList<>();
        Map<String,Breakdown> best = new LinkedHashMap<>();
        for (ScoringRule rule : config.rules()) {
            Object actual = lead.get(rule.field());
            if (!rule.isActive() || actual == null || !matches(actual, rule)) continue;
            var item = new Breakdown(rule.id(), rule.name(), rule.group(), rule.points(), actual, rule.description());
            if (config.aggregation().equals("SUM_ALL")) matches.add(item);
            else if (!best.containsKey(rule.group()) || best.get(rule.group()).points() < rule.points()) best.put(rule.group(), item);
        }
        if (config.aggregation().equals("MAX_PER_GROUP")) matches.addAll(best.values());
        int raw = matches.stream().mapToInt(Breakdown::points).sum(), score = raw;
        if (config.minScore() != null) score = Math.max(score, config.minScore());
        if (config.maxScore() != null) score = Math.min(score, config.maxScore());
        return new Result(raw, score, score >= config.hot() ? "HOT" : score >= config.warm() ? "WARM" : "COLD", List.copyOf(matches));
    }
    private boolean matches(Object actual, ScoringRule rule) {
        String value = actual.toString().trim().toLowerCase(Locale.ROOT), target = rule.value().trim().toLowerCase(Locale.ROOT);
        return switch (rule.operator()) {
            case "EQUALS" -> value.equals(target);
            case "CONTAINS" -> value.contains(target);
            // Existing FE labels explicitly define these legacy names as >= and <=.
            case "GREATER_THAN", "LESS_THAN" -> {
                try {
                    int comparison = new BigDecimal(value).compareTo(new BigDecimal(target));
                    yield rule.operator().equals("GREATER_THAN") ? comparison >= 0 : comparison <= 0;
                } catch (NumberFormatException ignored) { yield false; }
            }
            default -> false;
        };
    }
}
