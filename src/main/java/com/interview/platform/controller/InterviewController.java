package com.interview.platform.controller;

import com.interview.platform.dto.ApiResponse;
import com.interview.platform.dto.CreateInterviewRequest;
import com.interview.platform.dto.InterviewResponse;
import com.interview.platform.dto.RescheduleRequest;
import com.interview.platform.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the complete interview lifecycle.
 *
 * Protected routes (require HR JWT):  /api/interviews/**
 * Public routes (candidate action links): /api/interviews/action/**
 */
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    // ============================================================
    // HR Protected Endpoints
    // ============================================================

    /**
     * POST /api/interviews
     * HR creates a new interview → sends AI-generated invitation email.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InterviewResponse>> createInterview(
            @Valid @RequestBody CreateInterviewRequest request) {

        InterviewResponse interview = interviewService.createInterview(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Interview created and invitation email sent", interview));
    }

    /**
     * GET /api/interviews
     * Get all interviews for the dashboard.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getAllInterviews() {
        List<InterviewResponse> interviews = interviewService.getAllInterviews();
        return ResponseEntity.ok(ApiResponse.success(interviews));
    }

    /**
     * GET /api/interviews/upcoming
     * Get upcoming interviews (status INVITED or CONFIRMED, future dates).
     */
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getUpcomingInterviews() {
        List<InterviewResponse> interviews = interviewService.getUpcomingInterviews();
        return ResponseEntity.ok(ApiResponse.success(interviews));
    }

    /**
     * GET /api/interviews/stats
     * Dashboard statistics: counts per status.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getDashboardStats() {
        Map<String, Long> stats = interviewService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * GET /api/interviews/{id}
     * Get interview details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterviewById(@PathVariable Long id) {
        InterviewResponse interview = interviewService.getInterviewById(id);
        return ResponseEntity.ok(ApiResponse.success(interview));
    }

    /**
     * GET /api/interviews/status/{status}
     * Get all interviews filtered by status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getInterviewsByStatus(
            @PathVariable String status) {

        List<InterviewResponse> interviews = interviewService.getInterviewsByStatus(status.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success(interviews));
    }

    /**
     * PUT /api/interviews/{id}/reschedule
     * HR reschedules an interview.
     */
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<ApiResponse<InterviewResponse>> rescheduleInterview(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleRequest request) {

        InterviewResponse interview = interviewService.rescheduleInterview(id, request);
        return ResponseEntity.ok(ApiResponse.success("Interview rescheduled and email sent", interview));
    }

    /**
     * PUT /api/interviews/{id}/cancel
     * HR cancels an interview.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InterviewResponse>> cancelInterview(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {

        InterviewResponse interview = interviewService.cancelInterviewById(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Interview cancelled", interview));
    }

    /**
     * PUT /api/interviews/{id}/stop-ai
     * HR stops AI email processing for a specific interview.
     * Can also be triggered programmatically when candidate sends STOP reply.
     */
    @PutMapping("/{id}/stop-ai")
    public ResponseEntity<ApiResponse<InterviewResponse>> stopAiProcessing(@PathVariable Long id) {
        InterviewResponse interview = interviewService.stopAiForInterview(id);
        return ResponseEntity.ok(ApiResponse.success(
                "AI processing stopped for this interview. No further automated emails will be sent.", interview));
    }

    // ============================================================
    // Public Candidate Action Endpoints (via email links)
    // ============================================================

    /**
     * GET /api/interviews/action/confirm?token=...
     * Candidate clicks "Confirm" in their email.
     * Validates token → confirms interview → creates Google Meet → sends confirmation email.
     */
    @GetMapping("/action/confirm")
    public ResponseEntity<ApiResponse<InterviewResponse>> confirmInterview(
            @RequestParam String token) {

        InterviewResponse interview = interviewService.confirmInterview(token);
        return ResponseEntity.ok(ApiResponse.success(
                "Interview confirmed! You will receive a calendar invite with the Google Meet link.", interview));
    }

    /**
     * GET /api/interviews/action/reschedule?token=...
     * Candidate clicks "Reschedule" in their email.
     * Validates token → updates status to RESCHEDULED → HR can set new time.
     */
    @GetMapping("/action/reschedule")
    public ResponseEntity<ApiResponse<InterviewResponse>> requestReschedule(
            @RequestParam String token) {

        InterviewResponse interview = interviewService.candidateRequestsReschedule(token);
        return ResponseEntity.ok(ApiResponse.success(
                "Reschedule request received. Our HR team will contact you with a new time.", interview));
    }

    /**
     * GET /api/interviews/action/cancel?token=...
     * Candidate clicks "Cancel" in their email.
     * Validates token → cancels interview → deletes Google Calendar event → sends cancellation email.
     */
    @GetMapping("/action/cancel")
    public ResponseEntity<ApiResponse<InterviewResponse>> cancelByToken(
            @RequestParam String token) {

        InterviewResponse interview = interviewService.cancelInterviewByToken(token);
        return ResponseEntity.ok(ApiResponse.success(
                "Interview cancelled. A confirmation email has been sent.", interview));
    }

    /**
     * GET /api/interviews/token/{token}
     * Get interview details by action token (used on reschedule page).
     */
    @GetMapping("/token/{token}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getByToken(@PathVariable String token) {
        InterviewResponse interview = interviewService.getInterviewByToken(token);
        return ResponseEntity.ok(ApiResponse.success(interview));
    }
}
