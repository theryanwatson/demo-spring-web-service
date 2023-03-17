package org.watson.demos.controllers;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.Arguments;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.GreetingProbe;
import org.watson.demos.services.GreetingService;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Timed(value = "graphql.greetings.requests", extraTags = {"version", "1"}, description = "/greetings")
@Controller
@SchemaMapping(typeName = "Greeting")
public class GreetingGraphController {
    private final GreetingService service;

    @QueryMapping(name = "greeting")
    public Greeting getGreeting(@Argument UUID id) {
        return service.getOne(id)
                .orElseThrow();
    }

    @QueryMapping(name = "greetings")
    public Page<Greeting> getGreetings(@Valid @Arguments final GreetingProbe probe, @Valid @Arguments final PageWrapper pageable) {
        return service.getAll(probe, pageable.toPageable());
    }

    @MutationMapping
    public Greeting createGreeting(@Valid @Argument final Greeting greeting) {
        return service.createAll(List.of(greeting)).stream()
                .findFirst()
                .orElseThrow();
    }

    @MutationMapping
    public Collection<Greeting> createGreetings(@Argument final Collection<@Valid Greeting> greetings) {
        return service.createAll(greetings);
    }

    @MutationMapping
    public void deleteGreetings(@Argument final Set<UUID> ids) {
        service.deleteAll(ids);
    }

    // TODO: Make this a converter (or equivalent)
    @lombok.Value
    static class PageWrapper {
        @NonNull
        @Min(0)
        Integer page;
        @NonNull
        @Min(1)
        Integer size;
        @Nullable
        Collection<@NotBlank String> sort;

        @NonNull
        private Pageable toPageable() {
            return PageRequest.of(page, size, toSort(sort));
        }

        @NonNull
        private static Sort toSort(@Nullable final Collection<String> sort) {
            return sort == null ? Sort.unsorted() :
                    sort.stream()
                            .map(PageWrapper::toSort)
                            .reduce(Sort::and)
                            .orElseGet(Sort::unsorted);
        }

        private static Sort toSort(@NonNull String sort) {
            int splitIndex = sort.indexOf(',');
            if (splitIndex > 0) {
                return Sort.by(Sort.Direction.fromString(sort.substring(splitIndex + 1)).isAscending() ? Sort.Order.asc(sort.substring(0, splitIndex)) : Sort.Order.desc(sort.substring(0, splitIndex)));
            } else {
                return Sort.by(sort);
            }
        }
    }
}
