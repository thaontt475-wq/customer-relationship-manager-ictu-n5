package com.crm.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class JsonUtil {

    private static final Gson GSON =
            new GsonBuilder()
                    .serializeNulls()
                    .create();

    private JsonUtil() {
    }

    public static Gson getGson() {
        return GSON;
    }
}