package org.watson.demos.healthindicators;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.watson.demos.events.HealthEvent;
import org.watson.demos.models.HealthStatus;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HealthListenerHealthIndicator.class)
class HealthListenerHealthIndicatorTest {

    @Resource
    private ApplicationEventPublisher publisher;
    @Resource
    private HealthListenerHealthIndicator healthIndicator;

    @MethodSource({"healthEvents", "healthEventsWithDetails"})
    @ParameterizedTest
    void publishedEventsStored(final HealthEvent event) {
        publisher.publishEvent(event);

        assertThat(healthIndicator.health()).isEqualTo(event.getHealth());
    }

    static Stream<Arguments> healthEvents() {
        return Arrays.stream(HealthStatus.values())
                .map(HealthEvent.builder()::status)
                .map(HealthEvent.HealthChangeEventBuilder::build)
                .map(Arguments::of);
    }

    static Stream<Arguments> healthEventsWithDetails() {
        final AtomicInteger counter = new AtomicInteger();
        return Arrays.stream(HealthStatus.values())
                .map(HealthEvent.builder()::status)
                .map(h -> h.details(Map.of(String.valueOf(counter.getAndIncrement()), counter.getAndIncrement())))
                .map(HealthEvent.HealthChangeEventBuilder::build)
                .map(Arguments::of);
    }
}
