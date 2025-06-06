package com.example.timecapsule.scheduler;

import com.example.timecapsule.model.TimeCapsule;
import com.example.timecapsule.repository.TimeCapsuleRepository;
import com.example.timecapsule.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID; // Import UUID for generating tokens

@Component
public class CapsuleUnlockScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CapsuleUnlockScheduler.class);

    private final TimeCapsuleRepository capsuleRepository;
    private final EmailService emailService;

    public CapsuleUnlockScheduler(TimeCapsuleRepository capsuleRepository, EmailService emailService) {
        this.capsuleRepository = capsuleRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 3600000) // Running every 10 seconds for testing
    public void unlockAndNotify() {
        logger.info("Running scheduled task: Checking for capsules to unlock...");

        List<TimeCapsule> readyCapsules = capsuleRepository
                .findByUnlockDateBeforeAndStatus(Instant.now(), TimeCapsule.CapsuleStatus.LOCKED);

        if (readyCapsules.isEmpty()) {
            logger.info("No capsules found ready to unlock.");
            return;
        }

        logger.info("Found {} capsules ready to unlock.", readyCapsules.size());

        for (TimeCapsule capsule : readyCapsules) {
            try {
                // Generate a unique public access token
                String publicAccessToken = UUID.randomUUID().toString();
                capsule.setPublicAccessToken(publicAccessToken); // Set the new token

                // Update capsule status to UNLOCKED and save
                capsule.setStatus(TimeCapsule.CapsuleStatus.UNLOCKED);
                capsuleRepository.save(capsule);
                logger.info("Capsule ID {} status updated to UNLOCKED. Public Access Token generated.", capsule.getId());

                // Construct the link for the email. This links to a hypothetical FRONTEND route
                // where the recipient can view the capsule contents.
                // You will need a frontend application (e.g., React, Angular, Vue) running on a specific port
                // that consumes the /api/capsules/public-view/{accessToken} endpoint.
                // Replace 'http://localhost:3000' with the actual URL of your frontend application.
                String publicViewerLink = String.format("http://localhost:3000/capsules/view/%s", publicAccessToken);


                String subject = "🎁 Your Digital Time Capsule Is Ready!";
                String body = String.format(
                        "Hi %s,\n\nYour digital time capsule from %s is now unlocked and ready to access! " +
                                "Click the link below to view your memories:\n\n%s\n\nEnjoy!",
                        capsule.getRecipientEmail(),
                        capsule.getOwnerUsername(),
                        publicViewerLink
                );

                emailService.sendSimpleMessage(capsule.getRecipientEmail(), subject, body);
                logger.info("Unlock notification email sent to {} for capsule ID {}.", capsule.getRecipientEmail(), capsule.getId());

            } catch (Exception e) {
                logger.error("Error processing capsule ID {} for unlock: {}", capsule.getId(), e.getMessage(), e);
                // Optionally, add logic to retry or flag this capsule for manual review
            }
        }
    }
}