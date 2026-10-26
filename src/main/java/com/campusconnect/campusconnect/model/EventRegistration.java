package com.campusconnect.campusconnect.model;

import com.campusconnect.campusconnect.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_registrations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_student_email", columnNames = {"event_id", "student_email"}),
        @UniqueConstraint(name = "uk_event_roll_number", columnNames = {"event_id", "roll_number"})
})
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "student_email", nullable = false)
    private String studentEmail;

    @Column(name = "roll_number", nullable = false)
    private String rollNumber;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status = RegistrationStatus.CONFIRMED;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "confirmation_email_status")
    private String confirmationEmailStatus = "PENDING";

    @Column(name = "last_confirmation_sent_at")
    private LocalDateTime lastConfirmationSentAt;

    @Column(name = "confirmation_email_error", length = 500)
    private String confirmationEmailError;

    public EventRegistration() {
    }

    public EventRegistration(Event event, String studentName, String studentEmail, String rollNumber) {
        this.event = event;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.rollNumber = rollNumber;
        this.registeredAt = LocalDateTime.now();
        this.status = RegistrationStatus.CONFIRMED;
    }

    @PrePersist
    public void onPrePersist() {
        if (this.registeredAt == null) {
            this.registeredAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = RegistrationStatus.CONFIRMED;
        }
    }

    // Helper methods
    public boolean isConfirmed() {
        return this.status == RegistrationStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return this.status == RegistrationStatus.CANCELLED;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getConfirmationEmailStatus() {
        return confirmationEmailStatus != null ? confirmationEmailStatus : "PENDING";
    }

    public void setConfirmationEmailStatus(String confirmationEmailStatus) {
        this.confirmationEmailStatus = confirmationEmailStatus;
    }

    public LocalDateTime getLastConfirmationSentAt() {
        return lastConfirmationSentAt;
    }

    public void setLastConfirmationSentAt(LocalDateTime lastConfirmationSentAt) {
        this.lastConfirmationSentAt = lastConfirmationSentAt;
    }

    public String getConfirmationEmailError() {
        return confirmationEmailError;
    }

    public void setConfirmationEmailError(String confirmationEmailError) {
        this.confirmationEmailError = confirmationEmailError;
    }

    public boolean isEligibleForConfirmationResend() {
        if (!isConfirmed()) {
            return false;
        }
        if (lastConfirmationSentAt == null) {
            return true;
        }
        return lastConfirmationSentAt.plusSeconds(60).isBefore(LocalDateTime.now());
    }
}
