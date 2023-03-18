package org.watson.demos.controllers;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.watson.demos.models.CacheMode;
import org.watson.demos.services.TimeService;
import reactor.core.publisher.Flux;

import java.util.Optional;

@Validated
@RequiredArgsConstructor
@Timed(value = "http.time.requests", extraTags = {"version", "1"}, description = "/time")
@RequestMapping(path = "${spring.data.rest.base-path:}/time", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
public class CachedTimeController {
    private static final String EXAMPLE = "timezone/America/Los_Angeles";
    private final TimeService service;
    @Value("${cached.time.controller.default.mode:none}")
    private CacheMode defaultMode;

    @GetMapping
    public Flux<Object> get(@Parameter(example = EXAMPLE) final Optional<String> path,
                            @Parameter final Optional<CacheMode> mode) {
        return mode.or(() -> Optional.of(defaultMode))
                .map(service::route)
                .map(m -> m.apply(service, path))
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported mode"));
    }
}
