package com.sipex.server.controller;

import com.sipex.common.dto.ApiResponse;
import com.sipex.common.dto.MessageDTO;
import com.sipex.common.entity.Message;
import com.sipex.server.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ApiResponse<Message> sendMessage(@RequestBody MessageDTO messageDTO) {
        try {
            Message message = new Message();
            message.setFromUser(messageDTO.getFromUser());
            message.setToUser(messageDTO.getToUser());
            message.setContent(messageDTO.getContent());
            message.setType(messageDTO.getType());
            message.setFileUrl(messageDTO.getFileUrl());
            message.setIsGroup(messageDTO.getIsGroup());

            Message savedMessage = messageService.saveMessage(message);

            // 通过WebSocket推送消息
            if (messageDTO.getIsGroup()) {
                messagingTemplate.convertAndSend("/topic/group/" + messageDTO.getToUser(), savedMessage);
            } else {
                messagingTemplate.convertAndSend("/queue/user/" + messageDTO.getToUser(), savedMessage);
            }

            return ApiResponse.success(savedMessage);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/history")
    public ApiResponse<List<Message>> getChatHistory(
            @RequestParam String user1,
            @RequestParam String user2,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<Message> messages = messageService.getChatHistory(user1, user2, limit);
            return ApiResponse.success(messages);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/unread")
    public ApiResponse<List<Message>> getUnreadMessages(@RequestParam String username) {
        try {
            List<Message> messages = messageService.getUnreadMessages(username);
            return ApiResponse.success(messages);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{messageId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long messageId) {
        try {
            messageService.markAsRead(messageId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}

