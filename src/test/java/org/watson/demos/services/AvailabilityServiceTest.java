package org.watson.demos.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
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
import org.watson.demos.events.HealthEvent;
import org.watson.demos.models.HealthStatus;

import jakarta.annotation.Resource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = AvailabilityService.class)
@Import(AvailabilityServiceTest.PublisherTestConfiguration.class)
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

        verify(publisher).publishEvent(eventCaptor.capture());
        final ApplicationEvent event = eventCaptor.getValue();

        assertThat(event).isInstanceOf(HealthEvent.class);
        assertThat(((HealthEvent) event).getHealth()).satisfies(
                h -> assertThat(h).isNotNull(),
                h -> assertThat(h.getStatus()).isEqualTo(status.getStatus()),
                h -> assertThat(h.getDetails()).isEqualTo(details != null ? details : Map.of())
        );
    }

    private void updateAvailabilityState_callsPublish(final AvailabilityState state) {
        service.updateAvailabilityState(state);

        verify(publisher).publishEvent(eventCaptor.capture());
        final ApplicationEvent event = eventCaptor.getValue();

        assertThat(event).isInstanceOf(AvailabilityChangeEvent.class);
        assertThat(((AvailabilityChangeEvent<?>) event).getState())
                .isEqualTo(state);
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
