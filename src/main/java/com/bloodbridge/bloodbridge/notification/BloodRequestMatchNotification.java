package com.bloodbridge.bloodbridge.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class BloodRequestMatchNotification implements AppNotification {
    private Long bloodRequestId;
    private String title;
    private String body;
    private String icon;
    private String iconColor;
    private Double distance;

    public static BloodRequestMatchNotification create(
            Long bloodRequestId, String title, String body,
            String icon, String iconColor, Double distance) {
        return new BloodRequestMatchNotification(bloodRequestId, title, body, icon, iconColor, distance);
    }

    public Map<String, Object> toDatabasePayload() {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("body", body);
        data.put("icon", icon);
        data.put("iconColor", iconColor);
        data.put("bloodRequestId", bloodRequestId);
        if (distance != null) {
            data.put("distance", distance);
        }
        payload.put("data", data);
        return payload;
    }
}
