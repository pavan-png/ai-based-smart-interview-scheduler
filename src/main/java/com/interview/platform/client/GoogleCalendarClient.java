package com.interview.platform.client;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.interview.platform.exception.ExternalApiException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class GoogleCalendarClient {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarClient.class);

    // Path to the OAuth2 client_secret JSON file you placed in src/main/resources/static/
    // Update application.properties: app.google.credentials-file=classpath:static/client_secret_xxxx.json
    @Value("${app.google.credentials-file}")
    private Resource credentialsFile;

    @Value("${app.google.application-name}")
    private String applicationName;

    @Value("${app.google.calendar-id}")
    private String calendarId;

    @Value("${app.google.sender-email}")
    private String senderEmail;

    @Value("${app.google.enabled:false}")
    private boolean googleEnabled;

    // Directory where the OAuth token will be stored after first-time authorization.
    // Defaults to a "tokens" folder in the project working directory.
    @Value("${app.google.tokens-dir:tokens}")
    private String tokensDirPath;

    private Calendar calendarService;

    private static final List<String> SCOPES = List.of(CalendarScopes.CALENDAR);

    @PostConstruct
    public void init() {
        if (!googleEnabled) {
            logger.warn("Google APIs disabled (app.google.enabled=false). Calendar features will not work.");
            this.calendarService = null;
            return;
        }
        try {
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            Credential credential = authorize(transport, jsonFactory);
            this.calendarService = new Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName(applicationName)
                    .build();
            logger.info("Google Calendar client (OAuth2) initialized successfully.");
        } catch (Exception ex) {
            logger.warn("Google Calendar client could not be initialized: {}. Calendar features will not work.", ex.getMessage());
            this.calendarService = null;
        }
    }

    /**
     * Loads stored OAuth2 token or triggers browser-based authorization on first run.
     * After the first run, the token is persisted in `tokensDirPath` and reused automatically.
     */
    private Credential authorize(NetHttpTransport transport, GsonFactory jsonFactory) throws IOException {
        if (!credentialsFile.exists()) {
            throw new IOException("OAuth2 credentials file not found at: " + credentialsFile.getDescription() +
                    "\nPlace your client_secret_xxxx.json in src/main/resources/static/ " +
                    "and set app.google.credentials-file=classpath:static/<filename>.json");
        }

        GoogleClientSecrets clientSecrets;
        try (InputStream is = credentialsFile.getInputStream()) {
            clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(is));
        }

        // Validate it is an OAuth2 client secret (not a service account JSON)
        if (clientSecrets.getDetails() == null ||
                (clientSecrets.getDetails().getClientId() == null)) {
            throw new IOException(
                    "Invalid credentials file. Make sure you downloaded 'OAuth 2.0 Client ID' " +
                    "(not a Service Account key) from Google Cloud Console.");
        }

        File tokensDir = new File(tokensDirPath);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, jsonFactory, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
                .setAccessType("offline")   // gets a refresh token so it survives restarts
                .build();

        // LocalServerReceiver opens a temporary HTTP server on localhost to catch the OAuth redirect.
        // On first run this will open a browser — after that the token is cached in tokensDirPath.
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        logger.info("OAuth2 authorization successful. Token stored in: {}", tokensDir.getAbsolutePath());
        return credential;
    }

    /**
     * Create a Google Calendar event with a Google Meet link.
     */
    public Event createInterviewEvent(String title, String description,
                                       LocalDateTime startTime, int durationMinutes,
                                       String timezone, String candidateEmail,
                                       String interviewerEmail) {

        if (calendarService == null) {
            logger.warn("Google Calendar not configured. Skipping event creation.");
            return new Event().setSummary(title);
        }

        try {
            ZoneId zoneId = resolveZoneId(timezone);
            ZonedDateTime startZoned = startTime.atZone(zoneId);
            ZonedDateTime endZoned = startZoned.plusMinutes(durationMinutes);

            com.google.api.client.util.DateTime startDateTime =
                    new com.google.api.client.util.DateTime(startZoned.toInstant().toEpochMilli());
            com.google.api.client.util.DateTime endDateTime =
                    new com.google.api.client.util.DateTime(endZoned.toInstant().toEpochMilli());

            Event event = new Event()
                    .setSummary(title)
                    .setDescription(description);

            event.setStart(new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone(zoneId.getId()));

            event.setEnd(new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone(zoneId.getId()));

            List<EventAttendee> attendees = Arrays.asList(
                    new EventAttendee()
                            .setEmail(candidateEmail)
                            .setResponseStatus("needsAction"),
                    new EventAttendee()
                            .setEmail(interviewerEmail)
                            .setResponseStatus("accepted"),
                    new EventAttendee()
                            .setEmail(senderEmail)
                            .setResponseStatus("accepted")
            );
            event.setAttendees(attendees);

            // Request a Google Meet link
            String requestId = "interview-" + System.currentTimeMillis();
            ConferenceData conferenceData = new ConferenceData()
                    .setCreateRequest(new CreateConferenceRequest()
                            .setRequestId(requestId)
                            .setConferenceSolutionKey(
                                    new ConferenceSolutionKey()
                                            .setType("hangoutsMeet")));
            event.setConferenceData(conferenceData);

            event.setReminders(new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(
                            new EventReminder().setMethod("email").setMinutes(1440),
                            new EventReminder().setMethod("popup").setMinutes(30)
                    )));

            Event createdEvent = calendarService.events()
                    .insert(calendarId, event)
                    .setConferenceDataVersion(1)  // REQUIRED to get Meet link
                    .setSendUpdates("all")
                    .execute();

            logger.info("Google Calendar event created: id={}", createdEvent.getId());
            return createdEvent;

        } catch (IOException ex) {
            logger.error("Failed to create Google Calendar event: {}", ex.getMessage(), ex);
            throw new ExternalApiException("GoogleCalendar",
                    "Failed to create calendar event: " + ex.getMessage(), ex);
        }
    }

    /**
     * Delete a Google Calendar event by event ID.
     */
    public void deleteEvent(String eventId) {
        if (calendarService == null) {
            logger.warn("Google Calendar not configured. Skipping event deletion.");
            return;
        }
        if (eventId == null || eventId.isBlank()) {
            logger.warn("Skipping event deletion - no event ID provided.");
            return;
        }
        try {
            calendarService.events()
                    .delete(calendarId, eventId)
                    .setSendUpdates("all")
                    .execute();
            logger.info("Google Calendar event deleted: {}", eventId);
        } catch (IOException ex) {
            logger.error("Failed to delete Google Calendar event {}: {}", eventId, ex.getMessage());
        }
    }

    /**
     * Extract the Google Meet link from a Calendar Event.
     */
    public String extractMeetLink(Event event) {
        if (event == null || event.getConferenceData() == null) {
            return null;
        }
        List<EntryPoint> entryPoints = event.getConferenceData().getEntryPoints();
        if (entryPoints == null || entryPoints.isEmpty()) {
            return null;
        }
        return entryPoints.stream()
                .filter(ep -> "video".equals(ep.getEntryPointType()))
                .map(EntryPoint::getUri)
                .findFirst()
                .orElse(null);
    }

    // ---- Private helpers ----

    private ZoneId resolveZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception ex) {
            logger.warn("Invalid timezone '{}', defaulting to UTC", timezone);
            return ZoneId.of("UTC");
        }
    }
}