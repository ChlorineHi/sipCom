package com.sipex.server.service;

import com.sipex.common.dto.ConferenceRoom;
import com.sipex.common.dto.ConferenceMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会议室服务
 * 使用内存存储管理临时会议室
 */
@Service
public class ConferenceService {

    // 内存存储会议室
    private final Map<String, ConferenceRoom> activeRooms = new ConcurrentHashMap<>();

    // 会议室过期时间（30分钟）
    private static final int ROOM_EXPIRE_MINUTES = 30;

    @Autowired
    private ConferenceLogService conferenceLogService;

    @Autowired
    private ConferenceParticipantService participantService;

    @Autowired
    private ConferenceMessageService conferenceMessageService;

    @Autowired
    private UserActivityLogService activityLogService;

    /**
     * 创建会议室
     */
    public ConferenceRoom createRoom(String username) {
        String roomId = generateRoomId();
        ConferenceRoom room = new ConferenceRoom(roomId);
        room.addParticipant(username);
        activeRooms.put(roomId, room);

        System.out.println("创建会议室: " + roomId + ", 创建者: " + username);

        // ✅ 新增：创建会议室记录
        try {
            conferenceLogService.createLog(roomId, username);
            participantService.recordJoin(roomId, username, true, true);
            activityLogService.log(username, "CREATE_CONFERENCE", "创建会议室: " + roomId);
        } catch (Exception e) {
            System.err.println("记录会议室创建失败: " + e.getMessage());
        }

        return room;
    }

    /**
     * 加入会议室
     */
    public ConferenceRoom joinRoom(String roomId, String username) {
        ConferenceRoom room = activeRooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("会议室不存在");
        }

        if (room.isFull()) {
            throw new RuntimeException("会议室已满");
        }

        if (!room.addParticipant(username)) {
            throw new RuntimeException("加入会议室失败");
        }

        System.out.println("用户 " + username + " 加入会议室: " + roomId);

        // ✅ 新增：记录参与者加入
        try {
            participantService.recordJoin(roomId, username, true, true);
            conferenceLogService.updateMaxParticipants(roomId, room.getParticipants().size());
            activityLogService.log(username, "JOIN_CONFERENCE", "加入会议室: " + roomId);
        } catch (Exception e) {
            System.err.println("记录参与者加入失败: " + e.getMessage());
        }

        return room;
    }

    /**
     * 离开会议室
     */
    public ConferenceRoom leaveRoom(String roomId, String username) {
        ConferenceRoom room = activeRooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("会议室不存在");
        }

        room.removeParticipant(username);
        System.out.println("用户 " + username + " 离开会议室: " + roomId);

        // ✅ 新增：记录参与者离开
        try {
            participantService.recordLeave(roomId, username);
            activityLogService.log(username, "LEAVE_CONFERENCE", "离开会议室: " + roomId);
        } catch (Exception e) {
            System.err.println("记录参与者离开失败: " + e.getMessage());
        }

        // 如果会议室为空，删除会议室
        if (room.isEmpty()) {
            activeRooms.remove(roomId);
            System.out.println("会议室 " + roomId + " 已清空，已删除");

            // ✅ 新增：结束会议室记录
            try {
                conferenceLogService.endLog(roomId, room.getParticipants().size());
            } catch (Exception e) {
                System.err.println("结束会议室记录失败: " + e.getMessage());
            }
        }

        return room;
    }

    /**
     * 获取会议室信息
     */
    public ConferenceRoom getRoom(String roomId) {
        ConferenceRoom room = activeRooms.get(roomId);
        if (room != null) {
            room.updateActivity();
        }
        return room;
    }

    /**
     * 获取会议室参与者列表
     */
    public List<String> getParticipants(String roomId) {
        ConferenceRoom room = activeRooms.get(roomId);
        if (room == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(room.getParticipants());
    }

    /**
     * 检查会议室是否存在
     */
    public boolean roomExists(String roomId) {
        return activeRooms.containsKey(roomId);
    }

    /**
     * 生成房间号
     */
    private String generateRoomId() {
        Random random = new Random();
        String roomId;
        do {
            int number = 1000 + random.nextInt(9000); // 1000-9999
            roomId = "ROOM-" + number;
        } while (activeRooms.containsKey(roomId));
        return roomId;
    }

    /**
     * 定时清理过期会议室
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cleanupExpiredRooms() {
        LocalDateTime now = LocalDateTime.now();
        List<String> expiredRooms = new ArrayList<>();

        for (Map.Entry<String, ConferenceRoom> entry : activeRooms.entrySet()) {
            ConferenceRoom room = entry.getValue();
            if (room.getLastActivityAt().plusMinutes(ROOM_EXPIRE_MINUTES).isBefore(now)) {
                expiredRooms.add(entry.getKey());
            }
        }

        for (String roomId : expiredRooms) {
            ConferenceRoom room = activeRooms.remove(roomId);
            System.out.println("清理过期会议室: " + roomId);

            // ✅ 新增：标记会议室为过期
            try {
                if (room != null) {
                    conferenceLogService.endLog(roomId, room.getParticipants().size());
                }
            } catch (Exception e) {
                System.err.println("标记过期会议室失败: " + e.getMessage());
            }
        }

        if (!expiredRooms.isEmpty()) {
            System.out.println("清理了 " + expiredRooms.size() + " 个过期会议室");
        }
    }

    /**
     * 获取活跃会议室数量
     */
    public int getActiveRoomCount() {
        return activeRooms.size();
    }

    /**
     * 添加会议室消息
     */
    public ConferenceMessageDTO addMessage(String roomId, ConferenceMessageDTO message) {
        ConferenceRoom room = activeRooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("会议室不存在");
        }

        // 设置时间戳
        if (message.getTimestamp() == null || message.getTimestamp().isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            message.setTimestamp(LocalDateTime.now().format(formatter));
        }

        // 设置消息类型（默认为text）
        if (message.getMessageType() == null || message.getMessageType().isEmpty()) {
            message.setMessageType("text");
        }

        // 设置roomId
        message.setRoomId(roomId);

        // 添加到会议室（内存）
        room.addMessage(message);

        System.out.println("会议室 " + roomId + " 新消息: " + message.getFromUser() + ": " + message.getContent());

        // ✅ 新增：异步持久化消息到数据库
        try {
            conferenceMessageService.saveMessage(roomId, message.getFromUser(),
                    message.getContent(), message.getMessageType());
            conferenceLogService.incrementMessageCount(roomId);
            participantService.incrementMessageCount(roomId, message.getFromUser());
        } catch (Exception e) {
            System.err.println("持久化消息失败: " + e.getMessage());
        }

        return message;
    }

    /**
     * 获取会议室消息
     */
    public List<ConferenceMessageDTO> getMessages(String roomId, int limit) {
        ConferenceRoom room = activeRooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("会议室不存在");
        }

        return room.getRecentMessages(limit);
    }
}
