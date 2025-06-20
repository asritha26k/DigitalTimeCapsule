package com.example.timecapsule.service;

import com.example.timecapsule.model.TimeCapsule;
import com.example.timecapsule.repository.TimeCapsuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimeCapsuleServiceTest {

    private TimeCapsuleRepository repository;
    private TimeCapsuleService service;

    @BeforeEach
    void setUp() {
        repository = mock(TimeCapsuleRepository.class);
        service = new TimeCapsuleService(repository);

        // Create a temporary directory for test file storage
        String tempDir = System.getProperty("java.io.tmpdir");
        service.setUploadDir(tempDir); // Inject the uploadDir manually
    }

    @Test
    void testCreateCapsule_withFiles_success() throws IOException {
        // Given
        String owner = "user123";
        String recipient = "recipient@example.com";
        Instant unlockTime = Instant.now().plusSeconds(3600);

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello Capsule!".getBytes()
        );

        when(repository.save(any(TimeCapsule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TimeCapsule capsule = service.createCapsule(owner, recipient, unlockTime, List.of(mockFile));

        // Then
        assertEquals(owner, capsule.getOwnerUsername());
        assertEquals(recipient, capsule.getRecipientEmail());
        assertEquals(1, capsule.getFileMetadataList().size());
        assertEquals("test.txt", capsule.getFileMetadataList().get(0).getFileName());

        verify(repository, times(1)).save(any(TimeCapsule.class));
    }
}
