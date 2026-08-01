package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.entity.Notification;
import com.bloodbridge.bloodbridge.enumtype.NotificationType;
import com.bloodbridge.bloodbridge.notification.AppNotification;
import com.bloodbridge.bloodbridge.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void send(com.bloodbridge.bloodbridge.entity.User user, AppNotification notification, NotificationType type) {
        persistAndLog(user, notification.getNotificationClass(), notification.getTitle(), notification.toDatabasePayload());
    }

    @Transactional
    public void sendRaw(com.bloodbridge.bloodbridge.entity.User user, String notificationClass, String title, String body, NotificationType type) {
        Map<String, Object> payload = Map.of(
                "data", Map.of(
                        "title", title,
                        "body", body
                )
        );
        persistAndLog(user, notificationClass, title, payload);
    }

    private void persistAndLog(com.bloodbridge.bloodbridge.entity.User user, String notificationClass, String title, Map<String, Object> payload) {
        try {
            String locale = user.getLocale() != null ? user.getLocale() : "en";

            String jsonPayload = objectMapper.writeValueAsString(payload);

            Notification notification = Notification.builder()
                    .id(UUID.randomUUID().toString())
                    .type("App\\\\Notifications\\\\" + notificationClass)
                    .notifiableType("App\\Models\\User")
                    .notifiableId(user.getId())
                    .data(jsonPayload)
                    .build();

            notificationRepository.save(notification);

            log.info("Notification sent to user {} (locale={}): title='{}', type={}, id={}",
                    user.getId(), locale, title, notificationClass, notification.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification payload for user {}: {}", user.getId(), e.getMessage());
        }
    }
}
