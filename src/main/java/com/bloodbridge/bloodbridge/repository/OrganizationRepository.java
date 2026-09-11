package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.Organization;
import com.bloodbridge.bloodbridge.enumtype.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByUserId(Long userId);

    Optional<Organization> findBySlug(String slug);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT o FROM Organization o")
    Page<Organization> findAllForAdmin(Pageable pageable);

    List<Organization> findByApprovalStatus(OrganizationStatus status);

    @Query("SELECT o FROM Organization o WHERE o.approvalStatus = :status AND o.deletedAt IS NULL")
    List<Organization> findByApprovalStatusNotDeleted(@Param("status") OrganizationStatus status);

    @Query("SELECT o FROM Organization o WHERE o.userId = :userId AND o.deletedAt IS NULL")
    Optional<Organization> findByUserIdNotDeleted(@Param("userId") Long userId);

    boolean existsBySlug(String slug);
}