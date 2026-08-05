package com.neueda.transaction_monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final Path baseDir;
    private final JavaMailSender javaMailSender;
    private final boolean smtpEnabled;

    public MailService(@Value("${hawk.mail.output-dir:./account-mails}") String baseDir,
                       JavaMailSender javaMailSender,
                       @Value("${spring.mail.host:}") String mailHost) {
        this.baseDir = Paths.get(baseDir);
        this.javaMailSender = javaMailSender;
        this.smtpEnabled = mailHost != null && !mailHost.isBlank();
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            log.warn("Could not create mail base directory {}: {}", this.baseDir, e.getMessage());
        }
    }

    /**
     * Send mail to the account holder if SMTP is configured; otherwise write
     * a mail file into the account folder as a fallback.
     * If recipientEmail is null/blank we fall back to file output.
     */
    public void sendToAccountFolder(String accountNumber, String subject, String body) {
        sendToAccountFolder(accountNumber, null, subject, body);
    }

    public void sendToAccountFolder(String accountNumber, String recipientEmail, String subject, String body) {
        // Prefer SMTP if configured and recipient email provided
        if (smtpEnabled && recipientEmail != null && !recipientEmail.isBlank()) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(recipientEmail);
                msg.setSubject(subject);
                msg.setText(body == null ? "" : body);
                javaMailSender.send(msg);
                log.info("Sent SMTP mail to {} (account {})", recipientEmail, accountNumber);
                return;
            } catch (Exception e) {
                log.warn("SMTP send failed, falling back to file: {}", e.getMessage());
            }
        }

        // Fallback: write file under account folder
        if (accountNumber == null || accountNumber.isBlank()) accountNumber = "unknown";
        Path acct = baseDir.resolve(accountNumber);
        try {
            Files.createDirectories(acct);
            String filename = "mail-" + Instant.now().toEpochMilli() + ".txt";
            Path target = acct.resolve(filename);
            StringBuilder sb = new StringBuilder();
            sb.append("Subject: ").append(subject).append("\n");
            sb.append("Date: ").append(Instant.now().toString()).append("\n\n");
            sb.append(body == null ? "" : body);
            Files.writeString(target, sb.toString(), StandardOpenOption.CREATE_NEW);
            log.info("Wrote mail to {}", target.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write mail for account {}: {}", accountNumber, e.getMessage(), e);
        }
    }
}


