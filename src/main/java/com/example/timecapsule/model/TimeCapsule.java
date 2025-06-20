package com.example.timecapsule.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "timecapsules")
public class TimeCapsule {

    @Id
    private String id;

    private String ownerUsername;

    private String recipientEmail;

    private Instant unlockDate;

    private CapsuleStatus status;

    private List<FileMetadata> fileMetadataList;

    private Instant createdAt;

    private String publicAccessToken; // New field 1

    private String title; // New field 2
    private String topic; // e.g. "friendship", "gratitude", etc.

    public enum CapsuleStatus {
        LOCKED,
        UNLOCKED
    }
    public String getTopic() {
        return topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }



    public TimeCapsule() {
        this.createdAt = Instant.now();
        this.status = CapsuleStatus.LOCKED;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public Instant getUnlockDate() {
        return unlockDate;
    }

    public void setUnlockDate(Instant unlockDate) {
        this.unlockDate = unlockDate;
    }

    public CapsuleStatus getStatus() {
        return status;
    }

    public void setStatus(CapsuleStatus status) {
        this.status = status;
    }

    public List<FileMetadata> getFileMetadataList() {
        return fileMetadataList;
    }

    public void setFileMetadataList(List<FileMetadata> fileMetadataList) {
        this.fileMetadataList = fileMetadataList;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getPublicAccessToken() {
        return publicAccessToken;
    }

    public void setPublicAccessToken(String publicAccessToken) {
        this.publicAccessToken = publicAccessToken;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
