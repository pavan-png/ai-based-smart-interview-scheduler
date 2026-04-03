package com.interview.platform.service;

import com.interview.platform.dto.InterviewerRequest;
import com.interview.platform.dto.InterviewerResponse;
import com.interview.platform.entity.Interviewer;
import com.interview.platform.exception.ResourceNotFoundException;
import com.interview.platform.repository.InterviewerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing Interviewers.
 */
@Service
public class InterviewerService {

    private final InterviewerRepository interviewerRepository;

    public InterviewerService(InterviewerRepository interviewerRepository) {
        this.interviewerRepository = interviewerRepository;
    }

    /**
     * Get all active interviewers.
     */
    @Transactional(readOnly = true)
    public List<InterviewerResponse> getAllActiveInterviewers() {
        return interviewerRepository.findAllActiveOrderByName()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all interviewers (including inactive).
     */
    @Transactional(readOnly = true)
    public List<InterviewerResponse> getAllInterviewers() {
        return interviewerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get an interviewer by ID.
     */
    @Transactional(readOnly = true)
    public InterviewerResponse getInterviewerById(Long id) {
        Interviewer interviewer = findInterviewerById(id);
        return toResponse(interviewer);
    }

    /**
     * Create a new interviewer.
     */
    @Transactional
    public InterviewerResponse createInterviewer(InterviewerRequest request) {
        if (interviewerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Interviewer with email " + request.getEmail() + " already exists");
        }

        Interviewer interviewer = new Interviewer();
        interviewer.setName(request.getName());
        interviewer.setEmail(request.getEmail());
        interviewer.setDepartment(request.getDepartment());
        interviewer.setWorkingHours(request.getWorkingHours());
        interviewer.setCalendarProvider(request.getCalendarProvider());

        Interviewer saved = interviewerRepository.save(interviewer);
        return toResponse(saved);
    }

    /**
     * Update an existing interviewer.
     */
    @Transactional
    public InterviewerResponse updateInterviewer(Long id, InterviewerRequest request) {
        Interviewer interviewer = findInterviewerById(id);

        // Check if email is being changed to an already-taken email
        if (!interviewer.getEmail().equalsIgnoreCase(request.getEmail()) &&
                interviewerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email " + request.getEmail() + " is already in use by another interviewer");
        }

        interviewer.setName(request.getName());
        interviewer.setEmail(request.getEmail());
        interviewer.setDepartment(request.getDepartment());
        interviewer.setWorkingHours(request.getWorkingHours());
        if (request.getCalendarProvider() != null) {
            interviewer.setCalendarProvider(request.getCalendarProvider());
        }

        return toResponse(interviewerRepository.save(interviewer));
    }

    /**
     * Deactivate an interviewer (soft delete).
     */
    @Transactional
    public void deactivateInterviewer(Long id) {
        Interviewer interviewer = findInterviewerById(id);
        interviewer.setActive(false);
        interviewerRepository.save(interviewer);
    }

    /**
     * Find entity by ID — used internally by InterviewService.
     */
    @Transactional(readOnly = true)
    public Interviewer findInterviewerById(Long id) {
        return interviewerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer", "id", id));
    }

    // ---- Mapping ----

    private InterviewerResponse toResponse(Interviewer interviewer) {
        InterviewerResponse response = new InterviewerResponse();
        response.setId(interviewer.getId());
        response.setName(interviewer.getName());
        response.setEmail(interviewer.getEmail());
        response.setDepartment(interviewer.getDepartment());
        response.setWorkingHours(interviewer.getWorkingHours());
        response.setCalendarProvider(interviewer.getCalendarProvider());
        response.setActive(interviewer.isActive());
        response.setCreatedAt(interviewer.getCreatedAt());
        return response;
    }
}