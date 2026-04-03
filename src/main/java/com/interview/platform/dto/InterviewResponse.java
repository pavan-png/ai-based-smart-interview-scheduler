package com.interview.platform.dto;

import java.time.LocalDateTime;

/**
 * DTO representing the full interview detail returned to the frontend.
 */
public class InterviewResponse {

    private Long id;
    private String title;
    private String status;
    private LocalDateTime scheduledTime;
    private Integer durationMinutes;
    private String meetingLink;

    // Candidate info
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidateTimezone;

    // Interviewer info
    private Long interviewerId;
    private String interviewerName;
    private String interviewerEmail;
    private String interviewerDepartment;

    // Timestamps
    private LocalDateTime inviteSentAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime rescheduledAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String notes;
    private boolean aiStopped;

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getCandidateTimezone() { return candidateTimezone; }
    public void setCandidateTimezone(String candidateTimezone) { this.candidateTimezone = candidateTimezone; }

    public Long getInterviewerId() { return interviewerId; }
    public void setInterviewerId(Long interviewerId) { this.interviewerId = interviewerId; }

    public String getInterviewerName() { return interviewerName; }
    public void setInterviewerName(String interviewerName) { this.interviewerName = interviewerName; }

    public String getInterviewerEmail() { return interviewerEmail; }
    public void setInterviewerEmail(String interviewerEmail) { this.interviewerEmail = interviewerEmail; }

    public String getInterviewerDepartment() { return interviewerDepartment; }
    public void setInterviewerDepartment(String interviewerDepartment) { this.interviewerDepartment = interviewerDepartment; }

    public LocalDateTime getInviteSentAt() { return inviteSentAt; }
    public void setInviteSentAt(LocalDateTime inviteSentAt) { this.inviteSentAt = inviteSentAt; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getRescheduledAt() { return rescheduledAt; }
    public void setRescheduledAt(LocalDateTime rescheduledAt) { this.rescheduledAt = rescheduledAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isAiStopped() { return aiStopped; }
    public void setAiStopped(boolean aiStopped) { this.aiStopped = aiStopped; }
}
