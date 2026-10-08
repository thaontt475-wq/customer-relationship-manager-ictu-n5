package com.crm.contacts;

import com.crm.controller.contacts.ContactServlet;
import com.crm.dto.contacts.ContactWriteRequest;
import com.crm.service.contacts.ContactService;
import com.crm.util.JsonUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ContactValidationTest {
    private ContactWriteRequest valid() {
        var r=new ContactWriteRequest();
        r.fullName=" Nguyễn Văn Thắng ";r.title="Backend";r.email="thang@example.invalid";
        r.phone="+84 123 456 789";r.buyingRole="DECISION_MAKER";
        return r;
    }
    @Test void acceptsAllFourRolesAndTrimsNames() {
        for(String role:List.of("DECISION_MAKER","INFLUENCER","END_USER","BLOCKER")) {
            var r=valid();r.buyingRole=role;ContactService.validate(r);
            assertEquals("Nguyễn Văn Thắng",r.fullName);
        }
    }
    @Test void rejectsMissingMalformedAndOversizedFields() {
        assertThrows(IllegalArgumentException.class,()->ContactService.validate(null));
        for(String field:List.of("fullName","title","email","phone","buyingRole")) {
            var r=valid();
            try {ContactWriteRequest.class.getField(field).set(r," ");} catch(Exception e){throw new AssertionError(e);}
            assertThrows(IllegalArgumentException.class,()->ContactService.validate(r),field);
        }
        var email=valid();email.email="invalid@";
        assertThrows(IllegalArgumentException.class,()->ContactService.validate(email));
        var phone=valid();phone.phone="abcdef";
        assertThrows(IllegalArgumentException.class,()->ContactService.validate(phone));
        var name=valid();name.fullName="a".repeat(201);
        assertThrows(IllegalArgumentException.class,()->ContactService.validate(name));
        var role=valid();role.buyingRole="ADMIN";
        assertThrows(IllegalArgumentException.class,()->ContactService.validate(role));
    }
    @Test void rejectsInvalidCustomerIds() {
        assertThrows(IllegalArgumentException.class,()->ContactService.positive(null));
        assertThrows(IllegalArgumentException.class,()->ContactService.positive(0L));
        assertThrows(IllegalArgumentException.class,()->ContactService.positive(-1L));
    }
    private record Response(int status,String body) {}
    private Response request(String method,String path,boolean loggedIn) throws Exception {
        var body=new StringWriter();int[] status={0};
        HttpSession session=(HttpSession)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpSession.class},
            (p,m,a)->m.getName().equals("getAttribute")?1L:null);
        HttpServletRequest req=(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},
            (p,m,a)->switch(m.getName()) {
                case "getSession" -> loggedIn?session:null;
                case "getPathInfo" -> path;
                case "getMethod" -> method;
                default -> null;
            });
        HttpServletResponse resp=(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},
            (p,m,a)->{if(m.getName().equals("setStatus")) status[0]=(int)a[0];if(m.getName().equals("getWriter")) return new PrintWriter(body);return null;});
        new ContactServlet().service((ServletRequest)req,(ServletResponse)resp);
        return new Response(status[0],body.toString());
    }
    @Test void returnsJsonForMissingLoginBadRouteAndWrongMethod() throws Exception {
        var unauth=request("GET",null,false);
        assertEquals(401,unauth.status);
        assertFalse(JsonUtil.getGson().fromJson(unauth.body,com.google.gson.JsonObject.class).get("success").getAsBoolean());
        assertEquals(404,request("GET","/1/unsupported",true).status);
        assertEquals(405,request("POST","/1/history",true).status);
        assertEquals(405,request("DELETE",null,true).status);
        assertEquals(400,request("GET","/99999999999999999999999999999",true).status);
    }
}
