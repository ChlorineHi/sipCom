package com.sipex.server.service;

import com.sipex.common.entity.ConferenceLog;
import com.sipex.server.mapper.ConferenceLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConferenceLogService {

    @Autowired
    private ConferenceLogMapper conferenceLogMapper;

    /**
     * 创建会议室记录
     */
    public ConferenceLog createLog(String roomId, String creator) {
        ConferenceLog log = new ConferenceLog();
        log.setRoomId(roomId);
        log.setCreator(creator);
        log.setRoomName(roomId); // 可以后续添加自定义名称
        log.setStatus("ACTIVE");
        log.setMaxParticipants(1);
        log.setTotalMessages(0);
        log.setDuration(0);

        conferenceLogMapper.insert(log);
        System.out.println("✅ 会议室记录已创建: " + roomId);
        return log;
    }

    /**
     * 结束会议室记录
     */
    public void endLog(String roomId, int participantCount) {
        ConferenceLog log = conferenceLogMapper.findByRoomId(roomId);
        if (log != null) {
            log.setEndedAt(LocalDateTime.now());
            log.setStatus("ENDED");

            // 计算时长
            if (log.getCreatedAt() != null) {
                long duration = java.time.Duration.between(log.getCreatedAt(), log.getEndedAt()).getSeconds();
                log.setDuration((int) duration);
            }

            conferenceLogMapper.updateByRoomId(log);
            System.out.println("✅ 会议室记录已结束: " + roomId + ", 时长: " + log.getDuration() + "秒");
        }
    }

    /**
     * 更新最大参与人数
     */
    public void updateMaxParticipants(String roomId, int participantCount) {
        conferenceLogMapper.updateMaxParticipants(roomId, participantCount);
    }

    /**
     * 增加消息计数（异步）
     */
    @Async
    public void incrementMessageCount(String roomId) {
        conferenceLogMapper.incrementMessageCount(roomId);
    }

    /**
     * 根据房间ID查询
     */
    public ConferenceLog getByRoomId(String roomId) {
        return conferenceLogMapper.findByRoomId(roomId);
    }

    /**
     * 根据创建者查询
     */
    public List<ConferenceLog> getByCreator(String creator, int limit) {
        return conferenceLogMapper.findByCreator(creator, limit);
    }

    /**
     * 根据状态查询
     */
    public List<ConferenceLog> getByStatus(String status) {
        return conferenceLogMapper.findByStatus(status);
    }

    /**
     * 查询所有记录
     */
    public List<ConferenceLog> getAll(int limit) {
        return conferenceLogMapper.findAll(limit);
    }

    /**
     * 按日期范围查询
     */
    public List<ConferenceLog> getByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return conferenceLogMapper.findByDateRange(startDate, endDate);
    }

    /**
     * 统计今日会议数
     */
    public int countToday() {
        return conferenceLogMapper.countToday();
    }

    /**
     * 统计总会议数
     */
    public int countAll() {
        return conferenceLogMapper.countAll();
    }

    /**
     * 获取今日总会议时长
     */
    public Long getTodayTotalDuration() {
        Long duration = conferenceLogMapper.getTodayTotalDuration();
        return duration != null ? duration : 0L;
    }
}
