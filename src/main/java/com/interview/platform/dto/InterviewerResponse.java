package com.interview.platform.dto;

import java.time.LocalDateTime;

/**
 * DTO representing an Interviewer in API responses.
 */
public class InterviewerResponse {

    private Long id;
    private String name;
    private String email;
    private String department;
    private String workingHours;
    private String calendarProvider;
    private boolean active;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }

    public String getCalendarProvider() { return calendarProvider; }
    public void setCalendarProvider(String calendarProvider) { this.calendarProvider = calendarProvider; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
