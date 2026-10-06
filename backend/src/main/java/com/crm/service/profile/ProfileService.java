package com.crm.service.profile;

import com.crm.dao.profile.ProfileDAO;
import com.crm.service.audit.AuditLogService;
import com.crm.util.JsonUtil;

import java.util.Map;

public class ProfileService {

    private final ProfileDAO dao =
            new ProfileDAO();

    private final AuditLogService audit =
            new AuditLogService();


    public Map<String, Object> get(
            long userId
    ) throws Exception {

        return dao.findByUserId(
                userId
        );
    }


    public Map<String, Object> update(
            long userId,
            String fullName,
            String phone,
            String emailSignature
    ) throws Exception {

        if (
                fullName == null ||
                fullName.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Họ tên là bắt buộc"
            );
        }


        fullName =
                fullName.trim();


        if (fullName.length() > 150) {

            throw new IllegalArgumentException(
                    "Họ tên không được vượt quá 150 ký tự"
            );
        }


        if (
                phone != null &&
                !phone.isBlank() &&
                !phone.matches(
                        "^(0|\\+84)[0-9]{9,10}$"
                )
        ) {

            throw new IllegalArgumentException(
                    "Số điện thoại Việt Nam không hợp lệ"
            );
        }


        if (phone != null) {

            phone =
                    phone.trim();
        }


        if (emailSignature != null) {

            emailSignature =
                    emailSignature.trim();


            if (
                    emailSignature.length() >
                    2000
            ) {

                throw new IllegalArgumentException(
                        "Chữ ký email không được vượt quá 2000 ký tự"
                );
            }
        }


        Map<String, Object> before =
                dao.findByUserId(
                        userId
                );


        dao.update(
                userId,
                fullName,
                phone,
                emailSignature
        );


        Map<String, Object> after =
                dao.findByUserId(
                        userId
                );


        audit.log(
                userId,
                "USER",
                String.valueOf(
                        userId
                ),
                "UPDATE_PROFILE",
                "Cập nhật hồ sơ cá nhân",
                JsonUtil.getGson()
                        .toJson(before),
                JsonUtil.getGson()
                        .toJson(after)
        );


        return after;
    }
}