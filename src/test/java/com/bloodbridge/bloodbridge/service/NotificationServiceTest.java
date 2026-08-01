package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.AbstractIntegrationTest;
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.enumtype.NotificationType;
import com.bloodbridge.bloodbridge.enumtype.UserRole;
import com.bloodbridge.bloodbridge.notification.*;
import com.bloodbridge.bloodbridge.repository.NotificationRepository;
import com.bloodbridge.bloodbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceTest extends AbstractIntegrationTest {

    @Autowired private NotificationService notificationService;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Notify User");
        user.setEmail("notify" + System.currentTimeMillis() + "@test.com");
        user.setPassword("pass");
        user.setRole(UserRole.DONOR);
        user.setIsActive(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setLocale("en");
        user = userRepository.save(user);
    }

    @Test
    void shouldPersistBloodRequestMatchNotification() {
        var notif = new BloodRequestMatchNotification(1L, "Title", "Body", "bell", "red", 5.0);
        notificationService.send(user, notif, NotificationType.BLOOD_REQUEST_MATCH);

        var notifications = notificationRepository.findByNotifiable("App\\Models\\User", user.getId());
        assertThat(notifications).isNotEmpty();
    }

    @Test
    void shouldPersistDonorResponseNotification() {
        var notif = new DonorResponseNotification(2L, 1L, "Donor responded", "Status: accepted", "user", "green");
        notificationService.send(user, notif, NotificationType.DONOR_RESPONSE);

        var notifications = notificationRepository.findByNotifiable("App\\Models\\User", user.getId());
        assertThat(notifications).isNotEmpty();
    }

    @Test
    void shouldPersistAllNotificationTypes() {
        notificationService.send(user, new BloodRequestMatchNotification(10L, "Match", "Body", "bell", "red", 1.0), NotificationType.BLOOD_REQUEST_MATCH);
        notificationService.send(user, new DonorResponseNotification(20L, 1L, "Response", "Body", "user", "green"), NotificationType.DONOR_RESPONSE);
        notificationService.send(user, new ResponseNotNeededNotification(30L, "Not needed", "Another donor found"), NotificationType.RESPONSE_NOT_NEEDED);
        notificationService.send(user, new DonorIneligibilityNotification("ineligible", "Weight", "2026-08-01", "Org", "Ineligible", "Reason"), NotificationType.DONOR_INELIGIBILITY);
        notificationService.send(user, new SystemAnnouncementNotification(50L, "Announcement", "System maintenance", false), NotificationType.SYSTEM_ANNOUNCEMENT);

        var notifications = notificationRepository.findByNotifiable("App\\Models\\User", user.getId());
        assertThat(notifications).hasSize(5);
    }

    @Test
    void shouldMarkNotificationAsRead() {
        var notif = new BloodRequestMatchNotification(1L, "Title", "Body", "bell", "red", null);
        notificationService.send(user, notif, NotificationType.BLOOD_REQUEST_MATCH);

        var notifications = notificationRepository.findByNotifiable("App\\Models\\User", user.getId());
        assertThat(notifications).isNotEmpty();

        notificationRepository.markAsRead(notifications.get(0).getId(), LocalDateTime.now());

        var updated = notificationRepository.findById(notifications.get(0).getId()).orElseThrow();
        assertThat(updated.getReadAt()).isNotNull();
    }

    @Test
    void shouldCountUnreadNotifications() {
        notificationService.send(user, new BloodRequestMatchNotification(1L, "Match", "Body", "bell", "red", null), NotificationType.BLOOD_REQUEST_MATCH);
        notificationService.send(user, new DonorResponseNotification(2L, 1L, "Response", "Body", "user", "green"), NotificationType.DONOR_RESPONSE);

        long count = notificationRepository.countByNotifiableTypeAndNotifiableIdAndReadAtIsNull("App\\Models\\User", user.getId());
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldMarkAllAsRead() {
        notificationService.send(user, new BloodRequestMatchNotification(1L, "Match", "Body", "bell", "red", null), NotificationType.BLOOD_REQUEST_MATCH);
        notificationService.send(user, new DonorResponseNotification(2L, 1L, "Response", "Body", "user", "green"), NotificationType.DONOR_RESPONSE);

        notificationRepository.markAllAsRead("App\\Models\\User", user.getId(), LocalDateTime.now());

        long unread = notificationRepository.countByNotifiableTypeAndNotifiableIdAndReadAtIsNull("App\\Models\\User", user.getId());
        assertThat(unread).isEqualTo(0);
    }

    @Test
    void shouldRespectLocale() {
        user.setLocale("ar");
        userRepository.save(user);

        notificationService.send(user, new BloodRequestMatchNotification(1L, "Title AR", "Body AR", "bell", "red", null), NotificationType.BLOOD_REQUEST_MATCH);

        var notifications = notificationRepository.findByNotifiable("App\\Models\\User", user.getId());
        assertThat(notifications).isNotEmpty();
    }

    @Test
    void shouldExposeToDatabasePayload() {
        var notif = new BloodRequestMatchNotification(1L, "Title", "Body", "bell", "red", 5.0);
        var payload = notif.toDatabasePayload();
        assertThat(payload).containsKey("data");
    }
}
