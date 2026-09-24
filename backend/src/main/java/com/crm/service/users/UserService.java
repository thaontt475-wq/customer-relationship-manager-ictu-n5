package com.crm.service.users;

import com.crm.dao.users.UserDAO;
import com.crm.dto.users.UserRequest;
import com.crm.dto.users.UserResponse;
import com.crm.model.users.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.List;

public class UserService {

    private final UserDAO userDAO = new UserDAO();

    public UserResponse create(UserRequest request) throws SQLException {

        validateCreate(request);

        if (userDAO.existsByEmail(
                request.getEmail(),
                null
        )) {
            throw new IllegalArgumentException(
                    "Email đã tồn tại"
            );
        }

        User user = new User();

        user.setEmail(
                request.getEmail().trim()
        );

        user.setFullName(
                request.getFullName().trim()
        );

        user.setPhone(
                request.getPhone()
        );

        user.setTeamId(
                request.getTeamId()
        );

        user.setPasswordHash(
                BCrypt.hashpw(
                        request.getPassword(),
                        BCrypt.gensalt(12)
                )
        );

        long id =
                userDAO.create(user);

        return get(id);
    }

    public UserResponse get(long id) throws SQLException {

        User user =
                userDAO.findById(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Không tìm thấy User"
                                        )
                        );

        return UserResponse.from(user);
    }

    public List<UserResponse> search(
            String keyword,
            Boolean active,
            int page,
            int size
    ) throws SQLException {

        return userDAO
                .search(
                        keyword,
                        active,
                        page,
                        size
                )
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public int count(
            String keyword,
            Boolean active
    ) throws SQLException {

        return userDAO.count(
                keyword,
                active
        );
    }

    public UserResponse update(
            long id,
            UserRequest request
    ) throws SQLException {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request không hợp lệ"
            );
        }

        User user =
                userDAO.findById(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Không tìm thấy User"
                                        )
                        );

        if (request.getEmail() == null ||
                request.getEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "Email không được để trống"
            );
        }

        if (request.getFullName() == null ||
                request.getFullName().isBlank()) {

            throw new IllegalArgumentException(
                    "Họ tên không được để trống"
            );
        }

        if (userDAO.existsByEmail(
                request.getEmail(),
                id
        )) {

            throw new IllegalArgumentException(
                    "Email đã tồn tại"
            );
        }

        user.setEmail(
                request.getEmail().trim()
        );

        user.setFullName(
                request.getFullName().trim()
        );

        user.setPhone(
                request.getPhone()
        );

        user.setTeamId(
                request.getTeamId()
        );

        boolean updated =
                userDAO.update(user);

        if (!updated) {
            throw new IllegalStateException(
                    "Không thể cập nhật User"
            );
        }

        return get(id);
    }

    private void validateCreate(
            UserRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request không hợp lệ"
            );
        }

        if (request.getEmail() == null ||
                request.getEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "Email không được để trống"
            );
        }

        if (request.getFullName() == null ||
                request.getFullName().isBlank()) {

            throw new IllegalArgumentException(
                    "Họ tên không được để trống"
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().length() < 6) {

            throw new IllegalArgumentException(
                    "Mật khẩu tối thiểu 6 ký tự"
            );
        }
    }
}