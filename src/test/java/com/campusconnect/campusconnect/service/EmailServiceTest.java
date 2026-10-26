package com.campusconnect.campusconnect.service;

import com.campusconnect.campusconnect.Event;
import com.campusconnect.campusconnect.model.EventRegistration;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private Event sampleEvent;
    private EventRegistration sampleRegistration;

    @BeforeEach
    void setUp() {
        sampleEvent = new Event(
                "CodeSprint Hackathon",
                "24 hour hackathon",
                "Hackathon",
                "ACM Chapter",
                "https://example.com/cover.jpg",
                LocalDate.of(2026, 11, 15),
                LocalTime.of(10, 0),
                "Computer Science Block",
                100,
                null
        );
        sampleEvent.setId(10L);

        sampleRegistration = new EventRegistration(sampleEvent, "Aarav Sharma", "student@college.edu", "21CS001");
        sampleRegistration.setId(55L);
    }

    @Test
    @DisplayName("Email simulation mode works and logs safely when app.mail.enabled is false")
    void testEmailSimulationMode() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        EmailDeliveryResult result = emailService.sendRegistrationConfirmation(sampleRegistration);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.isSimulated());
        assertTrue(result.getMessage().contains("local dev mode"));
        verifyNoInteractions(javaMailSender);
    }

    @Test
    @DisplayName("Live SMTP dispatch sends email when app.mail.enabled is true")
    void testLiveEmailDispatchSuccess() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@campusconnect.edu");
        ReflectionTestUtils.setField(emailService, "senderName", "CampusConnect Team");

        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailDeliveryResult result = emailService.sendRegistrationConfirmation(sampleRegistration);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertFalse(result.isSimulated());
        assertTrue(result.getMessage().contains("student@college.edu"));
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("SMTP delivery failure returns failed result gracefully without throwing unhandled exception")
    void testEmailDispatchFailureHandledGracefully() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection refused")).when(javaMailSender).send(any(MimeMessage.class));

        EmailDeliveryResult result = emailService.sendRegistrationConfirmation(sampleRegistration);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("delivery failed"));
    }

    @Test
    @DisplayName("sendTestEmail dispatches via JavaMailSender when mail is enabled")
    void testSendTestEmail() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        EmailDeliveryResult result = emailService.sendTestEmail("test@college.edu");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(javaMailSender).send(any(MimeMessage.class));
    }
}
