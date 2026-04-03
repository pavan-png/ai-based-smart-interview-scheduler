package com.interview.platform.repository;

import com.interview.platform.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Candidate persistence operations.
 */
@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    Optional<Candidate> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT c FROM Candidate c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Candidate> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT c FROM Candidate c ORDER BY c.createdAt DESC")
    List<Candidate> findAllOrderByCreatedAtDesc();
}
