package com.crm.controller.customers;

import com.crm.dto.customers.CustomerWriteRequest;
import com.crm.service.customers.CustomerService;
import com.crm.dao.customers.CustomerDAO;
import com.crm.service.permissions.DataScopeService;
import com.crm.service.permissions.DataScopeContext;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerRegionServletTest {
    private CustomerWriteRequest received;
    private int request(String method, String json) throws Exception {
        int[] status={200}; var output=new StringWriter();
        var service=new CustomerService() {
            @Override public Map<String,Object> create(long user,CustomerWriteRequest body) { received=body; return Map.of("id",1); }
            @Override public Map<String,Object> update(long user,long id,CustomerWriteRequest body) { received=body; return Map.of("id",id); }
        };
        HttpSession session=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},(p,m,a)->10L);
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},
                (p,m,a)->switch(m.getName()) {
                    case "getSession"->session;
                    case "getPathInfo"->method.equals("PUT")?"/1":null;
                    case "getReader"->new BufferedReader(new StringReader(json));
                    default->null;
                });
        HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},
                (p,m,a)->{if(m.getName().equals("setStatus"))status[0]=(int)a[0];return m.getName().equals("getWriter")?new PrintWriter(output):null;});
        var servlet=new CustomerServlet(service);
        if(method.equals("POST"))servlet.doPost(req,resp);else servlet.doPut(req,resp);
        return status[0];
    }
    @Test void postAndPutTrackAbsentVersusExplicitNullRegion() throws Exception {
        assertEquals(201,request("POST","{\"name\":\"ABC\",\"region\":\"Hà Nội\"}"));
        assertTrue(received.hasRegion()); assertEquals("Hà Nội",received.getRegion());
        assertEquals(200,request("PUT","{\"name\":\"ABC\"}"));assertFalse(received.hasRegion());
        assertEquals(200,request("PUT","{\"name\":\"ABC\",\"region\":null}"));
        assertTrue(received.hasRegion());assertNull(received.getRegion());
    }
    @Test void rejectsNonStringRegionAndMalformedJson() throws Exception {
        assertEquals(400,request("POST","{\"name\":\"ABC\",\"region\":123}"));
        assertEquals(400,request("PUT","["));
    }
    @Test void serviceRejectsOverlongRegionAndKeepsExistingUpdatePermission() {
        var scopes=new DataScopeService() {
            @Override public DataScopeContext resolve(long user,String module,String action) {throw new SecurityException("No permission");}
        };
        var service=new CustomerService(new CustomerDAO(),scopes);
        var body=new CustomerWriteRequest();body.setName("ABC");body.setRegion("x".repeat(21));
        assertThrows(IllegalArgumentException.class,()->service.create(10,body));
        assertThrows(IllegalArgumentException.class,()->service.update(10,1,body));
        body.setRegion("Hà Nội");assertThrows(SecurityException.class,()->service.update(10,1,body));
        body.setRegion(null);assertThrows(SecurityException.class,()->service.update(10,1,body));
    }
}
