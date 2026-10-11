package com.crm.service.customers;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerHierarchyServiceTest {
    private Map<String,Object> node(long id,Long parent,int value) {
        Map<String,Object> row=new HashMap<>();row.put("id",id);row.put("parentCustomerId",parent);row.put("contractValue",BigDecimal.valueOf(value));return row;
    }
    @Test void sumsOwnAndDescendantSignedValuesWithoutCountingThemTwice() {
        var tree=CustomerHierarchyService.build(1,List.of(node(1,null,10),node(2,1L,20),node(3,2L,30)));
        assertEquals(BigDecimal.valueOf(60),tree.get("groupContractValue"));
        assertEquals(BigDecimal.TEN,tree.get("contractValue"));
        var child=(Map<?,?>)((List<?>)tree.get("children")).getFirst();
        assertEquals(BigDecimal.valueOf(50),child.get("groupContractValue"));
        assertEquals(1,child.get("depth"));
    }
    @Test void inaccessibleIntermediateCompanyIsNotExposedOrCounted() {
        var tree=CustomerHierarchyService.build(1,List.of(node(1,null,10),node(3,2L,30)));
        assertEquals(BigDecimal.valueOf(40),tree.get("groupContractValue"));
        var child=(Map<?,?>)((List<?>)tree.get("children")).getFirst();
        assertEquals(true,child.get("ancestorOmitted"));
        assertFalse(child.containsKey("parentCustomerId"));
    }
    @Test void rejectsMissingRootInsteadOfReturningAnEmptySuccessfulGroup() {
        assertThrows(NoSuchElementException.class,()->CustomerHierarchyService.build(1,List.of(node(3,2L,30))));
    }
}
