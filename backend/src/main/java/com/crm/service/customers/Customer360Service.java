package com.crm.service.customers;

import com.crm.dao.customers.Customer360DAO;
import com.crm.service.permissions.DataScopeService;
import com.crm.service.permissions.DataScopeContext;
import java.math.BigDecimal;
import java.util.*;

/** Never returns an incomplete/mock Customer 360 response. */
public class Customer360Service {
    private final CustomerService customers=new CustomerService();
    private final Customer360DAO dao=new Customer360DAO();
    private final DataScopeService scopes=new DataScopeService();
    private final com.crm.service.support.SupportRequestService support=new com.crm.service.support.SupportRequestService();

    public Map<String,Object> getCustomer360(long userId,long customerId) throws Exception {
        if(customerId<=0) throw new IllegalArgumentException("Customer ID không hợp lệ");
        Map<String,Object> customer=customers.getById(userId,customerId);
        if(customer==null) return null;
        if(!new com.crm.service.permissions.PermissionService().hasPermission(userId,"contact.read"))
            throw new SecurityException("Bạn không có quyền xem người liên hệ trong hồ sơ 360");
        DataScopeContext opportunityScope=scopes.resolve(userId,"opportunity","read");
        DataScopeContext activityScope=scopes.resolve(userId,"activity","read");
        List<Map<String,Object>> all=dao.opportunities(customerId,opportunityScope);
        List<Map<String,Object>> open=new ArrayList<>(), closed=new ArrayList<>();
        BigDecimal openValue=BigDecimal.ZERO;
        BigDecimal wonValue=BigDecimal.ZERO;
        for(Map<String,Object> o:all){
            String status=String.valueOf(o.get("status"));
            if("OPEN".equalsIgnoreCase(status)){
                open.add(o);
                Object amount=o.get("amount");
                if(amount instanceof BigDecimal b) openValue=openValue.add(b);
                else if(amount instanceof Number n) openValue=openValue.add(BigDecimal.valueOf(n.doubleValue()));
            }else {
                closed.add(o);
                if("WON".equalsIgnoreCase(status)){
                    Object amount=o.get("amount");
                    if(amount instanceof BigDecimal b) wonValue=wonValue.add(b);
                    else if(amount instanceof Number n) wonValue=wonValue.add(BigDecimal.valueOf(n.doubleValue()));
                }
            }
        }
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("customer",customer);
        out.put("contacts",dao.contacts(customerId));
        out.put("openOpportunities",open);
        out.put("closedOpportunities",closed);
        out.put("activities",dao.activities(customerId,activityScope));
        out.put("attachments",dao.attachments(customerId));
        BigDecimal contractValue = dao.signedValue(customerId);
        if (contractValue == null || contractValue.compareTo(BigDecimal.ZERO) == 0) {
            contractValue = wonValue;
        }
        out.put("totalContractValue", contractValue);
        out.put("openOpportunityValue", openValue);
        out.put("churnRisk",support.risk(userId,customerId));
        return out;
    }
}
