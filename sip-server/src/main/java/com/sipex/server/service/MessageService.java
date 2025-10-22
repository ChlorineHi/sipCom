package com.sipex.server.service;

import com.sipex.common.entity.Message;
import com.sipex.server.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    public Message saveMessage(Message message) {
        message.setIsRead(false);
        messageMapper.insert(message);
        return message;
    }

    public List<Message> getChatHistory(String user1, String user2, int limit) {
        return messageMapper.findChatHistory(user1, user2, limit);
    }

    public List<Message> getUnreadMessages(String username) {
        return messageMapper.findUnreadMessages(username);
    }

    public void markAsRead(Long messageId) {
        messageMapper.markAsRead(messageId);
    }

    public int countTodayMessages() {
        return messageMapper.countTodayMessages();
    }

    public int countAllMessages() {
        return messageMapper.countAll();
    }
}

