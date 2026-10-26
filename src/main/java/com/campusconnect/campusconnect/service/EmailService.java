package com.campusconnect.campusconnect.service;

import com.campusconnect.campusconnect.model.EventRegistration;

public interface EmailService {

    EmailDeliveryResult sendRegistrationConfirmation(EventRegistration registration);

    EmailDeliveryResult sendTestEmail(String toEmail);

    boolean isMailEnabled();

    String getConfiguredHost();

    int getConfiguredPort();

    String getConfiguredSenderEmail();

    boolean isCredentialsConfigured();

    void updateSettings(boolean mailEnabled, String senderEmail, String senderName);
}
