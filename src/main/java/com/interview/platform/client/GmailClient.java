package com.interview.platform.client;

import com.interview.platform.exception.ExternalApiException;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Sends HTML emails via Gmail SMTP and reads candidate replies via IMAP.
 */
@Component
public class GmailClient {

    private static final Logger logger = LoggerFactory.getLogger(GmailClient.class);

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${spring.mail.password}")
    private String smtpPassword;

    @Value("${app.company.name}")
    private String companyName;

    private final JavaMailSender mailSender;

    public GmailClient(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        logger.info("Gmail client initialized.");
    }

    // =========================================================
    // SEND HTML EMAIL (SMTP)
    // =========================================================

    /**
     * Send an HTML email to the specified recipient via Gmail SMTP.
     */
    public void sendHtmlEmail(String toEmail, String toName, String subject, String htmlBody) {
        logger.info("Sending email to: {} <{}>", toName, toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, companyName);
            helper.setTo(new jakarta.mail.internet.InternetAddress(toEmail, toName));
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", toEmail);
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            logger.error("Failed to send email to {}: {}", toEmail, ex.getMessage(), ex);
            throw new ExternalApiException("Gmail", "Failed to send email to " + toEmail + ": " + ex.getMessage(), ex);
        }
    }

    // =========================================================
    // FETCH UNREAD REPLIES (IMAP)
    // =========================================================

    /**
     * Fetch unread emails from the INBOX that contain the given subject keyword.
     * Marks fetched messages as READ to avoid re-processing.
     *
     * Returns a list of maps with keys: messageId, subject, from, body
     */
    public List<Map<String, String>> fetchUnreadReplies(String subjectKeyword) {
        List<Map<String, String>> results = new ArrayList<>();

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.ssl.trust", "imap.gmail.com");

        Store store = null;
        Folder inbox = null;
        try {
            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect("imap.gmail.com", senderEmail, smtpPassword);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            // Search for UNREAD messages with matching subject
            SearchTerm term = new AndTerm(
                new FlagTerm(new Flags(Flags.Flag.SEEN), false),
                new SubjectTerm(subjectKeyword)
            );

            Message[] messages = inbox.search(term);
            logger.info("Found {} unread reply email(s) for subject keyword: '{}'",
                    messages.length, subjectKeyword);

            for (Message msg : messages) {
                try {
                    Map<String, String> m = new HashMap<>();
                    m.put("messageId", String.valueOf(msg.getMessageNumber()));
                    m.put("subject", msg.getSubject() != null ? msg.getSubject() : "");
                    m.put("from", msg.getFrom() != null && msg.getFrom().length > 0
                            ? msg.getFrom()[0].toString() : "");
                    m.put("body", extractTextBody(msg));
                    results.add(m);
                    // Mark as read so we don't process it again
                    msg.setFlag(Flags.Flag.SEEN, true);
                } catch (Exception msgEx) {
                    logger.warn("Error reading individual message: {}", msgEx.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("IMAP fetch failed (subjectKeyword='{}'): {}", subjectKeyword, e.getMessage());
            // Non-fatal — scheduler will retry next cycle
        } finally {
            try { if (inbox != null && inbox.isOpen()) inbox.close(true); } catch (Exception ignored) {}
            try { if (store != null) store.close(); } catch (Exception ignored) {}
        }

        return results;
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private String extractTextBody(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        }
        if (message.isMimeType("text/html")) {
            // Strip HTML tags for plain text processing
            return message.getContent().toString().replaceAll("<[^>]+>", " ");
        }
        if (message.isMimeType("multipart/*")) {
            return extractFromMultipart((MimeMultipart) message.getContent());
        }
        return "";
    }

    private String extractFromMultipart(MimeMultipart multipart) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/plain")) {
                sb.append(part.getContent().toString());
            } else if (part.isMimeType("multipart/*")) {
                sb.append(extractFromMultipart((MimeMultipart) part.getContent()));
            }
        }
        return sb.toString();
    }
}
