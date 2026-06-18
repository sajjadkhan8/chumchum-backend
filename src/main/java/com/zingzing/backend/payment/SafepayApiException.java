package com.zingzing.backend.payment;

/** Thrown when the Safepay API returns an error or an unexpected response format. */
public class SafepayApiException extends RuntimeException {

    public SafepayApiException(String message) {
        super(message);
    }

    public SafepayApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
