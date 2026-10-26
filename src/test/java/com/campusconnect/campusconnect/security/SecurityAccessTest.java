package com.campusconnect.campusconnect.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Unauthenticated user accessing /admin is redirected to /login")
    void testUnauthenticatedAdminAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Unauthenticated user performing POST to admin operation is redirected to login")
    void testUnauthenticatedAdminPostBlocked() throws Exception {
        mockMvc.perform(post("/admin/events/1/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("Authenticated student accessing /admin receives 403 Forbidden")
    void testStudentAccessToAdminDenied() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("Authenticated student attempting to create an event via POST receives 403 Forbidden")
    void testStudentCreateEventDenied() throws Exception {
        mockMvc.perform(post("/admin/events/new")
                        .with(csrf())
                        .param("title", "Unauthorized Event")
                        .param("category", "Technical")
                        .param("organizer", "Intruder")
                        .param("description", "Hacked event description")
                        .param("eventDate", LocalDate.now().plusDays(5).toString())
                        .param("venue", "Unauthorized")
                        .param("capacity", "100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("Authenticated student attempting to cancel an event via POST receives 403 Forbidden")
    void testStudentCancelEventDenied() throws Exception {
        mockMvc.perform(post("/admin/events/1/cancel").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student@college.edu", roles = {"STUDENT"})
    @DisplayName("Authenticated student attempting to view admin participants list receives 403 Forbidden")
    void testStudentViewParticipantsDenied() throws Exception {
        mockMvc.perform(get("/admin/events/1/participants"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@campusconnect.edu", roles = {"ADMIN"})
    @DisplayName("Authenticated administrator can access /admin successfully")
    void testAdminAccessAllowed() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("stats", "recentEvents"));
    }

    @Test
    @DisplayName("Public user can access homepage, event details, and login without authentication")
    void testPublicRoutesAccessible() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));

        mockMvc.perform(get("/register-account"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-account"));
    }
}
