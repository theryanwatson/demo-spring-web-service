package org.watson.demos.controllers;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.GreetingProbe;
import org.watson.demos.services.GreetingService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.watson.demos.utilities.GeneratorTestUtility.generateGreetings;

@SpringBootTest(classes = GreetingRestController.class)
@Import(SimpleMeterRegistry.class)
class GreetingRestControllerTest {
    private static final List<Greeting> TEST_CONTENT = generateGreetings("controller-content");

    @MockBean
    private GreetingService service;

    @Resource
    private GreetingRestController controller;

    @Test
    void getOne_passesThroughToService() {
        final Greeting expected = TEST_CONTENT.stream().findAny().orElseThrow();
        when(service.getOne(any())).thenReturn(Optional.of(expected));

        assertThat(controller.getGreeting(expected.getId())).isSameAs(expected);

        verify(service).getOne(expected.getId());
    }

    @Test
    void getOne_throwsWhenOptionalEmpty() {
        when(service.getOne(any())).thenReturn(Optional.empty());

        final UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> controller.getGreeting(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("greeting '%s' Not Found", id.toString());
    }

    @Test
    void create_passesThroughToService() {
        when(service.createAll(any())).thenReturn(TEST_CONTENT);

        final List<Greeting> input = generateGreetings("create-controller-content");

        assertThat(controller.createGreetings(input)).containsExactlyElementsOf(TEST_CONTENT);

        verify(service).createAll(input);
    }

    @Test
    void getAll_passesThroughToService() {
        when(service.getAll(any(), any())).thenReturn(new PageImpl<>(TEST_CONTENT));

        final GreetingProbe probe = GreetingProbe.builder().locale(Locale.US).build();
        final Pageable pageable = Pageable.ofSize(2);

        assertThat(controller.getGreetings(probe, pageable)).containsExactlyElementsOf(TEST_CONTENT);

        verify(service).getAll(probe, pageable);
    }

    @Test
    void delete_passesThroughToService() {
        final Set<UUID> input = IntStream.range(0, 3).boxed()
                .map(i -> UUID.randomUUID())
                .collect(Collectors.toUnmodifiableSet());

        controller.deleteGreetings(input);

        verify(service).deleteAll(input);
    }
}
