package org.watson.demos.healthindicators;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.watson.demos.events.HealthEvent;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@Timed("health.indicator.health.listener")
public class HealthListenerHealthIndicator implements HealthIndicator {
    private final AtomicReference<Health> health = new AtomicReference<>(Health.up().build());

    @Override
    public Health health() {
        return health.get();
    }

    @Async
    @EventListener(HealthEvent.class)
    public void onEvent(@NonNull final HealthEvent event) {
        synchronized (health) {
            final Health updated = event.getHealth();
            final Health current = health.getAndSet(updated);
            if (!updated.equals(current)) {
                log.debug("Updated Health Indicator. current={}, updated={}", current, updated);
            } else {
                log.debug("Unchanged Health Indicator.");
            }
        }
    }
}
