package com.interview.platform.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.platform.dto.EmailReplyIntent;
import com.interview.platform.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Client for the Grok AI API.
 *
 * Generates professional HTML emails that instruct the candidate to REPLY
 * (no buttons/links). Also classifies candidate email replies and suggests
 * new interview times during reschedule cycles.
 */
@Component
public class GrokAiClient {

    private static final Logger logger = LoggerFactory.getLogger(GrokAiClient.class);
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a");

    @Value("${app.grok.api-url}")
    private String apiUrl;

    @Value("${app.grok.api-key}")
    private String apiKey;

    @Value("${app.grok.model}")
    private String model;

    @Value("${app.company.name}")
    private String companyName;

    @Value("${app.company.website}")
    private String companyWebsite;

    @Value("${app.company.logo-url}")
    private String companyLogoUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GrokAiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // =========================================================
    // 1. INVITATION EMAIL (no buttons — reply-based)
    // =========================================================

    public String generateInvitationEmail(String candidateName, String candidateEmail,
                                           String interviewTitle, String interviewerName,
                                           LocalDateTime scheduledTime, int durationMinutes,
                                           String interviewerEmail) {

        String formattedTime = scheduledTime.format(DISPLAY_FORMATTER);

        String prompt = String.format(
            "Generate a professional, warm HTML email for a job interview invitation.\n\n" +
            "CRITICAL REQUIREMENT — NO BUTTONS OR CLICKABLE ACTION LINKS:\n" +
            "The candidate must reply to this email. Include a clearly styled instruction box:\n\n" +
            "  Please reply to this email to respond:\n" +
            "  * To CONFIRM: Reply with the word CONFIRM\n" +
            "  * To RESCHEDULE: Reply with RESCHEDULE and your preferred dates/times\n" +
            "  * To CANCEL: Reply with the word CANCEL\n" +
            "  * To stop all AI communication: Reply with the word STOP\n\n" +
            "DESIGN:\n" +
            "- Inline CSS only (Gmail-compatible, no <style> tags)\n" +
            "- White card on light gray (#F3F4F6) background, max-width 600px\n" +
            "- Company blue #2563EB header with company logo\n" +
            "- Interview details in a styled info card\n" +
            "- Instruction box: background #EFF6FF, left border 4px solid #2563EB, padding 16px\n" +
            "- Small italic footer note: 'Your reply is read by our AI scheduling assistant.'\n" +
            "- Professional company footer\n\n" +
            "COMPANY: %s | WEBSITE: %s | LOGO: %s\n" +
            "CANDIDATE: %s <%s>\n" +
            "INTERVIEW: %s | INTERVIEWER: %s | INTERVIEWER EMAIL: %s\n" +
            "DATE/TIME: %s | DURATION: %d minutes\n\n" +
            "Output ONLY the complete HTML. No explanation. No markdown fences.",
            companyName, companyWebsite, companyLogoUrl,
            candidateName, candidateEmail,
            interviewTitle, interviewerName, interviewerEmail,
            formattedTime, durationMinutes
        );

        return callGrokApi(prompt, "interview invitation email");
    }

    // =========================================================
    // 2. CONFIRMATION EMAIL (with GMeet link, no action buttons)
    // =========================================================

    public String generateConfirmationEmail(String candidateName, String interviewTitle,
                                             LocalDateTime scheduledTime, String meetingLink) {

        String formattedTime = scheduledTime.format(DISPLAY_FORMATTER);

        String prompt = String.format(
            "Generate a professional HTML confirmation email for a confirmed job interview.\n\n" +
            "REQUIREMENTS:\n" +
            "- Inline CSS only (Gmail-compatible, no <style> tags)\n" +
            "- Celebratory/confirmation tone with a green success checkmark at the top\n" +
            "- Company color #2563EB. Include company logo.\n" +
            "- Show interview details in a styled card\n" +
            "- Display the Google Meet link as styled clickable TEXT LINK (not a button)\n" +
            "- Also show the raw URL for copy-paste\n" +
            "- NO action buttons (no reschedule/cancel)\n" +
            "- Professional footer, mobile-responsive\n\n" +
            "COMPANY: %s | LOGO: %s\n" +
            "CANDIDATE: %s | INTERVIEW: %s\n" +
            "DATE/TIME: %s | GOOGLE MEET: %s\n\n" +
            "Output ONLY the complete HTML. No explanation. No markdown fences.",
            companyName, companyLogoUrl,
            candidateName, interviewTitle, formattedTime, meetingLink
        );

        return callGrokApi(prompt, "interview confirmation email");
    }

    // =========================================================
    // 3. RESCHEDULE EMAIL (no buttons — reply-based)
    // =========================================================

    public String generateRescheduleEmail(String candidateName, String interviewTitle,
                                           LocalDateTime oldTime, LocalDateTime newTime,
                                           String interviewerEmail) {

        String prompt = String.format(
            "Generate a professional HTML email notifying a candidate their interview was rescheduled.\n\n" +
            "CRITICAL REQUIREMENT — NO BUTTONS OR CLICKABLE ACTION LINKS:\n" +
            "The candidate must reply. Include the instruction box:\n\n" +
            "  Please reply to this email to respond:\n" +
            "  * To CONFIRM the new time: Reply with CONFIRM\n" +
            "  * To RESCHEDULE again: Reply with RESCHEDULE and your preferred dates/times\n" +
            "  * To CANCEL: Reply with CANCEL\n" +
            "  * To stop all AI communication: Reply with STOP\n\n" +
            "DESIGN:\n" +
            "- Inline CSS only (Gmail-compatible)\n" +
            "- Show old time with strikethrough, new time highlighted in green\n" +
            "- Apologetic but forward-looking tone\n" +
            "- Instruction box: background #EFF6FF, left border 4px solid #2563EB, padding 16px\n" +
            "- Company logo, professional footer\n\n" +
            "COMPANY: %s | LOGO: %s\n" +
            "CANDIDATE: %s | INTERVIEW: %s\n" +
            "OLD TIME: %s | NEW TIME: %s | INTERVIEWER EMAIL: %s\n\n" +
            "Output ONLY the complete HTML. No explanation. No markdown fences.",
            companyName, companyLogoUrl,
            candidateName, interviewTitle,
            oldTime.format(DISPLAY_FORMATTER), newTime.format(DISPLAY_FORMATTER),
            interviewerEmail
        );

        return callGrokApi(prompt, "interview reschedule email");
    }

    // =========================================================
    // 4. CANCELLATION EMAIL
    // =========================================================

    public String generateCancellationEmail(String candidateName, String interviewTitle,
                                              LocalDateTime scheduledTime, String reason) {

        String prompt = String.format(
            "Generate a professional HTML email notifying a candidate their interview was cancelled.\n\n" +
            "REQUIREMENTS:\n" +
            "- Inline CSS only (Gmail-compatible)\n" +
            "- Empathetic and professional tone\n" +
            "- Company color #2563EB. Company logo.\n" +
            "- Show which interview was cancelled\n" +
            "- Invite them to reach out to reschedule in future\n" +
            "- Professional footer\n\n" +
            "COMPANY: %s | LOGO: %s\n" +
            "CANDIDATE: %s | INTERVIEW: %s\n" +
            "SCHEDULED TIME: %s | REASON: %s\n\n" +
            "Output ONLY the complete HTML. No explanation. No markdown fences.",
            companyName, companyLogoUrl,
            candidateName, interviewTitle,
            scheduledTime.format(DISPLAY_FORMATTER),
            reason != null ? reason : "Not specified"
        );

        return callGrokApi(prompt, "interview cancellation email");
    }

    // =========================================================
    // 5. AI EMAIL REPLY CLASSIFICATION
    // =========================================================

    /**
     * Ask AI to classify a candidate's email reply body.
     * Returns EmailReplyIntent: CONFIRM | RESCHEDULE | CANCEL | STOP | UNKNOWN
     */
    public EmailReplyIntent parseEmailReply(String emailBody, String interviewTitle,
                                             LocalDateTime currentScheduledTime) {

        String prompt = String.format(
            "You are an AI assistant that classifies candidate email replies about interview scheduling.\n\n" +
            "INTERVIEW POSITION: %s\n" +
            "CURRENTLY SCHEDULED: %s\n\n" +
            "CANDIDATE EMAIL REPLY:\n---\n%s\n---\n\n" +
            "Classify the reply as ONE of:\n" +
            "- CONFIRM: candidate agrees to the scheduled time\n" +
            "- RESCHEDULE: candidate wants a different date/time\n" +
            "- CANCEL: candidate wants to cancel\n" +
            "- STOP: candidate wants to stop all AI communication\n" +
            "- UNKNOWN: unclear or unrelated\n\n" +
            "Respond ONLY with this JSON (no markdown, no explanation):\n" +
            "{\"intent\":\"CONFIRM\",\"preferredTimes\":[],\"reason\":\"one sentence\"}\n\n" +
            "preferredTimes: array of 'YYYY-MM-DD HH:mm' strings if RESCHEDULE, else [].",
            interviewTitle,
            currentScheduledTime.format(DISPLAY_FORMATTER),
            emailBody
        );

        String response = callGrokApi(prompt, "email reply classification");
        response = response.trim().replace("```json", "").replace("```", "").trim();

        try {
            JsonNode node = objectMapper.readTree(response);
            EmailReplyIntent intent = new EmailReplyIntent();
            intent.setIntent(node.path("intent").asText("UNKNOWN").toUpperCase());
            intent.setReason(node.path("reason").asText(""));
            List<String> times = new ArrayList<>();
            node.path("preferredTimes").forEach(t -> times.add(t.asText()));
            intent.setPreferredTimes(times);
            return intent;
        } catch (Exception e) {
            logger.error("Failed to parse AI reply JSON: {}", e.getMessage());
            EmailReplyIntent fallback = new EmailReplyIntent();
            fallback.setIntent("UNKNOWN");
            fallback.setPreferredTimes(Collections.emptyList());
            fallback.setReason("Parse error");
            return fallback;
        }
    }

    // =========================================================
    // 6. AI NEW TIME SUGGESTION
    // =========================================================

    /**
     * Ask AI to suggest a new interview datetime given candidate's preferences.
     */
    public LocalDateTime suggestNewInterviewTime(String interviewerName,
                                                   List<String> candidatePreferences,
                                                   LocalDateTime originalTime) {

        String prefs = (candidatePreferences == null || candidatePreferences.isEmpty())
                ? "No specific preference — pick a reasonable weekday slot"
                : String.join(", ", candidatePreferences);

        String prompt = String.format(
            "Suggest ONE specific date/time for a job interview.\n\n" +
            "INTERVIEWER: %s\n" +
            "ORIGINAL TIME: %s\n" +
            "CANDIDATE PREFERRED TIMES: %s\n" +
            "TODAY: %s\n\n" +
            "Rules: weekday, 9AM-5PM, at least 1 business day from today.\n" +
            "Try to match candidate preference. If vague, use 3 days from today at 10:00 AM.\n\n" +
            "Respond ONLY with the datetime string: YYYY-MM-DDTHH:mm:ss\n" +
            "Example: 2025-04-10T10:00:00\nNo other text.",
            interviewerName,
            originalTime.format(DISPLAY_FORMATTER),
            prefs,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        String raw = callGrokApi(prompt, "new interview time suggestion")
                .trim().replace("\"", "").replace("'", "").trim();

        try {
            return LocalDateTime.parse(raw);
        } catch (Exception e) {
            LocalDateTime fallback = LocalDateTime.now().plusDays(3)
                    .withHour(10).withMinute(0).withSecond(0).withNano(0);
            logger.warn("Could not parse AI suggested time '{}', using fallback: {}", raw, fallback);
            return fallback;
        }
    }

    // =========================================================
    // PRIVATE: HTTP call to Grok API
    // =========================================================

    private String callGrokApi(String userPrompt, String taskType) {
        logger.info("Calling Grok AI for: {}", taskType);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", userPrompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 4096);
            requestBody.put("messages", List.of(message));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ExternalApiException("Grok", "HTTP " + response.getStatusCode());
            }

            return extractContent(response.getBody());

        } catch (ExternalApiException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Grok API call failed for {}: {}", taskType, ex.getMessage(), ex);
            throw new ExternalApiException("Grok", "Failed: " + taskType + " — " + ex.getMessage(), ex);
        }
    }

    private String extractContent(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) throw new ExternalApiException("Grok", "No choices in response");

        String content = choices.get(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) throw new ExternalApiException("Grok", "Empty content");

        content = content.trim();
        if (content.startsWith("```html")) content = content.substring(7);
        else if (content.startsWith("```")) content = content.substring(3);
        if (content.endsWith("```")) content = content.substring(0, content.length() - 3);
        return content.trim();
    }
}
