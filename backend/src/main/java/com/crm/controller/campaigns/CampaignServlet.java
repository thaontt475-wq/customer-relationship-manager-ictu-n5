package com.crm.controller.campaigns;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.campaigns.CampaignLeadLinkRequest;
import com.crm.dto.campaigns.CampaignWriteRequest;
import com.crm.service.campaigns.CampaignService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet({
        "/api/campaigns",
        "/api/campaigns/*"
})
public class CampaignServlet extends HttpServlet {

    private final CampaignService campaignService;
    @FunctionalInterface public interface ScopeResolver { com.crm.service.permissions.DataScopeContext resolve(long user,String action) throws java.sql.SQLException; }
    private final ScopeResolver scopes;
    private final com.crm.dao.campaigns.CampaignOpportunityDAO opportunities = new com.crm.dao.campaigns.CampaignOpportunityDAO();

    public CampaignServlet() {
        this(new CampaignService());
    }

    public CampaignServlet(CampaignService campaignService) {
        this(campaignService,(user,action)->new com.crm.service.permissions.DataScopeService().resolve(user,"campaign",action));
    }
    public CampaignServlet(CampaignService campaignService, ScopeResolver scopes) {
        this.campaignService = campaignService;
        this.scopes = scopes;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Number)) {
            ResponseUtil.json(resp, 401, ApiResponse.error("Chưa đăng nhập", null));
            return;
        }
        try {
            long user=((Number)session.getAttribute("userId")).longValue();
            String path=req.getPathInfo(),method=req.getMethod();
            boolean collection=path==null || path.equals("/");
            String action="GET".equals(method)?"read":collection?"create":path.contains("opportunities")?"opportunity_manage":path.contains("leads")?"lead_manage":"DELETE".equals(method)?"delete":"update";
            var scope=scopes.resolve(user,action);
            req.setAttribute("campaignScope",scope);
            if(!collection && path.matches("/\\d+.*")) {
                var campaign=campaignService.getById(parseFirstId(path));
                if(!scope.canAccessOwner(campaign.getOwnerId()==null?0:campaign.getOwnerId())) throw new SecurityException("Chiến dịch nằm ngoài phạm vi dữ liệu");
            }
            if(path!=null && path.matches("/\\d+/(available-opportunities|opportunities)(/\\d+)?/?")) {
                long campaign=parseFirstId(path);
                Object data;
                if("GET".equals(method)) data=opportunities.list(campaign,path.contains("available-"),scope);
                else if("POST".equals(method) && path.matches("/\\d+/opportunities/?")) {
                    var body=JsonUtil.getGson().fromJson(req.getReader(),com.google.gson.JsonObject.class);
                    if(body==null || !body.has("opportunityIds") || !body.get("opportunityIds").isJsonArray()) throw new IllegalArgumentException("Thiếu opportunityIds");
                    java.util.List<Long> ids=new java.util.ArrayList<>();
                    for(var id:body.getAsJsonArray("opportunityIds")) {
                        if(!id.isJsonPrimitive() || !id.getAsJsonPrimitive().isNumber() || !id.getAsString().matches("[1-9][0-9]*")) throw new IllegalArgumentException("ID cơ hội không hợp lệ");
                        ids.add(id.getAsLong());
                    }
                    data=Map.of("linkedCount",opportunities.link(campaign,ids,scope));
                } else if("DELETE".equals(method) && path.matches("/\\d+/opportunities/\\d+/?")) {
                    long id=Long.parseLong(path.replaceAll("/+$","").substring(path.lastIndexOf('/')+1));
                    opportunities.unlink(campaign,id,scope);data=Map.of("removed",true);
                } else { resp.setHeader("Allow","GET, POST, DELETE");ResponseUtil.json(resp,405,ApiResponse.error("Sai phương thức",null));return; }
                ResponseUtil.json(resp,"POST".equals(method)?201:200,ApiResponse.success("Liên kết cơ hội chiến dịch",data));return;
            }
            super.service(req, resp);
        } catch(SecurityException e) { ResponseUtil.json(resp,403,ApiResponse.error(e.getMessage(),null)); }
        catch(NoSuchElementException e) { ResponseUtil.json(resp,404,ApiResponse.error(e.getMessage(),null)); }
        catch(IllegalArgumentException | com.google.gson.JsonParseException e) { ResponseUtil.json(resp,400,ApiResponse.error("Dữ liệu yêu cầu không hợp lệ",null)); }
        catch(java.sql.SQLException e) { log("Campaign operation failed",e);ResponseUtil.json(resp,500,ApiResponse.error("Không thể xử lý chiến dịch",null)); }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            requireUser(req);
            String path = req.getPathInfo();

            // 1. GET /api/campaigns (List with search & pagination)
            if (path == null || path.equals("/")) {
                String keyword = req.getParameter("keyword");
                String status = req.getParameter("status");
                int page = intParam(req, "page", 1);
                int size = intParam(req, "size", 20);

                var result = campaignService.search((com.crm.service.permissions.DataScopeContext)req.getAttribute("campaignScope"),keyword, status, page, size);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy danh sách chiến dịch thành công", result));
                return;
            }

            // 2. GET /api/campaigns/{id}/leads
            if (path.matches("/\\d+/leads/?")) {
                long campaignId = parseFirstId(path);
                var leads = campaignService.getLeads(campaignId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Danh sách Lead của chiến dịch", leads));
                return;
            }

            // 3. GET /api/campaigns/{id}/available-leads
            if (path.matches("/\\d+/available-leads/?")) {
                long campaignId = parseFirstId(path);
                var leads = campaignService.getAvailableLeads(campaignId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Danh sách Lead khả dụng để liên kết", leads));
                return;
            }

            // 4. GET /api/campaigns/{id}
            if (path.matches("/\\d+/?")) {
                long id = parseFirstId(path);
                var campaign = campaignService.getById(id);
                ResponseUtil.json(resp, 200, ApiResponse.success("Lấy thông tin chiến dịch thành công", campaign));
                return;
            }

            throw new IllegalArgumentException("Đường dẫn không hợp lệ: " + path);
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ: " + e.getMessage(), null));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long currentUserId = requireUser(req);
            String path = req.getPathInfo();

            // 1. POST /api/campaigns/{id}/leads (Link leads)
            if (path != null && path.matches("/\\d+/leads/?")) {
                long campaignId = parseFirstId(path);
                CampaignLeadLinkRequest body = JsonUtil.getGson().fromJson(req.getReader(), CampaignLeadLinkRequest.class);
                int added = campaignService.linkLeads(campaignId, body);
                ResponseUtil.json(resp, 201, ApiResponse.success("Đã liên kết " + added + " Lead vào chiến dịch", Map.of("addedCount", added)));
                return;
            }

            // 2. POST /api/campaigns (Create new campaign)
            if (path == null || path.equals("/")) {
                CampaignWriteRequest body = JsonUtil.getGson().fromJson(req.getReader(), CampaignWriteRequest.class);
                long createdId = campaignService.create(currentUserId, body);
                var campaign = campaignService.getById(createdId);
                ResponseUtil.json(resp, 201, ApiResponse.success("Tạo chiến dịch thành công", campaign));
                return;
            }

            throw new IllegalArgumentException("Đường dẫn không hợp lệ: " + path);
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ: " + e.getMessage(), null));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            requireUser(req);
            String path = req.getPathInfo();

            // 1. PUT /api/campaigns/{id}/leads/{leadId} (Update lead status in campaign)
            if (path != null && path.matches("/\\d+/leads/\\d+/?")) {
                String[] parts = path.substring(1).split("/");
                long campaignId = Long.parseLong(parts[0]);
                long leadId = Long.parseLong(parts[2]);

                Map<?, ?> body = JsonUtil.getGson().fromJson(req.getReader(), Map.class);
                String status = body != null && body.get("status") != null ? String.valueOf(body.get("status")) : "SENT";
                String notes = body != null && body.get("notes") != null ? String.valueOf(body.get("notes")) : null;

                campaignService.updateLeadStatus(campaignId, leadId, status, notes);
                ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật trạng thái Lead thành công", null));
                return;
            }

            // 2. PUT /api/campaigns/{id} (Update campaign)
            if (path != null && path.matches("/\\d+/?")) {
                long id = parseFirstId(path);
                CampaignWriteRequest body = JsonUtil.getGson().fromJson(req.getReader(), CampaignWriteRequest.class);
                campaignService.update(id, body);
                var updated = campaignService.getById(id);
                ResponseUtil.json(resp, 200, ApiResponse.success("Cập nhật chiến dịch thành công", updated));
                return;
            }

            throw new IllegalArgumentException("Đường dẫn không hợp lệ: " + path);
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ: " + e.getMessage(), null));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            requireUser(req);
            String path = req.getPathInfo();

            // 1. DELETE /api/campaigns/{id}/leads/{leadId} (Unlink lead)
            if (path != null && path.matches("/\\d+/leads/\\d+/?")) {
                String[] parts = path.substring(1).split("/");
                long campaignId = Long.parseLong(parts[0]);
                long leadId = Long.parseLong(parts[2]);

                campaignService.unlinkLead(campaignId, leadId);
                ResponseUtil.json(resp, 200, ApiResponse.success("Gỡ Lead khỏi chiến dịch thành công", null));
                return;
            }

            // 2. DELETE /api/campaigns/{id} (Delete campaign)
            if (path != null && path.matches("/\\d+/?")) {
                long id = parseFirstId(path);
                campaignService.delete(id);
                ResponseUtil.json(resp, 200, ApiResponse.success("Xóa chiến dịch thành công", null));
                return;
            }

            throw new IllegalArgumentException("Đường dẫn không hợp lệ: " + path);
        } catch (SecurityException e) {
            ResponseUtil.json(resp, 403, ApiResponse.error(e.getMessage(), null));
        } catch (NoSuchElementException e) {
            ResponseUtil.json(resp, 404, ApiResponse.error(e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(resp, 400, ApiResponse.error(e.getMessage(), null));
        } catch (Exception e) {
            ResponseUtil.json(resp, 500, ApiResponse.error("Lỗi máy chủ: " + e.getMessage(), null));
        }
    }

    private long requireUser(HttpServletRequest req) {
        return com.crm.service.permissions.AuthorizationService.currentUserId(req);
    }

    private long parseFirstId(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/")) {
            throw new IllegalArgumentException("Thiếu ID chiến dịch");
        }
        String clean = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        int slash = clean.indexOf('/');
        String idStr = slash >= 0 ? clean.substring(0, slash) : clean;
        return Long.parseLong(idStr);
    }

    private int intParam(HttpServletRequest req, String name, int fallback) {
        String val = req.getParameter(name);
        if (val == null || val.isBlank()) return fallback;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
