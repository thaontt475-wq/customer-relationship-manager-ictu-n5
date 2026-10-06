package com.crm.service.organization;

import com.crm.dao.organization.OrganizationDAO;
import com.crm.dto.organization.OrganizationUnitRequest;

import java.util.*;

public class OrganizationService {

    private final OrganizationDAO dao =
            new OrganizationDAO();


    public List<Map<String, Object>> getUnits(
            Long parentId,
            Boolean active
    ) throws Exception {

        return dao.findAll(
                parentId,
                active
        );
    }


    public Map<String, Object> create(
            OrganizationUnitRequest request
    ) throws Exception {

        validate(
                request,
                null
        );

        long id =
                dao.create(request);

        return dao.findById(id);
    }


    public Map<String, Object> update(
            long id,
            OrganizationUnitRequest request
    ) throws Exception {

        if (dao.findById(id) == null) {
            throw new IllegalArgumentException(
                    "Đơn vị không tồn tại"
            );
        }

        validate(
                request,
                id
        );

        if (
                request.getParentId() != null
                &&
                request.getParentId() == id
        ) {
            throw new IllegalArgumentException(
                    "Đơn vị không thể là cha của chính nó"
            );
        }

        if (
                request.getParentId() != null
                &&
                createsCycle(
                        id,
                        request.getParentId()
                )
        ) {
            throw new IllegalArgumentException(
                    "Không được tạo vòng lặp cây tổ chức"
            );
        }

        dao.update(
                id,
                request
        );

        return dao.findById(id);
    }


    private void validate(
            OrganizationUnitRequest request,
            Long excludeId
    ) throws Exception {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu không hợp lệ"
            );
        }

        if (
                request.getCode() == null
                ||
                request.getCode().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Mã đơn vị là bắt buộc"
            );
        }

        if (
                request.getName() == null
                ||
                request.getName().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Tên đơn vị là bắt buộc"
            );
        }

        if (
                dao.existsCode(
                        request.getCode().trim(),
                        excludeId
                )
        ) {
            throw new IllegalArgumentException(
                    "Mã đơn vị đã tồn tại"
            );
        }

        if (
                request.getParentId() != null
                &&
                dao.findById(
                        request.getParentId()
                ) == null
        ) {
            throw new IllegalArgumentException(
                    "Đơn vị cha không tồn tại"
            );
        }

        if (
                request.getManagerId() != null
                &&
                !dao.userExists(
                        request.getManagerId()
                )
        ) {
            throw new IllegalArgumentException(
                    "Người quản lý không tồn tại"
            );
        }
    }


    private boolean createsCycle(
            long unitId,
            long parentId
    ) throws Exception {

        Long current = parentId;

        Set<Long> visited =
                new HashSet<>();

        while (current != null) {

            if (current == unitId) {
                return true;
            }

            if (!visited.add(current)) {
                return true;
            }

            Map<String, Object> item =
                    dao.findById(current);

            if (item == null) {
                return false;
            }

            Object next =
                    item.get("parentId");

            current =
                    next == null
                            ? null
                            : ((Number) next)
                                .longValue();
        }

        return false;
    }
}