package com.crm.service.activities;

import com.crm.dao.activities.ActivityDAO;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;

import java.sql.Timestamp;
import java.util.*;

public class ActivityService {

    private final ActivityDAO activityDAO = new ActivityDAO();
    private final DataScopeService dataScopeService = new DataScopeService();

    public List<Map<String, Object>> search(
            long currentUserId,
            Long customerId,
            Long opportunityId,
            String type,
            String status
    ) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "activity", "read");
        return activityDAO.search(scope, customerId, opportunityId, type, status);
    }

    public Map<String, Object> getById(long currentUserId, long id) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "activity", "read");
        Map<String, Object> item = activityDAO.findById(id);
        if (item == null) return null;

        long ownerId = (Long) item.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }
        return item;
    }

    public Map<String, Object> create(
            long currentUserId,
            String subject,
            String type,
            String description,
            String status,
            Timestamp dueDate,
            Long customerId,
            Long opportunityId
    ) throws Exception {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Tiêu đề hoạt động là bắt buộc.");
        }
        dataScopeService.resolve(currentUserId, "activity", "create");
        long id = activityDAO.create(subject, type, description, status, dueDate, customerId, opportunityId, currentUserId);
        return getById(currentUserId, id);
    }

    public Map<String, Object> update(
            long currentUserId,
            long id,
            String subject,
            String type,
            String description,
            String status,
            Timestamp dueDate
    ) throws Exception {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Tiêu đề hoạt động là bắt buộc.");
        }
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "activity", "update");
        Map<String, Object> existing = activityDAO.findById(id);
        if (existing == null) return null;

        long ownerId = (Long) existing.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        activityDAO.update(id, subject, type, description, status, dueDate);
        return getById(currentUserId, id);
    }

    public void delete(long currentUserId, long id) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "activity", "delete");
        Map<String, Object> existing = activityDAO.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Hoạt động không tồn tại.");
        }

        long ownerId = (Long) existing.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        activityDAO.softDelete(id);
    }
}
