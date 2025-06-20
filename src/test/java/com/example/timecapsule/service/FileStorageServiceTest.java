package com.example.timecapsule.service;

import com.example.timecapsule.model.FileMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempUploadDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        fileStorageService.setUploadDir(tempUploadDir.toString());
    }

    @Test
    void testSaveFile_success() {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "Hello World!".getBytes()
        );

        // When
        FileMetadata metadata = fileStorageService.saveFile(mockFile);

        // Then
        assertNotNull(metadata);
        assertEquals("hello.txt", metadata.getOriginalName());
        assertEquals("text/plain", metadata.getContentType());
        assertNotNull(metadata.getFileName());
        assertNotNull(metadata.getStoragePath());

        File savedFile = new File(metadata.getStoragePath());
        assertTrue(savedFile.exists());
        assertTrue(savedFile.length() > 0);
    }

    @Test
    void testLoadFile_success() {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "loadme.txt", "text/plain", "Test Content".getBytes()
        );

        FileMetadata metadata = fileStorageService.saveFile(mockFile);

        // When
        File loadedFile = fileStorageService.loadFile(metadata.getFileName());

        // Then
        assertTrue(loadedFile.exists());
        assertEquals(metadata.getFileName(), loadedFile.getName());
        assertTrue(loadedFile.length() > 0);
    }

    @Test
    void testDeleteFile_success() throws IOException {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "deleteme.txt", "text/plain", "To Be Deleted".getBytes()
        );

        FileMetadata metadata = fileStorageService.saveFile(mockFile);
        File fileBeforeDelete = new File(metadata.getStoragePath());
        assertTrue(fileBeforeDelete.exists());

        // When
        fileStorageService.deleteFile(metadata.getFileName());

        // Then
        assertFalse(fileBeforeDelete.exists());
    }
}
