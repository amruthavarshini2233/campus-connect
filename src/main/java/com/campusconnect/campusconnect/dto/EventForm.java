package com.campusconnect.campusconnect.dto;

import com.campusconnect.campusconnect.Event;
import com.campusconnect.campusconnect.model.EventStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class EventForm {

    private Long id;

    @NotBlank(message = "Event title is required")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Organizer is required")
    private String organizer;

    private String coverImageUrl;

    @NotNull(message = "Event date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;

    @NotBlank(message = "Venue is required")
    private String venue;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime registrationDeadline;

    private EventStatus status = EventStatus.OPEN;

    @Min(value = 0, message = "Cancellation cutoff must be 0 or more hours")
    private Integer cancellationCutoffHours = 24;

    public EventForm() {
    }

    public static EventForm fromEntity(Event event) {
        EventForm form = new EventForm();
        form.setId(event.getId());
        form.setTitle(event.getTitle());
        form.setDescription(event.getDescription());
        form.setCategory(event.getCategory());
        form.setOrganizer(event.getOrganizer());
        form.setCoverImageUrl(event.getCoverImageUrl());
        form.setEventDate(event.getEventDate());
        form.setStartTime(event.getStartTime());
        form.setVenue(event.getVenue());
        form.setCapacity(event.getCapacity());
        form.setRegistrationDeadline(event.getRegistrationDeadline());
        form.setStatus(event.getStatus());
        form.setCancellationCutoffHours(event.getCancellationCutoffHours());
        return form;
    }

    public void updateEntity(Event event) {
        event.setTitle(this.title);
        event.setDescription(this.description);
        event.setCategory(this.category);
        event.setOrganizer(this.organizer);
        event.setCoverImageUrl(this.coverImageUrl);
        event.setEventDate(this.eventDate);
        event.setStartTime(this.startTime);
        event.setVenue(this.venue);
        event.setCapacity(this.capacity != null ? this.capacity : 0);
        event.setRegistrationDeadline(this.registrationDeadline);
        if (this.status != null) {
            event.setStatus(this.status);
        }
        if (this.cancellationCutoffHours != null) {
            event.setCancellationCutoffHours(this.cancellationCutoffHours);
        }
    }

    public Event toEntity() {
        Event event = new Event();
        updateEntity(event);
        return event;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
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

    public Integer getCancellationCutoffHours() {
        return cancellationCutoffHours;
    }

    public void setCancellationCutoffHours(Integer cancellationCutoffHours) {
        this.cancellationCutoffHours = cancellationCutoffHours;
    }
}
