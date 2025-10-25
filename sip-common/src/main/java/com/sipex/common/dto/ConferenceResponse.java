package com.sipex.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会议室响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceResponse {
    private boolean success;            // 操作是否成功
    private String message;             // 消息
    private String roomId;              // 房间号
    private List<String> participants;  // 参与者列表
    
    public static ConferenceResponse success(String roomId, List<String> participants) {
        return new ConferenceResponse(true, "操作成功", roomId, participants);
    }
    
    public static ConferenceResponse error(String message) {
        return new ConferenceResponse(false, message, null, null);
    }
}

