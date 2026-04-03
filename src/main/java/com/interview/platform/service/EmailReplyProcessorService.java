package com.interview.platform.service;

import com.interview.platform.client.GmailClient;
import com.interview.platform.client.GrokAiClient;
import com.interview.platform.dto.EmailReplyIntent;
import com.interview.platform.dto.RescheduleRequest;
import com.interview.platform.entity.Interview;
import com.interview.platform.entity.InterviewStatus;
import com.interview.platform.repository.InterviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Polls the interviewer's Gmail INBOX every 2 minutes for candidate reply emails.
 * Classifies each reply using Grok AI and acts accordingly:
 *
 *   CONFIRM    → confirm interview → create Google Meet → send confirmation email
 *   RESCHEDULE → AI picks a new slot → update DB → send new invitation email (cycle repeats)
 *   CANCEL     → cancel interview → send cancellation email
 *   STOP       → set aiStopped=true → stop all further AI processing
 *   UNKNOWN    → log warning, skip
 */
@Service
public class EmailReplyProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(EmailReplyProcessorService.class);

    private final InterviewRepository interviewRepository;
    private final GmailClient gmailClient;
    private final GrokAiClient grokAiClient;
    private final InterviewService interviewService;
    private final EmailService emailService;

    public EmailReplyProcessorService(InterviewRepository interviewRepository,
                                       GmailClient gmailClient,
                                       GrokAiClient grokAiClient,
                                       InterviewService interviewService,
                                       EmailService emailService) {
        this.interviewRepository = interviewRepository;
        this.gmailClient = gmailClient;
        this.grokAiClient = grokAiClient;
        this.interviewService = interviewService;
        this.emailService = emailService;
    }

    /**
     * Runs every 2 minutes. Finds INVITED/RESCHEDULED interviews where AI is not stopped,
     * then checks Gmail for candidate replies.
     */
    @Scheduled(fixedDelay = 120_000)
    @Transactional
    public void pollForCandidateReplies() {
        logger.info("=== Polling Gmail for candidate interview replies ===");

        List<Interview> activeInterviews = interviewRepository.findByStatusInAndAiStoppedFalse(
                List.of(InterviewStatus.INVITED.name(), InterviewStatus.RESCHEDULED.name()));

        if (activeInterviews.isEmpty()) {
            logger.info("No active interviews awaiting candidate reply.");
            return;
        }

        for (Interview interview : activeInterviews) {
            try {
                processRepliesForInterview(interview);
            } catch (Exception e) {
                logger.error("Error processing replies for interview {}: {}",
                        interview.getId(), e.getMessage(), e);
            }
        }
    }

    // =========================================================
    // PRIVATE: Per-interview reply processing
    // =========================================================

    private void processRepliesForInterview(Interview interview) {
        // Use interview title as subject keyword to filter replies
        String subjectKeyword = interview.getTitle();

        List<Map<String, String>> replies = gmailClient.fetchUnreadReplies(subjectKeyword);

        for (Map<String, String> reply : replies) {
            String messageId = reply.get("messageId");
            String fromEmail  = reply.get("from");
            String emailBody  = reply.get("body");

            // Only process replies from this interview's candidate
            if (!isCandidateEmail(fromEmail, interview.getCandidate().getEmail())) {
                logger.debug("Skipping reply from '{}' — not the candidate for interview {}",
                        fromEmail, interview.getId());
                continue;
            }

            // Skip already-processed messages
            if (messageId.equals(interview.getLastProcessedEmailId())) {
                logger.debug("Skipping already-processed message {} for interview {}",
                        messageId, interview.getId());
                continue;
            }

            logger.info("Processing reply from {} for interview {} (msgId={})",
                    fromEmail, interview.getId(), messageId);

            // Mark message as processed before acting (prevents double-processing on error)
            interview.setLastProcessedEmailId(messageId);
            interviewRepository.save(interview);

            // Classify with AI
            EmailReplyIntent intent = grokAiClient.parseEmailReply(
                    emailBody, interview.getTitle(), interview.getScheduledTime());

            logger.info("AI classified reply as '{}' for interview {} — reason: {}",
                    intent.getIntent(), interview.getId(), intent.getReason());

            // Act on intent
            switch (intent.getIntent().toUpperCase()) {
                case "CONFIRM"    -> handleConfirm(interview);
                case "RESCHEDULE" -> handleReschedule(interview, intent);
                case "CANCEL"     -> handleCancel(interview, intent.getReason());
                case "STOP"       -> handleStop(interview);
                default -> logger.warn("Unrecognised intent '{}' for interview {} — skipping",
                        intent.getIntent(), interview.getId());
            }

            // Only process the FIRST matching reply per polling cycle per interview
            break;
        }
    }

    // =========================================================
    // HANDLERS
    // =========================================================

    private void handleConfirm(Interview interview) {
        logger.info("Candidate CONFIRMED interview {}", interview.getId());
        interview.setStatus(InterviewStatus.CONFIRMED.name());
        interview.setConfirmedAt(LocalDateTime.now());
        interviewRepository.save(interview);
        interviewRepository.flush(); // force the save before the next call
        
        try {
            interviewService.createGoogleMeetAndSendConfirmation(interview);
        } catch (Exception e) {
            logger.warn("Google Meet creation failed for interview {} but status is saved as CONFIRMED: {}",
                    interview.getId(), e.getMessage());
            // Status is already CONFIRMED in DB — this is non-fatal
        }
    }

    private void handleReschedule(Interview interview, EmailReplyIntent intent) {
        logger.info("Candidate requested RESCHEDULE for interview {}", interview.getId());

        // AI suggests a new slot based on candidate's preferred times
        LocalDateTime newTime = grokAiClient.suggestNewInterviewTime(
                interview.getInterviewer().getName(),
                intent.getPreferredTimes(),
                interview.getScheduledTime());

        // Reschedule via InterviewService (handles DB update + email)
        RescheduleRequest req = new RescheduleRequest();
        req.setNewScheduledTime(newTime);
        req.setDurationMinutes(interview.getDurationMinutes());
        interviewService.rescheduleInterview(interview.getId(), req);

        logger.info("Interview {} AI-rescheduled to {}", interview.getId(), newTime);
    }

    private void handleCancel(Interview interview, String reason) {
        logger.info("Candidate CANCELLED interview {}", interview.getId());
        String fullReason = "Cancelled by candidate via email reply."
                + (reason != null && !reason.isBlank() ? " Reason: " + reason : "");
        interviewService.cancelInterviewById(interview.getId(), fullReason);
    }

    private void handleStop(Interview interview) {
        logger.info("Candidate sent STOP for interview {} — halting AI processing", interview.getId());
        interview.setAiStopped(true);
        interviewRepository.save(interview);
    }

    // =========================================================
    // HELPER
    // =========================================================

    private boolean isCandidateEmail(String fromHeader, String candidateEmail) {
        return fromHeader != null &&
               fromHeader.toLowerCase().contains(candidateEmail.toLowerCase());
    }
}
