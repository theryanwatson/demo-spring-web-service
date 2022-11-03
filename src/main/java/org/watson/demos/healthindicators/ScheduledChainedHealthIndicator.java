package org.watson.demos.healthindicators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
@Component
public class ScheduledChainedHealthIndicator implements HealthIndicator {
    private final HealthListenerHealthIndicator healthIndicator;
    private final AtomicReference<Health> health = new AtomicReference<>(Health.up().build());

    @Override
    public Health health() {
        return health.get();
    }

    @Scheduled(fixedRateString = "${scheduled.task.health.listener.fixed.rate:PT5S}")
    public void onEvent() {
        final Health updated = healthIndicator.health();
        final Health current = health.getAndSet(updated);
        if (!updated.equals(current)) {
            log.debug("Updated Health Indicator. current={}, updated={}", current, updated);
        } else {
            log.trace("Unchanged Health Indicator.");
        }
    }
}
