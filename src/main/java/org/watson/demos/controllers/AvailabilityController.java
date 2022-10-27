package org.watson.demos.controllers;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.watson.demos.models.HealthStatus;
import org.watson.demos.services.AvailabilityService;

import java.util.Map;

@Validated
@RequiredArgsConstructor
@Timed(value = "http.availability.requests", extraTags = {"version", "1"}, description = "/availability")
@RequestMapping(path = "${server.rest.path.root}/availability", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
public class AvailabilityController {
    private final AvailabilityService service;

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PatchMapping("/liveness/{state}")
    public void updateLivenessState(@PathVariable final LivenessState state) {
        service.updateAvailabilityState(state);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PatchMapping("/readiness/{state}")
    public void updateReadinessState(@PathVariable final ReadinessState state) {
        service.updateAvailabilityState(state);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PatchMapping("/health/{status}")
    public void updateHealthStatus(@PathVariable final HealthStatus status) {
        updateHealthStatusDetail(status, null);
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PutMapping(value = "/health/{status}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateHealthStatusDetail(@PathVariable final HealthStatus status,
                                         @RequestBody final Map<String, Object> details) {
        service.updateHealthStatus(status, details);
    }
}
