package com.bloodbridge.bloodbridge.service;

import com.bloodbridge.bloodbridge.shared.audit.AuditLog;
import com.bloodbridge.bloodbridge.shared.audit.AuditLogRepository;
import com.bloodbridge.bloodbridge.shared.audit.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository, new ObjectMapper());
    }

    @Test
    void shouldCreateAuditLog() {
        auditLogService.logSimple("BloodRequest", 1L, "CREATED", 1L);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void shouldHandleNullValuesGracefully() {
        auditLogService.log("BloodRequest", 1L, "UPDATED", 1L, "ADMIN", null, null, null);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void shouldLogWithOldAndNewValues() {
        auditLogService.log("BloodRequest", 1L, "STATUS_CHANGE", 1L,
                "ADMIN", "PENDING", "BROADCASTED", null);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void shouldNotThrowWhenRepositoryFails() {
        doThrow(new RuntimeException("DB error")).when(auditLogRepository).save(any());

        auditLogService.logSimple("BloodRequest", 1L, "TEST", 1L);

        verify(auditLogRepository).save(any(AuditLog.class));
    }
}
