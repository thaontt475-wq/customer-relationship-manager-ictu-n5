package com.crm.service.customers;

import com.crm.dao.customers.CustomerRelationDAO;
import com.crm.service.permissions.DataScopeContext;
import com.crm.service.permissions.DataScopeService;
import java.util.*;

public class CustomerRelationService {
    private final CustomerService customers=new CustomerService();
    private final CustomerRelationDAO dao=new CustomerRelationDAO();
    private final DataScopeService scopes=new DataScopeService();
    public Map<String,Object> setParent(long user,long child,Long parent) throws Exception {
        if(child<=0 || (parent!=null && parent<=0))throw new IllegalArgumentException("ID không hợp lệ");
        DataScopeContext update=scopes.resolve(user,"customer","update");
        Map<String,Object> current=customers.getById(user,child);
        if(current==null)return null;
        if(!update.canAccessOwner(((Number)current.get("ownerUserId")).longValue()))throw new SecurityException("Không có quyền sửa khách hàng");
        if(parent!=null && customers.getById(user,parent)==null)throw new IllegalArgumentException("Công ty mẹ không tồn tại");
        dao.setParent(child,parent);
        return customers.getById(user,child);
    }
    public Map<String,Object> children(long user,long id) throws Exception {
        if(customers.getById(user,id)==null)return null;
        DataScopeContext scope=scopes.resolve(user,"customer","read");
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("children",dao.children(id,scope));
        out.put("groupContractValue",dao.groupContractValue(id,scope));
        return out;
    }
}
