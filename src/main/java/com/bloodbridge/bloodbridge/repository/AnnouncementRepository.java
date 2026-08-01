package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a WHERE a.isPublished = true AND a.deletedAt IS NULL ORDER BY a.publishedAt DESC")
    List<Announcement> findPublishedOrderByPublishedAtDesc();

    @Query("SELECT a FROM Announcement a WHERE a.isPublished = false AND a.deletedAt IS NULL ORDER BY a.createdAt DESC")
    List<Announcement> findDraftsOrderByCreatedAtDesc();
}