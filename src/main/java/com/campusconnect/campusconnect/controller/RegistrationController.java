package com.campusconnect.campusconnect.controller;

import com.campusconnect.campusconnect.exception.RegistrationException;
import com.campusconnect.campusconnect.model.EventRegistration;
import com.campusconnect.campusconnect.model.User;
import com.campusconnect.campusconnect.service.EmailDeliveryResult;
import com.campusconnect.campusconnect.service.RegistrationService;
import com.campusconnect.campusconnect.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/my-registrations")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final UserService userService;

    public RegistrationController(RegistrationService registrationService, UserService userService) {
        this.registrationService = registrationService;
        this.userService = userService;
    }

    @GetMapping
    public String showMyRegistrations(@RequestParam(name = "email", required = false) String requestedEmail,
                                      Model model) {
        Optional<User> currentUserOpt = userService.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        User currentUser = currentUserOpt.get();
        String lookupEmail = currentUser.getEmail();
        String lookupRoll = currentUser.getRollNumber();

        // Admins can optionally inspect another email, students are strictly locked to their own email
        if (currentUser.isAdmin() && requestedEmail != null && !requestedEmail.isBlank()) {
            lookupEmail = requestedEmail.trim().toLowerCase();
            lookupRoll = null;
        }

        List<EventRegistration> registrations = registrationService.getStudentRegistrations(lookupEmail, lookupRoll);

        model.addAttribute("registrations", registrations);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeCount", registrations.stream().filter(r -> r.getStatus().name().equals("CONFIRMED")).count());
        model.addAttribute("lookupEmail", lookupEmail);

        return "registrations/my-registrations";
    }

    @PostMapping("/cancel")
    public String cancelRegistration(@RequestParam("registrationId") Long registrationId,
                                     RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = userService.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        User currentUser = currentUserOpt.get();
        try {
            registrationService.cancelRegistration(registrationId, currentUser.getEmail(), currentUser.getRollNumber());
            redirectAttributes.addFlashAttribute("successMessage", "Your registration has been successfully cancelled.");
        } catch (RegistrationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/my-registrations";
    }

    @PostMapping("/resend-confirmation")
    public String resendConfirmation(@RequestParam("registrationId") Long registrationId,
                                     RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = userService.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        try {
            EmailDeliveryResult result = registrationService.resendConfirmation(registrationId, currentUserOpt.get());
            if (result.isSuccess()) {
                redirectAttributes.addFlashAttribute("successMessage", result.getMessage());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", result.getMessage());
            }
        } catch (RegistrationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/my-registrations";
    }
}
