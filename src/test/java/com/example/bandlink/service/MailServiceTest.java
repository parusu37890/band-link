package com.example.bandlink.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MailServiceTest {
    @Test
    void verificationMailContainsOneTimeLink() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MailService service = new MailService(sender, "smtp.example", "no-reply@bandlink.local", "https://bandlink.example/");

        service.sendVerification("member@example.com", "verification-token");

        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        SimpleMailMessage sent = message.getValue();
        assertTrue(sent.getText().contains("https://bandlink.example/verify-email?token=verification-token"));
        assertTrue(sent.getText().contains("本登録を完了するには"));
    }
}
