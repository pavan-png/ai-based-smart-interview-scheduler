package com.interview.platform.security;

import com.interview.platform.entity.HrUser;
import com.interview.platform.repository.HrUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService implementation.
 * Loads HR users by username for authentication.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final HrUserRepository hrUserRepository;

    public UserDetailsServiceImpl(HrUserRepository hrUserRepository) {
        this.hrUserRepository = hrUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        HrUser user = hrUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username));
        return new UserPrincipal(user);
    }
}
