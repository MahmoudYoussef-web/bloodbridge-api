package com.bloodbridge.bloodbridge.controller;

import com.bloodbridge.bloodbridge.dto.MessageResponse;
import com.bloodbridge.bloodbridge.dto.NotificationResponse;
import com.bloodbridge.bloodbridge.entity.Notification;
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.exception.ResourceNotFoundException;
import com.bloodbridge.bloodbridge.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    private static final String NOTIFIABLE_TYPE = "App\\Models\\User";

    private NotificationResponse toView(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .data(n.getData())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        List<Notification> all = unreadOnly
                ? notificationRepository.findUnreadByNotifiable(NOTIFIABLE_TYPE, user.getId())
                : notificationRepository.findByNotifiable(NOTIFIABLE_TYPE, user.getId());
        List<NotificationResponse> views = all.stream().map(this::toView).toList();

        int start = (int) Math.min(pageable.getOffset(), views.size());
        int end = Math.min(start + pageable.getPageSize(), views.size());
        Page<NotificationResponse> page = new PageImpl<>(views.subList(start, end), pageable, views.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal User user) {
        long count = notificationRepository.countByNotifiableTypeAndNotifiableIdAndReadAtIsNull(
                NOTIFIABLE_TYPE, user.getId());
        return ResponseEntity.ok(Map.of("unread", count));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<MessageResponse> markRead(
            @AuthenticationPrincipal User user, @PathVariable String id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!NOTIFIABLE_TYPE.equals(n.getNotifiableType()) || !user.getId().equals(n.getNotifiableId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        if (n.getReadAt() == null) {
            notificationRepository.markAsRead(id, LocalDateTime.now());
        }
        return ResponseEntity.ok(new MessageResponse("Notification marked as read"));
    }

    @PostMapping("/read-all")
    @Transactional
    public ResponseEntity<MessageResponse> markAllRead(@AuthenticationPrincipal User user) {
        notificationRepository.markAllAsRead(NOTIFIABLE_TYPE, user.getId(), LocalDateTime.now());
        return ResponseEntity.ok(new MessageResponse("All notifications marked as read"));
    }
}
