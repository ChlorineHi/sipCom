package com.sipex.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private Long id;
    private String fromUser;
    private String toUser;
    private String content;
    private String type;
    private String fileUrl;
    private Boolean isGroup;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

