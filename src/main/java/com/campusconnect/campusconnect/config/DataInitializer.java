package com.campusconnect.campusconnect.config;

import com.campusconnect.campusconnect.Event;
import com.campusconnect.campusconnect.model.EventRegistration;
import com.campusconnect.campusconnect.model.Role;
import com.campusconnect.campusconnect.model.User;
import com.campusconnect.campusconnect.repository.EventRegistrationRepository;
import com.campusconnect.campusconnect.repository.EventRepository;
import com.campusconnect.campusconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default.email:admin@campusconnect.edu}")
    private String adminEmail;

    @Value("${admin.default.password:Admin@12345}")
    private String adminPassword;

    public DataInitializer(EventRepository eventRepository,
                           EventRegistrationRepository registrationRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initializeUsers();
        initializeEvents();
    }

    private void initializeUsers() {
        if (!userRepository.existsByEmailIgnoreCase(adminEmail)) {
            User admin = new User(
                    "Campus Administrator",
                    adminEmail.trim().toLowerCase(),
                    passwordEncoder.encode(adminPassword),
                    "ADMIN-01",
                    Role.ROLE_ADMIN
            );
            userRepository.save(admin);
            log.info("Initialized default administrator account: {}", adminEmail);
        }

        if (!userRepository.existsByEmailIgnoreCase("student@college.edu")) {
            User student1 = new User(
                    "Aarav Sharma",
                    "student@college.edu",
                    passwordEncoder.encode("Student@123"),
                    "21CS001",
                    Role.ROLE_STUDENT
            );
            userRepository.save(student1);
        }

        if (!userRepository.existsByEmailIgnoreCase("diya.patel@college.edu")) {
            User student2 = new User(
                    "Diya Patel",
                    "diya.patel@college.edu",
                    passwordEncoder.encode("Student@123"),
                    "21CS042",
                    Role.ROLE_STUDENT
            );
            userRepository.save(student2);
            log.info("Initialized demo student accounts (student@college.edu, diya.patel@college.edu).");
        }
    }

    private void initializeEvents() {
        if (eventRepository.count() > 0) {
            log.info("Database already contains {} events. Skipping demo event seeding.", eventRepository.count());
            return;
        }

        log.info("Initializing realistic demo events for CampusConnect...");

        Event event1 = new Event(
                "CodeSprint 2026: 24-Hour Campus Hackathon",
                "Join the premier student hackathon of the year! Form teams of 2 to 4 to design, build, and pitch cutting-edge solutions across AI, Web3, FinTech, and Smart Campus systems. Mentorship, food, and cash prizes will be provided throughout the 24-hour sprint.",
                "Hackathon",
                "Google Developer Student Club & ACM Chapter",
                "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1200&q=80",
                LocalDate.now().plusDays(14),
                LocalTime.of(10, 0),
                "Innovation Lab & Computer Science Block",
                120,
                LocalDate.now().plusDays(13).atTime(23, 59)
        );

        Event event2 = new Event(
                "Hands-on AI & Deep Learning Masterclass",
                "An intensive 6-hour hands-on workshop covering neural network architectures, PyTorch fundamentals, computer vision, and building end-to-end generative AI applications. Bring your laptops with Python and Jupyter pre-installed.",
                "Workshop",
                "IEEE Computer Society",
                "https://images.unsplash.com/photo-1677442136019-21780ecad995?auto=format&fit=crop&w=1200&q=80",
                LocalDate.now().plusDays(7),
                LocalTime.of(14, 0),
                "Seminar Hall A (Ground Floor)",
                75,
                LocalDate.now().plusDays(6).atTime(18, 0)
        );

        Event event3 = new Event(
                "Tarang 2026: Annual Inter-College Cultural Fest",
                "Celebrate creative expression, live music, dance face-offs, drama competitions, and fashion runway showcases. Open to all students and guest entries from universities across the state. Special celebrity evening performance at sunset.",
                "Cultural",
                "Campus Cultural Council & Fine Arts Club",
                "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1200&q=80",
                LocalDate.now().plusDays(21),
                LocalTime.of(16, 30),
                "University Main Auditorium & Open Air Theatre",
                500,
                LocalDate.now().plusDays(20).atTime(20, 0)
        );

        Event event4 = new Event(
                "Full-Stack Cloud Native Systems with Spring Boot & Docker",
                "Learn industry best practices for building, containerizing, and orchestrating enterprise-grade microservices with Spring Boot 3+, Docker, and Kubernetes. Led by senior alumni software architects.",
                "Technical",
                "Department of Information Technology",
                "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1200&q=80",
                LocalDate.now().plusDays(10),
                LocalTime.of(11, 0),
                "Lab 4, Tech Building",
                60,
                LocalDate.now().plusDays(9).atTime(18, 0)
        );

        Event event5 = new Event(
                "Battle of the Bands: Musical Extravaganza",
                "Watch top student bands battle live on stage for the campus trophy. Acoustic, rock, pop, and fusion genres with live audience voting. Refreshments and merchandise will be available.",
                "Cultural",
                "Music & Acoustic Society",
                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=1200&q=80",
                LocalDate.now().plusDays(28),
                LocalTime.of(18, 0),
                "Student Amphitheatre",
                300,
                LocalDate.now().plusDays(27).atTime(15, 0)
        );

        Event event6 = new Event(
                "CyberSecurity CTF: Capture The Flag Challenge",
                "Put your ethical hacking, reverse engineering, cryptography, and network forensics skills to the test in this campus-wide jeopardy-style CTF challenge. Novice and expert tracks available.",
                "Technical",
                "Information Security Club",
                "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=1200&q=80",
                LocalDate.now().plusDays(18),
                LocalTime.of(9, 30),
                "Cyber Forensics Center (Block C)",
                80,
                LocalDate.now().plusDays(17).atTime(23, 59)
        );

        List<Event> savedEvents = eventRepository.saveAll(List.of(event1, event2, event3, event4, event5, event6));

        // Seed 2 initial sample registrations on the hackathon event for demonstration
        Event hackathon = savedEvents.get(0);
        EventRegistration reg1 = new EventRegistration(
                hackathon,
                "Aarav Sharma",
                "student@college.edu",
                "21CS001"
        );
        EventRegistration reg2 = new EventRegistration(
                hackathon,
                "Diya Patel",
                "diya.patel@college.edu",
                "21CS042"
        );
        registrationRepository.saveAll(List.of(reg1, reg2));

        log.info("Initialized {} demo events and 2 sample registrations successfully.", savedEvents.size());
    }
}
