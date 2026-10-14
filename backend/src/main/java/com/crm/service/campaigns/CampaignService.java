package com.crm.service.campaigns;

import com.crm.dao.campaigns.CampaignDAO;
import com.crm.dao.campaigns.CampaignLeadDAO;
import com.crm.dto.campaigns.CampaignLeadLinkRequest;
import com.crm.dto.campaigns.CampaignWriteRequest;
import com.crm.model.campaigns.Campaign;
import com.crm.model.campaigns.CampaignLead;
import com.crm.model.campaigns.Lead;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class CampaignService {

    private final CampaignDAO campaignDAO;
    private final CampaignLeadDAO campaignLeadDAO;

    public CampaignService() {
        this(new CampaignDAO(), new CampaignLeadDAO());
    }

    public CampaignService(CampaignDAO campaignDAO, CampaignLeadDAO campaignLeadDAO) {
        this.campaignDAO = campaignDAO;
        this.campaignLeadDAO = campaignLeadDAO;
    }

    public Map<String, Object> search(String keyword, String status, int page, int size) throws SQLException {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        int offset = (page - 1) * size;

        List<Campaign> items = campaignDAO.findPaged(keyword, status, offset, size);
        int total = campaignDAO.countTotal(keyword, status);
        int totalPages = (int) Math.ceil((double) total / size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        pagination.put("totalItems", total);
        pagination.put("totalPages", totalPages);
        result.put("pagination", pagination);

        return result;
    }

    public Campaign getById(long id) throws SQLException {
        Campaign campaign = campaignDAO.findById(id);
        if (campaign == null) {
            throw new NoSuchElementException("Không tìm thấy chiến dịch với mã ID: " + id);
        }
        return campaign;
    }

    public long create(long currentUserId, CampaignWriteRequest req) throws SQLException {
        validateRequest(req, null);

        Campaign c = new Campaign();
        mapFields(c, req);
        c.setOwnerId(req.getOwnerId() != null ? req.getOwnerId() : currentUserId);

        return campaignDAO.insert(c);
    }

    public void update(long id, CampaignWriteRequest req) throws SQLException {
        Campaign existing = campaignDAO.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Không tìm thấy chiến dịch để cập nhật");
        }
        validateRequest(req, id);
        mapFields(existing, req);
        campaignDAO.update(existing);
    }

    public void delete(long id) throws SQLException {
        Campaign existing = campaignDAO.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Không tìm thấy chiến dịch để xóa");
        }
        campaignDAO.softDelete(id);
    }

    public List<CampaignLead> getLeads(long campaignId) throws SQLException {
        getById(campaignId); // check exists
        return campaignLeadDAO.getLeadsByCampaign(campaignId);
    }

    public List<Lead> getAvailableLeads(long campaignId) throws SQLException {
        getById(campaignId);
        return campaignLeadDAO.getAvailableLeads(campaignId);
    }

    public int linkLeads(long campaignId, CampaignLeadLinkRequest req) throws SQLException {
        getById(campaignId);
        if (req == null || req.getLeadIds() == null || req.getLeadIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một Lead để liên kết vào chiến dịch");
        }
        return campaignLeadDAO.addLeads(campaignId, req.getLeadIds(), req.getStatus(), req.getNotes());
    }

    public void updateLeadStatus(long campaignId, long leadId, String status, String notes) throws SQLException {
        getById(campaignId);
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Trạng thái Lead không được để trống");
        }
        boolean ok = campaignLeadDAO.updateLeadStatus(campaignId, leadId, status, notes);
        if (!ok) {
            throw new NoSuchElementException("Không tìm thấy Lead trong chiến dịch này để cập nhật");
        }
    }

    public void unlinkLead(long campaignId, long leadId) throws SQLException {
        getById(campaignId);
        boolean ok = campaignLeadDAO.removeLead(campaignId, leadId);
        if (!ok) {
            throw new NoSuchElementException("Không tìm thấy Lead trong chiến dịch để gỡ bỏ");
        }
    }

    private void validateRequest(CampaignWriteRequest req, Long excludeId) throws SQLException {
        if (req == null) throw new IllegalArgumentException("Dữ liệu chiến dịch không hợp lệ");

        if (req.getName() == null || req.getName().trim().length() < 3) {
            throw new IllegalArgumentException("Tên chiến dịch phải có ít nhất 3 ký tự");
        }
        if (req.getCode() == null || req.getCode().trim().isBlank()) {
            throw new IllegalArgumentException("Mã chiến dịch không được để trống");
        }
        if (campaignDAO.existsByCode(req.getCode().trim(), excludeId)) {
            throw new IllegalArgumentException("Mã chiến dịch '" + req.getCode() + "' đã tồn tại trên hệ thống");
        }

        LocalDate start = parseDate(req.getStartDate());
        LocalDate end = parseDate(req.getEndDate());
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }

        if (req.getBudget() != null && req.getBudget().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Ngân sách dự kiến không được âm");
        }
    }

    private void mapFields(Campaign c, CampaignWriteRequest req) {
        c.setCode(req.getCode().trim());
        c.setName(req.getName().trim());
        c.setType(req.getType() != null && !req.getType().isBlank() ? req.getType().trim() : "EMAIL");
        c.setStatus(req.getStatus() != null && !req.getStatus().isBlank() ? req.getStatus().trim() : "PLANNING");
        c.setStartDate(parseDate(req.getStartDate()));
        c.setEndDate(parseDate(req.getEndDate()));
        c.setBudget(req.getBudget() != null ? req.getBudget() : BigDecimal.ZERO);
        c.setActualCost(req.getActualCost() != null ? req.getActualCost() : BigDecimal.ZERO);
        c.setExpectedRevenue(req.getExpectedRevenue() != null ? req.getExpectedRevenue() : BigDecimal.ZERO);
        c.setDescription(req.getDescription());
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return LocalDate.parse(val.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Định dạng ngày không hợp lệ (yêu cầu YYYY-MM-DD): " + val);
        }
    }
}
