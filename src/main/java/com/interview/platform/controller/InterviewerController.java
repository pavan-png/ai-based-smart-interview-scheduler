package com.interview.platform.controller;

import com.interview.platform.dto.ApiResponse;
import com.interview.platform.dto.InterviewerRequest;
import com.interview.platform.dto.InterviewerResponse;
import com.interview.platform.service.InterviewerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Interviewers.
 * All endpoints require HR authentication.
 */
@RestController
@RequestMapping("/api/interviewers")
public class InterviewerController {

    private final InterviewerService interviewerService;

    public InterviewerController(InterviewerService interviewerService) {
        this.interviewerService = interviewerService;
    }

    /**
     * GET /api/interviewers
     * Get all active interviewers (for the create interview dropdown).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<InterviewerResponse>>> getAllActiveInterviewers() {
        List<InterviewerResponse> interviewers = interviewerService.getAllActiveInterviewers();
        return ResponseEntity.ok(ApiResponse.success(interviewers));
    }

    /**
     * GET /api/interviewers/all
     * Get all interviewers including inactive.
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<InterviewerResponse>>> getAllInterviewers() {
        List<InterviewerResponse> interviewers = interviewerService.getAllInterviewers();
        return ResponseEntity.ok(ApiResponse.success(interviewers));
    }

    /**
     * GET /api/interviewers/{id}
     * Get interviewer by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewerResponse>> getById(@PathVariable Long id) {
        InterviewerResponse interviewer = interviewerService.getInterviewerById(id);
        return ResponseEntity.ok(ApiResponse.success(interviewer));
    }

    /**
     * POST /api/interviewers
     * Create a new interviewer.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InterviewerResponse>> createInterviewer(
            @Valid @RequestBody InterviewerRequest request) {

        InterviewerResponse interviewer = interviewerService.createInterviewer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Interviewer created successfully", interviewer));
    }

    /**
     * PUT /api/interviewers/{id}
     * Update an existing interviewer.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewerResponse>> updateInterviewer(
            @PathVariable Long id,
            @Valid @RequestBody InterviewerRequest request) {

        InterviewerResponse interviewer = interviewerService.updateInterviewer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Interviewer updated", interviewer));
    }

    /**
     * DELETE /api/interviewers/{id}
     * Soft-delete (deactivate) an interviewer.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateInterviewer(@PathVariable Long id) {
        interviewerService.deactivateInterviewer(id);
        return ResponseEntity.ok(ApiResponse.success("Interviewer deactivated", null));
    }
}
