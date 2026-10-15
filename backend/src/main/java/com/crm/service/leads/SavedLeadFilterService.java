package com.crm.service.leads;

import com.crm.dao.leads.SavedLeadFilterDAO;
import com.crm.model.leads.SavedLeadFilter;

import java.util.*;

public class SavedLeadFilterService {
    private final SavedLeadFilterDAO filterDAO;

    public SavedLeadFilterService() {
        this.filterDAO = new SavedLeadFilterDAO();
    }

    public SavedLeadFilterService(SavedLeadFilterDAO filterDAO) {
        this.filterDAO = filterDAO;
    }

    /**
     * S4-09 AC 3: Get system default filters and user's saved filters.
     */
    public List<SavedLeadFilter> getAllFilters(long currentUserId) {
        List<SavedLeadFilter> result = new ArrayList<>(getSystemDefaultFilters());
        List<SavedLeadFilter> userFilters = filterDAO.findByUserId(currentUserId);
        result.addAll(userFilters);
        return result;
    }

    public List<SavedLeadFilter> getSystemDefaultFilters() {
        List<SavedLeadFilter> sys = new ArrayList<>();

        // 1. Tất cả Lead
        Map<String, Object> allCriteria = new LinkedHashMap<>();
        allCriteria.put("keyword", "");
        allCriteria.put("status", "ALL");
        allCriteria.put("tier", "ALL");
        allCriteria.put("source", "ALL");
        allCriteria.put("owner", "ALL");
        allCriteria.put("dateRange", "ALL");
        allCriteria.put("slaOverdueOnly", false);
        allCriteria.put("sort", "SLA_URGENT");
        sys.add(new SavedLeadFilter("view_all", 0L, "📋 Tất cả Lead", allCriteria, true, true));

        // 2. Cần liên hệ gấp (SLA Overdue)
        Map<String, Object> slaCriteria = new LinkedHashMap<>();
        slaCriteria.put("keyword", "");
        slaCriteria.put("status", "ALL");
        slaCriteria.put("tier", "ALL");
        slaCriteria.put("source", "ALL");
        slaCriteria.put("owner", "ALL");
        slaCriteria.put("dateRange", "ALL");
        slaCriteria.put("slaOverdueOnly", true);
        slaCriteria.put("sort", "SLA_URGENT");
        sys.add(new SavedLeadFilter("view_sla_urgent", 0L, "🚨 Cần liên hệ gấp (SLA Overdue)", slaCriteria, false, true));

        // 3. Hot Lead chưa phân bổ
        Map<String, Object> hotCriteria = new LinkedHashMap<>();
        hotCriteria.put("keyword", "");
        hotCriteria.put("status", "NEW");
        hotCriteria.put("tier", "HOT");
        hotCriteria.put("source", "ALL");
        hotCriteria.put("owner", "UNASSIGNED");
        hotCriteria.put("dateRange", "ALL");
        hotCriteria.put("slaOverdueOnly", false);
        hotCriteria.put("sort", "SLA_URGENT");
        sys.add(new SavedLeadFilter("view_hot_unassigned", 0L, "🔥 Hot Lead chưa phân bổ", hotCriteria, false, true));

        // 4. Lead của tôi
        Map<String, Object> myCriteria = new LinkedHashMap<>();
        myCriteria.put("keyword", "");
        myCriteria.put("status", "IN_PROGRESS");
        myCriteria.put("tier", "ALL");
        myCriteria.put("source", "ALL");
        myCriteria.put("owner", "CURRENT_USER");
        myCriteria.put("dateRange", "ALL");
        myCriteria.put("slaOverdueOnly", false);
        myCriteria.put("sort", "SLA_URGENT");
        sys.add(new SavedLeadFilter("view_my_leads", 0L, "👤 Lead của tôi", myCriteria, false, true));

        return sys;
    }

    public SavedLeadFilter saveCustomFilter(SavedLeadFilter filter, long currentUserId) {
        filter.setUserId(currentUserId);
        filter.setSystem(false);
        long id = filterDAO.insert(filter);
        if (id > 0) {
            filter.setId("custom_filter_" + id);
            if (filter.isDefault()) {
                filterDAO.setDefault(id, currentUserId);
            }
            return filter;
        }
        return null;
    }

    public boolean updateCustomFilter(SavedLeadFilter filter, long currentUserId) {
        filter.setUserId(currentUserId);
        return filterDAO.update(filter);
    }

    public boolean deleteCustomFilter(long filterId, long currentUserId) {
        return filterDAO.delete(filterId, currentUserId);
    }

    public boolean setDefaultFilter(long filterId, long currentUserId) {
        return filterDAO.setDefault(filterId, currentUserId);
    }
}
