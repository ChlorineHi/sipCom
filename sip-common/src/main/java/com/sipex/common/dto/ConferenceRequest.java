package com.sipex.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会议室请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceRequest {
    private String roomId;      // 房间号
    private String username;    // 用户名
    private String action;      // 操作类型：create/join/leave
}

