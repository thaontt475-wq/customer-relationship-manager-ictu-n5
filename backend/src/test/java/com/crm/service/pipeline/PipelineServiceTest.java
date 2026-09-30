package com.crm.service.pipeline;

import com.crm.dao.pipeline.PipelineDAO;
import com.crm.model.PipelineStage;
import com.crm.service.pipeline.PipelineService.TransitionContext;
import com.crm.service.pipeline.PipelineService.TransitionValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CRM-47 PipelineService:
 * 1. CRUD Stage, Stage Order & Win Probability validation
 * 2. Stage Transition validation rules (Won amount check, Lost reason check, Exit criteria)
 * 3. Opportunity data protection (Block deletion if in use, reassign before deletion)
 */
@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    private static final long STAGE_ID = 3L;
    private static final long TARGET_STAGE_ID = 4L;

    @Mock
    private PipelineDAO pipelineDAO;

    private PipelineService pipelineService;

    @BeforeEach
    void setUp() {
        pipelineService = new PipelineService(pipelineDAO);
    }

    @Nested
    @DisplayName("Validation Tests (Win Probability & Stage Order)")
    class ValidationTests {

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 25, 50, 75, 99, 100})
        @DisplayName("Valid win_probability in [0, 100] is accepted")
        void validWinProbability_accepted(int probability) throws SQLException {
            PipelineStage stage = new PipelineStage(null, 1L, "TEST", "Test Stage", 1, probability, "", false, false, true);
            PipelineStage created = new PipelineStage(10L, 1L, "TEST", "Test Stage", 1, probability, "", false, false, true);

            when(pipelineDAO.existsByCode(any(Connection.class), eq(1L), eq("TEST"), isNull())).thenReturn(false);
            when(pipelineDAO.insert(any(Connection.class), any(PipelineStage.class))).thenReturn(10L);
            when(pipelineDAO.findById(any(Connection.class), eq(10L))).thenReturn(created);

            PipelineStage result = pipelineService.createStage(stage);

            assertNotNull(result);
            assertEquals(probability, result.getWinProbability());
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, -10, 101, 150})
        @DisplayName("Invalid win_probability (< 0 or > 100) throws IllegalArgumentException")
        void invalidWinProbability_throws(int probability) {
            PipelineStage stage = new PipelineStage(null, 1L, "TEST", "Test Stage", 1, probability, "", false, false, true);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pipelineService.createStage(stage));
            assertTrue(ex.getMessage().contains("Tỷ lệ thắng (win_probability)"));
        }

        @Test
        @DisplayName("Stage order less than 1 throws IllegalArgumentException")
        void invalidStageOrder_throws() {
            PipelineStage stage = new PipelineStage(null, 1L, "TEST", "Test Stage", 0, 50, "", false, false, true);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pipelineService.createStage(stage));
            assertTrue(ex.getMessage().contains("Thứ tự giai đoạn (stage_order)"));
        }

        @Test
        @DisplayName("Blank code or name throws IllegalArgumentException")
        void blankCodeOrName_throws() {
            PipelineStage blankCode = new PipelineStage(null, 1L, "  ", "Test Stage", 1, 50, "", false, false, true);
            assertThrows(IllegalArgumentException.class, () -> pipelineService.createStage(blankCode));

            PipelineStage blankName = new PipelineStage(null, 1L, "CODE", "  ", 1, 50, "", false, false, true);
            assertThrows(IllegalArgumentException.class, () -> pipelineService.createStage(blankName));
        }

        @Test
        @DisplayName("Duplicate stage code in same pipeline throws IllegalArgumentException")
        void duplicateCode_throws() throws SQLException {
            PipelineStage duplicate = new PipelineStage(null, 1L, "QUALIFICATION", "Đánh giá", 1, 25, "", false, false, true);
            when(pipelineDAO.existsByCode(any(Connection.class), eq(1L), eq("QUALIFICATION"), isNull())).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pipelineService.createStage(duplicate));
            assertTrue(ex.getMessage().contains("đã tồn tại"));
        }
    }

    @Nested
    @DisplayName("Opportunity Protection Tests (Bảo toàn dữ liệu & Chặn xóa khi có Opportunity)")
    class OpportunityProtectionTests {

        @Test
        @DisplayName("Deleting stage with active opportunities without targetStage throws StageInUseException")
        void deleteStageWithOpportunities_blocksDeletion() throws SQLException {
            PipelineStage stage = new PipelineStage(STAGE_ID, 1L, "PROPOSAL", "Báo giá", 3, 50, "", false, false, true);

            when(pipelineDAO.findById(any(Connection.class), eq(STAGE_ID))).thenReturn(stage);
            when(pipelineDAO.countOpportunitiesByStage(any(Connection.class), eq(STAGE_ID))).thenReturn(5L);

            StageInUseException ex = assertThrows(StageInUseException.class,
                    () -> pipelineService.deleteStage(STAGE_ID, null));

            assertEquals(STAGE_ID, ex.getStageId());
            assertEquals(5L, ex.getOpportunityCount());
            assertTrue(ex.getMessage().contains("đang có 5 cơ hội"));
            verify(pipelineDAO, never()).delete(any(Connection.class), anyLong());
        }

        @Test
        @DisplayName("Deleting stage with active opportunities WITH targetStage migrates and succeeds")
        void deleteStageWithOpportunities_withTargetStage_migratesAndDeletes() throws SQLException, StageInUseException {
            PipelineStage stage = new PipelineStage(STAGE_ID, 1L, "PROPOSAL", "Báo giá", 3, 50, "", false, false, true);
            PipelineStage target = new PipelineStage(TARGET_STAGE_ID, 1L, "NEGOTIATION", "Đàm phán", 4, 75, "", false, false, true);

            when(pipelineDAO.findById(any(Connection.class), eq(STAGE_ID))).thenReturn(stage);
            when(pipelineDAO.countOpportunitiesByStage(any(Connection.class), eq(STAGE_ID))).thenReturn(3L);
            when(pipelineDAO.findById(any(Connection.class), eq(TARGET_STAGE_ID))).thenReturn(target);
            when(pipelineDAO.reassignOpportunities(any(Connection.class), eq(STAGE_ID), eq(TARGET_STAGE_ID))).thenReturn(3);
            when(pipelineDAO.delete(any(Connection.class), eq(STAGE_ID))).thenReturn(1);

            boolean result = pipelineService.deleteStage(STAGE_ID, TARGET_STAGE_ID);

            assertTrue(result);
            verify(pipelineDAO).reassignOpportunities(any(Connection.class), eq(STAGE_ID), eq(TARGET_STAGE_ID));
            verify(pipelineDAO).delete(any(Connection.class), eq(STAGE_ID));
        }

        @Test
        @DisplayName("Deleting empty stage succeeds directly")
        void deleteEmptyStage_succeeds() throws SQLException, StageInUseException {
            PipelineStage stage = new PipelineStage(STAGE_ID, 1L, "CUSTOM", "Custom", 7, 30, "", false, false, true);

            when(pipelineDAO.findById(any(Connection.class), eq(STAGE_ID))).thenReturn(stage);
            when(pipelineDAO.countOpportunitiesByStage(any(Connection.class), eq(STAGE_ID))).thenReturn(0L);
            when(pipelineDAO.delete(any(Connection.class), eq(STAGE_ID))).thenReturn(1);

            boolean result = pipelineService.deleteStage(STAGE_ID, null);

            assertTrue(result);
            verify(pipelineDAO).delete(any(Connection.class), eq(STAGE_ID));
        }

        @Test
        @DisplayName("Database foreign key violation is translated to StageInUseException")
        void fkViolation_translatedToStageInUseException() throws SQLException {
            PipelineStage stage = new PipelineStage(STAGE_ID, 1L, "CUSTOM", "Custom", 7, 30, "", false, false, true);

            when(pipelineDAO.findById(any(Connection.class), eq(STAGE_ID))).thenReturn(stage);
            when(pipelineDAO.countOpportunitiesByStage(any(Connection.class), eq(STAGE_ID))).thenReturn(0L);
            when(pipelineDAO.delete(any(Connection.class), eq(STAGE_ID)))
                    .thenThrow(new SQLIntegrityConstraintViolationException("FK constraint error 1451"));

            assertThrows(StageInUseException.class, () -> pipelineService.deleteStage(STAGE_ID, null));
        }
    }

    @Nested
    @DisplayName("Stage Transition Validation Tests (Validate điều kiện chuyển stage)")
    class TransitionValidationTests {

        @Test
        @DisplayName("Transition to WON without amount fails validation")
        void transitionToWon_noAmount_fails() {
            PipelineStage current = new PipelineStage(1L, 1L, "NEGOTIATION", "Đàm phán", 4, 75, "", false, false, true);
            PipelineStage won = new PipelineStage(2L, 1L, "CLOSED_WON", "Thành công", 5, 100, "", true, false, true);

            TransitionContext context = new TransitionContext(BigDecimal.ZERO, "Nguyen Van B", null, true);

            TransitionValidationResult result = pipelineService.validateTransition(current, won, context);

            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("giá trị doanh số")));
        }

        @Test
        @DisplayName("Transition to WON with positive amount succeeds")
        void transitionToWon_withAmount_succeeds() {
            PipelineStage current = new PipelineStage(1L, 1L, "NEGOTIATION", "Đàm phán", 4, 75, "", false, false, true);
            PipelineStage won = new PipelineStage(2L, 1L, "CLOSED_WON", "Thành công", 5, 100, "", true, false, true);

            TransitionContext context = new TransitionContext(new BigDecimal("50000000"), "Nguyen Van B", null, true);

            TransitionValidationResult result = pipelineService.validateTransition(current, won, context);

            assertTrue(result.valid());
            assertTrue(result.errors().isEmpty());
        }

        @Test
        @DisplayName("Transition to LOST without lost_reason fails validation")
        void transitionToLost_noReason_fails() {
            PipelineStage current = new PipelineStage(1L, 1L, "PROPOSAL", "Báo giá", 3, 50, "", false, false, true);
            PipelineStage lost = new PipelineStage(3L, 1L, "CLOSED_LOST", "Thất bại", 6, 0, "", false, true, true);

            TransitionContext context = new TransitionContext(BigDecimal.ZERO, "Nguyen Van B", "   ", true);

            TransitionValidationResult result = pipelineService.validateTransition(current, lost, context);

            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("lý do thất bại")));
        }

        @Test
        @DisplayName("Transition to LOST with clear lost_reason succeeds")
        void transitionToLost_withReason_succeeds() {
            PipelineStage current = new PipelineStage(1L, 1L, "PROPOSAL", "Báo giá", 3, 50, "", false, false, true);
            PipelineStage lost = new PipelineStage(3L, 1L, "CLOSED_LOST", "Thất bại", 6, 0, "", false, true, true);

            TransitionContext context = new TransitionContext(BigDecimal.ZERO, "Nguyen Van B", "Khách hàng chọn nhà cung cấp khác do giá rẻ hơn", true);

            TransitionValidationResult result = pipelineService.validateTransition(current, lost, context);

            assertTrue(result.valid());
        }

        @Test
        @DisplayName("Transition when current stage exit criteria are unconfirmed fails")
        void transition_unconfirmedExitCriteria_fails() {
            PipelineStage current = new PipelineStage(1L, 1L, "QUALIFICATION", "Đánh giá", 2, 25, "Bắt buộc xác định rõ ngân sách BANT", false, false, true);
            PipelineStage target = new PipelineStage(2L, 1L, "PROPOSAL", "Báo giá", 3, 50, "", false, false, true);

            TransitionContext context = new TransitionContext(null, "Nguyen Van B", null, false);

            TransitionValidationResult result = pipelineService.validateTransition(current, target, context);

            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("điều kiện rời giai đoạn")));
        }
    }

    @Nested
    @DisplayName("Stage Reordering Tests")
    class StageReorderingTests {

        @Test
        @DisplayName("reorderStages updates order for each stage in sequence")
        void reorderStages_succeeds() throws SQLException {
            when(pipelineDAO.updateStageOrder(any(Connection.class), eq(10L), eq(1))).thenReturn(1);
            when(pipelineDAO.updateStageOrder(any(Connection.class), eq(20L), eq(2))).thenReturn(1);
            when(pipelineDAO.updateStageOrder(any(Connection.class), eq(30L), eq(3))).thenReturn(1);

            boolean result = pipelineService.reorderStages(1L, List.of(10L, 20L, 30L));

            assertTrue(result);
            verify(pipelineDAO).updateStageOrder(any(Connection.class), eq(10L), eq(1));
            verify(pipelineDAO).updateStageOrder(any(Connection.class), eq(20L), eq(2));
            verify(pipelineDAO).updateStageOrder(any(Connection.class), eq(30L), eq(3));
        }
    }
}
