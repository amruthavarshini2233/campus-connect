package com.campusconnect.campusconnect.service;

import com.campusconnect.campusconnect.Event;
import com.campusconnect.campusconnect.model.EventRegistration;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled = false;

    @Value("${app.mail.sender-email:noreply@campusconnect.edu}")
    private String senderEmail = "noreply@campusconnect.edu";

    @Value("${app.mail.sender-name:CampusConnect Team}")
    private String senderName = "CampusConnect Team";

    @Value("${spring.mail.username:}")
    private String mailUsername = "";

    @Value("${spring.mail.password:}")
    private String mailPassword = "";

    @Value("${spring.mail.host:localhost}")
    private String mailHost = "localhost";

    @Value("${spring.mail.port:587}")
    private int mailPort = 587;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    private String getEffectiveSenderEmail() {
        if (senderEmail == null || senderEmail.isBlank()) {
            if (mailUsername != null && !mailUsername.isBlank()) {
                return mailUsername.trim();
            }
            return "noreply@campusconnect.edu";
        }
        if (mailUsername != null && !mailUsername.isBlank() && senderEmail.equalsIgnoreCase("noreply@campusconnect.edu")) {
            return mailUsername.trim();
        }
        return senderEmail.trim();
    }

    @Override
    public boolean isMailEnabled() {
        return this.mailEnabled;
    }

    @Override
    public String getConfiguredHost() {
        return this.mailHost;
    }

    @Override
    public int getConfiguredPort() {
        return this.mailPort;
    }

    @Override
    public String getConfiguredSenderEmail() {
        return getEffectiveSenderEmail();
    }

    @Override
    public boolean isCredentialsConfigured() {
        return mailUsername != null && !mailUsername.isBlank() && mailPassword != null && !mailPassword.isBlank();
    }

    @Override
    public EmailDeliveryResult sendRegistrationConfirmation(EventRegistration registration) {
        if (registration == null || registration.getEvent() == null) {
            return EmailDeliveryResult.failed("Invalid registration details provided for email dispatch.");
        }

        Event event = registration.getEvent();
        String studentName = registration.getStudentName() != null ? registration.getStudentName() : "Student";
        String eventTitle = event.getTitle() != null ? event.getTitle() : "College Event";
        String eventDate = event.getEventDate() != null ? event.getEventDate().format(DATE_FORMATTER) : "TBD";
        String eventTime = event.getStartTime() != null ? event.getStartTime().format(TIME_FORMATTER) : "Check schedule";
        String venue = event.getVenue() != null ? event.getVenue() : "Campus Venue";
        String rollNumber = registration.getRollNumber() != null ? registration.getRollNumber() : "N/A";
        String studentEmail = registration.getStudentEmail();
        String refId = registration.getId() != null ? "CC-REG-" + registration.getId() : "CC-REG-PENDING";

        String plainText = buildPlainTextEmail(studentName, eventTitle, eventDate, eventTime, venue, rollNumber, studentEmail, refId);
        String htmlContent = buildHtmlEmail(studentName, eventTitle, eventDate, eventTime, venue, rollNumber, studentEmail, refId);

        if (!mailEnabled) {
            log.info("[SIMULATED EMAIL] To: {} | Subject: CampusConnect — Event Registration Successful | Host: {} (Disabled in config)",
                    studentEmail, mailHost);
            log.info("[SIMULATED EMAIL BODY]\n{}", plainText);
            return EmailDeliveryResult.simulated("Email simulated in local dev mode (Set APP_MAIL_ENABLED=true with SMTP to send real emails).");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender bean is unavailable on classpath. Cannot send to {}", studentEmail);
            return EmailDeliveryResult.failed("Mail server not configured on server.");
        }

        String effectiveFrom = getEffectiveSenderEmail();

        try {
            log.info("Attempting live SMTP dispatch to {} via {}:{} from {}", studentEmail, mailHost, mailPort, effectiveFrom);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String effectiveSenderName = (senderName != null && !senderName.isBlank()) ? senderName : "CampusConnect Team";
            helper.setFrom(new InternetAddress(effectiveFrom, effectiveSenderName));
            helper.setTo(studentEmail);
            helper.setSubject("CampusConnect — Event Registration Successful: " + eventTitle);
            helper.setText(plainText, htmlContent);

            mailSender.send(message);
            log.info("Live SMTP send succeeded for recipient: {}", studentEmail);
            return EmailDeliveryResult.success("Confirmation email dispatched to " + studentEmail);
        } catch (Exception ex) {
            log.error("SMTP delivery failed for {}: {}", studentEmail, ex.getMessage());
            return EmailDeliveryResult.failed("SMTP delivery failed: " + ex.getMessage());
        }
    }

    @Override
    public EmailDeliveryResult sendTestEmail(String toEmail) {
        if (toEmail == null || toEmail.isBlank() || !toEmail.contains("@")) {
            return EmailDeliveryResult.failed("Please provide a valid test recipient email address.");
        }

        String effectiveFrom = getEffectiveSenderEmail();

        if (!mailEnabled) {
            log.info("[SIMULATED TEST EMAIL] To: {} | From: {} | Host: {}:{}", toEmail, effectiveFrom, mailHost, mailPort);
            return EmailDeliveryResult.simulated("SMTP is currently inactive (APP_MAIL_ENABLED=false). Set APP_MAIL_ENABLED=true in environment to send real emails.");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return EmailDeliveryResult.failed("JavaMailSender is not initialized. Please verify spring.mail configuration.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String effectiveSenderName = (senderName != null && !senderName.isBlank()) ? senderName : "CampusConnect Team";
            helper.setFrom(new InternetAddress(effectiveFrom, effectiveSenderName));
            helper.setTo(toEmail.trim());
            helper.setSubject("CampusConnect — SMTP Diagnostics Test Successful");

            String plainText = "CAMPUSCONNECT — SMTP DIAGNOSTICS TEST\n" +
                    "====================================\n\n" +
                    "Hello,\n\n" +
                    "This is a real diagnostic test email dispatched from your CampusConnect server.\n\n" +
                    "- Timestamp: " + LocalDateTime.now() + "\n" +
                    "- Sender Address: " + effectiveFrom + "\n" +
                    "- SMTP Server Host: " + mailHost + ":" + mailPort + "\n\n" +
                    "Your SMTP configuration is properly operating!\n\n" +
                    "CampusConnect Platform Team\n";

            String html = "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif; padding: 20px; color: #1e293b;'>" +
                    "<div style='background-color: #0f172a; padding: 20px; border-radius: 8px 8px 0 0; color: #ffffff;'>" +
                    "<h3 style='margin: 0; color: #ffffff;'>CampusConnect SMTP Diagnostics</h3>" +
                    "</div>" +
                    "<div style='border: 1px solid #e2e8f0; border-top: none; padding: 20px; border-radius: 0 0 8px 8px;'>" +
                    "<p>Hello!</p>" +
                    "<p>This is a real diagnostic test email dispatched from your <strong>CampusConnect</strong> server.</p>" +
                    "<div style='background: #f8fafc; padding: 12px; border-radius: 6px; margin: 16px 0;'>" +
                    "<p style='margin: 4px 0;'><strong>Timestamp:</strong> " + LocalDateTime.now() + "</p>" +
                    "<p style='margin: 4px 0;'><strong>Sender Address:</strong> " + effectiveFrom + "</p>" +
                    "<p style='margin: 4px 0;'><strong>SMTP Server Host:</strong> " + mailHost + ":" + mailPort + "</p>" +
                    "</div>" +
                    "<p style='color: #16a34a; font-weight: bold;'>Your SMTP configuration is properly functioning!</p>" +
                    "<p>Regards,<br><strong>CampusConnect Platform Team</strong></p>" +
                    "</div></body></html>";

            helper.setText(plainText, html);

            mailSender.send(message);
            log.info("Live test email successfully dispatched to {}", toEmail);
            return EmailDeliveryResult.success("Test email successfully dispatched to " + toEmail + "! Check your inbox and spam folder.");
        } catch (Exception ex) {
            log.error("Test email delivery failed to {}: {}", toEmail, ex.getMessage());
            return EmailDeliveryResult.failed("SMTP Test Error: " + ex.getMessage());
        }
    }

    private String buildPlainTextEmail(String studentName, String eventTitle, String eventDate, String eventTime,
                                       String venue, String rollNumber, String studentEmail, String refId) {
        return "CAMPUSCONNECT — EVENT REGISTRATION CONFIRMATION\n" +
                "================================================\n\n" +
                "Dear " + studentName + ",\n\n" +
                "Congratulations! Your registration for the following event has been confirmed.\n\n" +
                "EVENT DETAILS:\n" +
                "- Event Name: " + eventTitle + "\n" +
                "- Date: " + eventDate + "\n" +
                "- Time: " + eventTime + "\n" +
                "- Venue: " + venue + "\n" +
                "- Student Name: " + studentName + "\n" +
                "- Roll Number: " + rollNumber + "\n" +
                "- Registered Email: " + studentEmail + "\n" +
                "- Pass Reference ID: " + refId + "\n\n" +
                "IMPORTANT INSTRUCTIONS:\n" +
                "Please present your Pass Reference ID (" + refId + ") or digital pass from 'My Registrations' at the event entrance.\n\n" +
                "CampusConnect Platform Team\n";
    }

    private String buildHtmlEmail(String studentName, String eventTitle, String eventDate, String eventTime,
                                  String venue, String rollNumber, String studentEmail, String refId) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #1e293b; max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<div style='background-color: #0f172a; padding: 24px; border-radius: 12px 12px 0 0; color: #ffffff; text-align: center;'>" +
                "<h2 style='margin: 0; color: #ffffff;'>CampusConnect</h2>" +
                "<p style='margin: 4px 0 0 0; color: #94a3b8; font-size: 14px;'>Official College Event Pass</p>" +
                "</div>" +
                "<div style='border: 1px solid #e2e8f0; border-top: none; padding: 28px; border-radius: 0 0 12px 12px; background-color: #ffffff;'>" +
                "<p>Dear " + escapeHtml(studentName) + ",</p>" +
                "<p>Congratulations! Your registration for the following event has been successfully completed.</p>" +
                "<div style='background-color: #f8fafc; border-left: 4px solid #4f46e5; padding: 16px; margin: 20px 0; border-radius: 6px;'>" +
                "<ul style='list-style: none; padding: 0; margin: 0;'>" +
                "<li style='margin-bottom: 8px;'><strong>Event Name:</strong> " + escapeHtml(eventTitle) + "</li>" +
                "<li style='margin-bottom: 8px;'><strong>Event Date:</strong> " + escapeHtml(eventDate) + "</li>" +
                "<li style='margin-bottom: 8px;'><strong>Event Time:</strong> " + escapeHtml(eventTime) + "</li>" +
                "<li style='margin-bottom: 8px;'><strong>Venue:</strong> " + escapeHtml(venue) + "</li>" +
                "<li style='margin-bottom: 8px;'><strong>Student Name:</strong> " + escapeHtml(studentName) + "</li>" +
                "<li style='margin-bottom: 8px;'><strong>Roll Number:</strong> " + escapeHtml(rollNumber) + "</li>" +
                "<li style='margin-bottom: 8px;'><strong>Pass Reference ID:</strong> <span style='color: #4f46e5; font-weight: bold;'>" + escapeHtml(refId) + "</span></li>" +
                "</ul>" +
                "</div>" +
                "<div style='background-color: #f0fdf4; border: 1px solid #bbf7d0; padding: 14px; border-radius: 6px; margin-bottom: 20px; font-size: 14px; color: #166534;'>" +
                "<strong>Notice:</strong> Please present your digital pass or reference ID (" + escapeHtml(refId) + ") at the venue entrance for verification." +
                "</div>" +
                "<p style='font-size: 13px; color: #64748b;'>You can view, download, or manage your event passes anytime from the <strong>My Registrations</strong> portal.</p>" +
                "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;'>" +
                "<p style='font-size: 12px; color: #94a3b8; text-align: center; margin: 0;'>&copy; 2026 CampusConnect Platform. All rights reserved.</p>" +
                "</div>" +
                "</body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Override
    public void updateSettings(boolean mailEnabled, String senderEmail, String senderName) {
        this.mailEnabled = mailEnabled;
        if (senderEmail != null && !senderEmail.isBlank()) {
            this.senderEmail = senderEmail.trim();
        }
        if (senderName != null && !senderName.isBlank()) {
            this.senderName = senderName.trim();
        }
    }
}
