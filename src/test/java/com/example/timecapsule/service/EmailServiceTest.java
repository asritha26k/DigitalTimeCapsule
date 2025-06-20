package com.example.timecapsule.service;

import com.example.timecapsule.model.TimeCapsule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailService = new EmailService(mailSender);
        emailService.setFromEmail("noreply@timecapsule.com"); // Set test email
    }

    @Test
    void testSendSimpleMessage_success() {
        String recipient = "test@example.com";
        String subject = "Hello";
        String body = "This is a test email.";

        emailService.sendSimpleMessage(recipient, subject, body);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertEquals(recipient, sentMessage.getTo()[0]);
        assertEquals(subject, sentMessage.getSubject());
        assertEquals(body, sentMessage.getText());
        assertEquals("noreply@timecapsule.com", sentMessage.getFrom());
    }

    @Test
    void testSendCapsuleUnlockEmail_success() {
        TimeCapsule capsule = new TimeCapsule();
        capsule.setRecipientEmail("friend@example.com");
        capsule.setOwnerUsername("owner123");

        String link = "http://example.com/unlock";
        String quote = "Mock quote about memories.";

        emailService.sendCapsuleUnlockEmail(capsule, link, quote);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("friend@example.com", message.getTo()[0]);
        assertTrue(message.getText().contains(link), "Email body should contain the unlock link");
        assertTrue(message.getText().contains(quote), "Email body should contain the quote");
        assertTrue(message.getText().contains("owner123"), "Email body should mention the sender/owner");
        assertEquals("noreply@timecapsule.com", message.getFrom());
        assertEquals("🎁 Your Digital Time Capsule Is Ready!", message.getSubject());
    }
}
