package com.sipex.client.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sipex.common.dto.ApiResponse;
import com.sipex.common.dto.MessageDTO;
import com.sipex.common.entity.Message;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class MessageService {

    private static final Gson gson = new Gson();

    public static void sendMessage(MessageDTO messageDTO) throws IOException {
        Type type = new TypeToken<ApiResponse<Message>>(){}.getType();
        HttpClientService.post("/api/messages", messageDTO, type);
    }

    public static List<Message> getChatHistory(String user1, String user2, int limit) throws IOException {
        Type type = new TypeToken<ApiResponse<List<Message>>>(){}.getType();
        String endpoint = String.format("/api/messages/history?user1=%s&user2=%s&limit=%d", user1, user2, limit);
        ApiResponse<List<Message>> response = HttpClientService.get(endpoint, type);
        return response.getData();
    }

    public static List<Message> getUnreadMessages(String username) throws IOException {
        Type type = new TypeToken<ApiResponse<List<Message>>>(){}.getType();
        String endpoint = String.format("/api/messages/unread?username=%s", username);
        ApiResponse<List<Message>> response = HttpClientService.get(endpoint, type);
        return response.getData();
    }
}

