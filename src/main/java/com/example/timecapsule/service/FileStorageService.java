package com.example.timecapsule.service;

import com.example.timecapsule.model.FileMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public FileMetadata storeFile(MultipartFile file) throws IOException {
        // Generate a unique filename to avoid conflicts
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // Make sure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save the file locally
        Path filePath = uploadPath.resolve(uniqueFileName);
        file.transferTo(filePath.toFile());

        // Prepare metadata to return
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(uniqueFileName);
        metadata.setOriginalName(originalFilename);
        metadata.setContentType(file.getContentType());
        metadata.setSize(file.getSize());
        metadata.setStoragePath(filePath.toString());

        return metadata;
    }

    public FileMetadata saveFile(MultipartFile file) {
        try {
            return storeFile(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + file.getOriginalFilename(), e);
        }
    }

    public File loadFile(String fileName) {
        Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
        File file = filePath.toFile();
        if (file.exists() && file.isFile()) {
            return file;
        } else {
            throw new RuntimeException("File not found: " + fileName);
        }
    }

    // NEW: Method to delete a file from storage
    public void deleteFile(String fileName) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
        File file = filePath.toFile();
        if (file.exists() && file.isFile()) {
            Files.delete(filePath); // Delete the file
        } else {
            // Log this if you want, but don't necessarily throw an error if the file is already gone
            // This can happen if a capsule was manually cleaned up, etc.
            System.out.println("Warning: Attempted to delete non-existent file: " + fileName);
        }
    }
}