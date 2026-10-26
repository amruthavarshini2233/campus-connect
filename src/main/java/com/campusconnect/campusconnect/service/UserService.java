package com.campusconnect.campusconnect.service;

import com.campusconnect.campusconnect.dto.StudentRegisterForm;
import com.campusconnect.campusconnect.exception.RegistrationException;
import com.campusconnect.campusconnect.model.Role;
import com.campusconnect.campusconnect.model.User;
import com.campusconnect.campusconnect.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }

    @Transactional
    public User registerStudent(StudentRegisterForm form) {
        if (!form.isPasswordMatching()) {
            throw new RegistrationException("Passwords do not match.");
        }

        String normalizedEmail = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new RegistrationException("An account with email '" + normalizedEmail + "' already exists.");
        }

        String normalizedRoll = form.getRollNumber().trim().toUpperCase();
        if (userRepository.existsByRollNumberIgnoreCase(normalizedRoll)) {
            throw new RegistrationException("An account with Roll Number '" + normalizedRoll + "' already exists.");
        }

        User user = new User(
                form.getFullName().trim(),
                normalizedEmail,
                passwordEncoder.encode(form.getPassword()),
                normalizedRoll,
                Role.ROLE_STUDENT
        );

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(User user, String newFullName, String currentPassword, String newPassword) {
        if (newFullName != null && !newFullName.isBlank()) {
            user.setFullName(newFullName.trim());
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || currentPassword.isBlank()) {
                throw new RegistrationException("Current password is required to set a new password.");
            }
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new RegistrationException("Current password verification failed. Please try again.");
            }
            if (newPassword.length() < 6) {
                throw new RegistrationException("New password must be at least 6 characters.");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return findByEmail(auth.getName());
    }

    public long countStudents() {
        return userRepository.countByRole(Role.ROLE_STUDENT);
    }

    public java.util.List<User> getAllUsers(String keyword, Role role) {
        if ((keyword != null && !keyword.isBlank()) || role != null) {
            return userRepository.searchUsers(keyword, role);
        }
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RegistrationException("User not found with ID: " + userId));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }
}
