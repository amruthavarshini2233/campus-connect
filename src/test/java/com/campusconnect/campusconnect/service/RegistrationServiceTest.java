package com.campusconnect.campusconnect.service;

import com.campusconnect.campusconnect.Event;
import com.campusconnect.campusconnect.dto.RegistrationForm;
import com.campusconnect.campusconnect.exception.RegistrationException;
import com.campusconnect.campusconnect.model.EventRegistration;
import com.campusconnect.campusconnect.model.EventStatus;
import com.campusconnect.campusconnect.model.RegistrationStatus;
import com.campusconnect.campusconnect.repository.EventRegistrationRepository;
import com.campusconnect.campusconnect.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RegistrationService registrationService;

    private Event openEvent;
    private RegistrationForm validForm;

    @BeforeEach
    void setUp() {
        openEvent = new Event(
                "AI Masterclass",
                "Deep dive into ML and neural networks",
                "Workshop",
                "IEEE",
                "https://example.com/ai.jpg",
                LocalDate.now().plusDays(7),
                LocalTime.of(14, 0),
                "Hall B",
                50,
                LocalDate.now().plusDays(6).atTime(23, 59)
        );
        openEvent.setId(10L);

        validForm = new RegistrationForm("Pooja Sharma", "pooja.sharma@college.edu", "21CS088");
    }

    @Test
    @DisplayName("Student registration succeeds with valid data")
    void testRegisterStudentSuccess() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(openEvent));
        when(registrationRepository.countByEventIdAndStatus(10L, RegistrationStatus.CONFIRMED)).thenReturn(10L);
        when(registrationRepository.findByEventIdAndStudentEmail(10L, "pooja.sharma@college.edu")).thenReturn(Optional.empty());
        when(registrationRepository.findByEventIdAndRollNumber(10L, "21CS088")).thenReturn(Optional.empty());
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(i -> {
            EventRegistration r = i.getArgument(0);
            r.setId(101L);
            return r;
        });

        when(emailService.sendRegistrationConfirmation(any())).thenReturn(EmailDeliveryResult.simulated("Simulated pass"));

        EventRegistration result = registrationService.registerStudent(10L, validForm);

        assertNotNull(result);
        assertEquals("Pooja Sharma", result.getStudentName());
        assertEquals("pooja.sharma@college.edu", result.getStudentEmail());
        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        verify(registrationRepository, atLeastOnce()).save(any(EventRegistration.class));
    }

    @Test
    @DisplayName("Duplicate registration with same student email is rejected")
    void testDuplicateRegistrationEmailRejected() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(openEvent));
        when(registrationRepository.countByEventIdAndStatus(10L, RegistrationStatus.CONFIRMED)).thenReturn(10L);
        
        EventRegistration existing = new EventRegistration(openEvent, "Pooja Sharma", "pooja.sharma@college.edu", "21CS088");
        existing.setStatus(RegistrationStatus.CONFIRMED);
        when(registrationRepository.findByEventIdAndStudentEmail(10L, "pooja.sharma@college.edu"))
                .thenReturn(Optional.of(existing));

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            registrationService.registerStudent(10L, validForm);
        });

        assertTrue(ex.getMessage().contains("already exists for this event"));
        verify(registrationRepository, never()).save(any(EventRegistration.class));
    }

    @Test
    @DisplayName("Duplicate registration with same roll number is rejected")
    void testDuplicateRegistrationRollNumberRejected() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(openEvent));
        when(registrationRepository.countByEventIdAndStatus(10L, RegistrationStatus.CONFIRMED)).thenReturn(10L);
        when(registrationRepository.findByEventIdAndStudentEmail(10L, "pooja.sharma@college.edu")).thenReturn(Optional.empty());

        EventRegistration existing = new EventRegistration(openEvent, "Another Student", "other@college.edu", "21CS088");
        existing.setStatus(RegistrationStatus.CONFIRMED);
        when(registrationRepository.findByEventIdAndRollNumber(10L, "21CS088"))
                .thenReturn(Optional.of(existing));

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            registrationService.registerStudent(10L, validForm);
        });

        assertTrue(ex.getMessage().contains("Roll Number '21CS088' already exists"));
        verify(registrationRepository, never()).save(any(EventRegistration.class));
    }

    @Test
    @DisplayName("Registration on full event is rejected")
    void testRegistrationFullEventRejected() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(openEvent));
        // Capacity is 50, active registrations is 50
        when(registrationRepository.countByEventIdAndStatus(10L, RegistrationStatus.CONFIRMED)).thenReturn(50L);

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            registrationService.registerStudent(10L, validForm);
        });

        assertTrue(ex.getMessage().contains("currently full"));
        verify(registrationRepository, never()).save(any(EventRegistration.class));
    }

    @Test
    @DisplayName("Registration on cancelled event is rejected")
    void testRegistrationCancelledEventRejected() {
        openEvent.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(openEvent));

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            registrationService.registerStudent(10L, validForm);
        });

        assertTrue(ex.getMessage().contains("has been cancelled"));
        verify(registrationRepository, never()).save(any(EventRegistration.class));
    }

    @Test
    @DisplayName("Registration after registration deadline is rejected")
    void testRegistrationDeadlinePassedRejected() {
        openEvent.setRegistrationDeadline(LocalDateTime.now().minusHours(1));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(openEvent));

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            registrationService.registerStudent(10L, validForm);
        });

        assertTrue(ex.getMessage().contains("registration deadline has passed"));
        verify(registrationRepository, never()).save(any(EventRegistration.class));
    }

    @Test
    @DisplayName("Student cancellation succeeds when within cancellation cutoff window")
    void testCancelRegistrationSuccess() {
        EventRegistration registration = new EventRegistration(openEvent, "Pooja Sharma", "pooja.sharma@college.edu", "21CS088");
        registration.setId(201L);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        when(registrationRepository.findById(201L)).thenReturn(Optional.of(registration));

        registrationService.cancelRegistration(201L, "pooja.sharma@college.edu", "21CS088");

        assertEquals(RegistrationStatus.CANCELLED, registration.getStatus());
        assertNotNull(registration.getCancelledAt());
        verify(registrationRepository).save(registration);
    }

    @Test
    @DisplayName("Student cancellation by unauthorized party is rejected")
    void testCancelRegistrationUnauthorizedRejected() {
        EventRegistration registration = new EventRegistration(openEvent, "Pooja Sharma", "pooja.sharma@college.edu", "21CS088");
        registration.setId(201L);

        when(registrationRepository.findById(201L)).thenReturn(Optional.of(registration));

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            registrationService.cancelRegistration(201L, "imposter@college.edu", "99XX999");
        });

        assertTrue(ex.getMessage().contains("Unauthorized"));
        verify(registrationRepository, never()).save(any(EventRegistration.class));
    }

    @Test
    @DisplayName("Cancellation after configured cutoff hours is rejected")
    void testCancelRegistrationPastCutoffRejected() {
        // Event starts in 2 hours, but cutoff is 24 hours
        openEvent.setEventDate(LocalDate.now());
        openEvent.setStartTime(LocalTime.now().plusHours(2));
        openEvent.setCancellationCutoffHours(24);

        EventRegistration registration = new EventRegistration(openEvent, "Pooja Sharma", "pooja.sharma@college.edu", "21CS088");
        registration.setId(201L);

        when(registrationRepository.findById(201L)).thenReturn(Optional.of(registration));

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            registrationService.cancelRegistration(201L, "pooja.sharma@college.edu", "21CS088");
        });

        assertTrue(ex.getMessage().contains("cancellation deadline has passed"));
        verify(registrationRepository, never()).save(any(EventRegistration.class));
    }

    @Test
    @DisplayName("resendConfirmation fails if student attempts to resend another user's pass")
    void testResendConfirmationUnauthorized() {
        EventRegistration registration = new EventRegistration(openEvent, "Pooja Sharma", "pooja.sharma@college.edu", "21CS088");
        registration.setId(501L);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        when(registrationRepository.findById(501L)).thenReturn(Optional.of(registration));

        com.campusconnect.campusconnect.model.User imposter = new com.campusconnect.campusconnect.model.User(
                "Imposter Student", "imposter@college.edu", "pass", "99XX999", com.campusconnect.campusconnect.model.Role.ROLE_STUDENT);

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            registrationService.resendConfirmation(501L, imposter);
        });

        assertTrue(ex.getMessage().contains("Access Denied"));
        verify(emailService, never()).sendRegistrationConfirmation(any());
    }

    @Test
    @DisplayName("resendConfirmation fails if within 60 second cooldown window")
    void testResendConfirmationCooldownBlocked() {
        EventRegistration registration = new EventRegistration(openEvent, "Pooja Sharma", "pooja.sharma@college.edu", "21CS088");
        registration.setId(502L);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setLastConfirmationSentAt(LocalDateTime.now().minusSeconds(20)); // Sent 20 seconds ago (< 60s)
        when(registrationRepository.findById(502L)).thenReturn(Optional.of(registration));

        com.campusconnect.campusconnect.model.User owner = new com.campusconnect.campusconnect.model.User(
                "Pooja Sharma", "pooja.sharma@college.edu", "pass", "21CS088", com.campusconnect.campusconnect.model.Role.ROLE_STUDENT);

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            registrationService.resendConfirmation(502L, owner);
        });

        assertTrue(ex.getMessage().contains("Please wait"));
        verify(emailService, never()).sendRegistrationConfirmation(any());
    }

    @Test
    @DisplayName("resendConfirmation succeeds after 60 second cooldown has expired")
    void testResendConfirmationSuccess() {
        EventRegistration registration = new EventRegistration(openEvent, "Pooja Sharma", "pooja.sharma@college.edu", "21CS088");
        registration.setId(503L);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setLastConfirmationSentAt(LocalDateTime.now().minusSeconds(75)); // Sent 75 seconds ago (> 60s)
        when(registrationRepository.findById(503L)).thenReturn(Optional.of(registration));
        when(emailService.sendRegistrationConfirmation(registration)).thenReturn(EmailDeliveryResult.sent("Pass sent"));

        com.campusconnect.campusconnect.model.User owner = new com.campusconnect.campusconnect.model.User(
                "Pooja Sharma", "pooja.sharma@college.edu", "pass", "21CS088", com.campusconnect.campusconnect.model.Role.ROLE_STUDENT);

        EmailDeliveryResult result = registrationService.resendConfirmation(503L, owner);

        assertTrue(result.isSuccess());
        assertEquals("SENT", registration.getConfirmationEmailStatus());
        verify(emailService).sendRegistrationConfirmation(registration);
        verify(registrationRepository).save(registration);
    }

    @Test
    @DisplayName("isStudentRegistered returns true for confirmed registration, false otherwise")
    void testIsStudentRegistered() {
        EventRegistration confirmed = new EventRegistration(openEvent, "Pooja Sharma", "pooja.sharma@college.edu", "21CS088");
        confirmed.setStatus(RegistrationStatus.CONFIRMED);

        when(registrationRepository.findByEventIdAndStudentEmail(10L, "pooja.sharma@college.edu"))
                .thenReturn(Optional.of(confirmed));
        when(registrationRepository.findByEventIdAndStudentEmail(10L, "absent@college.edu"))
                .thenReturn(Optional.empty());

        assertTrue(registrationService.isStudentRegistered(10L, "pooja.sharma@college.edu"));
        assertFalse(registrationService.isStudentRegistered(10L, "absent@college.edu"));
        assertFalse(registrationService.isStudentRegistered(10L, ""));
    }
}
