package com.campusconnect.campusconnect.controller;

import com.campusconnect.campusconnect.Event;
import com.campusconnect.campusconnect.HomeController;
import com.campusconnect.campusconnect.config.SecurityConfig;
import com.campusconnect.campusconnect.dto.DashboardStatsDto;
import com.campusconnect.campusconnect.model.EventRegistration;
import com.campusconnect.campusconnect.model.EventStatus;
import com.campusconnect.campusconnect.model.Role;
import com.campusconnect.campusconnect.model.User;
import com.campusconnect.campusconnect.service.EmailDeliveryResult;
import com.campusconnect.campusconnect.service.EmailService;
import com.campusconnect.campusconnect.service.EventService;
import com.campusconnect.campusconnect.service.RegistrationService;
import com.campusconnect.campusconnect.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({HomeController.class, EventController.class, RegistrationController.class, AdminController.class, AuthController.class})
@Import(SecurityConfig.class)
class WebControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private UserService userService;

    private Event createSampleEvent(Long id) {
        Event event = new Event(
                "CodeSprint Hackathon",
                "Innovative 24-hour hackathon for developers",
                "Hackathon",
                "ACM Chapter",
                "https://example.com/cover.jpg",
                LocalDate.now().plusDays(10),
                LocalTime.of(10, 0),
                "CS Hall",
                100,
                LocalDate.now().plusDays(9).atTime(23, 59)
        );
        event.setId(id);
        event.setActiveRegistrationsCount(2);
        return event;
    }

    @Test
    @DisplayName("GET / loads homepage with events and stats")
    void testHomepageLoads() throws Exception {
        Event ev = createSampleEvent(1L);
        when(eventService.getUpcomingEvents()).thenReturn(List.of(ev));
        when(eventService.getDashboardStats()).thenReturn(new DashboardStatsDto(1L, 1L, 2L, 2L, List.of(ev), Map.of("Hackathon", 1L)));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("events", "totalEvents", "upcomingCount", "totalRegistrations"));
    }

    @Test
    @DisplayName("GET /explore loads dedicated explore events page")
    void testExplorePageLoads() throws Exception {
        Event ev = createSampleEvent(1L);
        when(eventService.getUpcomingEvents()).thenReturn(List.of(ev));
        when(eventService.getDashboardStats()).thenReturn(new DashboardStatsDto(1L, 1L, 2L, 2L, List.of(ev), Map.of("Hackathon", 1L)));

        mockMvc.perform(get("/explore"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/explore"))
                .andExpect(model().attributeExists("events", "keyword", "selectedCategory", "upcomingCount"));
    }

    @Test
    @DisplayName("GET /events/{id} returns details page")
    void testEventDetailsPageLoads() throws Exception {
        Event ev = createSampleEvent(1L);
        when(eventService.getEventById(1L)).thenReturn(ev);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/details"))
                .andExpect(model().attributeExists("event", "registrationForm"));
    }

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("POST /events/{id}/register with blank fields triggers validation errors")
    void testRegistrationFormValidationErrors() throws Exception {
        Event ev = createSampleEvent(1L);
        when(eventService.getEventById(1L)).thenReturn(ev);
        when(userService.getCurrentUser()).thenReturn(Optional.of(new User("Test Student", "student@college.edu", "pass", "21CS001", Role.ROLE_STUDENT)));

        mockMvc.perform(post("/events/1/register")
                        .with(csrf())
                        .param("studentName", "")
                        .param("studentEmail", "invalid-email")
                        .param("rollNumber", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("events/register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "studentName"));
    }

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("POST /events/{id}/register with valid fields redirects to confirmation")
    void testRegistrationFormSuccessRedirects() throws Exception {
        Event ev = createSampleEvent(1L);
        when(eventService.getEventById(1L)).thenReturn(ev);
        when(userService.getCurrentUser()).thenReturn(Optional.of(new User("Sanjay Rao", "sanjay@college.edu", "pass", "21CS101", Role.ROLE_STUDENT)));

        EventRegistration reg = new EventRegistration(ev, "Sanjay Rao", "sanjay@college.edu", "21CS101");
        reg.setId(99L);
        when(registrationService.registerStudent(eq(1L), any())).thenReturn(reg);
        when(emailService.sendRegistrationConfirmation(any())).thenReturn(EmailDeliveryResult.simulated("Simulated email"));

        mockMvc.perform(post("/events/1/register")
                        .with(csrf())
                        .param("studentName", "Sanjay Rao")
                        .param("studentEmail", "sanjay@college.edu")
                        .param("rollNumber", "21CS101"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/1/registration-success"));
    }

    @Test
    @WithMockUser(username = "sanjay@college.edu", roles = {"STUDENT"})
    @DisplayName("GET /my-registrations returns student pass view")
    void testMyRegistrationsPageLoads() throws Exception {
        Event ev = createSampleEvent(1L);
        EventRegistration reg = new EventRegistration(ev, "Sanjay Rao", "sanjay@college.edu", "21CS101");
        User user = new User("Sanjay Rao", "sanjay@college.edu", "pass", "21CS101", Role.ROLE_STUDENT);

        when(userService.getCurrentUser()).thenReturn(Optional.of(user));
        when(registrationService.getStudentRegistrations("sanjay@college.edu", "21CS101")).thenReturn(List.of(reg));

        mockMvc.perform(get("/my-registrations"))
                .andExpect(status().isOk())
                .andExpect(view().name("registrations/my-registrations"))
                .andExpect(model().attributeExists("registrations", "currentUser"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("GET /admin loads admin dashboard with summary metrics")
    void testAdminDashboardLoads() throws Exception {
        Event ev = createSampleEvent(1L);
        when(eventService.getAllEvents()).thenReturn(List.of(ev));
        when(eventService.getDashboardStats()).thenReturn(new DashboardStatsDto(5L, 4L, 20L, 18L, List.of(ev), Map.of("Hackathon", 1L)));

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("stats", "recentEvents"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("GET /admin/events/{id}/participants loads participant management table")
    void testAdminParticipantsPageLoads() throws Exception {
        Event ev = createSampleEvent(1L);
        when(eventService.getEventById(1L)).thenReturn(ev);
        when(registrationService.getParticipantsForEvent(eq(1L), any(), any())).thenReturn(List.of());
        when(registrationService.countActiveRegistrations(1L)).thenReturn(0L);

        mockMvc.perform(get("/admin/events/1/participants"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/participants"))
                .andExpect(model().attributeExists("event", "participants", "activeCount", "remainingSeats"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("Regression Test: Student event view and Admin dashboard receive identical seat counts for event 4 (1 booked, 60 capacity, 59 available)")
    void testSeatCountConsistencyAcrossStudentAndAdminViews() throws Exception {
        Event event4 = new Event(
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
        event4.setId(4L);
        event4.setActiveRegistrationsCount(1L);

        when(eventService.getEventById(4L)).thenReturn(event4);
        when(eventService.getAllEvents()).thenReturn(List.of(event4));
        when(eventService.getRecentEvents()).thenReturn(List.of(event4));
        when(eventService.getDashboardStats()).thenReturn(new DashboardStatsDto(1L, 1L, 1L, 1L, List.of(event4), Map.of("Technical", 1L)));

        // 1. Verify Student View receives capacity=60, activeRegistrationsCount=1, availableSeats=59
        mockMvc.perform(get("/events/4"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("event", org.hamcrest.Matchers.hasProperty("capacity", org.hamcrest.Matchers.is(60))))
                .andExpect(model().attribute("event", org.hamcrest.Matchers.hasProperty("activeRegistrationsCount", org.hamcrest.Matchers.is(1L))))
                .andExpect(model().attribute("event", org.hamcrest.Matchers.hasProperty("availableSeats", org.hamcrest.Matchers.is(59))));

        // 2. Verify Admin Dashboard receives the exact same event attributes
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("recentEvents", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.is(4L)),
                                org.hamcrest.Matchers.hasProperty("capacity", org.hamcrest.Matchers.is(60)),
                                org.hamcrest.Matchers.hasProperty("activeRegistrationsCount", org.hamcrest.Matchers.is(1L)),
                                org.hamcrest.Matchers.hasProperty("availableSeats", org.hamcrest.Matchers.is(59))
                        )
                )));
    }

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("GET /profile returns unified profile view with student stats")
    void testProfilePageLoadsForStudent() throws Exception {
        User user = new User("Pooja Sharma", "student@college.edu", "pass", "21CS088", Role.ROLE_STUDENT);
        when(userService.getCurrentUser()).thenReturn(Optional.of(user));
        when(registrationService.getStudentRegistrations("student@college.edu", "21CS088")).thenReturn(List.of());

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user", "totalPasses", "activePasses", "cancelledPasses"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("GET /admin/email-settings returns dedicated email settings page")
    void testAdminEmailSettingsPageLoads() throws Exception {
        when(emailService.isMailEnabled()).thenReturn(false);
        when(emailService.getConfiguredHost()).thenReturn("smtp.gmail.com");
        when(emailService.getConfiguredPort()).thenReturn(587);
        when(emailService.getConfiguredSenderEmail()).thenReturn("noreply@campusconnect.edu");
        when(emailService.isCredentialsConfigured()).thenReturn(false);

        mockMvc.perform(get("/admin/email-settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/email-settings"))
                .andExpect(model().attributeExists("mailEnabled", "configuredHost", "configuredPort", "senderEmail", "credentialsConfigured"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("GET /admin/reports loads institutional reports and analytics page")
    void testAdminReportsPageLoads() throws Exception {
        Event ev = createSampleEvent(1L);
        when(eventService.getDashboardStats()).thenReturn(new DashboardStatsDto(5L, 4L, 20L, 18L, List.of(ev), Map.of("Hackathon", 1L)));
        when(eventService.getAllEventsAdmin()).thenReturn(List.of(ev));

        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(model().attributeExists("stats", "allEvents", "popularEvents", "overallUtilization"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("GET /admin/email-logs loads confirmation email and notification logs page")
    void testAdminEmailLogsPageLoads() throws Exception {
        when(registrationService.getRegistrationsByEmailStatus(any(), any())).thenReturn(List.of());
        when(emailService.isMailEnabled()).thenReturn(false);
        when(emailService.getConfiguredHost()).thenReturn("smtp.gmail.com");
        when(emailService.getConfiguredPort()).thenReturn(587);
        when(emailService.getConfiguredSenderEmail()).thenReturn("noreply@campusconnect.edu");
        when(emailService.isCredentialsConfigured()).thenReturn(false);

        mockMvc.perform(get("/admin/email-logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/email-logs"))
                .andExpect(model().attributeExists("logs", "sentCount", "simulatedCount", "failedCount", "mailEnabled"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("POST /admin/email-settings/test dispatches test email and redirects with flash message")
    void testAdminEmailSettingsTestEmail() throws Exception {
        when(emailService.sendTestEmail("test@example.com")).thenReturn(EmailDeliveryResult.simulated("Simulated test email"));

        mockMvc.perform(post("/admin/email-settings/test")
                        .with(csrf())
                        .param("testEmail", "test@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/email-settings"))
                .andExpect(flash().attributeExists("testResult", "successMessage"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("GET /admin/registrations returns global registrations management page")
    void testAdminRegistrationsPageLoads() throws Exception {
        when(registrationService.getAllRegistrations(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/admin/registrations"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/registrations"))
                .andExpect(model().attributeExists("registrations", "keyword", "selectedStatus"));
    }

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("POST /my-registrations/resend-confirmation calls registrationService.resendConfirmation")
    void testResendConfirmationEndpoint() throws Exception {
        User user = new User("Pooja Sharma", "student@college.edu", "pass", "21CS088", Role.ROLE_STUDENT);
        when(userService.getCurrentUser()).thenReturn(Optional.of(user));
        when(registrationService.resendConfirmation(eq(100L), any())).thenReturn(EmailDeliveryResult.sent("Pass sent"));

        mockMvc.perform(post("/my-registrations/resend-confirmation")
                        .with(csrf())
                        .param("registrationId", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-registrations"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("GET /admin/users returns dedicated user management page")
    void testAdminUsersPageLoads() throws Exception {
        User user = new User("Pooja Sharma", "student@college.edu", "pass", "21CS088", Role.ROLE_STUDENT);
        when(userService.getAllUsers(null, null)).thenReturn(List.of(user));
        when(userService.countStudents()).thenReturn(1L);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users", "totalStudents"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("POST /admin/users/{id}/toggle-status toggles user active status")
    void testAdminToggleUserStatus() throws Exception {
        mockMvc.perform(post("/admin/users/1/toggle-status").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("POST /admin/events/{id}/status changes event status")
    void testAdminChangeEventStatus() throws Exception {
        when(eventService.changeEventStatus(eq(1L), any())).thenReturn(createSampleEvent(1L));

        mockMvc.perform(post("/admin/events/1/status")
                        .with(csrf())
                        .param("status", "CANCELLED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("POST /admin/email-settings/update updates email configuration")
    void testAdminUpdateEmailSettings() throws Exception {
        mockMvc.perform(post("/admin/email-settings/update")
                        .with(csrf())
                        .param("mailEnabled", "true")
                        .param("senderEmail", "admin@campus.edu")
                        .param("senderName", "Campus Admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/email-settings"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("POST /profile updates student profile name")
    void testUpdateProfile() throws Exception {
        User user = new User("Pooja Sharma", "student@college.edu", "pass", "21CS088", Role.ROLE_STUDENT);
        when(userService.getCurrentUser()).thenReturn(Optional.of(user));

        mockMvc.perform(post("/profile")
                        .with(csrf())
                        .param("fullName", "Pooja S."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
