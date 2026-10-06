package com.crm.util;

import com.crm.dto.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public final class ResponseUtil {

    private ResponseUtil() {
    }

    public static void json(
            HttpServletResponse response,
            int status,
            ApiResponse<?> body
    ) throws IOException {

        response.setStatus(status);
        response.setHeader("Cache-Control", "no-store");

        response.setCharacterEncoding(
                "UTF-8"
        );

        response.setContentType(
                "application/json"
        );

        response.getWriter().write(
                JsonUtil.getGson().toJson(
                        body
                )
        );
    }
}
