package com.crm.service.scope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("S1-05: Kiểm thử tự động kiểm soát phạm vi dữ liệu sở hữu")
class DataScopeEnforcementTest {

    private ScopeAccessPolicy accessPolicy;

    @BeforeEach
    void setUp() {
        accessPolicy = new ScopeAccessPolicy();
    }

    @Test
    @DisplayName("Tiêu chí 1: Chứng minh Nhân viên A không thể đọc khách hàng của Nhân viên B")
    void nhanVienAKhongTheDocKhachHangCuaNhanVienB() {
        // Nhân viên A: id = 10, team = 1, scope = SELF
        ScopeContext nhanVienA = new ScopeContext(10L, 1L, "SELF");

        // Khách hàng do Nhân viên B sở hữu: id = 100, owner = 20
        ScopeRecord khachHangCuaB = new ScopeRecord(
                100L,
                "Công ty Cổ phần Alpha (Khách của B)",
                20L,
                1L
        );

        // Khẳng định: canAccess phải trả về false
        boolean coQuyen = accessPolicy.canAccess(nhanVienA, khachHangCuaB);
        assertFalse(coQuyen, "Nhân viên A tuyệt đối không được phép đọc khách hàng của nhân viên B");
    }

    @Test
    @DisplayName("Tiêu chí 2: Nhân viên chỉ xem được dữ liệu thuộc phạm vi của mình trên 4 loại thực thể")
    void nhanVienChiXemDuocDuLieuThuocPhamViCuaMinh() {
        ScopeContext nhanVienA = new ScopeContext(10L, 1L, "SELF");

        // 1. Khách hàng
        ScopeRecord myCustomer = new ScopeRecord(1L, "Khách hàng của tôi", 10L, 1L);
        ScopeRecord otherCustomer = new ScopeRecord(2L, "Khách hàng của người khác", 20L, 1L);
        assertTrue(accessPolicy.canAccess(nhanVienA, myCustomer));
        assertFalse(accessPolicy.canAccess(nhanVienA, otherCustomer));

        // 2. Cơ hội
        ScopeRecord myOpp = new ScopeRecord(3L, "Cơ hội bán phần mềm của tôi", 10L, 1L);
        ScopeRecord otherOpp = new ScopeRecord(4L, "Cơ hội của người khác", 20L, 1L);
        assertTrue(accessPolicy.canAccess(nhanVienA, myOpp));
        assertFalse(accessPolicy.canAccess(nhanVienA, otherOpp));

        // 3. Hoạt động
        ScopeRecord myAct = new ScopeRecord(5L, "Gặp mặt tư vấn của tôi", 10L, 1L);
        ScopeRecord otherAct = new ScopeRecord(6L, "Cuộc gọi của người khác", 20L, 1L);
        assertTrue(accessPolicy.canAccess(nhanVienA, myAct));
        assertFalse(accessPolicy.canAccess(nhanVienA, otherAct));

        // 4. Báo giá
        ScopeRecord myQuote = new ScopeRecord(7L, "BG-2026-001 của tôi", 10L, 1L);
        ScopeRecord otherQuote = new ScopeRecord(8L, "BG-2026-002 của người khác", 20L, 1L);
        assertTrue(accessPolicy.canAccess(nhanVienA, myQuote));
        assertFalse(accessPolicy.canAccess(nhanVienA, otherQuote));
    }

    @Test
    @DisplayName("Tiêu chí 3: Trưởng nhóm xem được toàn bộ dữ liệu của nhóm mình, bị chặn xem nhóm khác")
    void truongNhomXemDuocDuLieuCuaNhomMinh() {
        // Trưởng nhóm nhóm 1: id = 100, team = 1, scope = TEAM
        ScopeContext truongNhom1 = new ScopeContext(100L, 1L, "TEAM");

        // Bản ghi của thành viên A (thuộc nhóm 1)
        ScopeRecord recordNhanVienNhom1 = new ScopeRecord(10L, "Khách hàng thành viên nhóm 1", 10L, 1L);
        // Bản ghi của thành viên B (thuộc nhóm 1)
        ScopeRecord recordNhanVienNhom1B = new ScopeRecord(11L, "Khách hàng thành viên B nhóm 1", 11L, 1L);
        // Bản ghi do chính trưởng nhóm tạo
        ScopeRecord recordCuaTruongNhom = new ScopeRecord(12L, "Khách hàng của chính trưởng nhóm", 100L, 1L);

        // Bản ghi của nhân viên thuộc nhóm 2
        ScopeRecord recordNhom2 = new ScopeRecord(20L, "Khách hàng thuộc nhóm 2", 50L, 2L);
        // Bản ghi của nhân viên chưa thuộc nhóm nào
        ScopeRecord recordChuaGanNhom = new ScopeRecord(30L, "Khách hàng tự do", 99L, null);

        // Trưởng nhóm xem được tất cả bản ghi của nhóm mình
        assertTrue(accessPolicy.canAccess(truongNhom1, recordNhanVienNhom1));
        assertTrue(accessPolicy.canAccess(truongNhom1, recordNhanVienNhom1B));
        assertTrue(accessPolicy.canAccess(truongNhom1, recordCuaTruongNhom));

        // Trưởng nhóm bị CHẶN khi cố truy cập dữ liệu của nhóm 2 hoặc dữ liệu ngoài nhóm
        assertFalse(accessPolicy.canAccess(truongNhom1, recordNhom2));
        assertFalse(accessPolicy.canAccess(truongNhom1, recordChuaGanNhom));
    }

    @Test
    @DisplayName("Tiêu chí 4: Giám đốc kinh doanh có thể xem dữ liệu theo phạm vi 'tất cả'")
    void giamDocKinhDoanhXemDuLieuTheoPhamViTatCa() {
        // Giám đốc kinh doanh: id = 1, scope = ALL
        ScopeContext giamDoc = new ScopeContext(1L, null, "ALL");

        ScopeRecord recordNhom1 = new ScopeRecord(101L, "Hợp đồng lớn Nhóm 1", 10L, 1L);
        ScopeRecord recordNhom2 = new ScopeRecord(102L, "Hợp đồng lớn Nhóm 2", 20L, 2L);
        ScopeRecord recordNhom3 = new ScopeRecord(103L, "Hợp đồng lớn Nhóm 3", 30L, 3L);
        ScopeRecord recordKhongNhom = new ScopeRecord(104L, "Báo giá đặc biệt", 999L, null);

        // Giám đốc xem được tất cả dữ liệu trên hệ thống
        assertTrue(accessPolicy.canAccess(giamDoc, recordNhom1));
        assertTrue(accessPolicy.canAccess(giamDoc, recordNhom2));
        assertTrue(accessPolicy.canAccess(giamDoc, recordNhom3));
        assertTrue(accessPolicy.canAccess(giamDoc, recordKhongNhom));
    }
}
