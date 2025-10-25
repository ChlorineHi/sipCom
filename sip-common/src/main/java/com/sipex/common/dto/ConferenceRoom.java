package com.sipex.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会议室数据模型
 * 用于临时会议室管理（不持久化到数据库）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceRoom {
    private String roomId;                          // 房间号（如：ROOM-1234）
    private List<String> participants;               // 参与者用户名列表
    private LocalDateTime createdAt;                 // 创建时间
    private LocalDateTime lastActivityAt;            // 最后活动时间
    private int maxParticipants;                     // 最大参与者数量（默认5）
    
    public ConferenceRoom(String roomId) {
        this.roomId = roomId;
        this.participants = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
        this.maxParticipants = 5;
    }
    
    /**
     * 添加参与者
     */
    public boolean addParticipant(String username) {
        if (participants.size() >= maxParticipants) {
            return false;
        }
        if (!participants.contains(username)) {
            participants.add(username);
            lastActivityAt = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    /**
     * 移除参与者
     */
    public boolean removeParticipant(String username) {
        boolean removed = participants.remove(username);
        if (removed) {
            lastActivityAt = LocalDateTime.now();
        }
        return removed;
    }
    
    /**
     * 检查是否已满
     */
    public boolean isFull() {
        return participants.size() >= maxParticipants;
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return participants.isEmpty();
    }
    
    /**
     * 更新活动时间
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
}

