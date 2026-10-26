package com.campusconnect.campusconnect.event;

import com.campusconnect.campusconnect.model.EventRegistration;

public class RegistrationCreatedEvent {

    private final EventRegistration registration;

    public RegistrationCreatedEvent(EventRegistration registration) {
        this.registration = registration;
    }

    public EventRegistration getRegistration() {
        return registration;
    }
}
