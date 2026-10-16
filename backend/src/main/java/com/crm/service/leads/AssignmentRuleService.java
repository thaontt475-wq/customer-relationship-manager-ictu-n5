package com.crm.service.leads;

import com.crm.dao.leads.AssignmentRuleDAO;
import com.crm.model.leads.AssignmentRule;

import java.util.List;

public class AssignmentRuleService {

    private final AssignmentRuleDAO ruleDAO;

    public AssignmentRuleService() {
        this.ruleDAO = new AssignmentRuleDAO();
    }

    public AssignmentRuleService(AssignmentRuleDAO ruleDAO) {
        this.ruleDAO = ruleDAO;
    }

    public List<AssignmentRule> getAllRules() {
        return ruleDAO.findAllOrderedByPriority();
    }

    public AssignmentRule getRuleById(long id) {
        return ruleDAO.findById(id);
    }

    public AssignmentRule createRule(AssignmentRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Dữ liệu quy tắc không được để trống.");
        }
        if (rule.getName() == null || rule.getName().isBlank()) {
            throw new IllegalArgumentException("Tên quy tắc phân bổ không được để trống.");
        }

        if (rule.getPriority() <= 0) {
            List<AssignmentRule> existing = ruleDAO.findAllOrderedByPriority();
            rule.setPriority(existing.size() + 1);
        }

        long id = ruleDAO.insert(rule);
        if (id > 0) {
            return ruleDAO.findById(id);
        }
        return null;
    }

    public boolean updateRule(AssignmentRule rule) {
        if (rule == null || rule.getId() == null) {
            throw new IllegalArgumentException("Mã quy tắc không hợp lệ.");
        }
        return ruleDAO.update(rule);
    }

    public boolean deleteRule(long id) {
        return ruleDAO.delete(id);
    }

    public List<AssignmentRule> batchSaveRules(List<AssignmentRule> rules) {
        if (rules != null && !rules.isEmpty()) {
            ruleDAO.batchSaveOrUpdate(rules);
        }
        return ruleDAO.findAllOrderedByPriority();
    }
}
