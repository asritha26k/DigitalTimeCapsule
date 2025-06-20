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
    private final JwtUtils jwtUtils;
    private final QuoteService quoteService;

    public EmailSchedulerService(TimeCapsuleRepository capsuleRepository,
                                 EmailService emailService,
                                 JwtUtils jwtUtils,
                                 QuoteService quoteService) {
        this.capsuleRepository = capsuleRepository;
        this.emailService = emailService;
        this.jwtUtils = jwtUtils;
        this.quoteService = quoteService;
    }

    @Scheduled(fixedRate = 60_000) // Every minute
    public void checkAndSendCapsules() {
        Instant now = Instant.now();
        List<TimeCapsule> dueCapsules = capsuleRepository
                .findByUnlockDateBeforeAndStatus(now, TimeCapsule.CapsuleStatus.LOCKED);

        for (TimeCapsule capsule : dueCapsules) {
            try {
                String token = jwtUtils.generateJwtToken(capsule.getId());
                String secureLink = "https://your-domain.com/capsules/unlock?token=" + token;

                // Use capsule topic, fallback to "memories"
                String topic = capsule.getTopic() != null ? capsule.getTopic() : "memories";
                String quote = quoteService.getQuote(topic);

                // Send email
                emailService.sendCapsuleUnlockEmail(capsule, secureLink, quote);

                capsule.setStatus(TimeCapsule.CapsuleStatus.UNLOCKED);
                capsuleRepository.save(capsule);

            } catch (Exception e) {
                System.err.println("Error sending email for capsule " + capsule.getId() + ": " + e.getMessage());
            }
        }
    }
}
