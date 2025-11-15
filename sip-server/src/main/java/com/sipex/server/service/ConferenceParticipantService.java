package com.sipex.server.service;

import com.sipex.common.entity.ConferenceParticipant;
import com.sipex.server.mapper.ConferenceParticipantMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConferenceParticipantService {

    @Autowired
    private ConferenceParticipantMapper participantMapper;

    /**
     * 记录参与者加入
     */
    public ConferenceParticipant recordJoin(String roomId, String username, boolean videoEnabled, boolean audioEnabled) {
        // 检查是否已存在记录（避免重复）
        ConferenceParticipant existing = participantMapper.findByRoomIdAndUsername(roomId, username);
        if (existing != null) {
            System.out.println("⚠️ 参与者 " + username + " 已有记录，跳过");
            return existing;
        }

        ConferenceParticipant participant = new ConferenceParticipant();
        participant.setRoomId(roomId);
        participant.setUsername(username);
        participant.setIsVideoEnabled(videoEnabled);
        participant.setIsAudioEnabled(audioEnabled);
        participant.setMessagesSent(0);

        participantMapper.insert(participant);
        System.out.println("✅ 参与者加入记录: " + username + " -> " + roomId);
        return participant;
    }

    /**
     * 记录参与者离开
     */
    public void recordLeave(String roomId, String username) {
        ConferenceParticipant participant = participantMapper.findByRoomIdAndUsername(roomId, username);
        if (participant != null && participant.getLeftAt() == null) {
            participant.setLeftAt(LocalDateTime.now());

            // 计算参与时长
            if (participant.getJoinedAt() != null) {
                long duration = java.time.Duration.between(participant.getJoinedAt(), participant.getLeftAt()).getSeconds();
                participant.setDuration((int) duration);
            }

            participantMapper.updateLeaveInfo(participant);
            System.out.println("✅ 参与者离开记录: " + username + ", 时长: " + participant.getDuration() + "秒");
        }
    }

    /**
     * 增加消息计数（异步）
     */
    @Async
    public void incrementMessageCount(String roomId, String username) {
        participantMapper.incrementMessageCount(roomId, username);
    }

    /**
     * 查询会议室参与者列表
     */
    public List<ConferenceParticipant> getByRoomId(String roomId) {
        return participantMapper.findByRoomId(roomId);
    }

    /**
     * 查询用户参与记录
     */
    public List<ConferenceParticipant> getByUsername(String username, int limit) {
        return participantMapper.findByUsername(username, limit);
    }

    /**
     * 统计会议室参与人数
     */
    public int countByRoomId(String roomId) {
        return participantMapper.countByRoomId(roomId);
    }

    /**
     * 统计今日活跃用户数
     */
    public int countTodayActiveUsers() {
        return participantMapper.countTodayActiveUsers();
    }
}
