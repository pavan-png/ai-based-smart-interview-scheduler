package com.interview.platform.entity;

/**
 * Represents all possible lifecycle states of an Interview.
 */
public enum InterviewStatus {
    /** Initial state: invitation email sent to candidate */
    INVITED,

    /** Candidate confirmed the interview */
    CONFIRMED,

    /** Candidate or HR rescheduled the interview */
    RESCHEDULED,

    /** Interview was cancelled */
    CANCELLED,

    /** Interview was completed */
    COMPLETED
}
