package com.interview.platform.repository;

import com.interview.platform.entity.Interviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Interviewer persistence operations.
 */
@Repository
public interface InterviewerRepository extends JpaRepository<Interviewer, Long> {

    Optional<Interviewer> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Interviewer> findByActiveTrue();

    @Query("SELECT i FROM Interviewer i WHERE i.active = true ORDER BY i.name ASC")
    List<Interviewer> findAllActiveOrderByName();

    List<Interviewer> findByDepartmentIgnoreCase(String department);
}
