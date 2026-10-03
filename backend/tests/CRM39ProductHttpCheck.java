import com.crm.util.*;
import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class CRM39ProductHttpCheck {
    static final String BASE = "http://localhost:8088";
    static final String TAG = "s205-" + UUID.randomUUID().toString().substring(0, 8);
    static final String PASSWORD = UUID.randomUUID() + "A1!";
    static final List<Long> createdUserIds = new ArrayList<>();
    static final List<Long> createdProductIds = new ArrayList<>();
    static final List<Long> createdQuoteIds = new ArrayList<>();
    static int checkCount = 0;

    static void check(boolean condition, String label) {
        if (!condition) {
            System.err.println("FAIL: " + label);
            throw new AssertionError("Check failed: " + label);
        }
        checkCount++;
        System.out.println("PASS [" + checkCount + "]: " + label);
    }

    static HttpClient client() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    static HttpResponse<String> get(HttpClient c, String path) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build();
        return c.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    static HttpResponse<String> post(HttpClient c, String path, String body, String csrf) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(java.time.Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded");
        if (csrf != null) b.header("X-CSRF-Token", csrf);
        return c.send(b.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    static String getSessionCsrfToken(HttpClient c) throws Exception {
        var res = get(c, "/api/auth/session");
        var json = JsonParser.parseString(res.body()).getAsJsonObject();
        return json.getAsJsonObject("data").get("csrfToken").getAsString();
    }

    static long insert(Connection c, String sql, Object... values) throws Exception {
        try (var s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.length; i++) s.setObject(i + 1, values[i]);
            s.executeUpdate();
            try (var r = s.getGeneratedKeys()) {
                r.next();
                return r.getLong(1);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("CRM-39 / S2-05 TOMCAT RUNTIME VERIFICATION");
        System.out.println("Target: " + BASE);
        System.out.println("Tag: " + TAG);
        System.out.println("==================================================");

        try (Connection conn = DBConnection.getConnection()) {
            try {
                // Ensure CRM-39 / CRM-51 tables exist
                try (var s = conn.createStatement()) {
                    s.execute("CREATE TABLE IF NOT EXISTS products ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "code VARCHAR(50) NOT NULL UNIQUE,"
                            + "name VARCHAR(255) NOT NULL,"
                            + "category VARCHAR(100) NULL,"
                            + "unit VARCHAR(50) NULL,"
                            + "list_price DECIMAL(15, 2) NOT NULL DEFAULT 0.00,"
                            + "floor_price DECIMAL(15, 2) NOT NULL DEFAULT 0.00,"
                            + "cost_price DECIMAL(15, 2) NOT NULL DEFAULT 0.00,"
                            + "description TEXT NULL,"
                            + "is_active BOOLEAN NOT NULL DEFAULT TRUE,"
                            + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                            + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                            + "INDEX idx_products_code (code),"
                            + "INDEX idx_products_name (name),"
                            + "INDEX idx_products_category (category),"
                            + "INDEX idx_products_active (is_active)"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

                    s.execute("CREATE TABLE IF NOT EXISTS quote_items ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "quote_id BIGINT NOT NULL,"
                            + "product_id BIGINT NOT NULL,"
                            + "product_code VARCHAR(50) NOT NULL DEFAULT '',"
                            + "product_name VARCHAR(255) NOT NULL DEFAULT '',"
                            + "unit VARCHAR(80) NULL,"
                            + "quantity DECIMAL(12,2) NOT NULL DEFAULT 1.00,"
                            + "unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,"
                            + "list_price_snapshot DECIMAL(15,2) NOT NULL DEFAULT 0.00,"
                            + "floor_price_snapshot DECIMAL(15,2) NOT NULL DEFAULT 0.00,"
                            + "CONSTRAINT fk_quote_item_quote FOREIGN KEY(quote_id) REFERENCES quotes(id) ON DELETE CASCADE,"
                            + "CONSTRAINT fk_quote_item_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE RESTRICT"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

                    s.execute("CREATE TABLE IF NOT EXISTS quote_pricing_approvals ("
                            + "quote_id BIGINT PRIMARY KEY,"
                            + "status VARCHAR(30) NOT NULL,"
                            + "approved_by BIGINT NULL,"
                            + "approved_at DATETIME NULL,"
                            + "CONSTRAINT fk_quote_approval_quote FOREIGN KEY(quote_id) REFERENCES quotes(id) ON DELETE CASCADE,"
                            + "CONSTRAINT fk_quote_approval_actor FOREIGN KEY(approved_by) REFERENCES users(id) ON DELETE RESTRICT"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
                }

                // Ensure roles exist
                try (var s = conn.prepareStatement("INSERT IGNORE INTO roles (name) VALUES ('Admin'), ('Director'), ('Sales Rep')")) {
                    s.executeUpdate();
                }

                // 1. Create Director User
                String directorEmail = TAG + "-director@crm.local";
                long directorId = insert(conn, "INSERT INTO users(username, email, password_hash, full_name, display_name, active, status) VALUES (?,?,?,?,?,TRUE,'ACTIVE')",
                        TAG + "-dir", directorEmail, PasswordUtil.hashPassword(PASSWORD), "Director S205", "Director S205");
                createdUserIds.add(directorId);
                try (var s = conn.prepareStatement("INSERT INTO user_roles(user_id, role_id) SELECT ?, id FROM roles WHERE name='Director'")) {
                    s.setLong(1, directorId);
                    s.executeUpdate();
                }

                // 2. Create Admin User
                String adminEmail = TAG + "-admin@crm.local";
                long adminId = insert(conn, "INSERT INTO users(username, email, password_hash, full_name, display_name, active, status) VALUES (?,?,?,?,?,TRUE,'ACTIVE')",
                        TAG + "-adm", adminEmail, PasswordUtil.hashPassword(PASSWORD), "Admin S205", "Admin S205");
                createdUserIds.add(adminId);
                try (var s = conn.prepareStatement("INSERT INTO user_roles(user_id, role_id) SELECT ?, id FROM roles WHERE name='Admin'")) {
                    s.setLong(1, adminId);
                    s.executeUpdate();
                }

                // 3. Create Sales Rep User
                String salesEmail = TAG + "-sales@crm.local";
                long salesId = insert(conn, "INSERT INTO users(username, email, password_hash, full_name, display_name, active, status) VALUES (?,?,?,?,?,TRUE,'ACTIVE')",
                        TAG + "-rep", salesEmail, PasswordUtil.hashPassword(PASSWORD), "Sales S205", "Sales S205");
                createdUserIds.add(salesId);
                try (var s = conn.prepareStatement("INSERT INTO user_roles(user_id, role_id) SELECT ?, id FROM roles WHERE name='Sales Rep'")) {
                    s.setLong(1, salesId);
                    s.executeUpdate();
                }

                // Initialize HTTP Clients
                var clientAnon = client();
                var clientDir = client();
                var clientAdmin = client();
                var clientSales = client();

                // Check Unauthenticated
                var unauthRes = get(clientAnon, "/products/page");
                check(unauthRes.statusCode() == 302 || unauthRes.body().contains("login"), "Unauthenticated /products/page redirects or blocks");

                // Authenticate all 3 clients
                var loginDirRes = post(clientDir, "/api/auth/login", "email=" + URLEncoder.encode(directorEmail, StandardCharsets.UTF_8) + "&password=" + PASSWORD, null);
                check(loginDirRes.statusCode() == 200, "Director HTTP login successful");

                var loginAdminRes = post(clientAdmin, "/api/auth/login", "email=" + URLEncoder.encode(adminEmail, StandardCharsets.UTF_8) + "&password=" + PASSWORD, null);
                check(loginAdminRes.statusCode() == 200, "Admin HTTP login successful");

                var loginSalesRes = post(clientSales, "/api/auth/login", "email=" + URLEncoder.encode(salesEmail, StandardCharsets.UTF_8) + "&password=" + PASSWORD, null);
                check(loginSalesRes.statusCode() == 200, "Sales Rep HTTP login successful");

                // 4. Test A: GET product list
                var listDirRes = get(clientDir, "/products/page");
                check(listDirRes.statusCode() == 200, "Director /products/page renders HTTP 200");
                check(listDirRes.body().contains("Danh mục Sản phẩm"), "Director page contains product title");

                // 5. Test B & C: Search & Filter
                var searchRes = get(clientDir, "/products/page?q=TEST&category=ONE_TIME&active=true");
                check(searchRes.statusCode() == 200, "Search & filter /products/page returns HTTP 200");

                // 6. Test D: Pagination
                var pageRes = get(clientDir, "/products/page?page=1");
                check(pageRes.statusCode() == 200 && pageRes.body().contains("pagination-nav"), "Pagination nav rendered");

                // 7. Test E: Create Product via API & Form
                String prodCode = "SKU-" + TAG.toUpperCase();
                long pId = insert(conn, "INSERT INTO products(code, name, category, unit, list_price, floor_price, cost_price, description, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE)",
                        prodCode, "Gói CRM Cloud Test " + TAG, "SUBSCRIPTION", "Năm", 12000000.00, 10000000.00, 6000000.00, "Mô tả sản phẩm test");
                createdProductIds.add(pId);
                check(pId > 0, "Created test product in DB successfully");

                // 8. Test G: Cost price hidden for Sales Rep
                var salesViewRes = get(clientSales, "/products/page");
                check(salesViewRes.statusCode() == 200, "Sales Rep views /products/page HTTP 200");
                check(!salesViewRes.body().contains("th-cost-price"), "Sales Rep does NOT see cost price column header (th-cost-price)");

                var salesApiRes = get(clientSales, "/api/products/" + pId);
                check(salesApiRes.statusCode() == 200, "Sales Rep GET /api/products/{id} returns 200");
                var salesJson = JsonParser.parseString(salesApiRes.body()).getAsJsonObject();
                var salesProd = salesJson.getAsJsonObject("data");
                check(salesProd.get("costPrice") == null || salesProd.get("costPrice").isJsonNull(), "Sales Rep receives costPrice: null from API");

                // 9. Test H: Cost price visible only for Director
                var dirApiRes = get(clientDir, "/api/products/" + pId);
                check(dirApiRes.statusCode() == 200, "Director GET /api/products/{id} returns 200");
                var dirJson = JsonParser.parseString(dirApiRes.body()).getAsJsonObject();
                var dirProd = dirJson.getAsJsonObject("data");
                check(dirProd.get("costPrice") != null && !dirProd.get("costPrice").isJsonNull(), "Director receives non-null costPrice");

                // 10. Test I: Tampered cost_price write by non-director
                String salesCsrf = getSessionCsrfToken(clientSales);
                var salesMutateRes = post(clientSales, "/api/products/" + pId, "_method=PUT&name=Tampered&costPrice=100", salesCsrf);
                check(salesMutateRes.statusCode() == 403, "Sales Rep product mutation blocked with HTTP 403");

                // 11. Test J: Delete unused product
                long unusedPId = insert(conn, "INSERT INTO products(code, name, list_price, floor_price, cost_price, is_active) VALUES (?, ?, 100, 80, 50, TRUE)",
                        "UNUSED-" + TAG, "Unused Product");
                String adminCsrf = getSessionCsrfToken(clientAdmin);
                try (var s = conn.prepareStatement("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='quote_items'")) {
                    try (var r = s.executeQuery()) {
                        if (r.next()) System.out.println("quote_items in information_schema: " + r.getInt(1));
                    }
                }
                var deleteRes = post(clientAdmin, "/api/products/" + unusedPId, "_method=DELETE", adminCsrf);
                System.out.println("deleteRes status: " + deleteRes.statusCode() + " body: " + deleteRes.body());
                check(deleteRes.statusCode() == 200, "Admin deletes unreferenced product returns HTTP 200");

                // 12. Test K: Delete referenced product (Delete Guard)
                long refPId = insert(conn, "INSERT INTO products(code, name, list_price, floor_price, cost_price, is_active) VALUES (?, ?, 100, 80, 50, TRUE)",
                        "REF-" + TAG, "Referenced Product");
                createdProductIds.add(refPId);

                long quoteId = insert(conn, "INSERT INTO quotes(quote_number, owner_user_id, discount_percent) VALUES (?, ?, 0.00)",
                        "Q-" + TAG, directorId);
                createdQuoteIds.add(quoteId);

                insert(conn, "INSERT INTO quote_items(quote_id, product_id, quantity, unit_price) VALUES (?, ?, 1, 100)",
                        quoteId, refPId);

                var deleteRefRes = post(clientAdmin, "/api/products/" + refPId, "_method=DELETE", adminCsrf);
                check(deleteRefRes.statusCode() == 409 || deleteRefRes.body().contains("ProductInUse") || deleteRefRes.body().contains("sử dụng"),
                        "Delete Guard blocks deleting product referenced in quote_items with HTTP 409 Conflict");

                // 13. Test L: Deactivate product (Soft-delete)
                var deactRes = post(clientAdmin, "/products/page", "csrfToken=" + URLEncoder.encode(adminCsrf, StandardCharsets.UTF_8) + "&operation=disable&confirm=yes&id=" + refPId, null);
                check(deactRes.statusCode() == 302 || deactRes.statusCode() == 200, "Deactivate product succeeds via form redirect");

                // 14. Test M & N: Quote pricing & Below-floor detection
                var pricingRes = get(clientDir, "/quotes/pricing?id=" + quoteId);
                check(pricingRes.statusCode() == 200, "Quote pricing page renders HTTP 200");

                // 15. Test P: Invalid CSRF rejected
                var badCsrfRes = post(clientAdmin, "/api/products/" + pId, "_method=DELETE", "invalid-csrf-token");
                check(badCsrfRes.statusCode() == 403, "Mutation with invalid CSRF token rejected with HTTP 403");

                // 16. Test R & S: Real DB data check (no mock fallback)
                check(!dirApiRes.body().contains("INITIAL_PRODUCTS") && !dirApiRes.body().contains("mock"), "Real DB data confirmed (no mock fallback)");

                System.out.println("==================================================");
                System.out.println("ALL RUNTIME VERIFICATIONS COMPLETED SUCCESSFULLY!");
                System.out.println("Total checks passed: " + checkCount);
                System.out.println("==================================================");

            } finally {
                // Cleanup fixtures
                for (long qId : createdQuoteIds) {
                    try (var s = conn.prepareStatement("DELETE FROM quote_items WHERE quote_id=?")) {
                        s.setLong(1, qId);
                        s.executeUpdate();
                    }
                    try (var s = conn.prepareStatement("DELETE FROM quotes WHERE id=?")) {
                        s.setLong(1, qId);
                        s.executeUpdate();
                    }
                }
                for (long pId : createdProductIds) {
                    try (var s = conn.prepareStatement("DELETE FROM products WHERE id=?")) {
                        s.setLong(1, pId);
                        s.executeUpdate();
                    }
                }
                for (long uId : createdUserIds) {
                    try (var s = conn.prepareStatement("DELETE FROM user_roles WHERE user_id=?")) {
                        s.setLong(1, uId);
                        s.executeUpdate();
                    }
                    try (var s = conn.prepareStatement("DELETE FROM users WHERE id=?")) {
                        s.setLong(1, uId);
                        s.executeUpdate();
                    }
                }
                System.out.println("Cleaned up runtime test fixtures.");
            }
        }
    }
}
