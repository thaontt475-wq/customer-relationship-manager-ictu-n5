package com.crm.service.customers;

import com.crm.dao.customers.CustomerCareDAO;
import com.crm.service.permissions.*;
import java.util.*;

public class CustomerCareService {
    private final CustomerService customers=new CustomerService();
    private final CustomerCareDAO dao=new CustomerCareDAO();
    private final DataScopeService scopes=new DataScopeService();
    public Map<String,Object> list(long user,int days,int page,int size) throws Exception{
        if(days<1 || days>3650 || page<1 || size<1 || size>100)throw new IllegalArgumentException("days/page/size không hợp lệ");
        return dao.due(scopes.resolve(user,"customer","read"),days,page,size);
    }
    public Map<String,Object> markContacted(long user,long customer,String note)throws Exception{
        if(note!=null && note.length()>2000)throw new IllegalArgumentException("Ghi chú quá dài");
        Map<String,Object> current=customers.getById(user,customer);
        if(current==null)return null;
        DataScopeContext update=scopes.resolve(user,"customer","update");
        if(!update.canAccessOwner(((Number)current.get("ownerUserId")).longValue()))throw new SecurityException("Không có quyền cập nhật khách hàng");
        scopes.resolve(user,"activity","create");
        long id=dao.markContacted(customer,user,note);
        return Map.of("customerId",customer,"activityId",id,"contacted",true);
    }
}
