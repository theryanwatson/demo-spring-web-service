package org.watson.demos.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.GreetingProbe;
import org.watson.demos.repositories.GreetingRepository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.watson.demos.utilities.GeneratorTestUtility.generateGreetings;

@SpringBootTest(classes = GreetingService.class)
class GreetingServiceTest {
    private static final List<Greeting> TEST_CONTENT = generateGreetings("service-content");

    @MockBean
    private GreetingRepository repository;

    @Resource
    private GreetingService service;

    @Test
    void getOne_passesThroughToRepository() {
        final Optional<Greeting> expected = TEST_CONTENT.stream().findAny();
        final UUID id = expected.orElseThrow().getId();
        when(repository.findById(any())).thenReturn(expected);

        assertThat(service.getOne(id)).isSameAs(expected);

        verify(repository).findById(id);
    }

    @Test
    void create_passesThroughToRepository() {
        when(repository.saveAll(any())).thenReturn(TEST_CONTENT);

        final List<Greeting> input = generateGreetings("create-service-content");

        assertThat(service.createAll(input)).containsExactlyElementsOf(TEST_CONTENT);

        verify(repository).saveAll(input);
    }

    @MethodSource
    @ParameterizedTest
    void getAll_passesThroughToRepository(final GreetingProbe probe, final Pageable pageable) {
        if (probe.getLocale() != null) {
            when(repository.findAllByLocale(any(), any())).thenReturn(new PageImpl<>(TEST_CONTENT));
        } else {
            when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(TEST_CONTENT));
        }

        assertThat(service.getAll(probe, pageable)).containsExactlyElementsOf(TEST_CONTENT);

        if (probe.getLocale() != null) {
            verify(repository).findAllByLocale(probe.getLocale(), pageable);
        } else {
            verify(repository).findAll(pageable);
        }
    }

    static Stream<Arguments> getAll_passesThroughToRepository() {
        return Stream.of(
                Arguments.of(GreetingProbe.builder().locale(Locale.US).build(), Pageable.ofSize(2)),
                Arguments.of(GreetingProbe.builder().build(), Pageable.unpaged())
        );
    }

    @Test
    void delete_passesThroughToRepository() {
        final Set<UUID> input = IntStream.range(0, 3).boxed()
                .map(i -> UUID.randomUUID())
                .collect(Collectors.toUnmodifiableSet());

        service.deleteAll(input);

        verify(repository).deleteAllById(input);
    }
}
