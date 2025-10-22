package com.sipex.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private String fromUser;
    private String toUser;
    private String content;
    private String type;
    private String fileUrl;
    private Boolean isGroup;
    private String timestamp;
}

