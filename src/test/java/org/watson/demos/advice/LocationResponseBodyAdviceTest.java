package org.watson.demos.advice;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.watson.demos.models.Identifiable;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.watson.demos.utilities.GeneratorTestUtility.generateIdentifiable;

@SpringBootTest(classes = LocationResponseBodyAdvice.class)
@ImportAutoConfiguration(SpringDataWebAutoConfiguration.class)
class LocationResponseBodyAdviceTest {
    private static final String FAKE_URI = "https://www.fake.fake:5150/things/cool";
    private static final List<Identifiable<?>> EXPECTED = generateIdentifiable("advice", 11);

    private final HttpHeaders headers = new HttpHeaders();

    @Mock
    private ServerHttpRequest request;
    @Mock
    private ServerHttpResponse response;

    @Resource
    private LocationResponseBodyAdvice advice;

    @BeforeEach
    void setup() {
        when(response.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create(FAKE_URI));
    }

    @Test
    void supports_true() {
        assertThat(advice.supports(ExampleMethodController.getMethodParameter("supportedMethod"), null)).isTrue();
    }

    @ValueSource(strings = {"unsupportedResponseStatus", "unspecifiedResponseStatus", "unsupportedReturnType", "voidReturnType"})
    @ParameterizedTest
    void supports_false(final String methodName) {
        assertThat(advice.supports(ExampleMethodController.getMethodParameter(methodName), null)).isFalse();
    }

    @Test
    void beforeBodyWrite_passesObjectThrough() {
        assertThat(advice.beforeBodyWrite(EXPECTED, null, null, null, request, response))
                .isSameAs(EXPECTED);
    }

    @Test
    void beforeBodyWrite_singleEntry_writesLocationHeader() {
        advice.beforeBodyWrite(EXPECTED.subList(0, 1), null, null, null, request, response);

        assertThat(headers.getLocation())
                .isEqualTo(URI.create(FAKE_URI + "/" + EXPECTED.get(0).getId()));
    }

    @Test
    void beforeBodyWrite_singleEntry_handlesNullId() {
        advice.beforeBodyWrite(List.of(() -> null), null, null, null, request, response);

        assertThat(headers.getLocation()).isNull();
        assertThat(headers.get(HttpHeaders.CONTENT_LOCATION)).isNull();
    }

    @Test
    void beforeBodyWrite_manyEntries_writesContentLocationHeader() {
        advice.beforeBodyWrite(EXPECTED, null, null, null, request, response);

        assertThat(headers.get(HttpHeaders.CONTENT_LOCATION))
                .containsExactlyInAnyOrderElementsOf(EXPECTED.stream().map(e -> "/" + e.getId()).collect(Collectors.toUnmodifiableList()));
    }

    @NullAndEmptySource
    @MethodSource
    @ParameterizedTest
    void beforeBodyWrite_invalid_writesNoLocationHeaders(final List<Identifiable<?>> entries) {
        advice.beforeBodyWrite(entries, null, null, null, request, response);

        assertThat(headers.getLocation()).isNull();
        assertThat(headers.get(HttpHeaders.CONTENT_LOCATION)).isNull();
    }

    static Stream<Arguments> beforeBodyWrite_invalid_writesNoLocationHeaders() {
        return Stream.of(
                Arguments.of(List.<Identifiable<?>>of(() -> null))
        );
    }

    @Test
    void beanNotCreatedIfNotWebApplication() {
        new ApplicationContextRunner().withPropertyValues("spring.config.location=classpath:empty.properties")
                .withConfiguration(UserConfigurations.of(LocationResponseBodyAdvice.class))
                .run(context -> assertThat(context).doesNotHaveBean(LocationResponseBodyAdvice.class));
    }

    @MethodSource
    @ParameterizedTest
    void beforeBodyWrite_managesBeingMisconfigured(final String headerSize, final String reserveSize, final String charLength, int expectedSize, final Collection<Identifiable<?>> input) {
        new WebApplicationContextRunner()
                .withConfiguration(UserConfigurations.of(LocationResponseBodyAdvice.class))
                .withBean("conversionService", ApplicationConversionService.class)
                .withPropertyValues(
                        "server.max-http-header-size=" + headerSize,
                        "identifiable.location.header-reserve-size=" + reserveSize,
                        "identifiable.location.path.prefix=",
                        "identifiable.location.id.character.length=" + charLength
                )
                .run(context -> {
                    assertThat(context).getBean(LocationResponseBodyAdvice.class).isNotNull();
                    context.getBean(LocationResponseBodyAdvice.class)
                            .beforeBodyWrite(input, null, null, null, request, response);

                    if (expectedSize == 0) {
                        assertThat(headers.get(HttpHeaders.CONTENT_LOCATION)).isNullOrEmpty();
                    } else {
                        assertThat(headers.get(HttpHeaders.CONTENT_LOCATION)).hasSize(expectedSize);
                    }
                });
    }

    static Stream<Arguments> beforeBodyWrite_managesBeingMisconfigured() {
        final int inputSize = 150;
        final Named<List<Identifiable<?>>> input = Named.of("ExampleIdentifiable[" + inputSize + "]", generateIdentifiable("input", inputSize));
        return Stream.of(
                Arguments.of("8KB", "3KB", "36", 142, input), // Default config
                Arguments.of("1KB", "1KB", "1", 0, input), // 0 Header Space
                Arguments.of("1KB", "2KB", "1", 0, input), // Negative Header Space
                Arguments.of("2KB", "1KB", "0", 0, input), // Divide by 0
                Arguments.of("1KB", "0KB", "-1", 0, input), // Negative value
                Arguments.of("1000TB", "0KB", "1", inputSize, input), // Large value
                Arguments.of(String.valueOf(Integer.MAX_VALUE * (inputSize - 1L)), "0KB", String.valueOf(Integer.MAX_VALUE), inputSize - 1, input) // Large values
        );
    }

    @SuppressWarnings("unused")
    private static class ExampleMethodController {
        @SneakyThrows
        private static MethodParameter getMethodParameter(String methodName) {
            return MethodParameter.forExecutable(ExampleMethodController.class.getDeclaredMethod(methodName), -1);
        }

        @ResponseStatus(HttpStatus.CREATED)
        Collection<Identifiable<?>> supportedMethod() {return null;}

        @ResponseStatus(HttpStatus.ACCEPTED)
        Collection<Identifiable<?>> unsupportedResponseStatus() {return null;}

        Collection<Identifiable<?>> unspecifiedResponseStatus() {return null;}

        @ResponseStatus(HttpStatus.CREATED)
        String unsupportedReturnType() {return null;}

        @ResponseStatus(HttpStatus.CREATED)
        void voidReturnType() {}
    }
}
