package org.watson.demos.services;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.sleuth.annotation.ContinueSpan;
import org.springframework.cloud.sleuth.annotation.SpanTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.GreetingProbe;
import org.watson.demos.repositories.GreetingRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Timed("service.greeting")
@Service
@RequiredArgsConstructor
public class GreetingService {

    private final GreetingRepository repository;

    public Optional<Greeting> getOne(final UUID id) {
        return repository.findById(id);
    }

    @ContinueSpan
    public Page<Greeting> getAll(@SpanTag(key = "locale", expression = "locale") @NonNull final GreetingProbe probe, @NonNull final Pageable pageable) {
        if (probe.getLocale() != null) {
            return repository.findAllByLocale(probe.getLocale(), pageable);
        } else {
            return repository.findAll(pageable);
        }
    }

    public Collection<Greeting> createAll(@NonNull final Iterable<Greeting> greetings) {
        return StreamSupport.stream(repository.saveAll(greetings).spliterator(), false)
                .collect(Collectors.toUnmodifiableList());
    }

    public void deleteAll(@NonNull final Iterable<UUID> ids) {
        repository.deleteAllById(ids);
    }
}
