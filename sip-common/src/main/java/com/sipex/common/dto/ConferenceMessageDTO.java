package com.sipex.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会议室消息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceMessageDTO {
    private String roomId;          // 会议室ID
    private String fromUser;        // 发送者
    private String content;         // 消息内容
    private String timestamp;       // 时间戳
    private String messageType;     // 消息类型：text, system
}
