package com.sipex.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会议室记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceLog {
    private Long id;
    private String roomId;              // 会议室ID
    private String creator;             // 创建者用户名
    private String roomName;            // 会议室名称
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime endedAt;      // 结束时间
    private Integer duration;           // 会议时长（秒）
    private Integer maxParticipants;    // 最大参与人数
    private Integer totalMessages;      // 总消息数
    private String status;              // 状态: ACTIVE, ENDED, EXPIRED
}
