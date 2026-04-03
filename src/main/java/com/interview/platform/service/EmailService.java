package com.interview.platform.service;

import com.interview.platform.client.GmailClient;
import com.interview.platform.client.GrokAiClient;
import com.interview.platform.entity.Interview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates AI email generation (Grok) and sending (Gmail).
 *
 * All emails are reply-based — no confirm/reschedule/cancel buttons.
 * Candidate replies are processed by EmailReplyProcessorService.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final GrokAiClient grokAiClient;
    private final GmailClient gmailClient;

    public EmailService(GrokAiClient grokAiClient, GmailClient gmailClient) {
        this.grokAiClient = grokAiClient;
        this.gmailClient = gmailClient;
    }

    /**
     * Generate and send the initial invitation email to the candidate.
     * Email contains reply instructions (CONFIRM / RESCHEDULE / CANCEL / STOP).
     * No buttons or action URLs.
     */
    public void sendInvitationEmail(Interview interview) {
        logger.info("Sending invitation email for interview ID: {}", interview.getId());

        String htmlBody = grokAiClient.generateInvitationEmail(
                interview.getCandidate().getName(),
                interview.getCandidate().getEmail(),
                interview.getTitle(),
                interview.getInterviewer().getName(),
                interview.getScheduledTime(),
                interview.getDurationMinutes(),
                interview.getInterviewer().getEmail()
        );

        gmailClient.sendHtmlEmail(
                interview.getCandidate().getEmail(),
                interview.getCandidate().getName(),
                "Interview Invitation: " + interview.getTitle(),
                htmlBody
        );

        logger.info("Invitation email sent to: {}", interview.getCandidate().getEmail());
    }

    /**
     * Generate and send the confirmation email with Google Meet link.
     * No action buttons — just the meet link and details.
     */
    public void sendConfirmationEmail(Interview interview) {
        logger.info("Sending confirmation email for interview ID: {}", interview.getId());

        String htmlBody = grokAiClient.generateConfirmationEmail(
                interview.getCandidate().getName(),
                interview.getTitle(),
                interview.getScheduledTime(),
                interview.getMeetingLink()
        );

        gmailClient.sendHtmlEmail(
                interview.getCandidate().getEmail(),
                interview.getCandidate().getName(),
                "Interview Confirmed - " + interview.getTitle(),
                htmlBody
        );

        logger.info("Confirmation email sent to: {}", interview.getCandidate().getEmail());
    }

    /**
     * Generate and send a reschedule notification email.
     * Email contains reply instructions so the cycle can continue.
     */
    public void sendRescheduleEmail(Interview interview, java.time.LocalDateTime previousTime) {
        logger.info("Sending reschedule email for interview ID: {}", interview.getId());

        String htmlBody = grokAiClient.generateRescheduleEmail(
                interview.getCandidate().getName(),
                interview.getTitle(),
                previousTime,
                interview.getScheduledTime(),
                interview.getInterviewer().getEmail()
        );

        gmailClient.sendHtmlEmail(
                interview.getCandidate().getEmail(),
                interview.getCandidate().getName(),
                "Interview Rescheduled - " + interview.getTitle(),
                htmlBody
        );

        logger.info("Reschedule email sent to: {}", interview.getCandidate().getEmail());
    }

    /**
     * Generate and send a cancellation email.
     */
    public void sendCancellationEmail(Interview interview, String reason) {
        logger.info("Sending cancellation email for interview ID: {}", interview.getId());

        String htmlBody = grokAiClient.generateCancellationEmail(
                interview.getCandidate().getName(),
                interview.getTitle(),
                interview.getScheduledTime(),
                reason
        );

        gmailClient.sendHtmlEmail(
                interview.getCandidate().getEmail(),
                interview.getCandidate().getName(),
                "Interview Cancelled - " + interview.getTitle(),
                htmlBody
        );

        logger.info("Cancellation email sent to: {}", interview.getCandidate().getEmail());
    }
}
