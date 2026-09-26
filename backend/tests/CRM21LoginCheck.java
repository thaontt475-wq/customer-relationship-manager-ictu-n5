import com.crm.controller.auth.LoginServlet;
import com.crm.dao.users.UserDAO;
import com.crm.model.Role;
import com.crm.model.User;
import com.crm.service.auth.AuthService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.*;
import org.mindrot.jbcrypt.BCrypt;
import java.lang.reflect.*;
import java.sql.SQLException;
import java.util.*;

/** Standalone regression checks: no additional test dependencies required. */
public class CRM21LoginCheck {
    static int checks;
    static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }
    static void inject(Object object, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
    static class FakeDAO extends UserDAO {
        User user;
        String email;
        boolean fail;
        public User findForLogin(String email) throws SQLException {
            this.email = email;
            if (fail) throw new SQLException("test database unavailable");
            return user;
        }
    }
    static class Servlet extends LoginServlet {
        void post(HttpServletRequest req, HttpServletResponse res) throws Exception { doPost(req, res); }
        void get(HttpServletRequest req, HttpServletResponse res) throws Exception { doGet(req, res); }
    }
    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    static class Exchange {
        Map<String,Object> attrs = new HashMap<>(), session = new HashMap<>();
        String email, password, forward, redirect;
        boolean invalidated, created;
        HttpSession old = proxy(HttpSession.class, (p,m,a) -> {
            if (m.getName().equals("invalidate")) invalidated = true;
            return null;
        });
        HttpSession fresh = proxy(HttpSession.class, (p,m,a) -> {
            if (m.getName().equals("setAttribute")) session.put((String)a[0], a[1]);
            return null;
        });
        HttpServletRequest req = proxy(HttpServletRequest.class, (p,m,a) -> {
            return switch (m.getName()) {
                case "getParameter" -> a[0].equals("email") ? email : password;
                case "setAttribute" -> { attrs.put((String)a[0], a[1]); yield null; }
                case "getContextPath" -> "/crm";
                case "getSession" -> {
                    if ((boolean)a[0]) { check(invalidated, "old session invalidated before creation"); created = true; yield fresh; }
                    yield old;
                }
                case "getRequestDispatcher" -> {
                    forward = (String)a[0];
                    yield proxy(RequestDispatcher.class, (x,y,z) -> null);
                }
                default -> null;
            };
        });
        HttpServletResponse res = proxy(HttpServletResponse.class, (p,m,a) -> {
            if (m.getName().equals("sendRedirect")) redirect = (String)a[0];
            return null;
        });
    }
    public static void main(String[] args) throws Exception {
        FakeDAO dao = new FakeDAO();
        AuthService service = new AuthService();
        inject(service, AuthService.class, "userDAO", dao);
        User user = new User();
        user.setId(42L); user.setActive(true); user.setStatus("ACTIVE");
        user.setDisplayName("Local tester");
        user.setRoles(List.of(new Role(1,"Admin"), new Role(2,"Sales Rep"), new Role(3,"Accountant")));
        // Synthetic test-only password; never written to database.
        String password = "Local-Only-21!";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(4));
        user.setPasswordHash(hash); dao.user = user;
        var result = service.login("  TEST@EXAMPLE.INVALID  ", password);
        check(result != null && result.userId() == 42L, "valid BCrypt login");
        check(dao.email.equals("test@example.invalid"), "email normalization");
        check(result.roles().equals(List.of("Admin","Sales Rep","Accountant")), "all role names");
        check(service.login(null,password) == null && service.login(" ",password) == null, "missing email");
        check(service.login("x",null) == null && service.login("x"," ") == null, "missing password");
        check(service.login("x","wrong") == null, "wrong password");
        check(service.login("x",password + " ") == null, "password is not trimmed");
        user.setActive(false); check(service.login("x",password) == null, "inactive denied");
        user.setActive(true); user.setStatus("LOCKED"); check(service.login("x",password) == null, "locked denied");
        user.setStatus("ACTIVE"); user.setPasswordHash("invalid");
        check(service.login("x",password) == null, "invalid hash denied"); user.setPasswordHash(hash);
        dao.user = null; check(service.login("x",password) == null, "unknown user denied"); dao.user = user;
        dao.fail = true;
        try { service.login("x",password); throw new AssertionError("database failure hidden"); }
        catch (SQLException expected) { checks++; }
        dao.fail = false;
        Servlet servlet = new Servlet(); inject(servlet,LoginServlet.class,"authService",service);
        Exchange get = new Exchange(); servlet.get(get.req,get.res);
        check("/jsp/auth/login.jsp".equals(get.forward), "GET forwards JSP");
        Exchange missing = new Exchange(); servlet.post(missing.req,missing.res);
        check(missing.attrs.containsKey("error") && !missing.created, "missing form fields");
        Exchange bad = new Exchange(); bad.email="x"; bad.password="wrong"; servlet.post(bad.req,bad.res);
        check("Email hoặc mật khẩu không đúng".equals(bad.attrs.get("error")), "generic failure");
        check("/jsp/auth/login.jsp".equals(bad.forward) && !bad.created, "failure forwards without session");
        Exchange good = new Exchange(); good.email="x"; good.password=password; servlet.post(good.req,good.res);
        check(good.created && good.session.get("userId") instanceof Long, "Long session userId");
        check(good.session.get("roles") instanceof Collection<?> roles && roles.stream().allMatch(String.class::isInstance), "Collection<String> session roles");
        check(((Map<?,?>)good.session.get("currentUser")).get("id").equals(42L), "compatible safe currentUser");
        check(!good.session.toString().contains(hash) && !good.session.toString().contains(password), "no credentials in session");
        check("/crm/html/index.html".equals(good.redirect), "existing context-relative redirect");
        System.out.println("PASS: " + checks + " CRM-21 regression checks");
    }
}
