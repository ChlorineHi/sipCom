package com.sipex.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GsonUtil {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static Gson getGson() {
        return new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
                String dateTimeStr = json.getAsString();
                // 处理多种可能的时间格式
                if (dateTimeStr.contains("T")) {
                    return LocalDateTime.parse(dateTimeStr.replace("Z", ""));
                } else {
                    return LocalDateTime.parse(dateTimeStr, FORMATTER);
                }
            })
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> 
                new JsonPrimitive(src.format(FORMATTER))
            )
            .create();
    }
}

