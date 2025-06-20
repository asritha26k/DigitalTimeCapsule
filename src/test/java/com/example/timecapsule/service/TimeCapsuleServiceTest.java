package com.example.timecapsule.service;

import com.example.timecapsule.model.TimeCapsule;
import com.example.timecapsule.repository.TimeCapsuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimeCapsuleServiceTest {

    private TimeCapsuleRepository repository;
    private TimeCapsuleService service;

    @TempDir
    Path tempUploadDir;

    @BeforeEach
    void setUp() {
        repository = mock(TimeCapsuleRepository.class);
        service = new TimeCapsuleService(repository);

        // Ensure upload directory is created before usage
        service.setUploadDir(tempUploadDir.toString());
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
        TimeCapsule capsule = service.createCapsule(owner, recipient, unlockTime, List.of(mockFile), "Test Topic");

        // Then
        assertNotNull(capsule);
        assertEquals(owner, capsule.getOwnerUsername());
        assertEquals(recipient, capsule.getRecipientEmail());
        assertEquals("Test Topic", capsule.getTopic());
        assertEquals(1, capsule.getFileMetadataList().size());

        var metadata = capsule.getFileMetadataList().get(0);
        assertEquals("test.txt", metadata.getOriginalName());
        assertEquals("text/plain", metadata.getContentType());

        // Ensure the file was saved to disk
        File savedFile = new File(metadata.getStoragePath());
        assertTrue(savedFile.exists(), "Stored file does not exist at expected path.");
        assertTrue(savedFile.length() > 0, "Stored file is empty.");

        verify(repository, times(1)).save(any(TimeCapsule.class));
    }
}
