package com.crm.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class JsonUtil {

    private static final Gson GSON =
            new GsonBuilder()
                    .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                            src == null ? null : new JsonPrimitive(src.toString()))
                    .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                            json == null || json.isJsonNull() ? null : LocalDate.parse(json.getAsString()))
                    .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                            src == null ? null : new JsonPrimitive(src.toString()))
                    .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                            json == null || json.isJsonNull() ? null : LocalDateTime.parse(json.getAsString()))
                    .serializeNulls()
                    .create();

    private JsonUtil() {
    }

    public static Gson getGson() {
        return GSON;
    }
}