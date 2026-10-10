package com.crm.service.pipeline;

import com.crm.dao.customfields.CustomFieldValueDAO;
import com.crm.dao.pipeline.OpportunityDAO;
import com.crm.dao.pipeline.PipelineStageDAO;
import com.crm.dto.pipeline.OpportunityWriteRequest;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;

import java.util.*;

public class OpportunityService {

    private final OpportunityDAO opportunityDAO = new OpportunityDAO();
    private final PipelineStageDAO stageDAO = new PipelineStageDAO();
    private final CustomFieldValueDAO customFieldValueDAO = new CustomFieldValueDAO();
    private final DataScopeService dataScopeService = new DataScopeService();

    public List<Map<String, Object>> search(
            long currentUserId,
            Long stageId,
            String status,
            String keyword,
            int page,
            int size
    ) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "opportunity", "read");
        return opportunityDAO.search(scope, stageId, status, keyword, page, size);
    }

    public Map<String, Object> getById(long currentUserId, long id) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "opportunity", "read");
        Map<String, Object> opp = opportunityDAO.findById(id);
        if (opp == null) {
            return null;
        }

        long ownerId = (Long) opp.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        Map<String, Object> customFields = customFieldValueDAO.getValues("OPPORTUNITY", id);
        opp.put("customFields", customFields);
        return opp;
    }

    public Map<String, Object> create(long currentUserId, OpportunityWriteRequest req) throws Exception {
        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Tên cơ hội bán hàng là bắt buộc.");
        }

        dataScopeService.resolve(currentUserId, "opportunity", "create");

        // If stage is provided, get default probability
        if (req.getStageId() != null && req.getProbability() == null) {
            var stage = stageDAO.findById(req.getStageId());
            if (stage != null && stage.get("winProbability") != null) {
                req.setProbability(((Number) stage.get("winProbability")).intValue());
            }
        }

        long id = opportunityDAO.create(req, currentUserId);

        if (req.getCustomFields() != null && !req.getCustomFields().isEmpty()) {
            customFieldValueDAO.saveValues("OPPORTUNITY", id, req.getCustomFields());
        }

        return getById(currentUserId, id);
    }

    public Map<String, Object> update(long currentUserId, long id, OpportunityWriteRequest req) throws Exception {
        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("Tên cơ hội bán hàng là bắt buộc.");
        }

        DataScopeContext scope = dataScopeService.resolve(currentUserId, "opportunity", "update");
        Map<String, Object> existing = opportunityDAO.findById(id);
        if (existing == null) {
            return null;
        }

        long ownerId = (Long) existing.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        opportunityDAO.update(id, req);

        if (req.getCustomFields() != null && !req.getCustomFields().isEmpty()) {
            customFieldValueDAO.saveValues("OPPORTUNITY", id, req.getCustomFields());
        }

        return getById(currentUserId, id);
    }

    public Map<String, Object> changeStage(long currentUserId, long id, long targetStageId) throws Exception {
        DataScopeContext scope = dataScopeService.resolve(currentUserId, "opportunity", "update");
        Map<String, Object> opp = opportunityDAO.findById(id);
        if (opp == null) {
            throw new NoSuchElementException("Cơ hội không tồn tại.");
        }

        long ownerId = (Long) opp.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        // Validate target stage
        var targetStage = stageDAO.findById(targetStageId);
        if (targetStage == null) {
            throw new IllegalArgumentException("Giai đoạn đích không tồn tại.");
        }

        // CRM-37 Tiêu chí 3: Kiểm tra điều kiện bắt buộc rời giai đoạn hiện tại (nếu có cấu hình)
        Long currentStageId = (Long) opp.get("stageId");
        if (currentStageId != null && currentStageId != targetStageId) {
            var currentStage = stageDAO.findById(currentStageId);
            if (currentStage != null) {
                String requirements = (String) currentStage.get("requirements");
                if (requirements != null && !requirements.isBlank()) {
                    // Check if requirements are met
                    boolean isWonOrLost = Boolean.TRUE.equals(targetStage.get("isWon")) || Boolean.TRUE.equals(targetStage.get("isLost"));
                    if (!isWonOrLost && requirements.toLowerCase().contains("gặp") && opp.get("contactName") == null) {
                        throw new IllegalArgumentException("Điều kiện rời giai đoạn '" + currentStage.get("name") + "': " + requirements);
                    }
                }
            }
        }

        int prob = targetStage.get("winProbability") != null ? ((Number) targetStage.get("winProbability")).intValue() : 0;
        opportunityDAO.updateStage(id, targetStageId, prob);

        return getById(currentUserId, id);
    }

    public Map<String, Object> closeDeal(
            long currentUserId,
            long id,
            String status,
            Long winReasonId,
            Long lossReasonId,
            Long competitorId,
            String lostReason
    ) throws Exception {
        dataScopeService.resolve(currentUserId, "opportunity", "close");
        Map<String, Object> opp = opportunityDAO.findById(id);
        if (opp == null) {
            throw new NoSuchElementException("Cơ hội không tồn tại.");
        }

        DataScopeContext scope = dataScopeService.resolve(currentUserId, "opportunity", "update");
        long ownerId = (Long) opp.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        // CRM-42 Tiêu chí 3: Ràng buộc bắt buộc khi đóng cơ hội bán hàng
        if ("WON".equalsIgnoreCase(status)) {
            if (winReasonId == null) {
                throw new IllegalArgumentException("Bắt buộc phải chọn Lý do Thắng khi chốt thành công cơ hội bán hàng.");
            }
        } else if ("LOST".equalsIgnoreCase(status)) {
            if (lossReasonId == null) {
                throw new IllegalArgumentException("Bắt buộc phải chọn Lý do Thua khi đóng thất bại cơ hội bán hàng.");
            }
            if (competitorId == null) {
                throw new IllegalArgumentException("Bắt buộc phải chọn Đối thủ cạnh tranh khi đóng thất bại cơ hội bán hàng.");
            }
        } else {
            throw new IllegalArgumentException("Trạng thái đóng cơ hội phải là WON hoặc LOST.");
        }

        opportunityDAO.closeDeal(id, status.toUpperCase(), winReasonId, lossReasonId, competitorId, lostReason);
        return getById(currentUserId, id);
    }

    public void delete(long currentUserId, long id) throws Exception {
        dataScopeService.resolve(currentUserId, "opportunity", "update");
        Map<String, Object> opp = opportunityDAO.findById(id);
        if (opp == null) {
            throw new NoSuchElementException("Cơ hội không tồn tại.");
        }

        DataScopeContext scope = dataScopeService.resolve(currentUserId, "opportunity", "update");
        long ownerId = (Long) opp.get("ownerUserId");
        if (!scope.canAccessOwner(ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này do giới hạn phạm vi sở hữu.");
        }

        opportunityDAO.softDelete(id);
    }
}
