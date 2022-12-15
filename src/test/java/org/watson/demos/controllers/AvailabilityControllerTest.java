package org.watson.demos.controllers;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.watson.demos.models.HealthStatus;
import org.watson.demos.services.AvailabilityService;

import jakarta.annotation.Resource;
import java.util.Map;

import static org.mockito.Mockito.verify;

@SpringBootTest(classes = AvailabilityController.class)
@Import(SimpleMeterRegistry.class)
class AvailabilityControllerTest {
    @MockBean
    private AvailabilityService service;

    @Resource
    private AvailabilityController controller;

    @EnumSource(LivenessState.class)
    @ParameterizedTest
    void updateLivenessState_passesThroughToService(final LivenessState state) {
        controller.updateLivenessState(state);

        verify(service).updateAvailabilityState(state);
    }

    @EnumSource(ReadinessState.class)
    @ParameterizedTest
    void updateReadinessState_passesThroughToService(final ReadinessState state) {
        controller.updateReadinessState(state);

        verify(service).updateAvailabilityState(state);
    }

    @EnumSource(HealthStatus.class)
    @ParameterizedTest
    void updateHealthStatus_passesThroughToService(final HealthStatus status) {
        controller.updateHealthStatus(status);

        verify(service).updateHealthStatus(status, null);
    }

    @EnumSource(HealthStatus.class)
    @ParameterizedTest
    void updateHealthStatusDetail_passesThroughToService(final HealthStatus status) {
        final Map<String, Object> expected = Map.of("a", "b");
        controller.updateHealthStatusDetail(status, expected);

        verify(service).updateHealthStatus(status, expected);
    }
}
