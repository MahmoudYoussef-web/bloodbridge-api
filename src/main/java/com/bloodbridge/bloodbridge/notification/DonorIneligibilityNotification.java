package com.bloodbridge.bloodbridge.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class DonorIneligibilityNotification implements AppNotification {
    private String eligibilityStatus;
    private String rejectionReason;
    private String nextEligibleDate;
    private String organizationName;
    private String title;
    private String body;

    public Map<String, Object> toDatabasePayload() {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("body", body);
        data.put("eligibilityStatus", eligibilityStatus);
        data.put("rejectionReason", rejectionReason);
        data.put("nextEligibleDate", nextEligibleDate);
        data.put("organizationName", organizationName);
        payload.put("data", data);
        return payload;
    }
}
