package com.interview.platform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Core entity representing an interview session.
 * Tracks the complete lifecycle: INVITED → CONFIRMED → COMPLETED (or RESCHEDULED / CANCELLED)
 */
@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "interview_seq")
    @SequenceGenerator(name = "interview_seq", sequenceName = "seq_interview_id", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private Interviewer interviewer;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "meeting_link", length = 1000)
    private String meetingLink;

    @Column(name = "calendar_event_id", length = 500)
    private String calendarEventId;

    /**
     * Interview status: INVITED | CONFIRMED | RESCHEDULED | CANCELLED | COMPLETED
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /** Secure JWT token embedded in email action links */
    @Column(name = "action_token", length = 1000)
    private String actionToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "invite_sent_at")
    private LocalDateTime inviteSentAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "rescheduled_at")
    private LocalDateTime rescheduledAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * When true, AI email polling/auto-reply is stopped for this interview.
     * The HR team or the candidate can trigger this via the STOP instruction in the email.
     */
    @Column(name = "ai_stopped", nullable = false)
    private boolean aiStopped = false;

    /**
     * Tracks the last Gmail message number we processed for this interview,
     * to avoid re-processing the same reply.
     */
    @Column(name = "last_processed_email_id", length = 500)
    private String lastProcessedEmailId;

    @Lob
    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = InterviewStatus.INVITED.name();
        }
        if (this.durationMinutes == null) {
            this.durationMinutes = 60;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }

    public Interviewer getInterviewer() { return interviewer; }
    public void setInterviewer(Interviewer interviewer) { this.interviewer = interviewer; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getCalendarEventId() { return calendarEventId; }
    public void setCalendarEventId(String calendarEventId) { this.calendarEventId = calendarEventId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getActionToken() { return actionToken; }
    public void setActionToken(String actionToken) { this.actionToken = actionToken; }

    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public LocalDateTime getInviteSentAt() { return inviteSentAt; }
    public void setInviteSentAt(LocalDateTime inviteSentAt) { this.inviteSentAt = inviteSentAt; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getRescheduledAt() { return rescheduledAt; }
    public void setRescheduledAt(LocalDateTime rescheduledAt) { this.rescheduledAt = rescheduledAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public boolean isAiStopped() { return aiStopped; }
    public void setAiStopped(boolean aiStopped) { this.aiStopped = aiStopped; }

    public String getLastProcessedEmailId() { return lastProcessedEmailId; }
    public void setLastProcessedEmailId(String id) { this.lastProcessedEmailId = id; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
