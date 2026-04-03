package com.interview.platform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing an interviewer / panel member.
 */
@Entity
@Table(name = "interviewers")
public class Interviewer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "interviewer_seq")
    @SequenceGenerator(name = "interviewer_seq", sequenceName = "seq_interviewer_id", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "working_hours", length = 100)
    private String workingHours;

    @Column(name = "calendar_provider", length = 50)
    private String calendarProvider;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "interviewer", fetch = FetchType.LAZY)
    private List<Interview> interviews;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.calendarProvider == null) {
            this.calendarProvider = "GOOGLE";
        }
        if (this.workingHours == null) {
            this.workingHours = "09:00-18:00";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- Getters and Setters ----

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

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Interview> getInterviews() { return interviews; }
    public void setInterviews(List<Interview> interviews) { this.interviews = interviews; }
}
