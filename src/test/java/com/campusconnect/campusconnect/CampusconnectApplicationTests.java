package com.campusconnect.campusconnect;

import com.campusconnect.campusconnect.model.RegistrationStatus;
import com.campusconnect.campusconnect.repository.EventRegistrationRepository;
import com.campusconnect.campusconnect.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CampusconnectApplicationTests {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository registrationRepository;

    @Test
    @DisplayName("Context loads and database repositories are healthy")
    void contextLoads() {
        assertNotNull(eventRepository);
        assertNotNull(registrationRepository);

        long eventCount = eventRepository.count();
        assertTrue(eventCount > 0, "Database should contain initialized events");

        List<Event> upcoming = eventRepository.findByEventDateGreaterThanEqualOrderByEventDateAscStartTimeAsc(LocalDate.now());
        assertFalse(upcoming.isEmpty(), "There should be upcoming events in the catalog");

        List<String> categories = eventRepository.findDistinctCategories();
        assertFalse(categories.isEmpty(), "Distinct categories should be present");

        long activeRegistrations = registrationRepository.countByStatus(RegistrationStatus.CONFIRMED);
        assertTrue(activeRegistrations >= 0, "Active registrations count should be non-negative");
    }
}
