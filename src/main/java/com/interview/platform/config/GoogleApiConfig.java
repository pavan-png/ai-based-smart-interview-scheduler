package com.interview.platform.config;

import org.springframework.context.annotation.Configuration;

/**
 * Google API clients are self-contained in their respective
 * @Component classes (GoogleCalendarClient, GmailClient).
 * This config class is kept for future use.
 */
@Configuration
public class GoogleApiConfig {
    // Intentionally empty.
    // GoogleCalendarClient and GmailClient are @Component beans
    // that initialize Google API services internally.
}