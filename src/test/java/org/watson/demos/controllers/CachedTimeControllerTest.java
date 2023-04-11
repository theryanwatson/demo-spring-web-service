package org.watson.demos.controllers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.watson.demos.models.CacheMode;
import org.watson.demos.models.CacheStyle;
import org.watson.demos.services.TimeServiceAnnotation;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = CachedTimeController.class, properties = {
        "cached.time.controller.default.mode=none"
})
class CachedTimeControllerTest {

    @MockBean
    private TimeServiceAnnotation service;
    @Value("${cached.time.controller.default.mode}")
    private CacheMode defaultMode;
    @Resource
    private CachedTimeController controller;

    @NullSource
    @EnumSource(CacheMode.class)
    @ParameterizedTest
    void get_modeRoutesToExpectedServiceMethod(final CacheMode mode) {
        final Flux<Object> expected = Flux.just("some-result");
        final Optional<String> input = Optional.of("some/input");
        final Optional<CacheMode> selectedMode = Optional.ofNullable(mode);

        when(service.route(any())).thenReturn(TimeServiceAnnotation::get);
        when(service.get(any())).thenReturn(expected);

        final Flux<Object> actual = controller.get(input, selectedMode, Optional.of(CacheStyle.annotation));

        assertThat(actual).isEqualTo(expected);

        verify(service).route(selectedMode.orElse(defaultMode));
        verify(service).get(input);
        verifyNoMoreInteractions(service);
    }
}
