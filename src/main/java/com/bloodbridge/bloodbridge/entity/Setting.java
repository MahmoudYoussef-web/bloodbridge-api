package com.bloodbridge.bloodbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "settings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_name", "name"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private Boolean locked = false;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.JsonStringConverter.class)
    @Column(name = "payload", columnDefinition = "JSON", nullable = false)
    private String payload;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
