package com.interview.platform.repository;

import com.interview.platform.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Optional<Interview> findByActionToken(String actionToken);

    @Query("SELECT i FROM Interview i JOIN FETCH i.candidate c JOIN FETCH i.interviewer iv ORDER BY i.scheduledTime DESC")
    List<Interview> findAllWithDetails();

    List<Interview> findByStatusOrderByScheduledTimeDesc(String status);

    long countByStatus(String status);

    @Query("SELECT i FROM Interview i JOIN FETCH i.candidate c JOIN FETCH i.interviewer iv " +
           "WHERE i.status IN ('INVITED','CONFIRMED') AND i.scheduledTime > :now ORDER BY i.scheduledTime")
    List<Interview> findUpcomingInterviews(@Param("now") LocalDateTime now);

    /**
     * Find active interviews (INVITED or RESCHEDULED) where AI has not been stopped.
     * Used by the email reply polling scheduler.
     */
    @Query("SELECT i FROM Interview i JOIN FETCH i.candidate c JOIN FETCH i.interviewer iv " +
           "WHERE i.status IN :statuses AND i.aiStopped = false")
    List<Interview> findByStatusInAndAiStoppedFalse(@Param("statuses") List<String> statuses);
}
