package com.crm.service.scope;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopeAccessPolicyTest {

    private final ScopeAccessPolicy policy = new ScopeAccessPolicy();

    @Test
    @DisplayName("S1-05: Nhân viên A không thể đọc khách hàng của nhân viên B")
    void nhanVienAKhongTheDocKhachHangCuaNhanVienB() {
        // Nhân viên A có phạm vi dữ liệu "Của tôi" (SELF)
        ScopeContext employeeA = new ScopeContext(10L, 1L, "SELF");

        // Khách hàng thuộc sở hữu của nhân viên B (kể cả cùng nhóm hay khác nhóm)
        ScopeRecord customerOwnedByB = new ScopeRecord(
                100L,
                "Công ty TNHH Khách hàng của B",
                20L,
                1L
        );

        // Chứng minh: Nhân viên A tuyệt đối KHÔNG thể truy cập khách hàng của nhân viên B
        assertFalse(
                policy.canAccess(employeeA, customerOwnedByB),
                "Nhân viên A với scope SELF không được phép đọc khách hàng của nhân viên B"
        );
    }

    @Test
    @DisplayName("S1-05: Nhân viên chỉ xem được dữ liệu thuộc phạm vi của mình (Khách hàng, Cơ hội, Hoạt động, Báo giá)")
    void nhanVienChiXemDuocDuLieuThuocPhamViCuaMinh() {
        ScopeContext employeeA = new ScopeContext(10L, 1L, "SELF");

        // Các bản ghi do chính Nhân viên A sở hữu
        ScopeRecord myCustomer = new ScopeRecord(1L, "Khách hàng của A", 10L, 1L);
        ScopeRecord myOpportunity = new ScopeRecord(2L, "Cơ hội dự án của A", 10L, 1L);
        ScopeRecord myActivity = new ScopeRecord(3L, "Cuộc gọi chăm sóc của A", 10L, 1L);
        ScopeRecord myQuote = new ScopeRecord(4L, "Báo giá BG-2026-001 của A", 10L, 1L);

        // Nhân viên A xem được dữ liệu của chính mình
        assertTrue(policy.canAccess(employeeA, myCustomer));
        assertTrue(policy.canAccess(employeeA, myOpportunity));
        assertTrue(policy.canAccess(employeeA, myActivity));
        assertTrue(policy.canAccess(employeeA, myQuote));

        // Các bản ghi do người khác sở hữu
        ScopeRecord otherCustomer = new ScopeRecord(11L, "Khách hàng của người khác", 20L, 1L);
        ScopeRecord otherOpportunity = new ScopeRecord(12L, "Cơ hội của người khác", 20L, 1L);
        ScopeRecord otherActivity = new ScopeRecord(13L, "Hoạt động của người khác", 20L, 1L);
        ScopeRecord otherQuote = new ScopeRecord(14L, "Báo giá của người khác", 20L, 1L);

        // Nhân viên A bị CHẶN không xem được dữ liệu của người khác
        assertFalse(policy.canAccess(employeeA, otherCustomer));
        assertFalse(policy.canAccess(employeeA, otherOpportunity));
        assertFalse(policy.canAccess(employeeA, otherActivity));
        assertFalse(policy.canAccess(employeeA, otherQuote));
    }

    @Test
    @DisplayName("S1-05: Trưởng nhóm xem được dữ liệu của nhóm mình, chặn nhóm khác")
    void truongNhomXemDuocDuLieuCuaNhomMinh() {
        // Trưởng nhóm nhóm 1 (teamId = 1L)
        ScopeContext teamLeadTeam1 = new ScopeContext(100L, 1L, "TEAM");

        // Bản ghi của thành viên A trong nhóm 1
        ScopeRecord memberRecordInSameTeam = new ScopeRecord(
                201L,
                "Khách hàng do nhân viên nhóm 1 phụ trách",
                10L,
                1L
        );

        // Bản ghi của chính Trưởng nhóm
        ScopeRecord teamLeadOwnRecord = new ScopeRecord(
                202L,
                "Khách hàng do chính trưởng nhóm phụ trách",
                100L,
                1L
        );

        // Bản ghi của nhân viên thuộc nhóm 2 (khác nhóm)
        ScopeRecord otherTeamRecord = new ScopeRecord(
                301L,
                "Khách hàng thuộc nhóm 2",
                50L,
                2L
        );

        // Trưởng nhóm xem được dữ liệu trong toàn bộ nhóm mình
        assertTrue(policy.canAccess(teamLeadTeam1, memberRecordInSameTeam));
        assertTrue(policy.canAccess(teamLeadTeam1, teamLeadOwnRecord));

        // Trưởng nhóm BỊ CHẶN xem dữ liệu của nhóm khác
        assertFalse(policy.canAccess(teamLeadTeam1, otherTeamRecord));
    }

    @Test
    @DisplayName("S1-05: Giám đốc kinh doanh có thể xem dữ liệu theo phạm vi 'tất cả'")
    void giamDocKinhDoanhXemDuLieuTheoPhamViTatCa() {
        // Giám đốc kinh doanh có phạm vi "Tất cả" (ALL)
        ScopeContext director = new ScopeContext(1L, null, "ALL");

        ScopeRecord recordFromTeam1 = new ScopeRecord(101L, "Khách hàng VIP Nhóm 1", 10L, 1L);
        ScopeRecord recordFromTeam2 = new ScopeRecord(102L, "Dự án lớn Nhóm 2", 20L, 2L);
        ScopeRecord recordWithoutTeam = new ScopeRecord(103L, "Khách hàng tự do", 999L, null);

        // Giám đốc kinh doanh xem được tất cả bản ghi
        assertTrue(policy.canAccess(director, recordFromTeam1));
        assertTrue(policy.canAccess(director, recordFromTeam2));
        assertTrue(policy.canAccess(director, recordWithoutTeam));
    }

    @Test
    @DisplayName("S1-05: Kiểm tra an toàn khi actor hoặc record null hoặc dữ liệu không hợp lệ")
    void kiemTraAnToanVaEdgeCases() {
        ScopeContext employee = new ScopeContext(10L, 1L, "SELF");
        ScopeRecord record = new ScopeRecord(1L, "Khách hàng A", 10L, 1L);

        assertFalse(policy.canAccess(null, record));
        assertFalse(policy.canAccess(employee, null));
        assertFalse(policy.canAccess(null, null));

        // Scope không hợp lệ hoặc rỗng
        ScopeContext invalidScopeUser = new ScopeContext(10L, 1L, "UNKNOWN_SCOPE");
        assertFalse(policy.canAccess(invalidScopeUser, record));
    }
}