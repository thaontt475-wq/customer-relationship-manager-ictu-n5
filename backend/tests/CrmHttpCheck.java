import com.crm.util.*;
import com.google.gson.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

class CrmHttpCheck {
    static final String BASE = "http://127.0.0.1:18080";
    static final String TAG = "audit-" + UUID.randomUUID();
    static final String PASSWORD = UUID.randomUUID() + "A1";
    static final List<Long> users = new ArrayList<>(), teams = new ArrayList<>();
    static final Map<String,List<Long>> records = new LinkedHashMap<>();
    static int checks;
    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        checks++; System.out.println("PASS " + name);
    }
    static long insert(Connection c, String sql, Object... values) throws Exception {
        try(var s = c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<values.length;i++) s.setObject(i+1,values[i]);
            s.executeUpdate(); try(var r=s.getGeneratedKeys()) { r.next(); return r.getLong(1); }
        }
    }
    static HttpClient client() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build(); }
    static HttpResponse<byte[]> get(HttpClient c,String path) throws Exception {
        return c.send(HttpRequest.newBuilder(URI.create(BASE+path)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    static HttpResponse<byte[]> post(HttpClient c,String path,String body,String csrf) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(BASE+path)).header("Content-Type","application/x-www-form-urlencoded");
        if(csrf!=null) b.header("X-CSRF-Token",csrf);
        return c.send(b.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    static String token(HttpClient c) throws Exception {
        return JsonParser.parseString(new String(get(c,"/api/auth/session").body(),StandardCharsets.UTF_8))
            .getAsJsonObject().getAsJsonObject("data").get("csrfToken").getAsString();
    }
    static void remove(Connection c,String table,String column,long id) throws Exception {
        try(var s=c.prepareStatement("DELETE FROM " + table + " WHERE " + column + "=?")) {s.setLong(1,id);s.executeUpdate();}
    }
    public static void main(String[] args) throws Exception {
        try(var c=DBConnection.getConnection()) {
            try {
                teams.add(insert(c,"INSERT INTO teams(name) VALUES (?)",TAG+"-team1"));
                teams.add(insert(c,"INSERT INTO teams(name) VALUES (?)",TAG+"-team2"));
                String[] scopes={"SELF","SELF","TEAM","ALL"};
                String[] roles={"Sales Rep","Sales Rep","Team Lead","Director"};
                var clients=new ArrayList<HttpClient>();
                for(int i=0;i<4;i++) {
                    String email=TAG+"-"+i+"@example.invalid";
                    long id=insert(c,"INSERT INTO users(username,email,password_hash,full_name,display_name,active,status,team_id,data_scope) VALUES (?,?,?,?,?,TRUE,'ACTIVE',?,?)",
                        TAG+"-"+i,email,PasswordUtil.hashPassword(PASSWORD),"Kiểm thử CRM","Kiểm thử CRM",teams.get(i==1?1:0),scopes[i]);
                    users.add(id);
                    try(var s=c.prepareStatement("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE name=?")) {
                        s.setLong(1,id);s.setString(2,roles[i]);s.executeUpdate();
                    }
                    var hc=client();clients.add(hc);
                    check(post(hc,"/api/auth/login","email="+URLEncoder.encode(email,StandardCharsets.UTF_8)+"&password="+PASSWORD,null).statusCode()==200,"HTTP login " + scopes[i]);
                }
                String[] tables={"customers","opportunities","activities","quotes"};
                for(String table:tables) {
                    String label=table.equals("activities")?"subject":table.equals("quotes")?"quote_number":"name";
                    var ids=new ArrayList<Long>(); records.put(table,ids);
                    for(int i=0;i<2;i++) ids.add(insert(c,"INSERT INTO "+table+"("+label+",owner_user_id) VALUES (?,?)",TAG+"-"+i,users.get(i)));
                    for(int i:new int[]{0,2,3}) {
                        var result=get(clients.get(i),"/api/"+table+"/export?q="+TAG);
                        check(result.statusCode()==200,scopes[i]+" "+table+" export HTTP 200");
                        try(var wb=new XSSFWorkbook(new ByteArrayInputStream(result.body()))) {
                            check(wb.getSheetAt(0).getPhysicalNumberOfRows()==(i==3?3:2),scopes[i]+" "+table+" scoped Excel row count");
                            if(i!=3) check(wb.getSheetAt(0).getRow(1).getCell(2).getStringCellValue().equals(users.get(0).toString()),scopes[i]+" export excludes B");
                        }
                    }
                    check(get(clients.get(0),"/api/"+table+"/"+ids.get(1)).statusCode()==403,"SELF cannot read B: "+table);
                    check(get(clients.get(2),"/api/"+table+"/"+ids.get(1)).statusCode()==403,"TEAM cannot read B: "+table);
                    check(get(clients.get(3),"/api/"+table+"/"+ids.get(1)).statusCode()==200,"ALL can read B: "+table);
                }
                var director=clients.get(3);
                for(String path:List.of("/dashboard","/products/page","/organization/page","/configuration/page","/customfields/page","/pipeline/page","/winloss","/audit","/profile","/profile/avatar","/permissions","/users","/customers","/activities","/quotes","/opportunities")) {
                    check(get(director,path).statusCode()==200,"JSP renders "+path);
                }
                var self=clients.get(0);
                check(post(self,"/api/products","name=test",null).statusCode()==403,"CSRF rejects missing token");
                check(post(self,"/api/products","name=test",token(self)).statusCode()==403,"API rejects Sales Rep product mutation with valid token");
                check(get(director,"/jsp/products/product-list.jsp").statusCode()==404,"Direct JSP denied");
                check(post(self,"/api/auth/logout","",token(self)).statusCode()==200,"Logout accepted with token");
                check(get(self,"/api/auth/session").statusCode()==401,"Logged-out session immediately invalid");
                System.out.println("HTTP CHECKS PASSED: "+checks);
            } finally {
                for(var entry:records.entrySet()) for(long id:entry.getValue()) remove(c,entry.getKey(),"id",id);
                for(long id:users) {remove(c,"user_roles","user_id",id);remove(c,"users","id",id);}
                for(long id:teams) remove(c,"teams","id",id);
                System.out.println("Removed only fixture IDs created by this run.");
            }
        }
    }
}
