package com.interview.platform.controller;

import com.interview.platform.dto.ApiResponse;
import com.interview.platform.dto.CandidateResponse;
import com.interview.platform.service.CandidateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for viewing Candidates.
 * All endpoints require HR authentication.
 */
@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    /**
     * GET /api/candidates
     * Get all candidates.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CandidateResponse>>> getAllCandidates() {
        List<CandidateResponse> candidates = candidateService.getAllCandidates();
        return ResponseEntity.ok(ApiResponse.success(candidates));
    }

    /**
     * GET /api/candidates/{id}
     * Get candidate by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateResponse>> getCandidateById(@PathVariable Long id) {
        CandidateResponse candidate = candidateService.getCandidateById(id);
        return ResponseEntity.ok(ApiResponse.success(candidate));
    }
}
