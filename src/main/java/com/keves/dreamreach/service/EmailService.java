package com.keves.dreamreach.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:build-test@example.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Verification Code - Dreamreach");
            helper.setFrom(fromEmail);

            String htmlContent = generateVerificationHtml(code);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Verification email dispatched successfully to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Critical: Failed to dispatch verification email to {}. Error: {}", toEmail, e.getMessage());
        }
    }

    private String generateVerificationHtml(String code) {
        return String.format(
                "<div style='font-family: sans-serif; background: #0f172a; padding: 40px; color: #ffffff;'>" +
                        "  <div style='max-width: 600px; margin: auto; background: #1e293b; padding: 30px; border-radius: 12px; border: 1px solid #334155;'>" +
                        "    <h2 style='color: #3b82f6; margin-top: 0;'>Secure Access</h2>" +
                        "    <p style='color: #94a3b8;'>Please use the following single-use code to verify your account:</p>" +
                        "    <div style='background: #0f172a; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0; border: 1px solid #3b82f6;'>" +
                        "      <span style='font-size: 2.5rem; font-weight: bold; letter-spacing: 8px; color: #60a5fa;'>%s</span>" +
                        "    </div>" +
                        "    <p style='color: #64748b; font-size: 0.8rem;'>This code expires in 15 minutes. If you did not request this, no action is required.</p>" +
                        "  </div>" +
                        "</div>", code);
    }
}