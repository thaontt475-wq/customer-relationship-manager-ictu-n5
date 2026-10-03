package com.crm.controller.search;

import com.crm.controller.ServerForms;
import com.crm.model.Product;
import com.crm.model.User;
import com.crm.service.categories.CategoryService;
import com.crm.service.organization.OrganizationService;
import com.crm.service.products.ProductService;
import com.crm.service.scope.DataScopeService;
import com.crm.service.scope.ScopeEntityType;
import com.crm.service.scope.ScopeRecord;
import com.crm.service.users.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-rendered Global Enterprise Search Servlet.
 * Connects header search to real scope-aware, RBAC-aware database records:
 * - Scoped entities: Customers, Opportunities, Activities, Quotes (respecting SELF/TEAM/ALL data scopes)
 * - Products & Catalog
 * - Organization units & Teams
 * - System Users (Admin/Director RBAC restricted)
 */
@WebServlet("/search")
public class GlobalSearchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(GlobalSearchServlet.class.getName());
    private static final String SEARCH_JSP = "/jsp/shared/search-results.jsp";

    private final DataScopeService dataScopeService;
    private final ProductService productService;
    private final OrganizationService organizationService;
    private final UserService userService;

    public GlobalSearchServlet() {
        this(new DataScopeService(), new ProductService(), new OrganizationService(), new UserService());
    }

    public GlobalSearchServlet(DataScopeService dataScopeService,
                               ProductService productService,
                               OrganizationService organizationService,
                               UserService userService) {
        this.dataScopeService = dataScopeService != null ? dataScopeService : new DataScopeService();
        this.productService = productService != null ? productService : new ProductService();
        this.organizationService = organizationService != null ? organizationService : new OrganizationService();
        this.userService = userService != null ? userService : new UserService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if (!ServerForms.authorize(request, response, false)) {
            return;
        }
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

        Long actorUserId = ServerForms.actor(request);
        if (actorUserId == null) {
            response.sendRedirect(request.getContextPath() + "/login?expired=1");
            return;
        }

        String query = request.getParameter("q");
        if (query == null) {
            query = "";
        }
        query = query.trim();

        request.setAttribute("query", query);

        List<SearchResultItem> customerResults = new ArrayList<>();
        List<SearchResultItem> opportunityResults = new ArrayList<>();
        List<SearchResultItem> quoteResults = new ArrayList<>();
        List<SearchResultItem> productResults = new ArrayList<>();
        List<SearchResultItem> orgResults = new ArrayList<>();
        List<SearchResultItem> userResults = new ArrayList<>();

        if (!query.isEmpty()) {
            try {
                // 1. Scoped Customers (RBAC & Scope filtered)
                List<ScopeRecord> customers = dataScopeService.list(actorUserId, ScopeEntityType.CUSTOMERS, query);
                for (ScopeRecord r : customers) {
                    customerResults.add(new SearchResultItem(
                            r.id(),
                            r.label(),
                            "Khách hàng",
                            request.getContextPath() + "/customers?id=" + r.id(),
                            "Chủ sở hữu ID: " + r.ownerUserId()
                    ));
                }

                // 2. Scoped Opportunities (RBAC & Scope filtered)
                List<ScopeRecord> opps = dataScopeService.list(actorUserId, ScopeEntityType.OPPORTUNITIES, query);
                for (ScopeRecord r : opps) {
                    opportunityResults.add(new SearchResultItem(
                            r.id(),
                            r.label(),
                            "Cơ hội bán hàng",
                            request.getContextPath() + "/opportunities?id=" + r.id(),
                            "Chủ sở hữu ID: " + r.ownerUserId()
                    ));
                }

                // 3. Scoped Quotes (RBAC & Scope filtered)
                List<ScopeRecord> quotes = dataScopeService.list(actorUserId, ScopeEntityType.QUOTES, query);
                for (ScopeRecord r : quotes) {
                    quoteResults.add(new SearchResultItem(
                            r.id(),
                            r.label(),
                            "Báo giá",
                            request.getContextPath() + "/quotes?id=" + r.id(),
                            "Chủ sở hữu ID: " + r.ownerUserId()
                    ));
                }

                // 4. Products (Respects Cost Price & Active Status)
                var productSearchResult = productService.searchProducts(query, null, null, 1, 10, ServerForms.roles(request));
                if (productSearchResult != null && productSearchResult.items() != null) {
                    for (Product p : productSearchResult.items()) {
                        String priceInfo = p.getListPrice() != null ? String.format("%,.0f đ", p.getListPrice()) : "Chưa có giá niêm yết";
                        productResults.add(new SearchResultItem(
                                p.getId(),
                                p.getName() + " (" + p.getCode() + ")",
                                "Sản phẩm",
                                request.getContextPath() + "/products/page?q=" + java.net.URLEncoder.encode(p.getCode(), StandardCharsets.UTF_8),
                                "Danh mục: " + (p.getCategory() != null ? p.getCategory() : "Khác") + " • Giá: " + priceInfo
                        ));
                    }
                }

                // 5. Organization Units & Teams
                var allUnits = organizationService.getUnits();
                String lowerQuery = query.toLowerCase(Locale.ROOT);
                for (var unit : allUnits) {
                    if ((unit.getName() != null && unit.getName().toLowerCase(Locale.ROOT).contains(lowerQuery))
                            || (unit.getManagerName() != null && unit.getManagerName().toLowerCase(Locale.ROOT).contains(lowerQuery))) {
                        orgResults.add(new SearchResultItem(
                                unit.getId(),
                                unit.getName(),
                                "Cơ cấu tổ chức",
                                request.getContextPath() + "/organization/page?selected=" + unit.getId(),
                                "Trưởng đơn vị: " + (unit.getManagerName() != null ? unit.getManagerName() : "Chưa phân bổ") + " • Khu vực: " + (unit.getRegion() != null ? unit.getRegion() : "Toàn quốc")
                        ));
                    }
                }

                // 6. Users (Admin / Director only)
                if (ServerForms.admin(request)) {
                    var allUsers = userService.findAll();
                    if (allUsers != null) {
                        for (User u : allUsers) {
                            if ((u.getFullName() != null && u.getFullName().toLowerCase(Locale.ROOT).contains(lowerQuery))
                                    || (u.getEmail() != null && u.getEmail().toLowerCase(Locale.ROOT).contains(lowerQuery))
                                    || (u.getUsername() != null && u.getUsername().toLowerCase(Locale.ROOT).contains(lowerQuery))) {
                                userResults.add(new SearchResultItem(
                                        u.getId(),
                                        u.getFullName() + " (" + u.getEmail() + ")",
                                        "Người dùng",
                                        request.getContextPath() + "/users/detail?id=" + u.getId(),
                                        "Trạng thái: " + u.getStatus() + " • Nhóm: " + (u.getTeamName() != null ? u.getTeamName() : "Chưa gán")
                                ));
                            }
                        }
                    }
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error performing global enterprise search", e);
                request.setAttribute("error", "Không thể hoàn tất tìm kiếm do lỗi kết nối cơ sở dữ liệu.");
            }
        }

        int totalCount = customerResults.size() + opportunityResults.size() + quoteResults.size()
                + productResults.size() + orgResults.size() + userResults.size();

        request.setAttribute("customerResults", customerResults);
        request.setAttribute("opportunityResults", opportunityResults);
        request.setAttribute("quoteResults", quoteResults);
        request.setAttribute("productResults", productResults);
        request.setAttribute("orgResults", orgResults);
        request.setAttribute("userResults", userResults);
        request.setAttribute("totalCount", totalCount);

        request.getRequestDispatcher(SEARCH_JSP).forward(request, response);
    }

    public record SearchResultItem(long id, String title, String category, String url, String details) {}
}
