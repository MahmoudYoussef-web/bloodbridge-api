package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.ModelTrainingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModelTrainingLogRepository extends JpaRepository<ModelTrainingLog, Long> {

    @Query("SELECT mtl FROM ModelTrainingLog mtl ORDER BY mtl.trainingDate DESC")
    Optional<ModelTrainingLog> findLatestTrainingLog();

    Optional<ModelTrainingLog> findByModelVersion(String modelVersion);
}