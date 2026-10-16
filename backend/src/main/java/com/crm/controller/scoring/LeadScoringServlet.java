package com.crm.controller.scoring;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.scoring.*;
import com.crm.service.scoring.LeadScoringService;
import com.crm.util.*;
import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;

/** Separate namespace preserves S4-02 /api/leads/* including Excel endpoints. */
@WebServlet("/api/lead-scoring/*")
public class LeadScoringServlet extends HttpServlet {
    private final LeadScoringService scoring;
    public LeadScoringServlet() {this(new LeadScoringService());}
    public LeadScoringServlet(LeadScoringService scoring) {this.scoring=scoring;}
    @Override protected void service(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        try {
            var session=req.getSession(false);
            if(session==null || !(session.getAttribute("userId") instanceof Number user)) {
                ResponseUtil.json(resp,401,ApiResponse.error("Chưa đăng nhập",null));return;
            }
            String path=req.getPathInfo(),method=req.getMethod();Object data;
            if("/config".equals(path)) {
                if(method.equals("GET"))data=scoring.config(user.longValue());
                else if(method.equals("PUT"))data=scoring.saveConfig(user.longValue(),config(body(req),false),false);
                else {unsupported(resp,"GET, PUT");return;}
            } else if("/rules".equals(path)) {
                if(!method.equals("POST")){unsupported(resp,"POST");return;}
                data=scoring.saveConfig(user.longValue(),config(body(req),true),true);
            } else if("/re-score".equals(path)) {
                if(!method.equals("POST")){unsupported(resp,"POST");return;}
                if(body(req).size()!=0)throw new IllegalArgumentException("Chấm lại điểm chỉ nhận object rỗng");
                data=scoring.reScore(user.longValue());
            } else if("/leads".equals(path)) {
                if(!method.equals("GET")){unsupported(resp,"GET");return;}
                data=scoring.list(user.longValue(),integer(req.getParameter("page"),1),integer(req.getParameter("size"),100));
            } else if(path!=null && path.matches("/leads/[1-9][0-9]*(/profile)?")) {
                long id=Long.parseLong(path.split("/")[2]);
                if(path.endsWith("/profile")) {
                    if(!method.equals("PUT")){unsupported(resp,"PUT");return;}
                    data=scoring.updateProfile(user.longValue(),id,profile(body(req)));
                } else {
                    if(!method.equals("GET")){unsupported(resp,"GET");return;}
                    data=scoring.get(user.longValue(),id);
                }
            } else {ResponseUtil.json(resp,404,ApiResponse.error("Không tìm thấy API chấm điểm Lead",null));return;}
            ResponseUtil.json(resp,200,ApiResponse.success("Xử lý chấm điểm Lead thành công",data));
        } catch(SecurityException error) {ResponseUtil.json(resp,403,ApiResponse.error(error.getMessage(),null));}
        catch(NoSuchElementException error) {ResponseUtil.json(resp,404,ApiResponse.error(error.getMessage(),null));}
        catch(PayloadTooLarge error) {ResponseUtil.json(resp,413,ApiResponse.error(error.getMessage(),null));}
        catch(JsonParseException | IllegalArgumentException error) {ResponseUtil.json(resp,400,ApiResponse.error(error.getMessage(),null));}
        catch(IllegalStateException error) {ResponseUtil.json(resp,409,ApiResponse.error(error.getMessage(),null));}
        catch(SQLException error) {
            log("Lead scoring database failure",error);
            boolean conflict="40001".equals(error.getSQLState()) || "23000".equals(error.getSQLState()) || error.getErrorCode()==1205;
            ResponseUtil.json(resp,conflict?409:500,ApiResponse.error(conflict?"Giao dịch xung đột và đã rollback; hãy thử lại":"Không thể xử lý điểm Lead; kiểm tra DB và migration 024–025",null));
        } catch(Exception error) {log("Lead scoring failure",error);ResponseUtil.json(resp,500,ApiResponse.error("Không thể xử lý chấm điểm Lead",null));}
    }
    private static class PayloadTooLarge extends IllegalArgumentException {PayloadTooLarge(){super("JSON tối đa 262144 ký tự");}}
    private JsonObject body(HttpServletRequest req) throws IOException {
        if(req.getContentType()==null || !req.getContentType().split(";")[0].trim().equalsIgnoreCase("application/json"))
            throw new IllegalArgumentException("Yêu cầu application/json");
        req.setCharacterEncoding("UTF-8");char[] chars=new char[262145];int length=0,n;Reader reader=req.getReader();
        while(length<chars.length && (n=reader.read(chars,length,chars.length-length))!=-1)length+=n;
        if(length==chars.length)throw new PayloadTooLarge();
        var value=JsonParser.parseString(new String(chars,0,length));
        if(!value.isJsonObject())throw new IllegalArgumentException("Body phải là JSON object");return value.getAsJsonObject();
    }
    private ScoringConfig config(JsonObject json,boolean rulesOnly) {
        keys(json,rulesOnly?Set.of("version","rules"):Set.of("version","thresholds","aggregation","minScore","maxScore","rules"));
        long version=number(json.get("version"),"version",false);
        if(version<0)throw new IllegalArgumentException("version phải không âm");
        if(!json.has("rules") || !json.get("rules").isJsonArray())throw new IllegalArgumentException("rules phải là mảng");
        List<ScoringRule> rules=new ArrayList<>();
        for(var item:json.getAsJsonArray("rules")) {
            if(!item.isJsonObject())throw new IllegalArgumentException("Tiêu chí phải là object");var rule=item.getAsJsonObject();
            keys(rule,Set.of("id","group","name","field","operator","value","points","description","isActive"));
            if(!rule.has("isActive") || !rule.get("isActive").isJsonPrimitive() || !rule.getAsJsonPrimitive("isActive").isBoolean())
                throw new IllegalArgumentException("isActive phải là boolean");
            rules.add(new ScoringRule(string(rule,"id",false),string(rule,"group",false),string(rule,"name",false),string(rule,"field",false),
                    string(rule,"operator",false),string(rule,"value",false),intNumber(rule.get("points"),"points",false),string(rule,"description",true),rule.get("isActive").getAsBoolean()));
        }
        if(rulesOnly)return new ScoringConfig(version,null,null,null,null,null,rules);
        if(!json.has("thresholds") || !json.get("thresholds").isJsonObject())throw new IllegalArgumentException("thresholds phải là object");
        var thresholds=json.getAsJsonObject("thresholds");keys(thresholds,Set.of("hot","warm"));
        if(!json.has("minScore") || !json.has("maxScore"))throw new IllegalArgumentException("Khai báo rõ minScore/maxScore; null nghĩa là không giới hạn");
        return new ScoringConfig(version,intNumber(thresholds.get("hot"),"hot",false),intNumber(thresholds.get("warm"),"warm",false),
                string(json,"aggregation",false),intNumber(json.get("minScore"),"minScore",true),intNumber(json.get("maxScore"),"maxScore",true),rules);
    }
    private Map<String,Object> profile(JsonObject json) {
        var result=new LinkedHashMap<String,Object>();keys(json,Set.of("industry","employeeCount","title","recentActivity"));
        for(String key:json.keySet())result.put(key,key.equals("employeeCount")?intNumber(json.get(key),key,true):string(json,key,true));return result;
    }
    private void keys(JsonObject json,Set<String> allowed) {
        for(String key:json.keySet())if(!allowed.contains(key))throw new IllegalArgumentException("Trường không được hỗ trợ: "+key);
    }
    private String string(JsonObject json,String key,boolean nullable) {
        var value=json.get(key);if(nullable && (value==null || value.isJsonNull()))return null;
        if(value==null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())throw new IllegalArgumentException(key+" phải là chuỗi");
        return value.getAsString().trim();
    }
    private Long number(JsonElement value,String key,boolean nullable) {
        if(nullable && value!=null && value.isJsonNull())return null;
        if(value==null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber() || !value.getAsString().matches("-?(0|[1-9][0-9]*)"))
            throw new IllegalArgumentException(key+" phải là số nguyên");return Long.parseLong(value.getAsString());
    }
    private Integer intNumber(JsonElement value,String key,boolean nullable) {
        Long n=number(value,key,nullable);
        if(n!=null && (n<Integer.MIN_VALUE || n>Integer.MAX_VALUE))throw new IllegalArgumentException(key+" vượt giới hạn số nguyên");
        return n==null?null:n.intValue();
    }
    private int integer(String raw,int fallback) {return raw==null?fallback:Integer.parseInt(raw);}
    private void unsupported(HttpServletResponse resp,String allow) throws IOException {
        resp.setHeader("Allow",allow);ResponseUtil.json(resp,405,ApiResponse.error("Phương thức không được hỗ trợ",null));
    }
}
