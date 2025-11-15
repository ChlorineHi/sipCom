package com.sipex.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会议室消息持久化实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceMessage {
    private Long id;
    private String roomId;              // 会议室ID
    private String fromUser;            // 发送者用户名
    private String content;             // 消息内容
    private String messageType;         // 消息类型: text, system
    private LocalDateTime createdAt;    // 发送时间
}
