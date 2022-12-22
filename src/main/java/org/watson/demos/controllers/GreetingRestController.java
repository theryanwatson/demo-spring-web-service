package org.watson.demos.controllers;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.GreetingProbe;
import org.watson.demos.services.GreetingService;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Validated
@RequiredArgsConstructor
@Timed(value = "http.greetings.requests", extraTags = {"version", "1"}, description = "/greetings")
@RequestMapping(path = "${spring.data.rest.base-path:}/greetings", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
public class GreetingRestController {
    private final GreetingService service;

    @GetMapping("{id}")
    public Greeting getGreeting(@PathVariable final UUID id) {
        return service.getOne(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "greeting '%s' Not Found".formatted(id)));
    }

    @GetMapping
    public Page<Greeting> getGreetings(@Valid final GreetingProbe probe, @ParameterObject final Pageable pageable) {
        return service.getAll(probe, pageable);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Collection<Greeting> createGreetings(@RequestBody final Collection<@Valid Greeting> greetings) {
        return service.createAll(greetings);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping
    public void deleteGreetings(@RequestParam final Set<UUID> id) {
        service.deleteAll(id);
    }
}
