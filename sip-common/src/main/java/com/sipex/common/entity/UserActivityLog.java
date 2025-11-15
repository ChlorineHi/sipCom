package com.sipex.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户活动日志实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityLog {
    private Long id;
    private String username;            // 用户名
    private String activityType;        // 活动类型
    private String description;         // 活动描述
    private String ipAddress;           // IP地址
    private String userAgent;           // 用户代理
    private String metadata;            // 额外元数据（JSON格式）
    private LocalDateTime createdAt;    // 时间
}
