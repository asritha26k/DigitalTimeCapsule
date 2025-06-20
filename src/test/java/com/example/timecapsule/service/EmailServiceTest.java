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
        emailService.setFromEmail("noreply@timecapsule.com"); // Inject for test
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

        String link = "https://your-domain.com/unlock?token=abc123";

        emailService.sendCapsuleUnlockEmail(capsule, link);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertEquals("friend@example.com", sentMessage.getTo()[0]);
        assertTrue(sentMessage.getText().contains(link));
        assertEquals("noreply@timecapsule.com", sentMessage.getFrom());
    }
}
