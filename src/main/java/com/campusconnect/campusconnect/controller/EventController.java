package com.campusconnect.campusconnect.controller;

import com.campusconnect.campusconnect.Event;
import com.campusconnect.campusconnect.dto.RegistrationForm;
import com.campusconnect.campusconnect.exception.RegistrationException;
import com.campusconnect.campusconnect.model.EventRegistration;
import com.campusconnect.campusconnect.model.User;
import com.campusconnect.campusconnect.service.EmailDeliveryResult;
import com.campusconnect.campusconnect.service.EmailService;
import com.campusconnect.campusconnect.service.EventService;
import com.campusconnect.campusconnect.service.RegistrationService;
import com.campusconnect.campusconnect.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final RegistrationService registrationService;
    private final EmailService emailService;
    private final UserService userService;

    public EventController(EventService eventService,
                           RegistrationService registrationService,
                           EmailService emailService,
                           UserService userService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.emailService = emailService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public String viewEventDetails(@PathVariable Long id, Model model) {
        Event event = eventService.getEventById(id);
        model.addAttribute("event", event);
        
        boolean alreadyRegistered = false;
        Optional<User> currentUserOpt = userService.getCurrentUser();
        if (currentUserOpt.isPresent() && currentUserOpt.get().isStudent()) {
            alreadyRegistered = registrationService.isStudentRegistered(id, currentUserOpt.get().getEmail());
        }
        model.addAttribute("alreadyRegistered", alreadyRegistered);

        if (!model.containsAttribute("registrationForm")) {
            RegistrationForm form = new RegistrationForm();
            currentUserOpt.ifPresent(u -> {
                form.setStudentName(u.getFullName());
                form.setStudentEmail(u.getEmail());
                form.setRollNumber(u.getRollNumber());
            });
            model.addAttribute("registrationForm", form);
        }
        return "events/details";
    }

    @GetMapping("/{id}/register")
    public String showRegistrationForm(@PathVariable Long id, Model model) {
        Event event = eventService.getEventById(id);
        if (!event.isRegistrationOpen()) {
            return "redirect:/events/" + id + "?error=" +
                    java.net.URLEncoder.encode("Registration is not available for this event.", java.nio.charset.StandardCharsets.UTF_8);
        }

        Optional<User> currentUserOpt = userService.getCurrentUser();
        if (currentUserOpt.isPresent() && currentUserOpt.get().isStudent()) {
            if (registrationService.isStudentRegistered(id, currentUserOpt.get().getEmail())) {
                return "redirect:/events/" + id + "?error=" +
                        java.net.URLEncoder.encode("You have already registered for this event.", java.nio.charset.StandardCharsets.UTF_8);
            }
        }

        model.addAttribute("event", event);
        if (!model.containsAttribute("registrationForm")) {
            RegistrationForm form = new RegistrationForm();
            currentUserOpt.ifPresent(u -> {
                form.setStudentName(u.getFullName());
                form.setStudentEmail(u.getEmail());
                form.setRollNumber(u.getRollNumber());
            });
            model.addAttribute("registrationForm", form);
        }
        return "events/register";
    }

    @PostMapping("/{id}/register")
    public String processRegistration(@PathVariable Long id,
                                      @Valid @ModelAttribute("registrationForm") RegistrationForm form,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        Event event = eventService.getEventById(id);

        // If authenticated as student, lock registration fields to the authenticated student's identity
        Optional<User> currentUserOpt = userService.getCurrentUser();
        if (currentUserOpt.isPresent()) {
            User currentUser = currentUserOpt.get();
            if (currentUser.isStudent()) {
                form.setStudentName(currentUser.getFullName());
                form.setStudentEmail(currentUser.getEmail());
                if (currentUser.getRollNumber() != null && !currentUser.getRollNumber().isBlank()) {
                    form.setRollNumber(currentUser.getRollNumber());
                }
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("event", event);
            return "events/register";
        }

        try {
            EventRegistration registration = registrationService.registerStudent(id, form);

            EmailDeliveryResult emailResult;
            if ("SENT".equalsIgnoreCase(registration.getConfirmationEmailStatus())) {
                emailResult = EmailDeliveryResult.sent("Confirmation pass dispatched to " + registration.getStudentEmail());
            } else if ("SIMULATED".equalsIgnoreCase(registration.getConfirmationEmailStatus())) {
                emailResult = EmailDeliveryResult.simulated("Simulation mode: Email pass logged to server console.");
            } else {
                String err = registration.getConfirmationEmailError() != null ? registration.getConfirmationEmailError() : "Delivery pending";
                emailResult = EmailDeliveryResult.failed(err);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Congratulations, " + registration.getStudentName() + "! Your registration is confirmed.");
            redirectAttributes.addFlashAttribute("registration", registration);
            redirectAttributes.addFlashAttribute("emailStatus", emailResult);

            return "redirect:/events/" + id + "/registration-success";
        } catch (RegistrationException ex) {
            model.addAttribute("event", event);
            model.addAttribute("errorMessage", ex.getMessage());
            return "events/register";
        }
    }

    @GetMapping("/{id}/registration-success")
    public String showRegistrationSuccess(@PathVariable Long id, Model model) {
        Event event = eventService.getEventById(id);
        model.addAttribute("event", event);
        return "events/registration-success";
    }
}
