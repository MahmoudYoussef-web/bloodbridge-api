package com.bloodbridge.bloodbridge.notification;

import java.util.Map;

public interface AppNotification {
    String getTitle();
    Map<String, Object> toDatabasePayload();
    default String getNotificationClass() {
        return getClass().getSimpleName();
    }
}
