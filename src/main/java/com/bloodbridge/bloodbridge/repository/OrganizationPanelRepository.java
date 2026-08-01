package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationPanelRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findBySlugAndDeletedAtIsNull(String slug);
}