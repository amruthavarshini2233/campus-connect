package com.campusconnect.campusconnect.controller;

import com.campusconnect.campusconnect.dto.StudentRegisterForm;
import com.campusconnect.campusconnect.exception.RegistrationException;
import com.campusconnect.campusconnect.model.EventRegistration;
import com.campusconnect.campusconnect.model.RegistrationStatus;
import com.campusconnect.campusconnect.model.User;
import com.campusconnect.campusconnect.service.RegistrationService;
import com.campusconnect.campusconnect.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;
    private final RegistrationService registrationService;

    public AuthController(UserService userService, RegistrationService registrationService) {
        this.userService = userService;
        this.registrationService = registrationService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register-account")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new StudentRegisterForm());
        }
        return "auth/register-account";
    }

    @PostMapping("/register-account")
    public String processRegister(@Valid @ModelAttribute("registerForm") StudentRegisterForm form,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (!form.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.registerForm", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register-account";
        }

        try {
            userService.registerStudent(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account created successfully for " + form.getEmail() + "! Please log in with your password.");
            return "redirect:/login";
        } catch (RegistrationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register-account";
        }
    }

    @GetMapping("/profile")
    public String viewProfile(Model model) {
        Optional<User> currentUserOpt = userService.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = currentUserOpt.get();
        model.addAttribute("user", user);

        if (user.isStudent()) {
            List<EventRegistration> registrations = registrationService.getStudentRegistrations(user.getEmail(), user.getRollNumber());
            model.addAttribute("registrations", registrations);
            model.addAttribute("totalPasses", registrations.size());
            model.addAttribute("activePasses", registrations.stream().filter(r -> r.getStatus() == RegistrationStatus.CONFIRMED).count());
            model.addAttribute("cancelledPasses", registrations.stream().filter(r -> r.getStatus() == RegistrationStatus.CANCELLED).count());
        }

        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@org.springframework.web.bind.annotation.RequestParam("fullName") String fullName,
                                @org.springframework.web.bind.annotation.RequestParam(name = "currentPassword", required = false) String currentPassword,
                                @org.springframework.web.bind.annotation.RequestParam(name = "newPassword", required = false) String newPassword,
                                RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = userService.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        try {
            userService.updateProfile(currentUserOpt.get(), fullName, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Profile details updated successfully.");
        } catch (RegistrationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/profile";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }
}
