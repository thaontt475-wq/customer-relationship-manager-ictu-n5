package com.crm.service.customers;

import com.crm.dao.customers.CustomerRelationDAO;
import com.crm.service.permissions.DataScopeService;
import java.math.BigDecimal;
import java.util.*;

/** Loads the permitted company group in one query, including real signed values. */
public class CustomerHierarchyService {
    public Map<String,Object> get(long user, long root) throws Exception {
        if(root<=0) throw new IllegalArgumentException("Mã khách hàng không hợp lệ");
        var scope=new DataScopeService().resolve(user,"customer","read");
        var rows=new CustomerRelationDAO().hierarchy(root,scope);
        if(rows.stream().noneMatch(r->((Number)r.get("id")).longValue()==root)) {
            if(new CustomerService().getById(user,root)==null) return null;
            throw new SecurityException("Công ty nằm ngoài phạm vi truy cập");
        }
        return build(root,rows);
    }
    public static Map<String,Object> build(long root, List<Map<String,Object>> rows) {
        Map<Long,Map<String,Object>> nodes=new LinkedHashMap<>();
        for(var row:rows) {
            var node=new LinkedHashMap<>(row);
            node.put("children",new ArrayList<Map<String,Object>>());
            nodes.put(((Number)row.get("id")).longValue(),node);
        }
        var rootNode=nodes.get(root);
        if(rootNode==null) throw new NoSuchElementException("Không tìm thấy công ty gốc");
        for(var node:nodes.values()) {
            long id=((Number)node.get("id")).longValue();
            if(id==root)continue;
            Object parentId=node.get("parentCustomerId");
            var parent=parentId instanceof Number n ? nodes.get(n.longValue()) : null;
            // Preserve visible descendants without exposing an inaccessible intermediate company.
            if(parent==null) {parent=rootNode;node.put("ancestorOmitted",true);node.remove("parentCustomerId");}
            children(parent).add(node);
        }
        total(rootNode,0,new HashSet<>());
        return rootNode;
    }
    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> children(Map<String,Object> node) {
        return (List<Map<String,Object>>)node.get("children");
    }
    private static BigDecimal total(Map<String,Object> node,int depth,Set<Long> visited) {
        long id=((Number)node.get("id")).longValue();
        if(!visited.add(id)) throw new IllegalArgumentException("Cấu trúc công ty có vòng lặp");
        node.put("depth",depth);node.put("childrenCount",children(node).size());
        BigDecimal value=(BigDecimal)node.get("contractValue");
        for(var child:children(node))value=value.add(total(child,depth+1,visited));
        node.put("groupContractValue",value);
        return value;
    }
}
