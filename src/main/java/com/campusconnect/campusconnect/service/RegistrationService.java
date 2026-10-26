package com.campusconnect.campusconnect.service;

import com.campusconnect.campusconnect.Event;
import com.campusconnect.campusconnect.dto.RegistrationForm;
import com.campusconnect.campusconnect.exception.RegistrationException;
import com.campusconnect.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.campusconnect.model.EventRegistration;
import com.campusconnect.campusconnect.model.EventStatus;
import com.campusconnect.campusconnect.model.RegistrationStatus;
import com.campusconnect.campusconnect.model.User;
import com.campusconnect.campusconnect.repository.EventRegistrationRepository;
import com.campusconnect.campusconnect.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    public RegistrationService(EventRegistrationRepository registrationRepository,
                               EventRepository eventRepository,
                               EmailService emailService,
                               ApplicationEventPublisher eventPublisher) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EventRegistration registerStudent(Long eventId, RegistrationForm form) {
        Event event = eventRepository.findByIdWithLock(eventId)
                .or(() -> eventRepository.findById(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        // 1. Check event status
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new RegistrationException("Registration failed: This event has been cancelled.");
        }
        if (event.getStatus() == EventStatus.COMPLETED || event.isPast()) {
            throw new RegistrationException("Registration failed: This event has already taken place.");
        }

        // 2. Check registration deadline
        if (event.isDeadlinePassed()) {
            throw new RegistrationException("Registration failed: The registration deadline has passed.");
        }

        // 3. Check capacity against active registrations
        long activeCount = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        if (activeCount >= event.getCapacity()) {
            throw new RegistrationException("Registration failed: This event is currently full (maximum capacity reached).");
        }

        // 4. Duplicate checks
        String normalizedEmail = form.getStudentEmail().trim().toLowerCase();
        String normalizedRollNumber = form.getRollNumber().trim().toUpperCase();

        Optional<EventRegistration> existingByEmail = registrationRepository.findByEventIdAndStudentEmail(eventId, normalizedEmail);
        if (existingByEmail.isPresent()) {
            if (existingByEmail.get().getStatus() == RegistrationStatus.CONFIRMED) {
                throw new RegistrationException("A registration with email '" + normalizedEmail + "' already exists for this event.");
            } else {
                // Reactivate previous cancelled registration
                EventRegistration reactivated = existingByEmail.get();
                reactivated.setStatus(RegistrationStatus.CONFIRMED);
                reactivated.setStudentName(form.getStudentName().trim());
                reactivated.setRollNumber(normalizedRollNumber);
                reactivated.setRegisteredAt(LocalDateTime.now());
                reactivated.setCancelledAt(null);
                
                // Dispatch email confirmation
                dispatchEmail(reactivated);
                return registrationRepository.save(reactivated);
            }
        }

        Optional<EventRegistration> existingByRoll = registrationRepository.findByEventIdAndRollNumber(eventId, normalizedRollNumber);
        if (existingByRoll.isPresent() && existingByRoll.get().getStatus() == RegistrationStatus.CONFIRMED) {
            throw new RegistrationException("A registration with Roll Number '" + normalizedRollNumber + "' already exists for this event.");
        }

        // 5. Create new registration
        EventRegistration registration = new EventRegistration(
                event,
                form.getStudentName().trim(),
                normalizedEmail,
                normalizedRollNumber
        );

        try {
            EventRegistration saved = registrationRepository.save(registration);
            
            // Dispatch email confirmation safely
            dispatchEmail(saved);
            saved = registrationRepository.save(saved);

            eventPublisher.publishEvent(new com.campusconnect.campusconnect.event.RegistrationCreatedEvent(saved));
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new RegistrationException("You have already registered for this event.");
        }
    }

    private void dispatchEmail(EventRegistration registration) {
        if (emailService == null) {
            return;
        }
        try {
            EmailDeliveryResult result = emailService.sendRegistrationConfirmation(registration);
            if (result != null && result.isSuccess()) {
                registration.setConfirmationEmailStatus(result.isSimulated() ? "SIMULATED" : "SENT");
                registration.setConfirmationEmailError(null);
            } else if (result != null) {
                registration.setConfirmationEmailStatus("FAILED");
                registration.setConfirmationEmailError(result.getMessage());
            }
            registration.setLastConfirmationSentAt(LocalDateTime.now());
        } catch (Exception ex) {
            log.error("Failed to dispatch confirmation email for registration ID {}: {}", registration.getId(), ex.getMessage());
            registration.setConfirmationEmailStatus("FAILED");
            registration.setConfirmationEmailError(ex.getMessage());
            registration.setLastConfirmationSentAt(LocalDateTime.now());
        }
    }

    @Transactional
    public EmailDeliveryResult resendConfirmation(Long registrationId, User currentUser) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration record not found with ID: " + registrationId));

        // Authorization check
        if (currentUser.isStudent() && !registration.getStudentEmail().equalsIgnoreCase(currentUser.getEmail())) {
            throw new RegistrationException("Access Denied: You can only resend confirmation emails for your own passes.");
        }

        // Status check
        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new RegistrationException("Only active, confirmed event passes are eligible for confirmation emails.");
        }

        // Cooldown check (60 seconds)
        if (registration.getLastConfirmationSentAt() != null) {
            LocalDateTime cooldownExpiry = registration.getLastConfirmationSentAt().plusSeconds(60);
            if (LocalDateTime.now().isBefore(cooldownExpiry)) {
                long remainingSeconds = Duration.between(LocalDateTime.now(), cooldownExpiry).toSeconds();
                throw new RegistrationException("Please wait " + Math.max(1, remainingSeconds) + " second(s) before requesting another email pass.");
            }
        }

        EmailDeliveryResult result = emailService.sendRegistrationConfirmation(registration);
        if (result.isSuccess()) {
            registration.setConfirmationEmailStatus(result.isSimulated() ? "SIMULATED" : "SENT");
            registration.setConfirmationEmailError(null);
        } else {
            registration.setConfirmationEmailStatus("FAILED");
            registration.setConfirmationEmailError(result.getMessage());
        }
        registration.setLastConfirmationSentAt(LocalDateTime.now());
        registrationRepository.save(registration);

        return result;
    }

    public List<EventRegistration> getStudentRegistrations(String email, String rollNumber) {
        List<EventRegistration> list;
        if (email != null && !email.isBlank() && rollNumber != null && !rollNumber.isBlank()) {
            list = registrationRepository.findByStudentEmailAndRollNumberOrderByRegisteredAtDesc(
                    email.trim().toLowerCase(),
                    rollNumber.trim().toUpperCase()
            );
        } else if (email != null && !email.isBlank()) {
            list = registrationRepository.findByStudentEmailOrderByRegisteredAtDesc(email.trim().toLowerCase());
        } else {
            list = List.of();
        }

        // When an event is cancelled by admin, it is excluded from student passes
        return list.stream()
                .filter(r -> r.getEvent() != null && r.getEvent().getStatus() != com.campusconnect.campusconnect.model.EventStatus.CANCELLED)
                .toList();
    }

    @Transactional
    public void cancelRegistration(Long registrationId, String email, String rollNumber) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration record not found."));

        // Verify identity and ownership
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin && !registration.getStudentEmail().equalsIgnoreCase(auth.getName())) {
                throw new RegistrationException("Access Denied: You cannot cancel another student's registration.");
            }
        } else {
            boolean emailMatches = email != null && registration.getStudentEmail().equalsIgnoreCase(email.trim());
            boolean rollMatches = rollNumber != null && registration.getRollNumber().equalsIgnoreCase(rollNumber.trim());

            if (!emailMatches && !rollMatches) {
                throw new RegistrationException("Unauthorized: You can only cancel your own registration.");
            }
        }

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new RegistrationException("This registration has already been cancelled.");
        }

        // Check cancellation cutoff
        Event event = registration.getEvent();
        LocalTime startTime = event.getStartTime() != null ? event.getStartTime() : LocalTime.of(9, 0);
        LocalDateTime eventStartDateTime = event.getEventDate().atTime(startTime);
        int cutoffHours = event.getCancellationCutoffHours();
        LocalDateTime cancellationDeadline = eventStartDateTime.minusHours(cutoffHours);

        if (LocalDateTime.now().isAfter(cancellationDeadline)) {
            throw new RegistrationException("Cancellations are only permitted at least " + cutoffHours +
                    " hour(s) before the event starts. The cancellation deadline has passed.");
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        registrationRepository.save(registration);
    }

    public List<EventRegistration> getParticipantsForEvent(Long eventId, String keyword, RegistrationStatus status) {
        return registrationRepository.searchParticipants(eventId, keyword, status);
    }

    public List<EventRegistration> getAllRegistrations(String keyword, RegistrationStatus status) {
        if ((keyword != null && !keyword.isBlank()) || status != null) {
            return registrationRepository.searchAllRegistrations(keyword, status);
        }
        return registrationRepository.findAllByOrderByRegisteredAtDesc();
    }

    public List<EventRegistration> getRegistrationsByEmailStatus(String keyword, String emailStatus) {
        if ((keyword != null && !keyword.isBlank()) || (emailStatus != null && !emailStatus.isBlank() && !emailStatus.equalsIgnoreCase("ALL"))) {
            return registrationRepository.searchByEmailStatus(keyword, emailStatus);
        }
        return registrationRepository.findAllByOrderByRegisteredAtDesc();
    }

    public long countByEmailStatus(String emailStatus) {
        return registrationRepository.countByConfirmationEmailStatus(emailStatus);
    }

    public List<EventRegistration> getRecentRegistrations() {
        return registrationRepository.findTop10ByOrderByRegisteredAtDesc();
    }

    public long countActiveRegistrations(Long eventId) {
        return registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
    }

    public boolean isStudentRegistered(Long eventId, String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Optional<EventRegistration> reg = registrationRepository.findByEventIdAndStudentEmail(eventId, email.trim().toLowerCase());
        return reg.isPresent() && reg.get().getStatus() == RegistrationStatus.CONFIRMED;
    }
}
