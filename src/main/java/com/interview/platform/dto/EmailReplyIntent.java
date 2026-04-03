package com.interview.platform.dto;

import java.util.List;

/**
 * Represents the AI-classified intent from a candidate's email reply.
 * Intent values: CONFIRM | RESCHEDULE | CANCEL | STOP | UNKNOWN
 */
public class EmailReplyIntent {

    private String intent;
    private List<String> preferredTimes;
    private String reason;

    public EmailReplyIntent() {}

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public List<String> getPreferredTimes() { return preferredTimes; }
    public void setPreferredTimes(List<String> preferredTimes) { this.preferredTimes = preferredTimes; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
