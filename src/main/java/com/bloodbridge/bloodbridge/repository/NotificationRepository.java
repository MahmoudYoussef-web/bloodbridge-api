package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    @Query("SELECT n FROM Notification n WHERE n.notifiableType = :type AND n.notifiableId = :id ORDER BY n.createdAt DESC")
    List<Notification> findByNotifiable(@Param("type") String notifiableType, @Param("id") Long notifiableId);

    @Query("SELECT n FROM Notification n WHERE n.notifiableType = :type AND n.notifiableId = :id AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByNotifiable(@Param("type") String notifiableType, @Param("id") Long notifiableId);

    long countByNotifiableTypeAndNotifiableIdAndReadAtIsNull(String notifiableType, Long notifiableId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.id = :id AND n.readAt IS NULL")
    int markAsRead(@Param("id") String id, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.notifiableType = :type AND n.notifiableId = :id AND n.readAt IS NULL")
    int markAllAsRead(@Param("type") String notifiableType, @Param("id") Long notifiableId, @Param("now") LocalDateTime now);
}
