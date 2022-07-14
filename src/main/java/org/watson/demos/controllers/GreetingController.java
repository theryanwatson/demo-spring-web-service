package org.watson.demos.controllers;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.GreetingProbe;
import org.watson.demos.services.GreetingService;

import javax.validation.Valid;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Timed(value = "http.greetings.requests", extraTags = {"version", "1"}, description = "/greetings")
@RequestMapping(path = "${server.rest.path.root}/greetings", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
public class GreetingController {
    private final GreetingService service;

    @GetMapping
    public Page<Greeting> getGreetings(@Valid GreetingProbe probe, Pageable pageable) {
        return service.getGreetings(probe, pageable);
    }

    @PostMapping
    public Iterable<Greeting> createGreetings(@RequestBody Iterable<Greeting> greetings) {
        return service.createGreetings(greetings);
    }

    @DeleteMapping
    public void deleteGreetings(@RequestParam Set<UUID> id) {
        service.deleteGreetings(id);
    }
}
