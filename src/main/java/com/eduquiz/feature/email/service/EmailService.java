package com.eduquiz.feature.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * Gửi email HTML chứa mã OTP sử dụng Thymeleaf template.
     */
    public void sendOtpEmail(String toEmail, String subject, String title, String otpCode, String fullName, String verifyUrl) {
        try {
            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("title", title);
            context.setVariable("otpCode", otpCode);
            context.setVariable("verifyUrl", verifyUrl);

            String htmlContent = templateEngine.process("otp-email", context);

            // Build & send email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email OTP. Vui lòng thử lại sau.", e);
        }
    }
}
