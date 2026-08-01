package com.bloodbridge.bloodbridge.entity;

import com.bloodbridge.bloodbridge.enumtype.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "donors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "governorate_id")
    private Long governorateId;

    @Column(name = "national_id", length = 9, unique = true)
    private String nationalId;

    @Convert(converter = com.bloodbridge.bloodbridge.converter.GenderConverter.class)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    @Column(name = "auto_location_address", length = 500)
    private String autoLocationAddress;

    @Column
    private Double lat;

    @Column
    private Double lng;

    @Column(name = "points")
    @Builder.Default
    private Integer points = 0;

    @Column(name = "level")
    @Builder.Default
    private Integer level = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "governorate_id", insertable = false, updatable = false)
    private Governorate governorate;

    @OneToOne(mappedBy = "donor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DonorHealthProfile healthProfile;

    @OneToMany(mappedBy = "donor", fetch = FetchType.LAZY)
    private java.util.List<RequestResponse> responses;

    @OneToMany(mappedBy = "donor", fetch = FetchType.LAZY)
    private java.util.List<EligibilityLog> eligibilityLogs;

    @OneToMany(mappedBy = "donor", fetch = FetchType.LAZY)
    private java.util.List<DonorAchievement> donorAchievements;

    @OneToOne(mappedBy = "donor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private DonorPredictiveScore predictiveScore;

    @Version
    @Column(name = "version")
    private Long version;
}