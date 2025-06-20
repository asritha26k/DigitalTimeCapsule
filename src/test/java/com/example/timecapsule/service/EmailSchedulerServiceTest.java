package com.example.timecapsule.service;

import com.example.timecapsule.model.TimeCapsule;
import com.example.timecapsule.model.FileMetadata;
import com.example.timecapsule.repository.TimeCapsuleRepository;
import com.example.timecapsule.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class EmailSchedulerServiceTest {

    private TimeCapsuleRepository capsuleRepository;
    private EmailService emailService;
    private JwtUtils jwtUtils;
    private EmailSchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        capsuleRepository = mock(TimeCapsuleRepository.class);
        emailService = mock(EmailService.class);
        jwtUtils = mock(JwtUtils.class);

        schedulerService = new EmailSchedulerService(capsuleRepository, emailService, jwtUtils);
    }

    @Test
    void testCheckAndSendCapsules_sendsEmailAndUnlocksCapsule() {
        // Given
        TimeCapsule capsule = new TimeCapsule();
        capsule.setId("capsule123");
        capsule.setRecipientEmail("test@example.com");
        capsule.setOwnerUsername("owner1");
        capsule.setUnlockDate(Instant.now().minusSeconds(10));
        capsule.setStatus(TimeCapsule.CapsuleStatus.LOCKED);
        capsule.setFileMetadataList(List.of(new FileMetadata()));

        when(capsuleRepository.findByUnlockDateBeforeAndStatus(any(), eq(TimeCapsule.CapsuleStatus.LOCKED)))
                .thenReturn(List.of(capsule));

        when(jwtUtils.generateJwtToken("capsule123")).thenReturn("mocked-token");

        // When
        schedulerService.checkAndSendCapsules();

        // Then
        verify(emailService, times(1)).sendCapsuleUnlockEmail(eq(capsule), contains("mocked-token"));
        verify(capsuleRepository, times(1)).save(capsule);

        assertEquals(TimeCapsule.CapsuleStatus.UNLOCKED, capsule.getStatus());
    }

    @Test
    void testCheckAndSendCapsules_handlesExceptionGracefully() {
        // Given a capsule that throws an error on email send
        TimeCapsule capsule = new TimeCapsule();
        capsule.setId("capsule456");
        capsule.setRecipientEmail("error@example.com");
        capsule.setOwnerUsername("owner2");
        capsule.setUnlockDate(Instant.now().minusSeconds(10));
        capsule.setStatus(TimeCapsule.CapsuleStatus.LOCKED);
        capsule.setFileMetadataList(List.of());

        when(capsuleRepository.findByUnlockDateBeforeAndStatus(any(), any()))
                .thenReturn(List.of(capsule));
        when(jwtUtils.generateJwtToken("capsule456")).thenReturn("bad-token");

        doThrow(new RuntimeException("Email failed")).when(emailService)
                .sendCapsuleUnlockEmail(eq(capsule), anyString());

        // When
        assertDoesNotThrow(() -> schedulerService.checkAndSendCapsules());

        // Still should attempt to update
        verify(capsuleRepository, never()).save(any());
    }
}
