package com.sipex.server.service;

import com.sipex.common.dto.ConferenceRoom;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    
    /**
     * 创建会议室
     */
    public ConferenceRoom createRoom(String username) {
        String roomId = generateRoomId();
        ConferenceRoom room = new ConferenceRoom(roomId);
        room.addParticipant(username);
        activeRooms.put(roomId, room);
        
        System.out.println("创建会议室: " + roomId + ", 创建者: " + username);
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
        
        // 如果会议室为空，删除会议室
        if (room.isEmpty()) {
            activeRooms.remove(roomId);
            System.out.println("会议室 " + roomId + " 已清空，已删除");
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
            activeRooms.remove(roomId);
            System.out.println("清理过期会议室: " + roomId);
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
}

