package com.example.timecapsule.controller;

import com.example.timecapsule.model.FileMetadata;
import com.example.timecapsule.model.TimeCapsule;
import com.example.timecapsule.repository.TimeCapsuleRepository;
import com.example.timecapsule.security.JwtUtils;
import com.example.timecapsule.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/capsules")
public class CapsuleController {

    private final FileStorageService fileStorageService;
    private final TimeCapsuleRepository capsuleRepository;
    private final JwtUtils jwtUtils;

    public CapsuleController(FileStorageService fileStorageService,
                             TimeCapsuleRepository capsuleRepository,
                             JwtUtils jwtUtils) {
        this.fileStorageService = fileStorageService;
        this.capsuleRepository = capsuleRepository;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/{capsuleId}/upload")
    public ResponseEntity<?> uploadFiles(@PathVariable String capsuleId,
                                         @RequestParam("files") MultipartFile[] files,
                                         HttpServletRequest request) {

        String username = jwtUtils.getUsernameFromRequest(request); // Corrected
        Optional<TimeCapsule> optional = capsuleRepository.findById(capsuleId);

        if (optional.isEmpty()) return ResponseEntity.notFound().build();

        TimeCapsule capsule = optional.get();
        if (!capsule.getOwnerUsername().equals(username)) {
            return ResponseEntity.status(403).body("Unauthorized to upload to this capsule");
        }

        List<FileMetadata> metadataList = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                FileMetadata metadata = fileStorageService.storeFile(file);
                metadataList.add(metadata);
            }
            if (capsule.getFileMetadataList() == null) {
                capsule.setFileMetadataList(new ArrayList<>());
            }
            capsule.getFileMetadataList().addAll(metadataList);
            capsuleRepository.save(capsule);
            return ResponseEntity.ok("Files uploaded successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Create a new time capsule",
            description = "Creates a new time capsule with associated files, recipient, and unlock date. " +
                    "Unlock date can be provided as 'YYYY-MM-DD' (defaults to end of day UTC) " +
                    "or 'YYYY-MM-DDTHH:mm:ss' (defaults to UTC timezone)."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCapsule(
            @RequestPart("recipientEmail") String recipientEmail,
            @RequestPart("unlockDate") String unlockDateStr,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "topic", required = false) String topic,
            @RequestPart("files") List<MultipartFile> files,
            HttpServletRequest request) {

        String username = jwtUtils.getUsernameFromRequest(request);
        Instant unlockDate;
        ZoneId istZone = ZoneId.of("Asia/Kolkata");

        try {
            if (unlockDateStr.contains("T")) {
                // Full date-time like 2025-06-20T19:45:00
                LocalDateTime localDateTime = LocalDateTime.parse(unlockDateStr);
                unlockDate = localDateTime.atZone(istZone).toInstant();
            } else {
                // Just date like 2025-06-20
                LocalDate localDate = LocalDate.parse(unlockDateStr);
                LocalDateTime endOfDay = LocalDateTime.of(localDate, LocalTime.MAX);
                unlockDate = endOfDay.atZone(istZone).toInstant();
            }
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid unlockDate format. Please use 'YYYY-MM-DD' or 'YYYY-MM-DDTHH:mm:ss'.");
        }

        List<FileMetadata> metadataList = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    FileMetadata metadata = fileStorageService.storeFile(file);
                    metadataList.add(metadata);
                } catch (IOException e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error saving file: " + file.getOriginalFilename());
                }
            }
        }

        TimeCapsule capsule = new TimeCapsule();
        capsule.setOwnerUsername(username);
        capsule.setRecipientEmail(recipientEmail);
        capsule.setUnlockDate(unlockDate);
        capsule.setFileMetadataList(metadataList);
        capsule.setStatus(TimeCapsule.CapsuleStatus.LOCKED);
        if (title != null && !title.isEmpty()) {
            capsule.setTitle(title);
        }
        if (topic != null && !topic.isEmpty()) {
            capsule.setTopic(topic);
        }

        capsuleRepository.save(capsule);

        return ResponseEntity.status(HttpStatus.CREATED).body("Time capsule created successfully.");
    }



    @PutMapping("/{capsuleId}")
    public ResponseEntity<?> updateCapsule(@PathVariable String capsuleId,
                                           @RequestBody Map<String, String> updates,
                                           HttpServletRequest request) {
        String username = jwtUtils.getUsernameFromRequest(request); // Corrected
        Optional<TimeCapsule> optional = capsuleRepository.findById(capsuleId);

        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TimeCapsule capsule = optional.get();

        if (!capsule.getOwnerUsername().equals(username)) {
            return ResponseEntity.status(403).body("Access denied: You are not the owner of this capsule.");
        }

        updates.forEach((key, value) -> {
            switch (key) {
                case "recipientEmail":
                    capsule.setRecipientEmail(value);
                    break;
                case "unlockDate":
                    try {
                        if (value.contains("T")) {
                            LocalDateTime localDateTime = LocalDateTime.parse(value);
                            capsule.setUnlockDate(localDateTime.toInstant(ZoneOffset.UTC));
                        } else {
                            LocalDate localDate = LocalDate.parse(value);
                            LocalDateTime endOfDay = LocalDateTime.of(localDate, LocalTime.MAX);
                            capsule.setUnlockDate(endOfDay.toInstant(ZoneOffset.UTC));
                        }
                    } catch (DateTimeParseException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid unlockDate format. Use 'YYYY-MM-DD' or 'YYYY-MM-DDTHH:mm:ss'.");
                    }
                    break;
                case "title":
                    capsule.setTitle(value);
                    break;
            }
        });

        capsuleRepository.save(capsule);
        return ResponseEntity.ok("Capsule updated successfully.");
    }

    @DeleteMapping("/{capsuleId}")
    public ResponseEntity<?> deleteCapsule(@PathVariable String capsuleId,
                                           HttpServletRequest request) {
        String username = jwtUtils.getUsernameFromRequest(request); // Corrected
        Optional<TimeCapsule> optional = capsuleRepository.findById(capsuleId);

        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TimeCapsule capsule = optional.get();

        if (!capsule.getOwnerUsername().equals(username)) {
            return ResponseEntity.status(403).body("Access denied: You are not the owner of this capsule.");
        }

        try {
            if (capsule.getFileMetadataList() != null) {
                for (FileMetadata metadata : capsule.getFileMetadataList()) {
                    fileStorageService.deleteFile(metadata.getFileName());
                }
            }
            capsuleRepository.delete(capsule);
            return ResponseEntity.ok("Capsule and its files deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete capsule or its files: " + e.getMessage());
        }
    }

    @DeleteMapping("/{capsuleId}/files/{fileName}")
    public ResponseEntity<?> deleteFileFromCapsule(@PathVariable String capsuleId,
                                                   @PathVariable String fileName,
                                                   HttpServletRequest request) {
        String username = jwtUtils.getUsernameFromRequest(request); // Corrected
        Optional<TimeCapsule> optional = capsuleRepository.findById(capsuleId);

        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TimeCapsule capsule = optional.get();

        if (!capsule.getOwnerUsername().equals(username)) {
            return ResponseEntity.status(403).body("Access denied: You are not the owner of this capsule.");
        }

        Optional<FileMetadata> fileToDelete = capsule.getFileMetadataList().stream()
                .filter(fm -> fm.getFileName().equals(fileName))
                .findFirst();

        if (fileToDelete.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found in this capsule.");
        }

        try {
            fileStorageService.deleteFile(fileName);
            capsule.getFileMetadataList().removeIf(fm -> fm.getFileName().equals(fileName));
            capsuleRepository.save(capsule);

            return ResponseEntity.ok("File '" + fileToDelete.get().getOriginalName() + "' deleted successfully from capsule.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete file: " + e.getMessage());
        }
    }

    @GetMapping("/public-view/{accessToken}")
    public ResponseEntity<?> getPublicCapsuleMetadata(@PathVariable String accessToken) {
        try {
            TimeCapsule capsule = capsuleRepository.findByPublicAccessToken(accessToken)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid access token or capsule not found."));

            if (capsule.getStatus() != TimeCapsule.CapsuleStatus.UNLOCKED) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Capsule is not unlocked yet.", "unlockDate", capsule.getUnlockDate()));
            }

            return ResponseEntity.ok(capsule);

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving public capsule metadata: " + e.getMessage());
        }
    }

    @GetMapping("/public-download/{accessToken}/{fileName}")
    public ResponseEntity<?> publicDownloadFile(
            @PathVariable String accessToken,
            @PathVariable String fileName) {
        try {
            TimeCapsule capsule = capsuleRepository.findByPublicAccessToken(accessToken)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid access token or capsule not found."));

            if (capsule.getStatus() != TimeCapsule.CapsuleStatus.UNLOCKED) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Capsule is not unlocked yet.", "unlockDate", capsule.getUnlockDate()));
            }

            Optional<FileMetadata> fileMetadataOptional = capsule.getFileMetadataList().stream()
                    .filter(fm -> fm.getFileName().equals(fileName))
                    .findFirst();

            if (fileMetadataOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            File file = fileStorageService.loadFile(fileName);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = MediaType.parseMediaType(fileMetadataOptional.get().getContentType());
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileMetadataOptional.get().getOriginalName() + "\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(mediaType)
                    .body(new FileSystemResource(file));

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<TimeCapsule>> getUserCapsules(HttpServletRequest request) {
        String username = jwtUtils.getUsernameFromRequest(request); // Corrected
        List<TimeCapsule> capsules = capsuleRepository.findByOwnerUsername(username);
        return ResponseEntity.ok(capsules);
    }

    @GetMapping("/{capsuleId}")
    public ResponseEntity<?> getCapsuleById(@PathVariable String capsuleId,
                                            HttpServletRequest request) {
        String username = jwtUtils.getUsernameFromRequest(request); // Corrected
        Optional<TimeCapsule> optional = capsuleRepository.findById(capsuleId);

        if (optional.isEmpty()) return ResponseEntity.notFound().build();

        TimeCapsule capsule = optional.get();

        boolean isOwner = capsule.getOwnerUsername().equals(username);
        boolean isRecipient = capsule.getRecipientEmail().equalsIgnoreCase(username);

        if (!isOwner && !isRecipient) {
            return ResponseEntity.status(403).body("Access denied: You are neither the owner nor the recipient.");
        }

        Instant now = Instant.now();
        if (capsule.getStatus() == TimeCapsule.CapsuleStatus.LOCKED && capsule.getUnlockDate().isAfter(now)) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Capsule is locked.",
                    "unlockDate", capsule.getUnlockDate(),
                    "message", "This capsule will be accessible on " + capsule.getUnlockDate()
            ));
        }

        return ResponseEntity.ok(capsule);
    }
}