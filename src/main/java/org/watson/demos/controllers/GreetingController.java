package org.watson.demos.controllers;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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
@RequestMapping(path = "${server.rest.path.root}/greetings", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
public class GreetingController {
    private final GreetingService service;

    @GetMapping("{id}")
    public Greeting getGreeting(@PathVariable final UUID id) {
        return service.getOne(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("greeting '%s' Not Found", id)));
    }

    @GetMapping
    public Page<Greeting> getGreetings(final GreetingProbe probe, @ParameterObject final Pageable pageable) {
        return service.getAll(probe, pageable);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Collection<Greeting> createGreetings(@RequestBody final Collection<Greeting> greetings) {
        return service.createAll(greetings);
    }

    @DeleteMapping
    public void deleteGreetings(@RequestParam final Set<UUID> id) {
        service.deleteAll(id);
    }
}
