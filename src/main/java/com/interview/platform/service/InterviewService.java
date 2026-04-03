package com.interview.platform.service;

import com.google.api.services.calendar.model.Event;
import com.interview.platform.client.GoogleCalendarClient;
import com.interview.platform.dto.CreateInterviewRequest;
import com.interview.platform.dto.InterviewResponse;
import com.interview.platform.dto.RescheduleRequest;
import com.interview.platform.entity.Candidate;
import com.interview.platform.entity.Interview;
import com.interview.platform.entity.InterviewStatus;
import com.interview.platform.entity.Interviewer;
import com.interview.platform.exception.InterviewStateException;
import com.interview.platform.exception.InvalidTokenException;
import com.interview.platform.exception.ResourceNotFoundException;
import com.interview.platform.repository.InterviewRepository;
import com.interview.platform.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core service implementing the full interview lifecycle:
 * PHASE 1 (HR creates) → PHASE 2 (candidate confirms) →
 * PHASE 3 (Google Meet created) → PHASE 4 (confirmation email sent).
 * Also handles RESCHEDULE and CANCEL flows.
 */
@Service
public class InterviewService {

    private static final Logger logger = LoggerFactory.getLogger(InterviewService.class);

    private final InterviewRepository interviewRepository;
    private final CandidateService candidateService;
    private final InterviewerService interviewerService;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleCalendarClient googleCalendarClient;
    private final EmailService emailService;

    public InterviewService(InterviewRepository interviewRepository,
                             CandidateService candidateService,
                             InterviewerService interviewerService,
                             JwtTokenProvider jwtTokenProvider,
                             GoogleCalendarClient googleCalendarClient,
                             EmailService emailService) {
        this.interviewRepository = interviewRepository;
        this.candidateService = candidateService;
        this.interviewerService = interviewerService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleCalendarClient = googleCalendarClient;
        this.emailService = emailService;
    }

    // ============================================================
    // PHASE 1: HR Creates Interview
    // ============================================================

    /**
     * Create a new interview:
     * 1. Save/find candidate
     * 2. Save interview with INVITED status
     * 3. Generate secure action token
     * 4. Generate AI email via Grok
     * 5. Send email via Gmail
     */
    @Transactional
    public InterviewResponse createInterview(CreateInterviewRequest request) {
        logger.info("Creating interview for candidate: {}", request.getCandidateEmail());

        // Step 1: Find or create candidate
        Candidate candidate = candidateService.findOrCreateCandidate(
                request.getCandidateName(),
                request.getCandidateEmail(),
                request.getCandidatePhone(),
                request.getCandidateTimezone()
        );

        // Step 2: Validate interviewer exists
        Interviewer interviewer = interviewerService.findInterviewerById(request.getInterviewerId());

        // Step 3: Create interview record
        Interview interview = new Interview();
        interview.setCandidate(candidate);
        interview.setInterviewer(interviewer);
        interview.setTitle(request.getTitle());
        interview.setScheduledTime(request.getScheduledTime());
        interview.setDurationMinutes(request.getDurationMinutes());
        interview.setStatus(InterviewStatus.INVITED.name());
        interview.setNotes(request.getNotes());

        // Save first to get the ID
        Interview savedInterview = interviewRepository.save(interview);

        // Step 4: Generate action token embedding the interview ID
        String actionToken = jwtTokenProvider.generateActionToken(
                savedInterview.getId(), "CANDIDATE_ACTION");

        savedInterview.setActionToken(actionToken);
        savedInterview.setTokenExpiresAt(LocalDateTime.now().plusDays(7));

        Interview finalInterview = interviewRepository.save(savedInterview);

        // Step 5: Send invitation email
        // Interview creation is NOT rolled back if email fails —
        // HR can see inviteSentAt is null on the dashboard and retry.
        try {
            emailService.sendInvitationEmail(finalInterview);
            finalInterview.setInviteSentAt(LocalDateTime.now());
            finalInterview = interviewRepository.save(finalInterview);
            logger.info("Invitation email sent successfully for interview ID: {}",
                    finalInterview.getId());
        } catch (Exception ex) {
            logger.error("=== INVITATION EMAIL FAILED for interview ID: {} ===",
                    finalInterview.getId());
            logger.error("Root cause: {}", ex.getMessage(), ex);
            // inviteSentAt stays null — visible on dashboard as unsent
        }

        logger.info("Interview created successfully with ID: {}", finalInterview.getId());
        return toResponse(finalInterview);
    }

    // ============================================================
    // PHASE 2: Candidate Confirms
    // ============================================================

    /**
     * Process candidate confirmation via secure token link.
     * Validates token, updates status, then triggers Google Meet creation.
     */
    @Transactional
    public InterviewResponse confirmInterview(String token) {
        logger.info("Processing interview confirmation...");

        // Validate token
        jwtTokenProvider.validateActionToken(token);

        // Find interview by token
        Interview interview = interviewRepository.findByActionToken(token)
                .orElseThrow(() -> new InvalidTokenException(
                        "No interview found for this action link."));

        // Check valid state
        if (!InterviewStatus.INVITED.name().equals(interview.getStatus()) &&
            !InterviewStatus.RESCHEDULED.name().equals(interview.getStatus())) {
            throw new InterviewStateException(
                    "Interview cannot be confirmed. Current status: " + interview.getStatus());
        }

        // Update to CONFIRMED
        interview.setStatus(InterviewStatus.CONFIRMED.name());
        interview.setConfirmedAt(LocalDateTime.now());
        interviewRepository.save(interview);

        logger.info("Interview {} confirmed by candidate.", interview.getId());

        // PHASE 3: Create Google Meet and send confirmation email
        createGoogleMeetAndSendConfirmation(interview);

        return toResponse(interview);
    }

    // ============================================================
    // PHASE 3 + 4: Create Google Meet and Send Confirmation Email
    // ============================================================

    /**
     * Called after candidate confirms:
     * 1. Create Google Calendar event with Meet link
     * 2. Save meet link to DB
     * 3. Send confirmation email with Meet link
     */
    @Transactional
    public void createGoogleMeetAndSendConfirmation(Interview interview) {
        logger.info("Creating Google Meet for interview ID: {}", interview.getId());

        try {
            String description = String.format(
                    "Interview for: %s\nCandidate: %s <%s>\nDuration: %d minutes",
                    interview.getTitle(),
                    interview.getCandidate().getName(),
                    interview.getCandidate().getEmail(),
                    interview.getDurationMinutes()
            );

            // Create Google Calendar event
            Event calendarEvent = googleCalendarClient.createInterviewEvent(
                    interview.getTitle(),
                    description,
                    interview.getScheduledTime(),
                    interview.getDurationMinutes(),
                    interview.getCandidate().getTimezone(),
                    interview.getCandidate().getEmail(),
                    interview.getInterviewer().getEmail()
            );

            // Extract and save the Meet link
            String meetLink = googleCalendarClient.extractMeetLink(calendarEvent);
            interview.setMeetingLink(meetLink);
            interview.setCalendarEventId(calendarEvent.getId());
            interviewRepository.save(interview);

            logger.info("Google Meet created: {}", meetLink);

            // PHASE 4: Send confirmation email with Meet link
            try {
                emailService.sendConfirmationEmail(interview);
                logger.info("Confirmation email sent successfully for interview ID: {}",
                        interview.getId());
            } catch (Exception emailEx) {
                logger.error("=== CONFIRMATION EMAIL FAILED for interview ID: {} ===",
                        interview.getId());
                logger.error("Root cause: {}", emailEx.getMessage(), emailEx);
            }

        } catch (Exception ex) {
            logger.error("=== GOOGLE MEET CREATION FAILED for interview ID: {} ===",
                    interview.getId());
            logger.error("Root cause: {}", ex.getMessage(), ex);
            // Interview is already CONFIRMED — Meet failure is non-critical.
            // HR team can create the Meet link manually.
        }
    }

    // ============================================================
    // RESCHEDULE FLOW
    // ============================================================

    /**
     * Reschedule an interview triggered by candidate clicking reschedule link.
     * Just updates status to RESCHEDULED. HR then sets the new time.
     */
    @Transactional(readOnly = true)
    public InterviewResponse getInterviewByToken(String token) {
        jwtTokenProvider.validateActionToken(token);

        Interview interview = interviewRepository.findByActionToken(token)
                .orElseThrow(() -> new InvalidTokenException(
                        "No interview found for this action link."));

        return toResponse(interview);
    }

    /**
     * HR reschedules an interview with a new date/time.
     * Deletes old calendar event, updates time, sends reschedule email.
     */
    @Transactional
    public InterviewResponse rescheduleInterview(Long interviewId, RescheduleRequest request) {
        logger.info("Rescheduling interview ID: {}", interviewId);

        Interview interview = findInterviewById(interviewId);

        // Validate state
        if (InterviewStatus.CANCELLED.name().equals(interview.getStatus()) ||
            InterviewStatus.COMPLETED.name().equals(interview.getStatus())) {
            throw new InterviewStateException(
                    "Cannot reschedule interview with status: " + interview.getStatus());
        }

        LocalDateTime previousTime = interview.getScheduledTime();

        // Delete old calendar event if exists
        if (interview.getCalendarEventId() != null) {
            googleCalendarClient.deleteEvent(interview.getCalendarEventId());
            interview.setCalendarEventId(null);
            interview.setMeetingLink(null);
        }

        // Update interview details
        interview.setScheduledTime(request.getNewScheduledTime());
        if (request.getDurationMinutes() != null) {
            interview.setDurationMinutes(request.getDurationMinutes());
        }
        interview.setStatus(InterviewStatus.RESCHEDULED.name());
        interview.setRescheduledAt(LocalDateTime.now());
        interview.setConfirmedAt(null);

        // Regenerate action token for the new invite
        String newToken = jwtTokenProvider.generateActionToken(
                interview.getId(), "CANDIDATE_ACTION");
        interview.setActionToken(newToken);
        interview.setTokenExpiresAt(LocalDateTime.now().plusDays(7));
        interview.setInviteSentAt(null);

        interviewRepository.save(interview);

        // Send reschedule email
        try {
            emailService.sendRescheduleEmail(interview, previousTime);
            interview.setInviteSentAt(LocalDateTime.now());
            interviewRepository.save(interview);
            logger.info("Reschedule email sent successfully for interview ID: {}",
                    interview.getId());
        } catch (Exception ex) {
            logger.error("=== RESCHEDULE EMAIL FAILED for interview ID: {} ===",
                    interview.getId());
            logger.error("Root cause: {}", ex.getMessage(), ex);
        }

        return toResponse(interview);
    }

    /**
     * Candidate-triggered reschedule via token link (just updates status to RESCHEDULED).
     * HR will then set the new time via the dashboard.
     */
    @Transactional
    public InterviewResponse candidateRequestsReschedule(String token) {
        jwtTokenProvider.validateActionToken(token);

        Interview interview = interviewRepository.findByActionToken(token)
                .orElseThrow(() -> new InvalidTokenException(
                        "No interview found for this action link."));

        if (InterviewStatus.CANCELLED.name().equals(interview.getStatus()) ||
            InterviewStatus.COMPLETED.name().equals(interview.getStatus())) {
            throw new InterviewStateException(
                    "Cannot reschedule interview with status: " + interview.getStatus());
        }

        // Delete calendar event if exists
        if (interview.getCalendarEventId() != null) {
            googleCalendarClient.deleteEvent(interview.getCalendarEventId());
            interview.setCalendarEventId(null);
            interview.setMeetingLink(null);
        }

        interview.setStatus(InterviewStatus.RESCHEDULED.name());
        interview.setRescheduledAt(LocalDateTime.now());
        interviewRepository.save(interview);

        logger.info("Candidate requested reschedule for interview: {}", interview.getId());
        return toResponse(interview);
    }

    // ============================================================
    // CANCEL FLOW
    // ============================================================

    /**
     * Cancel an interview triggered by candidate clicking cancel link in email.
     */
    @Transactional
    public InterviewResponse cancelInterviewByToken(String token) {
        jwtTokenProvider.validateActionToken(token);

        Interview interview = interviewRepository.findByActionToken(token)
                .orElseThrow(() -> new InvalidTokenException(
                        "No interview found for this action link."));

        return performCancellation(interview, "Cancelled by candidate");
    }

    /**
     * Cancel an interview by HR (by interview ID from dashboard).
     */
    @Transactional
    public InterviewResponse cancelInterviewById(Long interviewId, String reason) {
        Interview interview = findInterviewById(interviewId);
        return performCancellation(interview, reason);
    }

    /**
     * Shared cancellation logic used by both HR and candidate cancel flows.
     */
    private InterviewResponse performCancellation(Interview interview, String reason) {
        logger.info("Cancelling interview ID: {}", interview.getId());

        if (InterviewStatus.CANCELLED.name().equals(interview.getStatus())) {
            throw new InterviewStateException("Interview is already cancelled.");
        }
        if (InterviewStatus.COMPLETED.name().equals(interview.getStatus())) {
            throw new InterviewStateException("Cannot cancel a completed interview.");
        }

        // Delete Google Calendar event if one exists
        if (interview.getCalendarEventId() != null) {
            googleCalendarClient.deleteEvent(interview.getCalendarEventId());
            interview.setCalendarEventId(null);
            interview.setMeetingLink(null);
        }

        interview.setStatus(InterviewStatus.CANCELLED.name());
        interview.setCancelledAt(LocalDateTime.now());
        interviewRepository.save(interview);

        // Send cancellation email
        try {
            emailService.sendCancellationEmail(interview, reason);
            logger.info("Cancellation email sent successfully for interview ID: {}",
                    interview.getId());
        } catch (Exception ex) {
            logger.error("=== CANCELLATION EMAIL FAILED for interview ID: {} ===",
                    interview.getId());
            logger.error("Root cause: {}", ex.getMessage(), ex);
        }

        return toResponse(interview);
    }

    // ============================================================
    // STOP AI PROCESSING
    // ============================================================

    /**
     * HR or candidate can stop AI email processing for a specific interview.
     * Once stopped, the polling scheduler will skip this interview.
     */
    @Transactional
    public InterviewResponse stopAiForInterview(Long interviewId) {
        logger.info("Stopping AI processing for interview ID: {}", interviewId);
        Interview interview = findInterviewById(interviewId);
        interview.setAiStopped(true);
        interviewRepository.save(interview);
        return toResponse(interview);
    }

    // ============================================================
    // Dashboard / Read Operations
    // ============================================================

    /**
     * Get all interviews with full candidate and interviewer details.
     */
    @Transactional(readOnly = true)
    public List<InterviewResponse> getAllInterviews() {
        return interviewRepository.findAllWithDetails()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single interview by ID.
     */
    @Transactional(readOnly = true)
    public InterviewResponse getInterviewById(Long id) {
        Interview interview = findInterviewById(id);
        return toResponse(interview);
    }

    /**
     * Get interviews filtered by status.
     */
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByStatus(String status) {
        return interviewRepository.findByStatusOrderByScheduledTimeDesc(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get dashboard summary counts by status.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getDashboardStats() {
        return Map.of(
                "total",       interviewRepository.count(),
                "invited",     interviewRepository.countByStatus(InterviewStatus.INVITED.name()),
                "confirmed",   interviewRepository.countByStatus(InterviewStatus.CONFIRMED.name()),
                "rescheduled", interviewRepository.countByStatus(InterviewStatus.RESCHEDULED.name()),
                "cancelled",   interviewRepository.countByStatus(InterviewStatus.CANCELLED.name()),
                "completed",   interviewRepository.countByStatus(InterviewStatus.COMPLETED.name())
        );
    }

    /**
     * Get upcoming INVITED and CONFIRMED interviews from now onwards.
     */
    @Transactional(readOnly = true)
    public List<InterviewResponse> getUpcomingInterviews() {
        return interviewRepository.findUpcomingInterviews(LocalDateTime.now())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ---- Internal helpers ----

    private Interview findInterviewById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", id));
    }

    // ---- Response Mapping ----

    private InterviewResponse toResponse(Interview interview) {
        InterviewResponse response = new InterviewResponse();
        response.setId(interview.getId());
        response.setTitle(interview.getTitle());
        response.setStatus(interview.getStatus());
        response.setScheduledTime(interview.getScheduledTime());
        response.setDurationMinutes(interview.getDurationMinutes());
        response.setMeetingLink(interview.getMeetingLink());
        response.setNotes(interview.getNotes());
        response.setAiStopped(interview.isAiStopped());
        response.setInviteSentAt(interview.getInviteSentAt());
        response.setConfirmedAt(interview.getConfirmedAt());
        response.setRescheduledAt(interview.getRescheduledAt());
        response.setCancelledAt(interview.getCancelledAt());
        response.setCreatedAt(interview.getCreatedAt());
        response.setUpdatedAt(interview.getUpdatedAt());

        if (interview.getCandidate() != null) {
            response.setCandidateId(interview.getCandidate().getId());
            response.setCandidateName(interview.getCandidate().getName());
            response.setCandidateEmail(interview.getCandidate().getEmail());
            response.setCandidateTimezone(interview.getCandidate().getTimezone());
        }

        if (interview.getInterviewer() != null) {
            response.setInterviewerId(interview.getInterviewer().getId());
            response.setInterviewerName(interview.getInterviewer().getName());
            response.setInterviewerEmail(interview.getInterviewer().getEmail());
            response.setInterviewerDepartment(interview.getInterviewer().getDepartment());
        }

        return response;
    }
}