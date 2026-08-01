package com.bloodbridge.bloodbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_notifiable", columnList = "notifiable_type, notifiable_id"),
    @Index(name = "idx_notif_read", columnList = "notifiable_type, notifiable_id, read_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "type", nullable = false, length = 255)
    private String type;

    @Column(name = "notifiable_type", nullable = false, length = 255)
    private String notifiableType;

    @Column(name = "notifiable_id", nullable = false)
    private Long notifiableId;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.JsonStringConverter.class)
    @Column(name = "data", nullable = false, columnDefinition = "JSON")
    private String data;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
