package com.example.timecapsule.service;

import com.example.timecapsule.model.FileMetadata;
import com.example.timecapsule.model.TimeCapsule;
import com.example.timecapsule.repository.TimeCapsuleRepository;
import com.example.timecapsule.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailSchedulerServiceTest {

    private TimeCapsuleRepository capsuleRepository;
    private EmailService emailService;
    private JwtUtils jwtUtils;
    private QuoteService quoteService;
    private EmailSchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        capsuleRepository = mock(TimeCapsuleRepository.class);
        emailService = mock(EmailService.class);
        jwtUtils = mock(JwtUtils.class);
        quoteService = mock(QuoteService.class);

        when(quoteService.getQuote(anyString())).thenReturn("Mock quote for testing.");
        schedulerService = new EmailSchedulerService(capsuleRepository, emailService, jwtUtils, quoteService);
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
        capsule.setTopic("birthday");

        when(capsuleRepository.findByUnlockDateBeforeAndStatus(any(), eq(TimeCapsule.CapsuleStatus.LOCKED)))
                .thenReturn(List.of(capsule));
        when(jwtUtils.generateJwtToken("capsule123")).thenReturn("mocked-token");

        // When
        schedulerService.checkAndSendCapsules();

        // Then
        verify(quoteService, times(1)).getQuote("birthday");
        verify(emailService, times(1)).sendCapsuleUnlockEmail(eq(capsule), contains("mocked-token"), eq("Mock quote for testing."));
        verify(capsuleRepository, times(1)).save(capsule);

        assertEquals(TimeCapsule.CapsuleStatus.UNLOCKED, capsule.getStatus());
    }

    @Test
    void testCheckAndSendCapsules_handlesExceptionGracefully() {
        // Given
        TimeCapsule capsule = new TimeCapsule();
        capsule.setId("capsule456");
        capsule.setRecipientEmail("error@example.com");
        capsule.setOwnerUsername("owner2");
        capsule.setUnlockDate(Instant.now().minusSeconds(10));
        capsule.setStatus(TimeCapsule.CapsuleStatus.LOCKED);
        capsule.setFileMetadataList(List.of());
        capsule.setTopic("failure");

        when(capsuleRepository.findByUnlockDateBeforeAndStatus(any(), any()))
                .thenReturn(List.of(capsule));
        when(jwtUtils.generateJwtToken("capsule456")).thenReturn("bad-token");
        when(quoteService.getQuote(anyString())).thenReturn("Mock quote");

        // Simulate email failure
        doThrow(new RuntimeException("Email failed")).when(emailService)
                .sendCapsuleUnlockEmail(eq(capsule), anyString(), anyString());

        // When
        assertDoesNotThrow(() -> schedulerService.checkAndSendCapsules());

        // Then
        verify(emailService, times(1)).sendCapsuleUnlockEmail(eq(capsule), anyString(), anyString());
        verify(capsuleRepository, never()).save(any());
    }

    @Test
    void testCheckAndSendCapsules_usesDefaultTopicIfNull() {
        // Given
        TimeCapsule capsule = new TimeCapsule();
        capsule.setId("capsule789");
        capsule.setRecipientEmail("default@example.com");
        capsule.setOwnerUsername("owner3");
        capsule.setUnlockDate(Instant.now().minusSeconds(10));
        capsule.setStatus(TimeCapsule.CapsuleStatus.LOCKED);
        capsule.setFileMetadataList(List.of()); // No files
        capsule.setTopic(null); // null topic

        when(capsuleRepository.findByUnlockDateBeforeAndStatus(any(), any()))
                .thenReturn(List.of(capsule));
        when(jwtUtils.generateJwtToken("capsule789")).thenReturn("token789");

        // When
        schedulerService.checkAndSendCapsules();

        // Then
        verify(quoteService).getQuote("memories"); // default topic fallback
        verify(emailService).sendCapsuleUnlockEmail(eq(capsule), anyString(), anyString());
        verify(capsuleRepository).save(capsule);
        assertEquals(TimeCapsule.CapsuleStatus.UNLOCKED, capsule.getStatus());
    }
}
