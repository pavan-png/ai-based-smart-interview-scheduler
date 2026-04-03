package com.interview.platform.exception;

/**
 * Thrown when an external API call (Grok, Google) fails.
 */
public class ExternalApiException extends RuntimeException {

    private final String apiName;

    public ExternalApiException(String apiName, String message) {
        super(String.format("[%s API Error] %s", apiName, message));
        this.apiName = apiName;
    }

    public ExternalApiException(String apiName, String message, Throwable cause) {
        super(String.format("[%s API Error] %s", apiName, message), cause);
        this.apiName = apiName;
    }

    public String getApiName() { return apiName; }
}
