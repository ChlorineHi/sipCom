package com.sipex.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallLog {
    private Long id;
    private String caller;
    private String callee;
    private String callType;
    private String status;
    private Integer duration;
    private Boolean isGroupCall;
    private Long groupId;
    private LocalDateTime createdAt;
}

