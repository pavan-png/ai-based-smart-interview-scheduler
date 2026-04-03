package com.interview.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the AI-Powered Interview Lifecycle Automation Platform.
 *
 * Workflow:
 * 1. HR creates interview → AI generates reply-based invitation email (no buttons)
 * 2. Candidate replies with CONFIRM / RESCHEDULE / CANCEL / STOP
 * 3. EmailReplyProcessorService polls Gmail every 2 min, classifies reply via Grok AI
 * 4. CONFIRM → Google Meet created → confirmation email sent → link on dashboard
 * 5. RESCHEDULE → AI picks new slot → new invitation email → cycle repeats
 * 6. CANCEL → cancellation email sent
 * 7. STOP → AI processing halted for that interview
 */
@SpringBootApplication
@EnableScheduling
public class InterviewPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewPlatformApplication.class, args);
    }
}
