package com.sipex.client.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sipex.client.config.ClientConfig;
import com.sipex.client.util.GsonUtil;
import com.sipex.common.dto.ApiResponse;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpClientService {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final Gson gson = GsonUtil.getGson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static <T> ApiResponse<T> post(String endpoint, Object body, Type responseType) throws IOException {
        String json = gson.toJson(body);
        RequestBody requestBody = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(ClientConfig.SERVER_URL + endpoint)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            return gson.fromJson(responseBody, responseType);
        }
    }

    public static <T> ApiResponse<T> get(String endpoint, Type responseType) throws IOException {
        Request request = new Request.Builder()
                .url(ClientConfig.SERVER_URL + endpoint)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            return gson.fromJson(responseBody, responseType);
        }
    }

    public static ApiResponse<Map<String, String>> uploadFile(File file) throws IOException {
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(ClientConfig.SERVER_URL + "/api/files/upload")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            Type type = new TypeToken<ApiResponse<Map<String, String>>>(){}.getType();
            return gson.fromJson(responseBody, type);
        }
    }
}

