package com.example.timecapsule.service;

import com.example.timecapsule.model.FileMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private String testUploadDir;

    @BeforeEach
    void setUp() throws IOException {
        fileStorageService = new FileStorageService();

        // Set a temporary directory for testing
        testUploadDir = Files.createTempDirectory("file-storage-test").toString();
        fileStorageService.setUploadDir(testUploadDir);
    }

    @Test
    void testSaveFile_success() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "Hello World!".getBytes()
        );

        FileMetadata metadata = fileStorageService.saveFile(mockFile);

        assertNotNull(metadata);
        assertEquals("hello.txt", metadata.getOriginalName());
        assertTrue(new File(metadata.getStoragePath()).exists());
    }

    @Test
    void testLoadFile_success() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "loadme.txt", "text/plain", "Test Content".getBytes()
        );

        FileMetadata metadata = fileStorageService.saveFile(mockFile);
        File loadedFile = fileStorageService.loadFile(metadata.getFileName());

        assertTrue(loadedFile.exists());
        assertEquals(metadata.getFileName(), loadedFile.getName());
    }

    @Test
    void testDeleteFile_success() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "deleteme.txt", "text/plain", "To Be Deleted".getBytes()
        );

        FileMetadata metadata = fileStorageService.saveFile(mockFile);
        File fileBeforeDelete = new File(metadata.getStoragePath());
        assertTrue(fileBeforeDelete.exists());

        fileStorageService.deleteFile(metadata.getFileName());

        assertFalse(fileBeforeDelete.exists());
    }
}

