package com.campusconnect.campusconnect.dto;

import com.campusconnect.campusconnect.Event;

import java.util.List;
import java.util.Map;

public class DashboardStatsDto {

    private long totalEvents;
    private long upcomingEvents;
    private long totalRegistrations;
    private long activeRegistrations;
    private long totalCapacity;
    private long availableCapacity;
    private long totalStudents;
    private List<Event> popularEvents;
    private Map<String, Long> categoryCounts;

    public DashboardStatsDto() {
    }

    public DashboardStatsDto(long totalEvents, long upcomingEvents, long totalRegistrations,
                             long activeRegistrations, List<Event> popularEvents,
                             Map<String, Long> categoryCounts) {
        this.totalEvents = totalEvents;
        this.upcomingEvents = upcomingEvents;
        this.totalRegistrations = totalRegistrations;
        this.activeRegistrations = activeRegistrations;
        this.popularEvents = popularEvents;
        this.categoryCounts = categoryCounts;
    }

    public DashboardStatsDto(long totalEvents, long upcomingEvents, long totalRegistrations,
                             long activeRegistrations, long totalCapacity, long availableCapacity,
                             List<Event> popularEvents, Map<String, Long> categoryCounts) {
        this.totalEvents = totalEvents;
        this.upcomingEvents = upcomingEvents;
        this.totalRegistrations = totalRegistrations;
        this.activeRegistrations = activeRegistrations;
        this.totalCapacity = totalCapacity;
        this.availableCapacity = availableCapacity;
        this.popularEvents = popularEvents;
        this.categoryCounts = categoryCounts;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public long getUpcomingEvents() {
        return upcomingEvents;
    }

    public void setUpcomingEvents(long upcomingEvents) {
        this.upcomingEvents = upcomingEvents;
    }

    public long getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(long totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public long getActiveRegistrations() {
        return activeRegistrations;
    }

    public void setActiveRegistrations(long activeRegistrations) {
        this.activeRegistrations = activeRegistrations;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public List<Event> getPopularEvents() {
        return popularEvents;
    }

    public void setPopularEvents(List<Event> popularEvents) {
        this.popularEvents = popularEvents;
    }

    public Map<String, Long> getCategoryCounts() {
        return categoryCounts;
    }

    public void setCategoryCounts(Map<String, Long> categoryCounts) {
        this.categoryCounts = categoryCounts;
    }

    public long getCancelledRegistrations() {
        return Math.max(0, totalRegistrations - activeRegistrations);
    }
}
