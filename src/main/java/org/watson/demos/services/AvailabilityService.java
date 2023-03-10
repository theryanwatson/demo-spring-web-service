package org.watson.demos.services;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.watson.demos.events.HealthEvent;
import org.watson.demos.models.HealthStatus;

import java.util.HashMap;
import java.util.Map;

@Timed("service.availability")
@Slf4j
@RequiredArgsConstructor
@Service
public class AvailabilityService {
    private final TraceService traceService;
    private final ApplicationEventPublisher publisher;

    public void updateAvailabilityState(@NonNull final AvailabilityState state) {
        publishEvent(new AvailabilityChangeEvent<>(state, state));
        auditEvent("AVAILABILITY_" + state, null);
    }

    public void updateHealthStatus(@NonNull final HealthStatus status, @Nullable final Map<String, ?> details) {
        publishEvent(HealthEvent.builder().status(status).details(details).build());
        auditEvent("HEALTH_STATUS_" + status.getName(), details);
    }

    private void publishEvent(@NonNull final ApplicationEvent event) {
        log.debug("Publishing Event. event={}", event);
        publisher.publishEvent(event);
    }

    private void auditEvent(@NonNull final String type, @Nullable final Map<String, ?> details) {
        final Map<String, Object> data = new HashMap<>(details != null ? details : Map.of());

        traceService.getCurrentTraceId()
                .ifPresent(id -> data.put("traceId", id));

        publishEvent(new AuditApplicationEvent(null, type, data));
    }
}
