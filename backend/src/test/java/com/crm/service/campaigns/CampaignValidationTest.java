package com.crm.service.campaigns;

import com.crm.dao.campaigns.CampaignDAO;
import com.crm.dao.campaigns.CampaignLeadDAO;
import com.crm.dto.campaigns.CampaignLeadLinkRequest;
import com.crm.dto.campaigns.CampaignWriteRequest;
import com.crm.model.campaigns.Campaign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CampaignValidationTest {

    private CampaignDAO mockCampaignDAO;
    private CampaignLeadDAO mockLeadDAO;
    private CampaignService service;

    @BeforeEach
    void setUp() {
        mockCampaignDAO = new CampaignDAO() {
            @Override
            public boolean existsByCode(String code, Long excludeId) {
                return "DUPLICATE-CODE".equalsIgnoreCase(code);
            }

            @Override
            public Campaign findById(long id) {
                if (id == 999L) return null;
                Campaign c = new Campaign();
                c.setId(id);
                c.setCode("CAMP-1001");
                c.setName("Summer Promo");
                return c;
            }

            @Override
            public long insert(Campaign c) {
                return 100L;
            }

            @Override
            public boolean update(Campaign c) {
                return true;
            }

            @Override
            public boolean softDelete(long id) {
                return true;
            }
        };

        mockLeadDAO = new CampaignLeadDAO();
        service = new CampaignService(mockCampaignDAO, mockLeadDAO);
    }

    @Test
    void rejectsEmptyOrShortName() {
        CampaignWriteRequest req = new CampaignWriteRequest();
        req.setCode("CAMP-01");
        req.setName("AB"); // < 3 characters

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(1L, req));
        assertTrue(ex.getMessage().contains("Tên chiến dịch phải có ít nhất 3 ký tự"));
    }

    @Test
    void rejectsEmptyCode() {
        CampaignWriteRequest req = new CampaignWriteRequest();
        req.setName("Chiến dịch mùa hè");
        req.setCode("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(1L, req));
        assertTrue(ex.getMessage().contains("Mã chiến dịch không được để trống"));
    }

    @Test
    void rejectsDuplicateCode() {
        CampaignWriteRequest req = new CampaignWriteRequest();
        req.setName("Chiến dịch mùa đông");
        req.setCode("DUPLICATE-CODE");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(1L, req));
        assertTrue(ex.getMessage().contains("đã tồn tại"));
    }

    @Test
    void rejectsInvalidDateRange() {
        CampaignWriteRequest req = new CampaignWriteRequest();
        req.setName("Chiến dịch Black Friday");
        req.setCode("BF-2026");
        req.setStartDate("2026-11-30");
        req.setEndDate("2026-11-01"); // End date before start date

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(1L, req));
        assertTrue(ex.getMessage().contains("Ngày bắt đầu không được lớn hơn ngày kết thúc"));
    }

    @Test
    void rejectsNegativeBudget() {
        CampaignWriteRequest req = new CampaignWriteRequest();
        req.setName("Chiến dịch Tết");
        req.setCode("TET-2027");
        req.setBudget(new BigDecimal("-1000"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(1L, req));
        assertTrue(ex.getMessage().contains("Ngân sách dự kiến không được âm"));
    }

    @Test
    void rejectsEmptyLeadIdsWhenLinking() {
        CampaignLeadLinkRequest linkReq = new CampaignLeadLinkRequest();
        linkReq.setLeadIds(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.linkLeads(1L, linkReq));
        assertTrue(ex.getMessage().contains("chọn ít nhất một Lead"));
    }
}
