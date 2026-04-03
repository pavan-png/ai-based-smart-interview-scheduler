package com.interview.platform.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * DTO for creating a new interview from the HR dashboard.
 */
public class CreateInterviewRequest {

    @NotBlank(message = "Candidate name is required")
    @Size(max = 255, message = "Candidate name must not exceed 255 characters")
    private String candidateName;

    @NotBlank(message = "Candidate email is required")
    @Email(message = "Please provide a valid candidate email")
    private String candidateEmail;

    private String candidatePhone;

    private String candidateTimezone;

    @NotNull(message = "Interviewer ID is required")
    private Long interviewerId;

    @NotBlank(message = "Interview title is required")
    private String title;

    @NotNull(message = "Scheduled time is required")
    @Future(message = "Interview must be scheduled in the future")
    private LocalDateTime scheduledTime;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration must not exceed 480 minutes")
    private Integer durationMinutes;

    private String notes;

    // ---- Getters and Setters ----

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getCandidatePhone() { return candidatePhone; }
    public void setCandidatePhone(String candidatePhone) { this.candidatePhone = candidatePhone; }

    public String getCandidateTimezone() { return candidateTimezone; }
    public void setCandidateTimezone(String candidateTimezone) { this.candidateTimezone = candidateTimezone; }

    public Long getInterviewerId() { return interviewerId; }
    public void setInterviewerId(Long interviewerId) { this.interviewerId = interviewerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
