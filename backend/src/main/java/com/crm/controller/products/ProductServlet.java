package com.crm.controller.products;

import com.crm.dao.products.ProductDAO;
import com.crm.dto.common.ApiResponse;
import com.crm.service.permissions.PermissionService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet({
        "/api/products",
        "/api/products/*"
})
public class ProductServlet
        extends HttpServlet {

    private final ProductDAO dao =
            new ProductDAO();

    private final PermissionService permissions =
            new PermissionService();


    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        try {

            long currentUserId =
                    currentUser(req);

            require(
                    currentUserId,
                    "product.read"
            );

            Boolean active =
                    parseBoolean(
                            req.getParameter(
                                    "active"
                            )
                    );

            List<Map<String,Object>> items =
                    dao.findAll(
                            req.getParameter(
                                    "keyword"
                            ),
                            active
                    );

            boolean canViewCost =
                    canViewCostPrice(
                            currentUserId
                    );

            if (!canViewCost) {

                for (
                        Map<String,Object> item :
                        items
                ) {

                    item.remove(
                            "costPrice"
                    );
                }
            }

            Map<String,Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "items",
                    items
            );

            data.put(
                    "page",
                    1
            );

            data.put(
                    "size",
                    items.size()
            );

            data.put(
                    "totalItems",
                    items.size()
            );

            data.put(
                    "totalPages",
                    items.isEmpty()
                            ? 0
                            : 1
            );

            ResponseUtil.json(
                    res,
                    200,
                    ApiResponse.success(
                            "Lấy danh sách sản phẩm thành công",
                            data
                    )
            );

        } catch (SecurityException e) {

            forbidden(res);

        } catch (IllegalArgumentException e) {

            badRequest(
                    res,
                    e.getMessage()
            );

        } catch (Exception e) {

            error(
                    res,
                    e
            );
        }
    }


    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        try {

            long currentUserId =
                    currentUser(req);

            require(
                    currentUserId,
                    "product.create"
            );

            JsonObject body =
                    JsonUtil.getGson()
                            .fromJson(
                                    req.getReader(),
                                    JsonObject.class
                            );

            if (body == null) {

                throw new IllegalArgumentException(
                        "Dữ liệu không hợp lệ"
                );
            }

            String code =
                    stringValue(
                            body,
                            "code",
                            true
                    );

            String name =
                    stringValue(
                            body,
                            "name",
                            true
                    );

            String type =
                    stringValue(
                            body,
                            "type",
                            false
                    );

            String unit =
                    stringValue(
                            body,
                            "unit",
                            false
                    );

            BigDecimal listPrice =
                    decimalValue(
                            body,
                            "listPrice"
                    );

            BigDecimal floorPrice =
                    decimalValue(
                            body,
                            "floorPrice"
                    );

            BigDecimal costPrice =
                    decimalValue(
                            body,
                            "costPrice"
                    );

            boolean active =
                    !body.has("active") ||
                    body.get("active")
                            .getAsBoolean();

            validatePrices(
                    listPrice,
                    floorPrice,
                    costPrice
            );

            long id =
                    dao.create(
                            code,
                            name,
                            type,
                            unit,
                            listPrice,
                            floorPrice,
                            costPrice,
                            active
                    );

            Map<String,Object> product =
                    dao.findById(id);

            if (
                    !canViewCostPrice(
                            currentUserId
                    )
            ) {

                product.remove(
                        "costPrice"
                );
            }

            ResponseUtil.json(
                    res,
                    201,
                    ApiResponse.success(
                            "Tạo sản phẩm thành công",
                            product
                    )
            );

        } catch (
                SQLIntegrityConstraintViolationException e
        ) {

            ResponseUtil.json(
                    res,
                    409,
                    ApiResponse.error(
                            "Mã sản phẩm đã tồn tại",
                            null
                    )
            );

        } catch (SecurityException e) {

            forbidden(res);

        } catch (IllegalArgumentException e) {

            badRequest(
                    res,
                    e.getMessage()
            );

        } catch (Exception e) {

            error(
                    res,
                    e
            );
        }
    }


    @Override
    protected void doPut(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        try {

            long currentUserId =
                    currentUser(req);

            require(
                    currentUserId,
                    "product.update"
            );

            long id =
                    id(req);

            if (
                    dao.findById(id) ==
                    null
            ) {

                notFound(res);
                return;
            }

            JsonObject body =
                    JsonUtil.getGson()
                            .fromJson(
                                    req.getReader(),
                                    JsonObject.class
                            );

            if (body == null) {

                throw new IllegalArgumentException(
                        "Dữ liệu không hợp lệ"
                );
            }

            String code =
                    stringValue(
                            body,
                            "code",
                            true
                    );

            String name =
                    stringValue(
                            body,
                            "name",
                            true
                    );

            String type =
                    stringValue(
                            body,
                            "type",
                            false
                    );

            String unit =
                    stringValue(
                            body,
                            "unit",
                            false
                    );

            BigDecimal listPrice =
                    decimalValue(
                            body,
                            "listPrice"
                    );

            BigDecimal floorPrice =
                    decimalValue(
                            body,
                            "floorPrice"
                    );

            BigDecimal costPrice =
                    decimalValue(
                            body,
                            "costPrice"
                    );

            boolean active =
                    !body.has("active") ||
                    body.get("active")
                            .getAsBoolean();

            validatePrices(
                    listPrice,
                    floorPrice,
                    costPrice
            );

            dao.update(
                    id,
                    code,
                    name,
                    type,
                    unit,
                    listPrice,
                    floorPrice,
                    costPrice,
                    active
            );

            Map<String,Object> product =
                    dao.findById(id);

            if (
                    !canViewCostPrice(
                            currentUserId
                    )
            ) {

                product.remove(
                        "costPrice"
                );
            }

            ResponseUtil.json(
                    res,
                    200,
                    ApiResponse.success(
                            "Cập nhật sản phẩm thành công",
                            product
                    )
            );

        } catch (
                SQLIntegrityConstraintViolationException e
        ) {

            ResponseUtil.json(
                    res,
                    409,
                    ApiResponse.error(
                            "Mã sản phẩm đã tồn tại",
                            null
                    )
            );

        } catch (SecurityException e) {

            forbidden(res);

        } catch (IllegalArgumentException e) {

            badRequest(
                    res,
                    e.getMessage()
            );

        } catch (Exception e) {

            error(
                    res,
                    e
            );
        }
    }


    @Override
    protected void doDelete(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        try {

            long currentUserId =
                    currentUser(req);

            require(
                    currentUserId,
                    "product.delete"
            );

            long id =
                    id(req);

            if (
                    dao.findById(id) ==
                    null
            ) {

                notFound(res);
                return;
            }

            /*
             * Theo acceptance:
             * sản phẩm đã tham chiếu không được xóa.
             * Với sản phẩm chưa tham chiếu cũng ưu tiên
             * discontinue thay vì hard delete.
             */

            dao.deactivate(id);

            ResponseUtil.json(
                    res,
                    200,
                    ApiResponse.success(
                            dao.isReferenced(id)
                                    ? "Sản phẩm đang được sử dụng nên đã chuyển sang ngừng kinh doanh"
                                    : "Đã ngừng kinh doanh sản phẩm",
                            null
                    )
            );

        } catch (SecurityException e) {

            forbidden(res);

        } catch (IllegalArgumentException e) {

            badRequest(
                    res,
                    e.getMessage()
            );

        } catch (Exception e) {

            error(
                    res,
                    e
            );
        }
    }


    private long currentUser(
            HttpServletRequest req
    ) {

        return ((Number) req
                .getSession(false)
                .getAttribute(
                        "userId"
                ))
                .longValue();
    }


    private long id(
            HttpServletRequest req
    ) {

        String path =
                req.getPathInfo();

        if (
                path == null ||
                path.length() <= 1
        ) {

            throw new IllegalArgumentException(
                    "Thiếu product id"
            );
        }

        try {

            return Long.parseLong(
                    path.substring(1)
            );

        } catch (
                NumberFormatException e
        ) {

            throw new IllegalArgumentException(
                    "Product id không hợp lệ"
            );
        }
    }


    private String stringValue(
            JsonObject body,
            String name,
            boolean required
    ) {

        if (
                !body.has(name) ||
                body.get(name).isJsonNull()
        ) {

            if (required) {

                throw new IllegalArgumentException(
                        name +
                        " là bắt buộc"
                );
            }

            return null;
        }

        String value =
                body.get(name)
                        .getAsString()
                        .trim();

        if (
                required &&
                value.isBlank()
        ) {

            throw new IllegalArgumentException(
                    name +
                    " là bắt buộc"
            );
        }

        return value.isBlank()
                ? null
                : value;
    }


    private BigDecimal decimalValue(
            JsonObject body,
            String name
    ) {

        if (
                !body.has(name) ||
                body.get(name).isJsonNull()
        ) {

            return BigDecimal.ZERO;
        }

        try {

            return body.get(name)
                    .getAsBigDecimal();

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    name +
                    " không hợp lệ"
            );
        }
    }


    private void validatePrices(
            BigDecimal listPrice,
            BigDecimal floorPrice,
            BigDecimal costPrice
    ) {

        if (
                listPrice.signum() < 0 ||
                floorPrice.signum() < 0 ||
                costPrice.signum() < 0
        ) {

            throw new IllegalArgumentException(
                    "Giá không được âm"
            );
        }

        if (
                floorPrice.compareTo(
                        listPrice
                ) > 0
        ) {

            throw new IllegalArgumentException(
                    "Giá sàn không được lớn hơn giá niêm yết"
            );
        }
    }


    private boolean canViewCostPrice(
            long userId
    ) throws Exception {

        /*
         * Nếu project chưa có product.cost.read riêng
         * thì admin/product.update được xem cost.
         */

        return permissions.hasPermission(
                userId,
                "product.cost.read"
        )
        ||
        permissions.hasPermission(
                userId,
                "product.update"
        );
    }


    private Boolean parseBoolean(
            String value
    ) {

        if (
                value == null ||
                value.isBlank()
        ) {

            return null;
        }

        if (
                value.equalsIgnoreCase(
                        "true"
                )
        ) {
            return true;
        }

        if (
                value.equalsIgnoreCase(
                        "false"
                )
        ) {
            return false;
        }

        throw new IllegalArgumentException(
                "active không hợp lệ"
        );
    }


    private void require(
            long userId,
            String permission
    ) throws Exception {

        if (
                !permissions.hasPermission(
                        userId,
                        permission
                )
        ) {

            throw new SecurityException();
        }
    }


    private void forbidden(
            HttpServletResponse res
    ) throws IOException {

        ResponseUtil.json(
                res,
                403,
                ApiResponse.error(
                        "Không có quyền",
                        null
                )
        );
    }


    private void badRequest(
            HttpServletResponse res,
            String message
    ) throws IOException {

        ResponseUtil.json(
                res,
                400,
                ApiResponse.error(
                        message,
                        null
                )
        );
    }


    private void notFound(
            HttpServletResponse res
    ) throws IOException {

        ResponseUtil.json(
                res,
                404,
                ApiResponse.error(
                        "Không tìm thấy sản phẩm",
                        null
                )
        );
    }


    private void error(
            HttpServletResponse res,
            Exception e
    ) throws IOException {

        e.printStackTrace();

        ResponseUtil.json(
                res,
                500,
                ApiResponse.error(
                        "Lỗi hệ thống",
                        null
                )
        );
    }
}