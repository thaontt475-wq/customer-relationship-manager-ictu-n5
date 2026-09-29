import com.crm.util.PasswordUtil;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Opt-in integration check. Use ONLY a disposable database initialized with schema.sql.
 * Args: base URL, JDBC URL, DB username, avatar directory. DB password: CRM36_TEST_DB_PASSWORD.
 * Creates synthetic crm36-test users; never point this at a shared or production database.
 */
public class CRM36AvatarHttpCheck {
    static String base;
    static int checks;
    static HttpClient client() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build(); }
    static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++; System.out.println("PASS " + label);
    }
    static HttpResponse<byte[]> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }
    static String token(HttpClient client) throws Exception {
        var response = get(client, "/api/users/me/avatar");
        check(response.statusCode() == 200, "metadata GET");
        return JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("data").get("csrfToken").getAsString();
    }
    static HttpResponse<byte[]> upload(HttpClient client, String token, byte[] bytes, String name, String path) throws Exception {
        String boundary = "CRM36" + UUID.randomUUID();
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"csrfToken\"\r\n\r\n" + token + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"avatar\"; filename=\"" + name + "\"\r\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(bytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofByteArray());
    }
    static byte[] image(String format, int w, int h) throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB),format,out); return out.toByteArray();
    }
    static void dimensions(HttpClient client, String path, int expected) throws Exception {
        var response = get(client,path);
        check(response.statusCode() == 200, path + " served");
        var image = ImageIO.read(new ByteArrayInputStream(response.body()));
        check(image != null && image.getWidth() == expected && image.getHeight() == expected, path + " dimensions");
    }
    public static void main(String[] args) throws Exception {
        base = args[0];
        String password = "AvatarTest-" + UUID.randomUUID();
        try (Connection conn = DriverManager.getConnection(args[1], args[2], System.getenv().getOrDefault("CRM36_TEST_DB_PASSWORD", ""))) {
            for (int i=1; i<=2; i++) try (var stmt = conn.prepareStatement("INSERT INTO users (username,email,password_hash,full_name) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash)")) {
                stmt.setString(1,"crm36-test-" + i); stmt.setString(2,"crm36-test-" + i + "@example.invalid");
                stmt.setString(3,PasswordUtil.hashPassword(password)); stmt.setString(4,"CRM36 Test"); stmt.executeUpdate();
            }
            HttpClient anonymous = client(), first = client(), second = client();
            check(get(anonymous,"/profile/avatar").statusCode() == 401,"anonymous view denied");
            check(upload(anonymous,"",image("png",10,10),"x.png","/api/users/me/avatar").statusCode() == 401,"anonymous upload denied");
            int i=0;
            for (HttpClient browser : new HttpClient[]{first,second}) {
                i++;
                String json = "email=crm36-test-" + i + "%40example.invalid&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
                var response = browser.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/login")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(json)).build(),HttpResponse.BodyHandlers.ofByteArray());
                check(response.statusCode() == 200,"login " + i);
            }
            String csrf = token(first);
            check(get(first,"/profile/avatar").statusCode() == 200,"JSP compiles before upload");
            byte[] png = image("png",600,300);
            check(upload(first,"wrong",png,"x.png","/api/users/me/avatar").statusCode() == 403,"CSRF denied");
            for (String format : new String[]{"jpg","png"}) for (int[] size : new int[][]{{600,300},{300,600},{300,300}}) {
                var response = upload(first,csrf,image(format,size[0],size[1]),"x."+format,"/api/users/me/avatar");
                check(response.statusCode() == 200,"upload " + format + " " + Arrays.toString(size));
                dimensions(first,"/profile/avatar/image",512); dimensions(first,"/profile/avatar/thumbnail",128);
            }
            check(upload(first,csrf,Arrays.copyOf(png,2097152),"limit.png","/api/users/me/avatar").statusCode() == 200,"exact 2 MiB accepted");
            check(upload(first,csrf,Arrays.copyOf(png,2097153),"large.png","/api/users/me/avatar").statusCode() == 413,"over 2 MiB rejected");
            for (String name : new String[]{"x.pdf","x.gif","x.exe","fake.jpg"})
                check(upload(first,csrf,"not an image".getBytes(),name,"/api/users/me/avatar").statusCode() == 400,"invalid " + name);
            check(upload(first,csrf,image("gif",20,20),"fake.jpg","/api/users/me/avatar").statusCode() == 400,"GIF renamed jpg rejected");
            check(upload(first,csrf,new byte[0],"empty.png","/api/users/me/avatar").statusCode() == 400,"empty rejected");
            check(upload(first,csrf,png,"square.png","/profile/avatar").statusCode() == 302,"JSP form redirects after success");
            check(get(first,"/profile/avatar?updated=1").statusCode() == 200,"JSP with previews compiles");
            check(get(second,"/profile/avatar/image").statusCode() == 404,"other user has no avatar");
            check(get(second,"/profile/avatar/image?userId=1").statusCode() == 404,"client cannot select another user");
            try (var stmt = conn.createStatement(); var rs = stmt.executeQuery("SELECT image_path,thumbnail_path FROM user_avatars")) {
                check(rs.next(),"database metadata exists");
                check(Files.exists(Path.of(args[3],rs.getString(1))) && Files.exists(Path.of(args[3],rs.getString(2))),"database paths point to files");
                check(!rs.next(),"one avatar record");
            }
            try (var files = Files.list(Path.of(args[3]))) { check(files.count() == 2,"old files cleaned after replacement"); }
            // Live DAO/schema failure must not delete the previous working pair.
            try (var stmt = conn.createStatement()) { stmt.execute("RENAME TABLE user_avatars TO user_avatars_test_backup"); }
            try { check(upload(first,csrf,png,"x.png","/api/users/me/avatar").statusCode() == 500,"SQL failure returns generic 500"); }
            finally { try (var stmt = conn.createStatement()) { stmt.execute("RENAME TABLE user_avatars_test_backup TO user_avatars"); } }
            dimensions(first,"/profile/avatar/image",512);
            try (var files = Files.list(Path.of(args[3]))) { check(files.count() == 2,"failed SQL upload cleaned up"); }
        }
        System.out.println("ALL " + checks + " HTTP/JDBC CHECKS PASSED");
    }
}
