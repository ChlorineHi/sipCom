package com.sipex.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日统计实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatistic {
    private Long id;
    private LocalDate statDate;             // 统计日期
    private Integer totalUsers;             // 总用户数
    private Integer activeUsers;            // 活跃用户数
    private Integer totalCalls;             // 通话次数
    private Integer totalCallDuration;      // 通话时长（秒）
    private Integer totalMessages;          // 消息数
    private Integer totalConferences;       // 会议室数
    private Integer totalConferenceDuration;// 会议总时长（秒）
    private LocalDateTime createdAt;        // 创建时间
    private LocalDateTime updatedAt;        // 更新时间
}
