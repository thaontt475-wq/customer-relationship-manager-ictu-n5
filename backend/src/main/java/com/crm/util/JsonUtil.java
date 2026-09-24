package com.crm.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public final class JsonUtil {

    private static final Gson GSON =
            new GsonBuilder().create();

    private JsonUtil() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static void write(
            HttpServletResponse response,
            int status,
            Object body)
            throws IOException {

        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(
                "application/json;charset=UTF-8"
        );

        response
                .getWriter()
                .write(GSON.toJson(body));
    }
}