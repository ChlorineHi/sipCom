package com.sipex.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会议室参与者记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceParticipant {
    private Long id;
    private String roomId;              // 会议室ID
    private String username;            // 参与者用户名
    private LocalDateTime joinedAt;     // 加入时间
    private LocalDateTime leftAt;       // 离开时间
    private Integer duration;           // 参与时长（秒）
    private Boolean isVideoEnabled;     // 是否开启视频
    private Boolean isAudioEnabled;     // 是否开启音频
    private Integer messagesSent;       // 发送消息数
}
