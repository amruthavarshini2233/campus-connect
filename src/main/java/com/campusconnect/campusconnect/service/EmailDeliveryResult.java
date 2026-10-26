package com.campusconnect.campusconnect.service;

import java.time.LocalDateTime;

public class EmailDeliveryResult {

    private final boolean success;
    private final boolean simulated;
    private final String message;
    private final LocalDateTime timestamp;

    public EmailDeliveryResult(boolean success, boolean simulated, String message) {
        this.success = success;
        this.simulated = simulated;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static EmailDeliveryResult success(String message) {
        return new EmailDeliveryResult(true, false, message);
    }

    public static EmailDeliveryResult sent(String message) {
        return new EmailDeliveryResult(true, false, message);
    }

    public static EmailDeliveryResult simulated(String message) {
        return new EmailDeliveryResult(true, true, message);
    }

    public static EmailDeliveryResult failed(String message) {
        return new EmailDeliveryResult(false, false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
