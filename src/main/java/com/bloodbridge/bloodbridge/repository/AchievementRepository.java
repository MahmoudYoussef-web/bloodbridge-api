package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    @Query("SELECT a FROM Achievement a ORDER BY a.displayOrder ASC")
    List<Achievement> findActiveOrderByDisplayOrder();

    List<Achievement> findByCriteriaType(String criteriaType);
}