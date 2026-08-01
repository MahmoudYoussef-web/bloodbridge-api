package com.bloodbridge.bloodbridge.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "governorates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Governorate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_ar", nullable = false, length = 100)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
    
    public String getName(String locale) {
        return "ar".equals(locale) ? nameAr : nameEn;
    }
}