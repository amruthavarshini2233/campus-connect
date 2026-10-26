package com.campusconnect.campusconnect;

import com.campusconnect.campusconnect.model.EventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String category;

    private String organizer;

    @Column(length = 500)
    private String coverImageUrl;

    @Column(nullable = false)
    private LocalDate eventDate;

    private LocalTime startTime;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private int capacity;

    private LocalDateTime registrationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.OPEN;

    private int cancellationCutoffHours = 24;

    private LocalDateTime createdAt;

    @Transient
    private long activeRegistrationsCount = 0;

    public Event() {
    }

    public Event(String title, String description, String category,
                 String organizer, String coverImageUrl,
                 LocalDate eventDate, LocalTime startTime,
                 String venue, int capacity,
                 LocalDateTime registrationDeadline) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.organizer = organizer;
        this.coverImageUrl = coverImageUrl;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.venue = venue;
        this.capacity = capacity;
        this.registrationDeadline = registrationDeadline;
        this.status = EventStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = EventStatus.OPEN;
        }
        if (this.registrationDeadline == null && this.eventDate != null) {
            // Default deadline: start of event date
            this.registrationDeadline = this.eventDate.atStartOfDay();
        }
    }

    // Business helper methods
    public boolean isPast() {
        if (eventDate == null) return false;
        LocalDate today = LocalDate.now();
        if (eventDate.isBefore(today)) return true;
        if (eventDate.isEqual(today) && startTime != null) {
            return startTime.isBefore(LocalTime.now());
        }
        return false;
    }

    public boolean isDeadlinePassed() {
        if (registrationDeadline == null) {
            return isPast();
        }
        return LocalDateTime.now().isAfter(registrationDeadline);
    }

    public int getAvailableSeats() {
        return Math.max(0, capacity - (int) activeRegistrationsCount);
    }

    public boolean isFull() {
        return getAvailableSeats() <= 0;
    }

    public boolean isRegistrationOpen() {
        if (status == EventStatus.CANCELLED || status == EventStatus.COMPLETED) {
            return false;
        }
        if (isPast() || isDeadlinePassed()) {
            return false;
        }
        return !isFull();
    }

    public String getComputedStatusText() {
        if (status == EventStatus.CANCELLED) return "Cancelled";
        if (isPast() || status == EventStatus.COMPLETED) return "Completed";
        if (isFull()) return "Full (Waitlist/Closed)";
        if (isDeadlinePassed()) return "Registration Closed";
        return "Registration Open";
    }

    public String getStatusBadgeClass() {
        if (status == EventStatus.CANCELLED) return "bg-danger";
        if (isPast() || status == EventStatus.COMPLETED) return "bg-secondary";
        if (isFull()) return "bg-warning text-dark";
        if (isDeadlinePassed()) return "bg-secondary";
        return "bg-success";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public LocalDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public int getCancellationCutoffHours() {
        return cancellationCutoffHours;
    }

    public void setCancellationCutoffHours(int cancellationCutoffHours) {
        this.cancellationCutoffHours = cancellationCutoffHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getActiveRegistrationsCount() {
        return activeRegistrationsCount;
    }

    public void setActiveRegistrationsCount(long activeRegistrationsCount) {
        this.activeRegistrationsCount = activeRegistrationsCount;
    }
}
