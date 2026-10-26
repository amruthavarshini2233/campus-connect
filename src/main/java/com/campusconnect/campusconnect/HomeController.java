package com.campusconnect.campusconnect;

import com.campusconnect.campusconnect.dto.DashboardStatsDto;
import com.campusconnect.campusconnect.model.User;
import com.campusconnect.campusconnect.service.EventService;
import com.campusconnect.campusconnect.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private final EventService eventService;
    private final UserService userService;

    public HomeController(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    /**
     * Homepage: Focuses strictly on platform showcase, statistics, category discovery,
     * and platform features.
     * When accessed by an authenticated Administrator, it immediately routes them to /admin.
     */
    @GetMapping("/")
    public String home(@RequestParam(name = "keyword", required = false) String keyword,
                       @RequestParam(name = "category", required = false) String category,
                       @RequestParam(name = "publicView", required = false, defaultValue = "false") boolean publicView,
                       Model model) {
        // Authenticated administrators are routed strictly to the Admin Portal
        if (!publicView && userService != null) {
            Optional<User> currentUserOpt = userService.getCurrentUser();
            if (currentUserOpt.isPresent() && currentUserOpt.get().isAdmin()) {
                return "redirect:/admin";
            }
        }

        // If search or specific category was submitted from the home search bar, forward to the dedicated explore page
        if ((keyword != null && !keyword.isBlank()) || (category != null && !category.isBlank() && !category.equalsIgnoreCase("All"))) {
            String q = keyword != null ? URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8) : "";
            String cat = category != null ? URLEncoder.encode(category.trim(), StandardCharsets.UTF_8) : "All";
            return "redirect:/explore?keyword=" + q + "&category=" + cat;
        }

        List<Event> upcomingEvents = eventService.getUpcomingEvents();
        DashboardStatsDto stats = eventService.getDashboardStats();

        model.addAttribute("events", upcomingEvents);
        model.addAttribute("featuredEvent", !upcomingEvents.isEmpty() ? upcomingEvents.get(0) : null);
        model.addAttribute("totalEvents", stats.getTotalEvents());
        model.addAttribute("upcomingCount", stats.getUpcomingEvents());
        model.addAttribute("totalRegistrations", stats.getTotalRegistrations());
        model.addAttribute("activeRegistrations", stats.getActiveRegistrations());

        return "index";
    }

    /**
     * Dedicated Explore Page: Displays the complete catalog of upcoming college events,
     * live search input, category filter pills, real-time seat counts, and registration links.
     * Events cancelled by admin are strictly excluded from results.
     */
    @GetMapping("/explore")
    public String explore(@RequestParam(name = "keyword", required = false) String keyword,
                          @RequestParam(name = "category", required = false) String category,
                          Model model) {
        List<Event> events;
        if ((keyword != null && !keyword.isBlank()) || (category != null && !category.isBlank() && !category.equalsIgnoreCase("All"))) {
            events = eventService.searchUpcomingEvents(keyword, category);
        } else {
            events = eventService.getUpcomingEvents();
        }

        DashboardStatsDto stats = eventService.getDashboardStats();

        model.addAttribute("events", events);
        model.addAttribute("featuredEvent", !events.isEmpty() ? events.get(0) : null);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedCategory", (category != null && !category.isBlank()) ? category : "All");
        model.addAttribute("totalEvents", stats.getTotalEvents());
        model.addAttribute("upcomingCount", stats.getUpcomingEvents());
        model.addAttribute("totalRegistrations", stats.getTotalRegistrations());
        model.addAttribute("activeRegistrations", stats.getActiveRegistrations());

        return "events/explore";
    }
}
