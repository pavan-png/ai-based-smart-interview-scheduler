package com.interview.platform.service;

import com.interview.platform.dto.CandidateResponse;
import com.interview.platform.entity.Candidate;
import com.interview.platform.exception.ResourceNotFoundException;
import com.interview.platform.repository.CandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing Candidates.
 * Automatically creates candidates if they don't exist when an interview is scheduled.
 */
@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    /**
     * Find an existing candidate by email or create a new one.
     * This is used when HR creates an interview for a candidate.
     */
    @Transactional
    public Candidate findOrCreateCandidate(String name, String email, String phone, String timezone) {
        return candidateRepository.findByEmail(email)
                .orElseGet(() -> {
                    Candidate candidate = new Candidate();
                    candidate.setName(name);
                    candidate.setEmail(email);
                    candidate.setPhone(phone);
                    candidate.setTimezone(timezone != null ? timezone : "UTC");
                    return candidateRepository.save(candidate);
                });
    }

    /**
     * Get all candidates.
     */
    @Transactional(readOnly = true)
    public List<CandidateResponse> getAllCandidates() {
        return candidateRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get candidate by ID.
     */
    @Transactional(readOnly = true)
    public CandidateResponse getCandidateById(Long id) {
        Candidate candidate = findCandidateById(id);
        return toResponse(candidate);
    }

    /**
     * Find entity by ID (internal use).
     */
    @Transactional(readOnly = true)
    public Candidate findCandidateById(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", id));
    }

    // ---- Mapping ----

    public CandidateResponse toResponse(Candidate candidate) {
        CandidateResponse response = new CandidateResponse();
        response.setId(candidate.getId());
        response.setName(candidate.getName());
        response.setEmail(candidate.getEmail());
        response.setPhone(candidate.getPhone());
        response.setTimezone(candidate.getTimezone());
        response.setCreatedAt(candidate.getCreatedAt());
        return response;
    }
}
