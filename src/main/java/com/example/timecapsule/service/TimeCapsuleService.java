package com.example.timecapsule.service;


import com.example.timecapsule.model.FileMetadata;
import com.example.timecapsule.model.TimeCapsule;
import com.example.timecapsule.repository.TimeCapsuleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TimeCapsuleService {

    private final TimeCapsuleRepository repository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public TimeCapsuleService(TimeCapsuleRepository repository) {
        this.repository = repository;
    }
    public void setUploadDir(String uploadDir) { // this is for the usage of test method
        this.uploadDir = uploadDir;
    }


    public TimeCapsule createCapsule(String ownerUserId, String recipientEmail, Instant unlockDate, List<MultipartFile> files, String topic) throws IOException{
        List<FileMetadata> metadataList = new ArrayList<>();

        String folderName = "capsule_" + System.currentTimeMillis();
        Path folderPath = Paths.get(uploadDir, folderName);
        Files.createDirectories(folderPath);

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            if (fileName == null) fileName = "unknown";

            Path filePath = folderPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            FileMetadata metadata = new FileMetadata();
            metadata.setFileName(fileName);
            metadata.setContentType(file.getContentType());
            metadata.setSize(file.getSize());
            metadata.setStoragePath(filePath.toString());

            metadataList.add(metadata);
        }

        TimeCapsule capsule = new TimeCapsule();
        capsule.setOwnerUsername(ownerUserId);
        capsule.setRecipientEmail(recipientEmail);
        capsule.setUnlockDate(unlockDate);
        capsule.setFileMetadataList(metadataList);
        capsule.setTopic(topic); // 👈 NEW
        capsule.setStatus(TimeCapsule.CapsuleStatus.LOCKED);

        return repository.save(capsule);
    }

}
