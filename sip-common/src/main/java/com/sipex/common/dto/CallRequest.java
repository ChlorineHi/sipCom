package com.sipex.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallRequest {
    private String caller;
    private String callee;
    private String callType;
    private String sdp;
    private Boolean isGroupCall;
    private Long groupId;
}

