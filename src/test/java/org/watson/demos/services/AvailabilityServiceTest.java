package org.watson.demos.services;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.watson.demos.events.HealthEvent;
import org.watson.demos.models.HealthStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = AvailabilityService.class)
@Import(TraceService.class)
@ContextConfiguration(classes = AvailabilityServiceTest.PublisherTestConfiguration.class)
class AvailabilityServiceTest {

    @SpyBean
    private ApplicationEventPublisher publisher;
    @Captor
    private ArgumentCaptor<ApplicationEvent> eventCaptor;
    @Resource
    private AvailabilityService service;

    @AfterEach
    void afterEach() {
        reset(publisher);
    }

    @EnumSource(LivenessState.class)
    @ParameterizedTest
    void updateAvailabilityState_livenessState_callsPublish(final LivenessState state) {
        updateAvailabilityState_callsPublish(state);
    }

    @EnumSource(ReadinessState.class)
    @ParameterizedTest
    void updateAvailabilityState_readinessState_callsPublish(final ReadinessState state) {
        updateAvailabilityState_callsPublish(state);
    }

    @EnumSource(HealthStatus.class)
    @ParameterizedTest
    void updateHealthStatus_healthStatus_callsPublish(final HealthStatus status) {
        updateHealthStatus_callsPublish(status, null);
    }

    @EnumSource(HealthStatus.class)
    @ParameterizedTest
    void updateHealthStatus_healthStatusDetail_callsPublish(final HealthStatus status) {
        final Map<String, String> detail = Map.of("1", "2");
        updateHealthStatus_callsPublish(status, detail);
    }

    private void updateHealthStatus_callsPublish(final HealthStatus status, final Map<String, ?> details) {
        service.updateHealthStatus(status, details);

        verify(publisher, times(2)).publishEvent(eventCaptor.capture());
        final List<ApplicationEvent> events = eventCaptor.getAllValues();

        assertThat(events).anyMatch(HealthEvent.class::isInstance);
        final Health event = events.stream()
                .filter(HealthEvent.class::isInstance)
                .map(HealthEvent.class::cast)
                .findFirst()
                .map(HealthEvent::getHealth)
                .orElseThrow();

        assertThat(event).satisfies(
                h -> assertThat(h).isNotNull(),
                h -> assertThat(h.getStatus()).isEqualTo(status.getStatus()),
                h -> assertThat(h.getDetails()).isEqualTo(details != null ? details : Map.of())
        );

        update_callsAudit(events, status.getName());
    }

    private void updateAvailabilityState_callsPublish(final AvailabilityState state) {
        service.updateAvailabilityState(state);

        verify(publisher, times(2)).publishEvent(eventCaptor.capture());
        final List<ApplicationEvent> events = eventCaptor.getAllValues();

        assertThat(events).anyMatch(AvailabilityChangeEvent.class::isInstance);
        final AvailabilityChangeEvent<?> event = events.stream()
                .filter(AvailabilityChangeEvent.class::isInstance)
                .map(AvailabilityChangeEvent.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(event.getState()).isEqualTo(state);

        update_callsAudit(events, state.toString());
    }

    private void update_callsAudit(List<ApplicationEvent> events, final String status) {
        assertThat(events).anyMatch(AuditApplicationEvent.class::isInstance);

        final AuditApplicationEvent event = events.stream()
                .filter(AuditApplicationEvent.class::isInstance)
                .map(AuditApplicationEvent.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(event).satisfies(
                a -> assertThat(a).isNotNull(),
                a -> assertThat(a.getAuditEvent().getType()).contains(status)
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PublisherTestConfiguration {
        @Primary
        @Bean
        public ApplicationEventPublisher publisher(final ApplicationEventPublisher publisher) {
            return Mockito.spy(publisher);
        }
    }
}
