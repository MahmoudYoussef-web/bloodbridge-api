package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.Governorate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernorateRepository extends JpaRepository<Governorate, Long> {

    List<Governorate> findByIsActiveTrueOrderByDisplayOrderAsc();
}