package com.campusconnect.campusconnect.service;

import com.campusconnect.campusconnect.Event;
import com.campusconnect.campusconnect.dto.DashboardStatsDto;
import com.campusconnect.campusconnect.dto.EventForm;
import com.campusconnect.campusconnect.exception.RegistrationException;
import com.campusconnect.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.campusconnect.model.EventRegistration;
import com.campusconnect.campusconnect.model.EventStatus;
import com.campusconnect.campusconnect.model.RegistrationStatus;
import com.campusconnect.campusconnect.model.Role;
import com.campusconnect.campusconnect.repository.EventRegistrationRepository;
import com.campusconnect.campusconnect.repository.EventRepository;
import com.campusconnect.campusconnect.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    @Autowired
    public EventService(EventRepository eventRepository,
                        EventRegistrationRepository registrationRepository,
                        ObjectProvider<UserRepository> userRepositoryProvider) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepositoryProvider != null ? userRepositoryProvider.getIfAvailable() : null;
    }

    public EventService(EventRepository eventRepository,
                        EventRegistrationRepository registrationRepository) {
        this(eventRepository, registrationRepository, null);
    }

    public List<Event> getAllEvents() {
        List<Event> events = eventRepository.findAllByOrderByEventDateAscStartTimeAsc();
        populateRegistrationCounts(events);
        return events;
    }

    public List<Event> getAllEventsAdmin() {
        List<Event> events = eventRepository.findAllByOrderByIdDesc();
        populateRegistrationCounts(events);
        return events;
    }

    public List<Event> getRecentEvents() {
        List<Event> events = eventRepository.findTop10ByOrderByIdDesc();
        populateRegistrationCounts(events);
        return events;
    }

    public List<Event> getUpcomingEvents() {
        List<Event> events = eventRepository.findByEventDateGreaterThanEqualAndStatusNotOrderByEventDateAscStartTimeAsc(
                LocalDate.now(), EventStatus.CANCELLED);
        populateRegistrationCounts(events);
        return events;
    }

    public List<Event> searchEvents(String keyword, String category) {
        List<Event> events = eventRepository.searchEvents(keyword, category);
        populateRegistrationCounts(events);
        return events;
    }

    public List<Event> searchEvents(String keyword, String category, EventStatus status) {
        if (status == null && (keyword == null || keyword.isBlank()) && (category == null || category.isBlank() || category.equalsIgnoreCase("All"))) {
            return getAllEventsAdmin();
        }
        List<Event> events = eventRepository.searchEventsWithStatus(keyword, category, status);
        populateRegistrationCounts(events);
        return events;
    }

    public List<Event> searchUpcomingEvents(String keyword, String category) {
        List<Event> events = eventRepository.searchUpcomingEvents(keyword, category, LocalDate.now());
        populateRegistrationCounts(events);
        return events;
    }

    public Event getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));
        populateRegistrationCount(event);
        return event;
    }

    public List<String> getAllCategories() {
        return eventRepository.findDistinctCategories();
    }

    public long countEventsByStatus(EventStatus status) {
        return eventRepository.countByStatus(status);
    }

    @Transactional
    public Event createEvent(EventForm form) {
        validateEventForm(form, null);
        Event event = form.toEntity();
        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(Long id, EventForm form) {
        Event existing = getEventById(id);
        long activeCount = registrationRepository.countByEventIdAndStatus(id, RegistrationStatus.CONFIRMED);

        if (form.getCapacity() != null && form.getCapacity() < activeCount) {
            throw new RegistrationException("Capacity cannot be reduced below current active registrations (" + activeCount + ").");
        }

        validateEventForm(form, existing);
        form.updateEntity(existing);
        return eventRepository.save(existing);
    }

    @Transactional
    public void cancelEvent(Long id) {
        changeEventStatus(id, EventStatus.CANCELLED);
    }

    @Transactional
    public Event changeEventStatus(Long id, EventStatus newStatus) {
        Event event = getEventById(id);
        if (newStatus != null) {
            event.setStatus(newStatus);
            Event saved = eventRepository.save(event);
            if (newStatus == EventStatus.CANCELLED) {
                List<EventRegistration> registrations = registrationRepository.findByEventIdOrderByRegisteredAtDesc(id);
                for (EventRegistration reg : registrations) {
                    if (reg.getStatus() == RegistrationStatus.CONFIRMED) {
                        reg.setStatus(RegistrationStatus.CANCELLED);
                        reg.setCancelledAt(LocalDateTime.now());
                        registrationRepository.save(reg);
                    }
                }
            }
            return saved;
        }
        return event;
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = getEventById(id);
        long regCount = registrationRepository.countByEventIdAndStatus(id, RegistrationStatus.CONFIRMED);
        if (regCount > 0) {
            throw new RegistrationException("Cannot delete event with active participants. Please cancel the event instead.");
        }
        eventRepository.delete(event);
    }

    public DashboardStatsDto getDashboardStats() {
        long totalEvents = eventRepository.count();
        long upcomingEvents = eventRepository.countByEventDateGreaterThanEqualAndStatusNot(LocalDate.now(), EventStatus.CANCELLED);
        long totalRegistrations = registrationRepository.count();
        long activeRegistrations = registrationRepository.countByStatus(RegistrationStatus.CONFIRMED);

        List<Event> popularEvents = eventRepository.findMostPopularEvents(PageRequest.of(0, 5));
        populateRegistrationCounts(popularEvents);

        Map<String, Long> categoryCounts = new HashMap<>();
        List<Event> all = eventRepository.findAll();
        long totalCapacity = 0;
        for (Event e : all) {
            if (e.getCategory() != null) {
                categoryCounts.put(e.getCategory(), categoryCounts.getOrDefault(e.getCategory(), 0L) + 1);
            }
            if (e.getStatus() != com.campusconnect.campusconnect.model.EventStatus.CANCELLED) {
                totalCapacity += e.getCapacity();
            }
        }
        long availableCapacity = Math.max(0, totalCapacity - activeRegistrations);

        DashboardStatsDto stats = new DashboardStatsDto(totalEvents, upcomingEvents, totalRegistrations,
                activeRegistrations, totalCapacity, availableCapacity, popularEvents, categoryCounts);
        if (userRepository != null) {
            stats.setTotalStudents(userRepository.countByRole(Role.ROLE_STUDENT));
        }
        return stats;
    }

    private void validateEventForm(EventForm form, Event existing) {
        if (form.getEventDate() == null) {
            throw new RegistrationException("Event date is required.");
        }
        if (existing == null && form.getEventDate().isBefore(LocalDate.now())) {
            throw new RegistrationException("Event date cannot be in the past for new events.");
        }
        if (form.getRegistrationDeadline() != null) {
            LocalDateTime eventStart = form.getEventDate().atTime(
                    form.getStartTime() != null ? form.getStartTime() : java.time.LocalTime.of(9, 0)
            );
            if (form.getRegistrationDeadline().isAfter(eventStart)) {
                throw new RegistrationException("Registration deadline must be before the event start time.");
            }
        }
    }

    private void populateRegistrationCounts(List<Event> events) {
        for (Event event : events) {
            populateRegistrationCount(event);
        }
    }

    private void populateRegistrationCount(Event event) {
        long count = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.CONFIRMED);
        event.setActiveRegistrationsCount(count);
    }
}
