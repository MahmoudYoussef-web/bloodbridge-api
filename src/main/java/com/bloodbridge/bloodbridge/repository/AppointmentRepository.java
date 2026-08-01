package com.bloodbridge.bloodbridge.repository;

import com.bloodbridge.bloodbridge.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByBloodRequestId(Long bloodRequestId);

    List<Appointment> findByDonorId(Long donorId);

    List<Appointment> findByOrganizationId(Long organizationId);

    List<Appointment> findByStatus(String status);
}