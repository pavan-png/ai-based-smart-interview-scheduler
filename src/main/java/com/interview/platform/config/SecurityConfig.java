package com.interview.platform.config;

import com.interview.platform.security.JwtAuthEntryPoint;
import com.interview.platform.security.JwtAuthenticationFilter;
import com.interview.platform.security.JwtTokenProvider;
import com.interview.platform.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration.
 * - JWT stateless sessions
 * - Public endpoints: login, candidate action links
 * - Protected endpoints: all /api/hr/** routes
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(JwtAuthEntryPoint jwtAuthEntryPoint,
                          JwtTokenProvider jwtTokenProvider,
                          UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF since we use JWT (stateless)
            .csrf(AbstractHttpConfigurer::disable)

            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless session management
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Auth entry point for 401
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(jwtAuthEntryPoint))

            // Route authorization
            .authorizeHttpRequests(auth -> auth
                // Public: static assets and login page
            		.requestMatchers("/", "/index.html", "/login.html", "/dashboard.html",
                            "/action-result.html",
                            "/css/**", "/js/**", "/favicon.ico").permitAll()

                // Public: authentication endpoint
                .requestMatchers("/api/auth/**").permitAll()

                // Public: candidate action endpoints (confirm/reschedule/cancel via email link)
                .requestMatchers("/api/interviews/action/**").permitAll()

                // Public: candidate confirmation page
                .requestMatchers(HttpMethod.GET, "/api/interviews/token/**").permitAll()

                // All HR APIs require authentication
                .requestMatchers("/api/hr/**").hasRole("HR")
                .requestMatchers("/api/interviews/**").hasRole("HR")
                .requestMatchers("/api/interviewers/**").hasRole("HR")
                .requestMatchers("/api/candidates/**").hasRole("HR")

                .anyRequest().authenticated()
            );

        // Add JWT filter before Spring's default authentication filter
        http.addFilterBefore(jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
