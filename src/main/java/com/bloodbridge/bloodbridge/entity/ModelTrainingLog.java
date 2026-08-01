package com.bloodbridge.bloodbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "model_training_logs", indexes = {
    @Index(name = "idx_mtl_training_date", columnList = "training_date"),
    @Index(name = "idx_mtl_model_version", columnList = "model_version", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelTrainingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_version", nullable = false, length = 50, unique = true)
    private String modelVersion;

    @Column(name = "training_date", nullable = false)
    private LocalDateTime trainingDate;

    @Column(name = "data_records_used")
    private Integer dataRecordsUsed;

    @Column(name = "algorithm", length = 50)
    @Builder.Default
    private String algorithm = "xgboost";

    @Convert(converter = com.bloodbridge.bloodbridge.converter.JsonStringConverter.class)
    @Column(name = "hyperparameters", columnDefinition = "JSON")
    private String hyperparameters;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.JsonStringConverter.class)
    @Column(name = "metrics", columnDefinition = "JSON")
    private String metrics;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.JsonStringConverter.class)
    @Column(name = "feature_importance", columnDefinition = "JSON")
    private String featureImportance;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}