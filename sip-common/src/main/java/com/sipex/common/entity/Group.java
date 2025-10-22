package com.sipex.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Group {
    private Long id;
    private String groupName;
    private Long creatorId;
    private String avatar;
    private String description;
    private LocalDateTime createdAt;
}

