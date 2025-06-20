package com.example.timecapsule.service;

import com.example.timecapsule.model.TimeCapsule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }


    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendCapsuleUnlockEmail(TimeCapsule capsule, String secureLink, String quote) {
        String recipient = capsule.getRecipientEmail();
        String subject = "🎁 A Digital Time Capsule Just Arrived!";

        String body = """
        Hello!

        You’ve received a digital time capsule from the past 🎉.
        Click below to view the contents:

        %s

        %s

        Best,
        Time Capsule Team
        """.formatted(secureLink, (quote != null && !quote.isBlank()) ? "\n✨ Quote:\n" + quote : "");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient);
        message.setFrom(fromEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }


    public void sendSimpleMessage(String recipientEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setFrom(fromEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
