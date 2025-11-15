package com.sipex.server.service;

import com.sipex.common.entity.ConferenceMessage;
import com.sipex.server.mapper.ConferenceMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConferenceMessageService {

    @Autowired
    private ConferenceMessageMapper messageMapper;

    /**
     * 保存消息（异步）
     */
    @Async
    public void saveMessage(String roomId, String fromUser, String content, String messageType) {
        ConferenceMessage message = new ConferenceMessage();
        message.setRoomId(roomId);
        message.setFromUser(fromUser);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : "text");

        messageMapper.insert(message);
        System.out.println("✅ 会议室消息已持久化: " + roomId + " - " + fromUser);
    }

    /**
     * 查询会议室消息
     */
    public List<ConferenceMessage> getByRoomId(String roomId, int limit) {
        return messageMapper.findByRoomId(roomId, limit);
    }

    /**
     * 查询用户发送的消息
     */
    public List<ConferenceMessage> getByUsername(String username, int limit) {
        return messageMapper.findByUsername(username, limit);
    }

    /**
     * 统计会议室消息数
     */
    public int countByRoomId(String roomId) {
        return messageMapper.countByRoomId(roomId);
    }

    /**
     * 统计今日消息数
     */
    public int countToday() {
        return messageMapper.countToday();
    }
}
