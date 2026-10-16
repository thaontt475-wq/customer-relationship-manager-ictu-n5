package com.crm.controller.leads;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.leads.LeadFilterRequest;
import com.crm.model.leads.Lead;
import com.crm.model.leads.SavedLeadFilter;
import com.crm.service.leads.LeadService;
import com.crm.service.leads.SavedLeadFilterService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet({"/api/leads", "/api/leads/*"})
public class LeadServlet extends HttpServlet {

    private final LeadService leadService;
    private final SavedLeadFilterService savedFilterService;

    public LeadServlet() {
        this.leadService = new LeadService();
        this.savedFilterService = new SavedLeadFilterService();
    }

    public LeadServlet(LeadService leadService, SavedLeadFilterService savedFilterService) {
        this.leadService = leadService;
        this.savedFilterService = savedFilterService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long currentUserId = requireUserId(req);
        String path = req.getPathInfo();

        try {
            // 1. Saved Filters API: GET /api/leads/saved-filters (AC 3)
            if ("/saved-filters".equals(path)) {
                var filters = savedFilterService.getAllFilters(currentUserId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách bộ lọc lưu sẵn thành công", filters));
                return;
            }

            // 2. Statistics API: GET /api/leads/stats
            if ("/stats".equals(path)) {
                var stats = leadService.getLeadStats(currentUserId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy thống kê Lead thành công", stats));
                return;
            }

            // 3. Lead Detail API: GET /api/leads/{id}
            if (path != null && path.matches("^/\\d+$")) {
                long id = Long.parseLong(path.substring(1));
                Lead lead = leadService.getLeadById(id, currentUserId);
                if (lead != null) {
                    ResponseUtil.json(resp, 200, ApiResponse.success("Chi tiết Lead", lead));
                } else {
                    ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy Lead với ID " + id, null));
                }
                return;
            }

            // 4. Advanced Multi-Criteria Search & Filter API: GET /api/leads (AC 1 & AC 2)
            if (path == null || "/".equals(path) || path.isBlank()) {
                LeadFilterRequest filter = parseFilterRequest(req);
                Map<String, Object> result = leadService.searchLeads(filter, currentUserId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách Lead thành công", result));
                return;
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Endpoint không tồn tại", null));

        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            System.err.println("Error processing GET /api/leads: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi xử lý yêu cầu danh sách Lead: " + e.getMessage(), null));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long currentUserId = requireUserId(req);
        String path = req.getPathInfo();
        String body = readBodyString(req);

        try {
            // 1. Create Saved Filter API: POST /api/leads/saved-filters (AC 3)
            if ("/saved-filters".equals(path)) {
                if (body == null || body.isBlank()) {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Dữ liệu bộ lọc không được để trống", null));
                    return;
                }

                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                String name = json.has("name") ? json.get("name").getAsString() : "Bộ lọc chưa đặt tên";
                boolean isDefault = json.has("setAsDefault") && json.get("setAsDefault").getAsBoolean();

                SavedLeadFilter filter = new SavedLeadFilter();
                filter.setName(name);
                filter.setDefault(isDefault);

                if (json.has("filter")) {
                    filter.setFilter(json.get("filter"));
                    filter.setFilterCriteria(json.get("filter").toString());
                } else if (json.has("filterCriteria")) {
                    filter.setFilterCriteria(json.get("filterCriteria").getAsString());
                }

                SavedLeadFilter saved = savedFilterService.saveCustomFilter(filter, currentUserId);
                if (saved != null) {
                    ResponseUtil.json(resp, 201, ApiResponse.success("Lưu bộ lọc thành công", saved));
                } else {
                    ResponseUtil.json(resp, 500, ApiResponse.error("Không thể lưu bộ lọc", null));
                }
                return;
            }

            // 2. Create Lead API: POST /api/leads
            if (path == null || "/".equals(path) || path.isBlank()) {
                if (body == null || body.isBlank()) {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Dữ liệu Lead không được để trống", null));
                    return;
                }

                Lead lead = JsonUtil.getGson().fromJson(body, Lead.class);
                if (lead.getFullName() == null || lead.getFullName().isBlank()) {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Họ và tên khách hàng là bắt buộc", null));
                    return;
                }

                Lead created = leadService.createLead(lead, currentUserId);
                ResponseUtil.json(resp, 201, ApiResponse.success("Tạo Lead thành công", created));
                return;
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Endpoint không tồn tại", null));

        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            System.err.println("Error processing POST /api/leads: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi tạo dữ liệu: " + e.getMessage(), null));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long currentUserId = requireUserId(req);
        String path = req.getPathInfo();
        String body = readBodyString(req);

        try {
            // 1. Set Default Filter API: PUT /api/leads/saved-filters/{id}/default
            if (path != null && path.startsWith("/saved-filters/") && path.endsWith("/default")) {
                String idPart = path.substring("/saved-filters/".length(), path.indexOf("/default"));
                long filterId = parseIdSafe(idPart);
                boolean success = savedFilterService.setDefaultFilter(filterId, currentUserId);
                if (success) {
                    ResponseUtil.json(resp, 200, ApiResponse.success("Đặt bộ lọc mặc định thành công", null));
                } else {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Không thể đặt bộ lọc mặc định", null));
                }
                return;
            }

            // 2. Update Saved Filter API: PUT /api/leads/saved-filters/{id}
            if (path != null && path.startsWith("/saved-filters/")) {
                String idPart = path.substring("/saved-filters/".length());
                long filterId = parseIdSafe(idPart);

                SavedLeadFilter filter = JsonUtil.getGson().fromJson(body, SavedLeadFilter.class);
                filter.setId(filterId);
                boolean updated = savedFilterService.updateCustomFilter(filter, currentUserId);
                if (updated) {
                    ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật bộ lọc thành công", filter));
                } else {
                    ResponseUtil.json(resp, 400, ApiResponse.error("Không thể cập nhật bộ lọc", null));
                }
                return;
            }

            // 3. Update Lead API: PUT /api/leads/{id}
            if (path != null && path.matches("^/\\d+$")) {
                String idPart = path.substring(1);
                long leadId = parseIdSafe(idPart);
                if (leadId > 0) {
                    Lead lead = JsonUtil.getGson().fromJson(body, Lead.class);
                    lead.setId(leadId);
                    boolean updated = leadService.updateLead(lead, currentUserId);
                    if (updated) {
                        ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật Lead thành công", lead));
                    } else {
                        ResponseUtil.json(resp, 400, ApiResponse.error("Không thể cập nhật Lead", null));
                    }
                    return;
                }
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Endpoint không tồn tại", null));

        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            System.err.println("Error processing PUT /api/leads: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi cập nhật dữ liệu: " + e.getMessage(), null));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long currentUserId = requireUserId(req);
        String path = req.getPathInfo();

        try {
            // 1. Delete Saved Filter API: DELETE /api/leads/saved-filters/{id} (AC 3)
            if (path != null && path.startsWith("/saved-filters/")) {
                String idPart = path.substring("/saved-filters/".length());
                long filterId = parseIdSafe(idPart);
                boolean deleted = savedFilterService.deleteCustomFilter(filterId, currentUserId);
                if (deleted) {
                    ResponseUtil.json(resp, 200, ApiResponse.success("Xóa bộ lọc thành công", null));
                } else {
                    ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy bộ lọc để xóa", null));
                }
                return;
            }

            // 2. Delete Lead API: DELETE /api/leads/{id}
            if (path != null && path.matches("^/\\d+$")) {
                String idPart = path.substring(1);
                long leadId = parseIdSafe(idPart);
                if (leadId > 0) {
                    boolean deleted = leadService.deleteLead(leadId, currentUserId);
                    if (deleted) {
                        ResponseUtil.json(resp, 200, ApiResponse.success("Xóa Lead thành công", null));
                    } else {
                        ResponseUtil.json(resp, 404, ApiResponse.error("Không tìm thấy Lead để xóa", null));
                    }
                    return;
                }
            }

            ResponseUtil.json(resp, 404, ApiResponse.error("Endpoint không tồn tại", null));

        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            ResponseUtil.json(resp, 409, ApiResponse.error(e.getMessage(), null));
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            System.err.println("Error processing DELETE /api/leads: " + e.getMessage());
            e.printStackTrace();
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi xóa dữ liệu: " + e.getMessage(), null));
        }
    }

    private LeadFilterRequest parseFilterRequest(HttpServletRequest req) {
        LeadFilterRequest f = new LeadFilterRequest();

        // Keyword
        String search = req.getParameter("search");
        if (search == null || search.isBlank()) search = req.getParameter("keyword");
        if (search != null) f.setKeyword(search);

        // Status
        String status = req.getParameter("status");
        if (status != null && !status.isBlank()) f.setStatus(status);

        // Tier / Rating
        String tier = req.getParameter("tier");
        if (tier == null || tier.isBlank()) tier = req.getParameter("rating");
        if (tier != null && !tier.isBlank()) f.setTier(tier);

        // Source
        String source = req.getParameter("source");
        if (source != null && !source.isBlank()) f.setSource(source);

        // Owner
        String owner = req.getParameter("owner_id");
        if (owner == null || owner.isBlank()) owner = req.getParameter("owner");
        if (owner == null || owner.isBlank()) owner = req.getParameter("assigned_to");
        if (owner != null && !owner.isBlank()) {
            f.setOwner(owner);
            if (owner.matches("\\d+")) {
                f.setOwnerId(Long.parseLong(owner));
            }
        }

        // Date range
        String dateRange = req.getParameter("dateRange");
        if (dateRange != null && !dateRange.isBlank()) f.setDateRange(dateRange);

        String from = req.getParameter("from");
        if (from == null || from.isBlank()) from = req.getParameter("fromDate");
        if (from == null || from.isBlank()) from = req.getParameter("created_from");
        if (from != null) f.setFrom(from);

        String to = req.getParameter("to");
        if (to == null || to.isBlank()) to = req.getParameter("toDate");
        if (to == null || to.isBlank()) to = req.getParameter("created_to");
        if (to != null) f.setTo(to);

        // SLA Overdue Only
        String overdue = req.getParameter("slaOverdueOnly");
        if (overdue == null || overdue.isBlank()) overdue = req.getParameter("is_overdue_sla");
        if ("true".equalsIgnoreCase(overdue) || "1".equals(overdue)) {
            f.setSlaOverdueOnly(true);
        }

        // Sort
        String sort = req.getParameter("sort");
        if (sort != null && !sort.isBlank()) f.setSort(sort);

        // Pagination
        f.setPage(parseIntSafe(req.getParameter("page"), 1));
        int size = parseIntSafe(req.getParameter("size"), 10);
        if (size <= 0) size = parseIntSafe(req.getParameter("pageSize"), 10);
        f.setSize(size);

        return f;
    }

    private long requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") instanceof Number num) {
            return num.longValue();
        }
        return 1L; // Fallback for development / public forms
    }

    private long parseIdSafe(String idStr) {
        if (idStr == null || idStr.isBlank()) return -1;
        try {
            String digits = idStr.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? -1 : Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int parseIntSafe(String val, int def) {
        if (val == null || val.isBlank()) return def;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private String readBodyString(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
