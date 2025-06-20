package com.example.timecapsule.scheduler;

import com.example.timecapsule.model.TimeCapsule;
import com.example.timecapsule.repository.TimeCapsuleRepository;
import com.example.timecapsule.service.EmailService;
import com.example.timecapsule.service.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class CapsuleUnlockScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CapsuleUnlockScheduler.class);

    private final TimeCapsuleRepository capsuleRepository;
    private final EmailService emailService;
    private final QuoteService quoteService;

    public CapsuleUnlockScheduler(TimeCapsuleRepository capsuleRepository,
                                  EmailService emailService,
                                  QuoteService quoteService) {
        this.capsuleRepository = capsuleRepository;
        this.emailService = emailService;
        this.quoteService = quoteService;
    }

    @Scheduled(fixedRate = 10000) // Every 10 seconds
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
                // Generate public access token and update status
                String publicAccessToken = UUID.randomUUID().toString();
                capsule.setPublicAccessToken(publicAccessToken);
                capsule.setStatus(TimeCapsule.CapsuleStatus.UNLOCKED);
                capsuleRepository.save(capsule);

                logger.info("Capsule ID {} unlocked. Token: {}", capsule.getId(), publicAccessToken);

                // Build frontend link
                String publicViewerLink = String.format("http://localhost:3000/capsules/view/%s", publicAccessToken);

                // Fetch quote based on topic
                String quote;
                String topic = capsule.getTopic() != null ? capsule.getTopic() : "life";
                try {
                    quote = quoteService.getQuote(topic);
                    logger.info("Quote for topic '{}': {}", topic, quote);
                } catch (Exception e) {
                    quote = "“Cherish yesterday, dream tomorrow, live today.”";
                    logger.warn("Failed to fetch quote from Gemini: {}", e.getMessage());
                }

                // Compose the email
                String subject = "🎁 Your Digital Time Capsule Is Ready!";
                String body = String.format(
                        "Hi %s,\n\nYour digital time capsule from %s is now unlocked and ready to access! " +
                                "Click the link below to view your memories:\n\n%s\n\n" +
                                "✨ Quote on '%s' ✨\n%s\n\nEnjoy!",
                        capsule.getRecipientEmail(),
                        capsule.getOwnerUsername(),
                        publicViewerLink,
                        topic,
                        quote
                );

                // Send the email
                emailService.sendSimpleMessage(capsule.getRecipientEmail(), subject, body);
                logger.info("Email sent to {}", capsule.getRecipientEmail());

            } catch (Exception e) {
                logger.error("Failed to process capsule {}: {}", capsule.getId(), e.getMessage(), e);
            }
        }
    }
}
