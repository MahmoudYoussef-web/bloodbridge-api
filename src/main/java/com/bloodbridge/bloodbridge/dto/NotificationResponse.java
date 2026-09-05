package com.bloodbridge.bloodbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private String type;
    private String data;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
