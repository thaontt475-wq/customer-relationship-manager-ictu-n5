package com.crm.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class JsonUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Gson GSON =
            new GsonBuilder()
                    .serializeNulls()
                    .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                            src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.format(DATE_FORMATTER)))
                    .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> {
                        if (json == null || json.isJsonNull() || json.getAsString().isBlank()) return null;
                        return LocalDate.parse(json.getAsString().trim(), DATE_FORMATTER);
                    })
                    .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                            src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.format(DATE_TIME_FORMATTER)))
                    .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
                        if (json == null || json.isJsonNull() || json.getAsString().isBlank()) return null;
                        String s = json.getAsString().trim().replace("T", " ");
                        if (s.length() == 10) s += " 00:00:00";
                        return LocalDateTime.parse(s, DATE_TIME_FORMATTER);
                    })
                    .create();

    private JsonUtil() {
    }

    public static Gson getGson() {
        return GSON;
    }
}