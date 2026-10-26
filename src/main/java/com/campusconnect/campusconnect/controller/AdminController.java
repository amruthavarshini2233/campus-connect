package com.campusconnect.campusconnect.controller;

import com.campusconnect.campusconnect.Event;
import com.campusconnect.campusconnect.dto.DashboardStatsDto;
import com.campusconnect.campusconnect.dto.EventForm;
import com.campusconnect.campusconnect.exception.RegistrationException;
import com.campusconnect.campusconnect.model.EventRegistration;
import com.campusconnect.campusconnect.model.EventStatus;
import com.campusconnect.campusconnect.model.RegistrationStatus;
import com.campusconnect.campusconnect.model.Role;
import com.campusconnect.campusconnect.model.User;
import com.campusconnect.campusconnect.service.EmailDeliveryResult;
import com.campusconnect.campusconnect.service.EmailService;
import com.campusconnect.campusconnect.service.EventService;
import com.campusconnect.campusconnect.service.RegistrationService;
import com.campusconnect.campusconnect.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final EventService eventService;
    private final RegistrationService registrationService;
    private final EmailService emailService;
    private final UserService userService;

    public AdminController(EventService eventService,
                           RegistrationService registrationService,
                           EmailService emailService,
                           UserService userService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.emailService = emailService;
        this.userService = userService;
    }

    @GetMapping
    public String dashboard(Model model) {
        DashboardStatsDto stats = eventService.getDashboardStats();
        List<Event> recentEvents = eventService.getRecentEvents();
        List<Event> upcomingEvents = eventService.getUpcomingEvents();
        List<EventRegistration> recentRegistrations = registrationService.getRecentRegistrations().stream()
                .limit(2)
                .toList();

        // Calculate events nearing capacity (>= 80% booked or <= 10 seats left)
        List<Event> eventsNearingCapacity = upcomingEvents.stream()
                .filter(e -> e.getCapacity() > 0 && (((double) e.getActiveRegistrationsCount() / e.getCapacity() >= 0.80) || e.getAvailableSeats() <= 10))
                .toList();

        model.addAttribute("stats", stats);
        model.addAttribute("recentEvents", recentEvents);
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("eventsNearingCapacity", eventsNearingCapacity);
        model.addAttribute("recentRegistrations", recentRegistrations);
        return "admin/dashboard";
    }

    @GetMapping("/reports")
    public String reportsAndAnalytics(Model model) {
        DashboardStatsDto stats = eventService.getDashboardStats();
        List<Event> allEvents = eventService.getAllEventsAdmin();
        long openCount = eventService.countEventsByStatus(EventStatus.OPEN);
        long cancelledCount = eventService.countEventsByStatus(EventStatus.CANCELLED);
        long completedCount = eventService.countEventsByStatus(EventStatus.COMPLETED);

        double overallUtilization = stats.getTotalCapacity() > 0 
                ? ((double) stats.getActiveRegistrations() * 100.0) / stats.getTotalCapacity() 
                : 0.0;

        List<String> popularTitles = new ArrayList<>();
        List<Long> popularBooked = new ArrayList<>();
        List<Integer> popularCapacities = new ArrayList<>();
        if (stats.getPopularEvents() != null) {
            for (Event e : stats.getPopularEvents()) {
                popularTitles.add(e.getTitle());
                popularBooked.add((long) e.getActiveRegistrationsCount());
                popularCapacities.add(e.getCapacity());
            }
        }

        List<String> categoryLabels = new ArrayList<>();
        List<Long> categoryCounts = new ArrayList<>();
        if (stats.getCategoryCounts() != null) {
            for (Map.Entry<String, Long> entry : stats.getCategoryCounts().entrySet()) {
                categoryLabels.add(entry.getKey());
                categoryCounts.add(entry.getValue());
            }
        }

        model.addAttribute("stats", stats);
        model.addAttribute("allEvents", allEvents);
        model.addAttribute("popularEvents", stats.getPopularEvents());
        model.addAttribute("popularTitles", popularTitles);
        model.addAttribute("popularBooked", popularBooked);
        model.addAttribute("popularCapacities", popularCapacities);
        model.addAttribute("categoryLabels", categoryLabels);
        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("openCount", openCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("overallUtilization", Math.round(overallUtilization * 10.0) / 10.0);
        return "admin/reports";
    }

    @GetMapping("/email-logs")
    public String emailLogsAndNotifications(@RequestParam(name = "keyword", required = false) String keyword,
                                            @RequestParam(name = "status", required = false) String emailStatus,
                                            Model model) {
        List<EventRegistration> logs = registrationService.getRegistrationsByEmailStatus(keyword, emailStatus);
        long sentCount = registrationService.countByEmailStatus("SENT");
        long simulatedCount = registrationService.countByEmailStatus("SIMULATED");
        long failedCount = registrationService.countByEmailStatus("FAILED");
        long totalLogs = registrationService.getAllRegistrations(null, null).size();

        model.addAttribute("logs", logs);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedStatus", emailStatus != null ? emailStatus : "ALL");
        model.addAttribute("sentCount", sentCount);
        model.addAttribute("simulatedCount", simulatedCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("totalLogs", totalLogs);

        // Safe SMTP Status Diagnostic (NO raw passwords revealed)
        model.addAttribute("mailEnabled", emailService.isMailEnabled());
        model.addAttribute("configuredHost", emailService.getConfiguredHost());
        model.addAttribute("configuredPort", emailService.getConfiguredPort());
        model.addAttribute("senderEmail", emailService.getConfiguredSenderEmail());
        model.addAttribute("credentialsConfigured", emailService.isCredentialsConfigured());
        return "admin/email-logs";
    }

    @PostMapping("/email-logs/{id}/retry")
    public String retryEmailConfirmation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        java.util.Optional<User> currentUserOpt = userService.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        try {
            EmailDeliveryResult result = registrationService.resendConfirmation(id, currentUserOpt.get());
            if (result.isSuccess()) {
                redirectAttributes.addFlashAttribute("successMessage", "Confirmation pass dispatched successfully: " + result.getMessage());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Email delivery issue: " + result.getMessage());
            }
        } catch (RegistrationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/email-logs";
    }

    @PostMapping("/email-logs/test")
    public String sendTestEmailFromLogs(@RequestParam("testEmail") String testEmail, RedirectAttributes redirectAttributes) {
        EmailDeliveryResult result = emailService.sendTestEmail(testEmail);
        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("successMessage", "Diagnostic test email dispatched successfully: " + result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Diagnostic test email failed: " + result.getMessage());
        }
        return "redirect:/admin/email-logs";
    }

    @GetMapping("/settings")
    public String settingsAndProfile() {
        return "redirect:/profile";
    }

    @GetMapping("/email-settings")
    public String emailSettings(Model model) {
        model.addAttribute("mailEnabled", emailService.isMailEnabled());
        model.addAttribute("configuredHost", emailService.getConfiguredHost());
        model.addAttribute("configuredPort", emailService.getConfiguredPort());
        model.addAttribute("senderEmail", emailService.getConfiguredSenderEmail());
        model.addAttribute("credentialsConfigured", emailService.isCredentialsConfigured());
        return "admin/email-settings";
    }

    @PostMapping("/email-settings/update")
    public String updateEmailSettings(@RequestParam(name = "mailEnabled", defaultValue = "false") boolean mailEnabled,
                                      @RequestParam(name = "senderEmail", required = false) String senderEmail,
                                      @RequestParam(name = "senderName", required = false) String senderName,
                                      RedirectAttributes redirectAttributes) {
        try {
            emailService.updateSettings(mailEnabled, senderEmail, senderName);
            redirectAttributes.addFlashAttribute("successMessage", "Email configuration updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update email settings: " + ex.getMessage());
        }
        return "redirect:/admin/email-settings";
    }

    @PostMapping("/email-settings/test")
    public String sendTestEmailFromSettings(@RequestParam("testEmail") String testEmail, RedirectAttributes redirectAttributes) {
        EmailDeliveryResult result = emailService.sendTestEmail(testEmail);
        redirectAttributes.addFlashAttribute("testResult", result);
        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("successMessage", result.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result.getMessage());
        }
        return "redirect:/admin/email-settings";
    }

    @PostMapping("/test-email")
    public String sendTestEmail(@RequestParam("testEmail") String testEmail, RedirectAttributes redirectAttributes) {
        return sendTestEmailFromSettings(testEmail, redirectAttributes);
    }

    @GetMapping("/registrations")
    public String manageAllRegistrations(@RequestParam(name = "keyword", required = false) String keyword,
                                         @RequestParam(name = "status", required = false) String statusStr,
                                         Model model) {
        RegistrationStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !statusStr.equalsIgnoreCase("ALL")) {
            try {
                status = RegistrationStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        List<EventRegistration> registrations = registrationService.getAllRegistrations(keyword, status);
        long confirmedCount = registrations.stream().filter(r -> r.getStatus() == RegistrationStatus.CONFIRMED).count();
        long cancelledCount = registrations.stream().filter(r -> r.getStatus() == RegistrationStatus.CANCELLED).count();
        model.addAttribute("registrations", registrations);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedStatus", statusStr != null ? statusStr : "ALL");
        return "admin/registrations";
    }

    @GetMapping("/users")
    public String manageUsers(@RequestParam(name = "keyword", required = false) String keyword,
                              @RequestParam(name = "role", required = false) String roleStr,
                              Model model) {
        Role role = null;
        if (roleStr != null && !roleStr.isBlank() && !roleStr.equalsIgnoreCase("ALL")) {
            try {
                role = Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        List<User> users = userService.getAllUsers(keyword, role);
        model.addAttribute("users", users);
        model.addAttribute("totalStudents", userService.countStudents());
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedRole", roleStr != null ? roleStr : "ALL");
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "User account status toggled successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/events")
    public String listEvents(@RequestParam(name = "keyword", required = false) String keyword,
                             @RequestParam(name = "category", required = false) String category,
                             @RequestParam(name = "status", required = false) String statusStr,
                             Model model) {
        EventStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !statusStr.equalsIgnoreCase("ALL")) {
            try {
                status = EventStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        List<Event> events;
        if ((keyword != null && !keyword.isBlank()) || (category != null && !category.isBlank() && !category.equalsIgnoreCase("All")) || status != null) {
            events = eventService.searchEvents(keyword, category, status);
        } else {
            events = eventService.getAllEventsAdmin();
        }

        long openCount = events.stream().filter(e -> e.getStatus() == EventStatus.OPEN).count();
        long totalBookedSeats = events.stream().mapToLong(Event::getActiveRegistrationsCount).sum();
        model.addAttribute("events", events);
        model.addAttribute("openCount", openCount);
        model.addAttribute("totalBookedSeats", totalBookedSeats);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("category", category != null ? category : "All");
        model.addAttribute("selectedStatus", statusStr != null ? statusStr : "ALL");
        return "admin/events-list";
    }

    @GetMapping("/events/new")
    public String newEventForm(Model model) {
        if (!model.containsAttribute("eventForm")) {
            model.addAttribute("eventForm", new EventForm());
        }
        model.addAttribute("isEdit", false);
        return "admin/event-form";
    }

    @PostMapping("/events/new")
    public String createEvent(@Valid @ModelAttribute("eventForm") EventForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "admin/event-form";
        }

        try {
            Event created = eventService.createEvent(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Event '" + created.getTitle() + "' was successfully created!");
            return "redirect:/admin/events";
        } catch (RegistrationException ex) {
            model.addAttribute("isEdit", false);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/event-form";
        }
    }

    @GetMapping("/events/{id}/edit")
    public String editEventForm(@PathVariable Long id, Model model) {
        Event event = eventService.getEventById(id);
        if (!model.containsAttribute("eventForm")) {
            model.addAttribute("eventForm", EventForm.fromEntity(event));
        }
        model.addAttribute("event", event);
        model.addAttribute("isEdit", true);
        return "admin/event-form";
    }

    @PostMapping("/events/{id}/edit")
    public String updateEvent(@PathVariable Long id,
                              @Valid @ModelAttribute("eventForm") EventForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        Event event = eventService.getEventById(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("event", event);
            model.addAttribute("isEdit", true);
            return "admin/event-form";
        }

        try {
            Event updated = eventService.updateEvent(id, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Event '" + updated.getTitle() + "' was successfully updated!");
            return "redirect:/admin/events";
        } catch (RegistrationException ex) {
            model.addAttribute("event", event);
            model.addAttribute("isEdit", true);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/event-form";
        }
    }

    @PostMapping("/events/{id}/status")
    public String updateEventStatus(@PathVariable Long id,
                                    @RequestParam("status") EventStatus status,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        try {
            Event event = eventService.changeEventStatus(id, status);
            String title = (event != null && event.getTitle() != null) ? event.getTitle() : "Event #" + id;
            redirectAttributes.addFlashAttribute("successMessage",
                    "Event '" + title + "' status changed to " + status + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/admin") && !referer.contains("/edit") && !referer.contains("/new")) {
            return "redirect:" + referer;
        }
        return "redirect:/admin/events";
    }

    @PostMapping("/events/{id}/cancel")
    public String cancelEvent(@PathVariable Long id, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            eventService.cancelEvent(id);
            redirectAttributes.addFlashAttribute("successMessage", "Event was cancelled successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/admin") && !referer.contains("/edit") && !referer.contains("/new")) {
            return "redirect:" + referer;
        }
        return "redirect:/admin/events";
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            eventService.deleteEvent(id);
            redirectAttributes.addFlashAttribute("successMessage", "Event deleted successfully.");
        } catch (RegistrationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/admin") && !referer.contains("/edit") && !referer.contains("/new")) {
            return "redirect:" + referer;
        }
        return "redirect:/admin/events";
    }

    @GetMapping("/events/{id}/participants")
    public String viewParticipants(@PathVariable Long id,
                                   @RequestParam(name = "keyword", required = false) String keyword,
                                   @RequestParam(name = "status", required = false) String statusStr,
                                   Model model) {
        Event event = eventService.getEventById(id);
        RegistrationStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !statusStr.equalsIgnoreCase("ALL")) {
            try {
                status = RegistrationStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<EventRegistration> participants = registrationService.getParticipantsForEvent(id, keyword, status);
        long activeCount = registrationService.countActiveRegistrations(id);

        model.addAttribute("event", event);
        model.addAttribute("participants", participants);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedStatus", statusStr != null ? statusStr : "ALL");
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("remainingSeats", Math.max(0, event.getCapacity() - (int) activeCount));

        return "admin/participants";
    }

    @GetMapping("/events/{id}/participants/export")
    public void exportParticipantsCsv(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Event event = eventService.getEventById(id);
        List<EventRegistration> participants = registrationService.getParticipantsForEvent(id, null, null);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"event-" + id + "-participants.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Registration ID,Student Name,College Email,Roll Number,Registered At,Status");

        for (EventRegistration p : participants) {
            writer.println(String.format("\"CC-REG-%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                    p.getId(),
                    escapeCsv(p.getStudentName()),
                    escapeCsv(p.getStudentEmail()),
                    escapeCsv(p.getRollNumber()),
                    p.getRegisteredAt() != null ? p.getRegisteredAt().toString() : "",
                    p.getStatus().name()
            ));
        }
        writer.flush();
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        return data.replace("\"", "\"\"");
    }
}
