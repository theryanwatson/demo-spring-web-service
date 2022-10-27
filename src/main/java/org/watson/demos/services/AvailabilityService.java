package org.watson.demos.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.watson.demos.events.HealthEvent;
import org.watson.demos.models.HealthStatus;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class AvailabilityService {
    private final ApplicationEventPublisher publisher;

    public void updateAvailabilityState(AvailabilityState state) {
        publishEvent(new AvailabilityChangeEvent<>(state, state));
    }

    public void updateHealthStatus(HealthStatus status, Map<String, ?> details) {
        publishEvent(HealthEvent.builder().status(status).details(details).build());
    }

    private void publishEvent(ApplicationEvent event) {
        log.debug("Publishing Event. event={}", event);
        publisher.publishEvent(event);
    }
}
