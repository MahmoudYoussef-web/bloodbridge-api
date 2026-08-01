package com.bloodbridge.bloodbridge.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class DonorResponseNotification implements AppNotification {
    private Long responseId;
    private Long bloodRequestId;
    private String title;
    private String body;
    private String icon;
    private String iconColor;

    public Map<String, Object> toDatabasePayload() {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("body", body);
        data.put("icon", icon);
        data.put("iconColor", iconColor);
        data.put("responseId", responseId);
        data.put("bloodRequestId", bloodRequestId);
        payload.put("data", data);
        return payload;
    }
}
