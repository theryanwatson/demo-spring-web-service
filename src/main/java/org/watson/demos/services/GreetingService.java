package org.watson.demos.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.GreetingProbe;
import org.watson.demos.repositories.GreetingRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GreetingService {

    private final GreetingRepository repository;

    public Optional<Greeting> getOne(UUID id) {
        return repository.findById(id);
    }

    public Page<Greeting> getAll(@NonNull final GreetingProbe probe, @NonNull final Pageable pageable) {
        if (probe.getLocale() != null) {
            return repository.findAllByLocale(probe.getLocale(), pageable);
        } else {
            return repository.findAll(pageable);
        }
    }

    public Iterable<Greeting> createAll(@NonNull final Iterable<Greeting> greetings) {
        return repository.saveAll(greetings);
    }

    public void deleteAll(@NonNull final Iterable<UUID> ids) {
        repository.deleteAllById(ids);
    }
}
