package com.bloodbridge.bloodbridge.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ResponseNotNeededNotification implements AppNotification {
    private Long responseId;
    private String title;
    private String body;

    public Map<String, Object> toDatabasePayload() {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("body", body);
        data.put("responseId", responseId);
        payload.put("data", data);
        return payload;
    }
}
