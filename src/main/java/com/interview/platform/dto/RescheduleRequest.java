package com.interview.platform.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO for rescheduling an existing interview.
 */
public class RescheduleRequest {

    @NotNull(message = "New scheduled time is required")
    @Future(message = "New interview time must be in the future")
    private LocalDateTime newScheduledTime;

    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration must not exceed 480 minutes")
    private Integer durationMinutes;

    private String reason;

    public LocalDateTime getNewScheduledTime() { return newScheduledTime; }
    public void setNewScheduledTime(LocalDateTime newScheduledTime) { this.newScheduledTime = newScheduledTime; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
