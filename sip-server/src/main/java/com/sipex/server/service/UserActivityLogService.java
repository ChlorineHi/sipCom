package com.sipex.server.service;

import com.sipex.common.entity.UserActivityLog;
import com.sipex.server.mapper.UserActivityLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserActivityLogService {

    @Autowired
    private UserActivityLogMapper activityLogMapper;

    /**
     * 记录用户活动（异步）
     */
    @Async
    public void log(String username, String activityType, String description) {
        log(username, activityType, description, null, null, null);
    }

    /**
     * 记录用户活动（完整信息，异步）
     */
    @Async
    public void log(String username, String activityType, String description,
                    String ipAddress, String userAgent, String metadata) {
        UserActivityLog log = new UserActivityLog();
        log.setUsername(username);
        log.setActivityType(activityType);
        log.setDescription(description);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setMetadata(metadata);

        activityLogMapper.insert(log);
        System.out.println("✅ 用户活动已记录: " + username + " - " + activityType);
    }

    /**
     * 查询用户活动日志
     */
    public List<UserActivityLog> getByUsername(String username, int limit) {
        return activityLogMapper.findByUsername(username, limit);
    }

    /**
     * 根据活动类型查询
     */
    public List<UserActivityLog> getByActivityType(String activityType, int limit) {
        return activityLogMapper.findByActivityType(activityType, limit);
    }

    /**
     * 按日期范围查询
     */
    public List<UserActivityLog> getByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return activityLogMapper.findByDateRange(startDate, endDate);
    }

    /**
     * 查询最近活动
     */
    public List<UserActivityLog> getRecent(int limit) {
        return activityLogMapper.findRecent(limit);
    }

    /**
     * 统计今日活动数
     */
    public int countToday() {
        return activityLogMapper.countToday();
    }
}
