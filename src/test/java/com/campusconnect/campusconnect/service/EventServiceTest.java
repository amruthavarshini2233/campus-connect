package com.campusconnect.campusconnect.service;

import com.campusconnect.campusconnect.Event;
import com.campusconnect.campusconnect.dto.DashboardStatsDto;
import com.campusconnect.campusconnect.dto.EventForm;
import com.campusconnect.campusconnect.exception.RegistrationException;
import com.campusconnect.campusconnect.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository registrationRepository;

    @InjectMocks
    private EventService eventService;

    private Event sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = new Event(
                "CodeSprint Hackathon",
                "24 hour annual coding challenge",
                "Hackathon",
                "ACM Chapter",
                "https://example.com/cover.jpg",
                LocalDate.now().plusDays(10),
                LocalTime.of(10, 0),
                "CS Block",
                100,
                LocalDate.now().plusDays(9).atTime(23, 59)
        );
        sampleEvent.setId(1L);
    }

    @Test
    @DisplayName("Create event with valid data succeeds")
    void testCreateEventSuccess() {
        EventForm form = EventForm.fromEntity(sampleEvent);
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> {
            Event e = i.getArgument(0);
            e.setId(1L);
            return e;
        });

        Event created = eventService.createEvent(form);

        assertNotNull(created);
        assertEquals("CodeSprint Hackathon", created.getTitle());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Create event with past date is rejected")
    void testCreateEventPastDateRejected() {
        EventForm form = EventForm.fromEntity(sampleEvent);
        form.setEventDate(LocalDate.now().minusDays(2));

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            eventService.createEvent(form);
        });

        assertTrue(ex.getMessage().contains("past"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("Update event with capacity below active registrations is rejected")
    void testUpdateEventCapacityBelowActiveRejected() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(50L);

        EventForm form = EventForm.fromEntity(sampleEvent);
        form.setCapacity(40); // 40 < 50 active attendees

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            eventService.updateEvent(1L, form);
        });

        assertTrue(ex.getMessage().contains("Capacity cannot be reduced below"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("Search upcoming events delegates to repository with today's date")
    void testSearchUpcomingEvents() {
        when(eventRepository.searchUpcomingEvents(eq("Sprint"), eq("Hackathon"), any(LocalDate.class)))
                .thenReturn(List.of(sampleEvent));
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(5L);

        List<Event> results = eventService.searchUpcomingEvents("Sprint", "Hackathon");

        assertEquals(1, results.size());
        assertEquals(5L, results.get(0).getActiveRegistrationsCount());
        verify(eventRepository).searchUpcomingEvents(eq("Sprint"), eq("Hackathon"), any(LocalDate.class));
    }

    @Test
    @DisplayName("Cancel event sets status to CANCELLED")
    void testCancelEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(0L);

        eventService.cancelEvent(1L);

        assertEquals(EventStatus.CANCELLED, sampleEvent.getStatus());
        verify(eventRepository).save(sampleEvent);
    }

    @Test
    @DisplayName("Delete event with active attendees is rejected")
    void testDeleteEventWithActiveAttendeesRejected() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(10L);

        RegistrationException ex = assertThrows(RegistrationException.class, () -> {
            eventService.deleteEvent(1L);
        });

        assertTrue(ex.getMessage().contains("Cannot delete event with active participants"));
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    @DisplayName("Delete event with 0 active attendees succeeds")
    void testDeleteEventSafeSuccess() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(0L);

        eventService.deleteEvent(1L);

        verify(eventRepository).delete(sampleEvent);
    }

    @Test
    @DisplayName("Dashboard metrics calculate accurately")
    void testDashboardStats() {
        when(eventRepository.count()).thenReturn(10L);
        when(eventRepository.countByEventDateGreaterThanEqualAndStatusNot(any(LocalDate.class), eq(EventStatus.CANCELLED))).thenReturn(8L);
        when(registrationRepository.count()).thenReturn(50L);
        when(registrationRepository.countByStatus(RegistrationStatus.CONFIRMED)).thenReturn(45L);
        when(eventRepository.findMostPopularEvents(any(PageRequest.class))).thenReturn(List.of(sampleEvent));
        when(eventRepository.findAll()).thenReturn(List.of(sampleEvent));

        DashboardStatsDto stats = eventService.getDashboardStats();

        assertEquals(10L, stats.getTotalEvents());
        assertEquals(8L, stats.getUpcomingEvents());
        assertEquals(50L, stats.getTotalRegistrations());
        assertEquals(45L, stats.getActiveRegistrations());
        assertEquals(1, stats.getPopularEvents().size());
        assertEquals(1L, stats.getCategoryCounts().get("Hackathon"));
    }

    @Test
    @DisplayName("Regression Test: Event with capacity 60 and 1 active registration reports exactly 1 booked and 59 available across all portals")
    void testSeatCountConsistencyRegression() {
        Event event = new Event(
                "Full-Stack Cloud Native Systems with Spring Boot & Docker",
                "Advanced Spring Boot Workshop",
                "Technical",
                "Dept of IT",
                null,
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                "Lab 4",
                60,
                LocalDate.now().plusDays(4).atTime(18, 0)
        );
        event.setId(4L);

        when(eventRepository.findById(4L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatus(4L, RegistrationStatus.CONFIRMED)).thenReturn(1L);

        Event retrieved = eventService.getEventById(4L);

        assertEquals(60, retrieved.getCapacity(), "Capacity must be exactly 60");
        assertEquals(1L, retrieved.getActiveRegistrationsCount(), "Confirmed registration count must be exactly 1");
        assertEquals(59, retrieved.getAvailableSeats(), "Available seats must be exactly 60 - 1 = 59");
        assertTrue(retrieved.isRegistrationOpen(), "Registration must remain open when 59 seats are available");
    }

    @Test
    @DisplayName("Change event status updates and saves event")
    void testChangeEventStatus() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        Event updated = eventService.changeEventStatus(1L, EventStatus.CANCELLED);

        assertEquals(EventStatus.CANCELLED, updated.getStatus());
        verify(eventRepository).save(sampleEvent);
    }

    @Test
    @DisplayName("getRecentEvents returns latest events with active counts")
    void testGetRecentEvents() {
        when(eventRepository.findTop10ByOrderByIdDesc()).thenReturn(List.of(sampleEvent));
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(5L);

        List<Event> recent = eventService.getRecentEvents();

        assertEquals(1, recent.size());
        assertEquals(5L, recent.get(0).getActiveRegistrationsCount());
    }

    @Test
    @DisplayName("getUpcomingEvents excludes CANCELLED events")
    void testUpcomingEventsExcludesCancelled() {
        when(eventRepository.findByEventDateGreaterThanEqualAndStatusNotOrderByEventDateAscStartTimeAsc(any(LocalDate.class), eq(EventStatus.CANCELLED)))
                .thenReturn(List.of(sampleEvent));
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(2L);

        List<Event> upcoming = eventService.getUpcomingEvents();

        assertEquals(1, upcoming.size());
        verify(eventRepository).findByEventDateGreaterThanEqualAndStatusNotOrderByEventDateAscStartTimeAsc(any(LocalDate.class), eq(EventStatus.CANCELLED));
    }
}
