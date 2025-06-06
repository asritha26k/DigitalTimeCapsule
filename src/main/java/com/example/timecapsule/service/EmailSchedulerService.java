package com.example.timecapsule.service;

import com.example.timecapsule.model.TimeCapsule;
import com.example.timecapsule.repository.TimeCapsuleRepository;
import com.example.timecapsule.security.JwtUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class EmailSchedulerService {

    private final TimeCapsuleRepository capsuleRepository;
    private final EmailService emailService;
    private final JwtUtils jwtUtils; // To create secure tokens

    public EmailSchedulerService(TimeCapsuleRepository capsuleRepository, EmailService emailService, JwtUtils jwtUtils) {
        this.capsuleRepository = capsuleRepository;
        this.emailService = emailService;
        this.jwtUtils = jwtUtils;
    }

    // Run every minute (for testing)
    @Scheduled(fixedRate = 60_000)
    public void checkAndSendCapsules() {
        Instant now = Instant.now();
        List<TimeCapsule> dueCapsules = capsuleRepository
                .findByUnlockDateBeforeAndStatus(now, TimeCapsule.CapsuleStatus.LOCKED);

        for (TimeCapsule capsule : dueCapsules) {
            try {
                // Generate a secure token for this capsule ID (token expires after some time)
                String token = jwtUtils.generateJwtToken(capsule.getId());

                // Build secure link with token as query param
                String secureLink = "https://your-domain.com/capsules/unlock?token=" + token;

                // Send email with the secure link
                emailService.sendCapsuleUnlockEmail(capsule, secureLink);

                // Update status to unlocked
                capsule.setStatus(TimeCapsule.CapsuleStatus.UNLOCKED);
                capsuleRepository.save(capsule);

            } catch (Exception e) {
                System.err.println("Error sending email for capsule " + capsule.getId() + ": " + e.getMessage());
            }
        }
    }
}
