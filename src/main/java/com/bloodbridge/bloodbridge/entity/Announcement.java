package com.bloodbridge.bloodbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements", indexes = {
    @Index(name = "idx_ann_is_published", columnList = "is_published"),
    @Index(name = "idx_ann_published_at", columnList = "published_at"),
    @Index(name = "idx_ann_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title_ar", nullable = false, length = 255)
    private String titleAr;

    @Column(name = "title_en", nullable = false, length = 255)
    private String titleEn;

    @Column(name = "content_ar", columnDefinition = "TEXT", nullable = false)
    private String contentAr;

    @Column(name = "content_en", columnDefinition = "TEXT", nullable = false)
    private String contentEn;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    public String getTitle(String locale) {
        return "ar".equals(locale) ? titleAr : titleEn;
    }
    
    public String getContent(String locale) {
        return "ar".equals(locale) ? contentAr : contentEn;
    }
}