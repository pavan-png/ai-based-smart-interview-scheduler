package com.interview.platform.repository;

import com.interview.platform.entity.HrUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository for HR User persistence operations.
 */
@Repository
public interface HrUserRepository extends JpaRepository<HrUser, Long> {

    Optional<HrUser> findByUsername(String username);

    Optional<HrUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
