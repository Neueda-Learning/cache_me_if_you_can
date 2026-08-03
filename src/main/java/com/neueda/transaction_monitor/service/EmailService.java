package com.neueda.transaction_monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendAlertEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.debug("EmailService: recipient is empty, skipping send");
            return;
        }
        try {
            log.info("EmailService: queuing email to {} subject={}", to, subject);
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("EmailService: email successfully sent to {}", to);
        } catch (Exception e) {
            // Log and swallow — we don't want email failures to break transaction flow
            log.error("Failed to send alert email to {}: {}", to, e.getMessage(), e);
        }
    }
}


