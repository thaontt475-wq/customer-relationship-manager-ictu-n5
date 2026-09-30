package com.crm.service.pipeline;

import com.crm.dao.pipeline.PipelineDAO;
import com.crm.model.PipelineStage;
import com.crm.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing Pipeline Stages and Transition Rules (CRM-47):
 * 1. CRUD Stage Pipeline (Name, Order, Win Probability, Requirements/Exit criteria).
 * 2. Validate Win Probability (0% - 100%) and Stage Order (>= 1, ascending order).
 * 3. Validate Stage Transition requirements for Opportunities.
 * 4. Protect ongoing Opportunities (Data Integrity & Block deletion when stage is in use).
 */
public class PipelineService {
    private static final Logger LOGGER = Logger.getLogger(PipelineService.class.getName());

    private final PipelineDAO pipelineDAO;

    public PipelineService() {
        this.pipelineDAO = new PipelineDAO();
    }

    public PipelineService(PipelineDAO pipelineDAO) {
        this.pipelineDAO = pipelineDAO != null ? pipelineDAO : new PipelineDAO();
    }

    /**
     * Retrieve all stages for a pipeline, sorted by stage_order ASC.
     */
    public List<PipelineStage> getStages(long pipelineId, Boolean activeOnly) throws SQLException {
        long targetPipelineId = pipelineId > 0 ? pipelineId : 1L;
        try (Connection conn = DBConnection.getConnection()) {
            return pipelineDAO.findAll(conn, targetPipelineId, activeOnly);
        }
    }

    /**
     * Retrieve single stage by ID.
     */
    public PipelineStage getStageById(long stageId) throws SQLException {
        if (stageId <= 0) {
            return null;
        }
        try (Connection conn = DBConnection.getConnection()) {
            return pipelineDAO.findById(conn, stageId);
        }
    }

    /**
     * Create a new pipeline stage.
     * Enforces:
     * - Name & Code required
     * - Win Probability in range [0, 100]
     * - Stage order >= 1
     * - Code uniqueness within pipeline
     */
    public PipelineStage createStage(PipelineStage stage) throws SQLException {
        if (stage == null) {
            throw new IllegalArgumentException("Thông tin giai đoạn không được để trống.");
        }
        validateStageFields(stage);

        long pipelineId = stage.getPipelineId() != null && stage.getPipelineId() > 0 ? stage.getPipelineId() : 1L;
        stage.setPipelineId(pipelineId);

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                if (pipelineDAO.existsByCode(conn, pipelineId, stage.getCode(), null)) {
                    conn.rollback();
                    throw new IllegalArgumentException("Mã giai đoạn '" + stage.getCode() + "' đã tồn tại trong quy trình bán hàng.");
                }

                long newId = pipelineDAO.insert(conn, stage);
                if (newId <= 0) {
                    conn.rollback();
                    throw new SQLException("Không thể tạo giai đoạn bán hàng mới.");
                }

                PipelineStage created = pipelineDAO.findById(conn, newId);
                conn.commit();
                return created;

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Update an existing pipeline stage.
     * Enforces:
     * - Stage must exist
     * - Win probability in range [0, 100]
     * - Code uniqueness within pipeline
     * - CRITICAL DATA INTEGRITY: Preserves all existing opportunities currently linked to this stage.
     */
    public PipelineStage updateStage(PipelineStage stage) throws SQLException {
        if (stage == null || stage.getId() == null || stage.getId() <= 0) {
            throw new IllegalArgumentException("ID giai đoạn không hợp lệ.");
        }
        validateStageFields(stage);

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                PipelineStage existing = pipelineDAO.findById(conn, stage.getId());
                if (existing == null) {
                    conn.rollback();
                    throw new IllegalArgumentException("Không tìm thấy giai đoạn bán hàng với ID: " + stage.getId());
                }

                long pipelineId = stage.getPipelineId() != null && stage.getPipelineId() > 0
                        ? stage.getPipelineId()
                        : existing.getPipelineId();
                stage.setPipelineId(pipelineId);

                if (pipelineDAO.existsByCode(conn, pipelineId, stage.getCode(), stage.getId())) {
                    conn.rollback();
                    throw new IllegalArgumentException("Mã giai đoạn '" + stage.getCode() + "' đã được sử dụng bởi giai đoạn khác.");
                }

                int updated = pipelineDAO.update(conn, stage);
                if (updated <= 0) {
                    conn.rollback();
                    throw new SQLException("Không thể cập nhật giai đoạn bán hàng.");
                }

                PipelineStage result = pipelineDAO.findById(conn, stage.getId());
                conn.commit();
                return result;

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Reorders multiple stages in a pipeline according to the provided list of stage IDs.
     */
    public boolean reorderStages(long pipelineId, List<Long> orderedStageIds) throws SQLException {
        if (orderedStageIds == null || orderedStageIds.isEmpty()) {
            return false;
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int order = 1;
                for (Long id : orderedStageIds) {
                    if (id != null && id > 0) {
                        pipelineDAO.updateStageOrder(conn, id, order++);
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Delete a pipeline stage with Opportunity data protection (CRM-47).
     *
     * Business rules:
     * 1. Check if any Opportunity is currently assigned to this stage.
     * 2. If opportunities exist and no reassign target is specified -> Strictly BLOCK deletion.
     * 3. If opportunities exist and a valid targetStageId is provided -> Migrate opportunities first, then delete.
     *
     * @param stageId                    ID of stage to delete
     * @param targetStageIdForMigration  Optional target stage ID to safely move opportunities to before deletion
     * @return true if deleted, false if stage does not exist
     */
    public boolean deleteStage(long stageId, Long targetStageIdForMigration) throws SQLException, StageInUseException {
        if (stageId <= 0) {
            throw new IllegalArgumentException("ID giai đoạn không hợp lệ.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                PipelineStage existing = pipelineDAO.findById(conn, stageId);
                if (existing == null) {
                    conn.rollback();
                    return false;
                }

                long opportunityCount = pipelineDAO.countOpportunitiesByStage(conn, stageId);
                if (opportunityCount > 0) {
                    // Check if safe migration was requested
                    if (targetStageIdForMigration != null && targetStageIdForMigration > 0
                            && !targetStageIdForMigration.equals(stageId)) {

                        PipelineStage targetStage = pipelineDAO.findById(conn, targetStageIdForMigration);
                        if (targetStage == null) {
                            conn.rollback();
                            throw new IllegalArgumentException("Giai đoạn đích chuyển tiếp (ID: "
                                    + targetStageIdForMigration + ") không tồn tại.");
                        }

                        // Safely migrate opportunities
                        pipelineDAO.reassignOpportunities(conn, stageId, targetStageIdForMigration);
                        LOGGER.log(Level.INFO, "Migrated {0} opportunities from stage {1} to stage {2}",
                                new Object[]{opportunityCount, stageId, targetStageIdForMigration});

                    } else {
                        // CRITICAL BUSINESS RULE 4: Block deletion of stage in use
                        conn.rollback();
                        throw new StageInUseException(stageId, opportunityCount,
                                "Không thể xóa giai đoạn '" + existing.getName() +
                                        "' vì đang có " + opportunityCount +
                                        " cơ hội (opportunity) trong giai đoạn này. Vui lòng chuyển các cơ hội sang giai đoạn khác trước khi xóa.");
                    }
                }

                try {
                    int deleted = pipelineDAO.delete(conn, stageId);
                    conn.commit();
                    return deleted > 0;
                } catch (SQLIntegrityConstraintViolationException fkEx) {
                    conn.rollback();
                    throw new StageInUseException(stageId, opportunityCount,
                            "Không thể xóa giai đoạn '" + existing.getName() + "' do có ràng buộc dữ liệu liên quan trong hệ thống.");
                }

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Validates whether an opportunity meets the necessary exit/entry criteria
     * to transition from currentStage to targetStage (CRM-47 Rule 3).
     */
    public TransitionValidationResult validateTransition(PipelineStage currentStage, PipelineStage targetStage,
                                                         TransitionContext context) {
        List<String> errors = new ArrayList<>();
        List<String> satisfied = new ArrayList<>();

        if (targetStage == null) {
            errors.add("Giai đoạn đích không tồn tại.");
            return new TransitionValidationResult(false, errors, satisfied);
        }

        if (!targetStage.isActive()) {
            errors.add("Không thể chuyển sang giai đoạn đã bị vô hiệu hóa: " + targetStage.getName());
        }

        // Rule 3.1: If transitioning to WON (100% win probability), opportunity amount must be > 0
        if (targetStage.isWon() || targetStage.getWinProbability() == 100) {
            if (context == null || context.amount() == null || context.amount().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Cơ hội chốt thành công (Won) bắt buộc phải có giá trị doanh số (amount > 0).");
            } else {
                satisfied.add("Đã xác nhận giá trị doanh số hợp lệ: " + context.amount());
            }
        }

        // Rule 3.2: If transitioning to LOST (0% win probability), lost reason is mandatory
        if (targetStage.isLost() || targetStage.getWinProbability() == 0) {
            if (context == null || context.lostReason() == null || context.lostReason().trim().isEmpty()) {
                errors.add("Bắt buộc phải nhập lý do thất bại (lost_reason) khi chuyển sang giai đoạn Thất bại (Lost).");
            } else {
                satisfied.add("Đã ghi nhận lý do thất bại: " + context.lostReason().trim());
            }
        }

        // Rule 3.3: Stage requirements / exit criteria confirmation
        if (currentStage != null && currentStage.getRequirements() != null && !currentStage.getRequirements().isBlank()) {
            if (context != null && !context.requirementsConfirmed()) {
                errors.add("Chưa xác nhận hoàn thành điều kiện rời giai đoạn '" + currentStage.getName()
                        + "': " + currentStage.getRequirements());
            } else {
                satisfied.add("Đã hoàn thành tiêu chí rời giai đoạn: " + currentStage.getRequirements());
            }
        }

        boolean valid = errors.isEmpty();
        return new TransitionValidationResult(valid, errors, satisfied);
    }

    private void validateStageFields(PipelineStage stage) {
        if (stage.getCode() == null || stage.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã giai đoạn không được để trống.");
        }
        if (stage.getCode().trim().length() > 50) {
            throw new IllegalArgumentException("Mã giai đoạn không được vượt quá 50 ký tự.");
        }

        if (stage.getName() == null || stage.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên giai đoạn không được để trống.");
        }
        if (stage.getName().trim().length() > 150) {
            throw new IllegalArgumentException("Tên giai đoạn không được vượt quá 150 ký tự.");
        }

        // CRITICAL BUSINESS RULE 2: win_probability must be between 0 and 100
        int prob = stage.getWinProbability();
        if (prob < 0 || prob > 100) {
            throw new IllegalArgumentException("Tỷ lệ thắng (win_probability) phải nằm trong khoảng từ 0% đến 100%. Nhận được: " + prob + "%");
        }

        // CRITICAL BUSINESS RULE 2: stage_order must be >= 1
        if (stage.getStageOrder() < 1) {
            throw new IllegalArgumentException("Thứ tự giai đoạn (stage_order) phải lớn hơn hoặc bằng 1.");
        }
    }

    // Records for transition validation
    public record TransitionContext(
            BigDecimal amount,
            String contactName,
            String lostReason,
            boolean requirementsConfirmed
    ) {}

    public record TransitionValidationResult(
            boolean valid,
            List<String> errors,
            List<String> satisfiedRequirements
    ) {}
}