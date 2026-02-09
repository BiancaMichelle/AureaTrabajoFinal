package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private final JavaMailSender emailSender;
    private final InstitutoService institutoService;
    private final String defaultFrom;

    public EmailService(final JavaMailSender emailSender,
                        final InstitutoService institutoService,
                        @Value("${spring.mail.username}") String defaultFrom) {
        this.emailSender = emailSender;
        this.institutoService = institutoService;
        this.defaultFrom = defaultFrom;
    }

    private String resolveFromAddress() {
        try {
            var instituto = institutoService.obtenerInstituto();
            if (instituto != null && instituto.getEmail() != null && !instituto.getEmail().trim().isEmpty()) {
                return instituto.getEmail().trim();
            }
        } catch (Exception e) {
            System.out.println("⚠️ No se pudo obtener el email del instituto, usando el configurado: " + e.getMessage());
        }
        return defaultFrom;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.setFrom(resolveFromAddress());

            emailSender.send(message);
            System.out.println("📧 Email enviado exitosamente a: " + to);

        } catch (Exception e) {
            System.out.println("❌ Error enviando email: " + e.getMessage());
            throw new RuntimeException("Error enviando email", e);
        }
    }

    public void sendEmailWithAttachment(String to, String subject, String body, byte[] attachmentData, String attachmentName) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.setFrom(resolveFromAddress());

            if (attachmentData != null && attachmentData.length > 0) {
                helper.addAttachment(attachmentName, new ByteArrayResource(attachmentData));
            }

            emailSender.send(message);
            System.out.println("📧 Email con adjunto enviado exitosamente a: " + to);

        } catch (Exception e) {
            System.out.println("❌ Error enviando email con adjunto: " + e.getMessage());
            throw new RuntimeException("Error enviando email con adjunto", e);
        }
    }
}
