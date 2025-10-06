package com.example.demo.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private final JavaMailSender emailSender;

    public EmailService(final JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.setFrom("EspacioVirtual.ICEP@gmail.com");
            
            emailSender.send(message);
            System.out.println("📧 Email enviado exitosamente a: " + to);
            
        } catch (Exception e) {
            System.out.println("❌ Error enviando email: " + e.getMessage());
            throw new RuntimeException("Error enviando email", e);
        }
    }
}