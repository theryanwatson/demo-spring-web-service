package org.watson.demos.controllers;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.watson.demos.models.Greeting;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("Integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureTestDatabase
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GreetingControllerIntegrationTest {
    private static final String VERSION_1 = "v1";
    private static final Locale[] AVAILABLE_LOCALES = Locale.getAvailableLocales();
    private static final List<Greeting> TEST_VALUES = IntStream.range(0, 10).boxed()
            .map(i -> Greeting.builder()
                    .locale(AVAILABLE_LOCALES[i % AVAILABLE_LOCALES.length])
                    .content("integrate " + i))
            .map(Greeting.GreetingBuilder::build)
            .collect(Collectors.toUnmodifiableList());

    @Value("${spring.data.web.pageable.max-page-size:2000}")
    private int maxPageSize;
    @Value("${server.max-http-header-size:8KB}")
    private DataSize maxHeaderSize;

    @LocalServerPort
    private int port;
    private final TestRestTemplate template = new TestRestTemplate(new RestTemplateBuilder()
            .rootUri("http://localhost:{port}/{version}")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

    @Order(0)
    @EmptySource
    @MethodSource
    @ParameterizedTest
    void postGreetings(final List<Greeting> expected) {
        final Object request = expected.size() == 1 ? expected.get(0) : expected;
        final ResponseEntity<ListOfGreetings> actual = template.postForEntity("/greetings", request, ListOfGreetings.class, port, VERSION_1);

        assertCreatedHeaders(actual, expected.size());
        assertActualMatchesExpected(actual.getBody(), expected);
    }

    Stream<Arguments> postGreetings() {
        return Stream.of(
                Arguments.of(Named.of("Greeting[1]", TEST_VALUES.subList(0, 1))),
                Arguments.of(Named.of("Greeting[" + (TEST_VALUES.size() - 1) + "]", TEST_VALUES.subList(1, TEST_VALUES.size())))
        );
    }

    @Order(10)
    @MethodSource
    @ParameterizedTest
    void getGreetings(final Pageable pageable) {
        final ResponseEntity<ListOfGreetings> actual = template.getForEntity("/greetings" + buildQueryString(pageable), ListOfGreetings.class, port, VERSION_1);

        assertPageHeaders(actual, pageable);
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getHeaders().get(HttpHeaders.LINK)).isNotEmpty();
    }

    Stream<Arguments> getGreetings() {
        return Stream.of(
                Arguments.of(Pageable.unpaged()),
                Arguments.of(PageRequest.ofSize(1)),
                Arguments.of(PageRequest.of(1, Math.min(TEST_VALUES.size() / 2, maxPageSize), Sort.by("locale", "content"))),
                Arguments.of(PageRequest.of(2, Math.min(TEST_VALUES.size() / 3, maxPageSize), Sort.Direction.DESC, "content")),
                Arguments.of(PageRequest.of(3, Math.min(TEST_VALUES.size() / 4, maxPageSize)))
        );
    }

    @Order(20)
    @Test
    void getGreeting() {
        final ResponseEntity<ListOfGreetings> sourceEntries = template.getForEntity("/greetings", ListOfGreetings.class, port, VERSION_1);

        assertThat(sourceEntries.getBody()).isNotNull();
        sourceEntries.getBody().forEach(entry -> {
            final ResponseEntity<Greeting> actual = template.getForEntity("/greetings/{id}", Greeting.class, port, VERSION_1, entry.getId());

            assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(actual.getBody()).isEqualTo(entry);
        });
    }

    @Order(30)
    @MethodSource
    @ParameterizedTest
    void deleteGreeting(final Pageable pageable) {
        final ResponseEntity<ListOfGreetings> expected = template.getForEntity("/greetings" + buildQueryString(pageable), ListOfGreetings.class, port, VERSION_1);
        assertThat(expected.getBody()).isNotEmpty();

        final String idParameters = expected.getBody().stream()
                .map(Greeting::getId)
                .map(id -> "id=" + id)
                .collect(Collectors.joining("&", "?", ""));
        ResponseEntity<Void> actual = template.exchange("/greetings" + idParameters, HttpMethod.DELETE, null, Void.class, port, VERSION_1);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    Stream<Arguments> deleteGreeting() {
        final int maxUrlLength = Math.toIntExact((maxHeaderSize.toBytes() - 200) / 40); // (Max-Request size - room for headers and base URL) / Length of "&id={UUID}" (4+36)
        return Stream.of(
                Arguments.of(PageRequest.ofSize(1)),
                Arguments.of(PageRequest.ofSize(IntStream.of(TEST_VALUES.size(), maxPageSize, maxUrlLength).min().orElse(1)))
        );
    }

    @SuppressWarnings("UastIncorrectHttpHeaderInspection")
    private void assertPageHeaders(final ResponseEntity<?> actual, final Pageable pageable) {
        assertThat(actual.getHeaders().get("Page-Index")).satisfies(
                h -> assertThat(h).isNotEmpty(),
                h -> assertThat(h.get(0)).isEqualTo(String.valueOf(pageable.isUnpaged() ? 0 : pageable.getPageNumber()))
        );
        assertThat(actual.getHeaders().get("Page-Total-Pages")).satisfies(
                h -> assertThat(h).isNotEmpty(),
                h -> assertThat(h.get(0)).isEqualTo(String.valueOf(calculateTotalPages(pageable)))
        );
        assertThat(actual.getHeaders().get("Page-Total-Elements")).satisfies(
                h -> assertThat(h).isNotEmpty(),
                h -> assertThat(h.get(0)).isEqualTo(String.valueOf(TEST_VALUES.size()))
        );
        assertThat(actual.getHeaders().get("Page-Size")).satisfies(
                h -> assertThat(h).isNotEmpty(),
                h -> assertThat(h.get(0)).isEqualTo(String.valueOf(pageable.isUnpaged() ? 20 : pageable.getPageSize()))
        );
        assertThat(actual.getHeaders().get("Page-Sort")).satisfies(
                h -> assertThat(h).isNotEmpty(),
                h -> assertThat(h.get(0)).isEqualTo(String.valueOf(pageable.getSort()))
        );
    }

    private void assertCreatedHeaders(final ResponseEntity<?> actual, final int expectedEntries) {
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(actual.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        if (expectedEntries == 0) {
            assertThat(actual.getHeaders())
                    .doesNotContainKey(HttpHeaders.LOCATION)
                    .doesNotContainKey(HttpHeaders.CONTENT_LOCATION);
        } else if (expectedEntries == 1) {
            assertThat(actual.getHeaders())
                    .containsKey(HttpHeaders.LOCATION)
                    .doesNotContainKey(HttpHeaders.CONTENT_LOCATION);
        } else {
            assertThat(actual.getHeaders())
                    .doesNotContainKey(HttpHeaders.LOCATION)
                    .containsKey(HttpHeaders.CONTENT_LOCATION);
        }
    }

    private void assertActualMatchesExpected(final List<Greeting> actual, final List<Greeting> expected) {
        assertThat(actual).hasSize(expected.size());

        for (int i = 0; i < expected.size(); i++) {
            final Greeting expectedEntry = expected.get(i);

            assertThat(actual.get(i)).satisfies(
                    a -> assertThat(a).isNotNull(),
                    a -> assertThat(a.getId()).isNotNull(),
                    a -> assertThat(a.getCreated()).isBetween(ZonedDateTime.now().minusMinutes(5), ZonedDateTime.now()),
                    a -> assertThat(a.getLocale()).isEqualTo(expectedEntry.getLocale()),
                    a -> assertThat(a.getContent()).isEqualTo(expectedEntry.getContent())
            );
        }
    }

    private String buildQueryString(final Pageable pageable) {
        final List<String> queryParameters = new ArrayList<>();
        if (pageable.isPaged()) {
            queryParameters.add("page=" + pageable.getPageNumber());
            queryParameters.add("size=" + pageable.getPageSize());
        }
        pageable.getSort().get()
                .map(s -> "sort=" + s.getProperty() + (s.isDescending() ? ",desc" : ""))
                .forEach(queryParameters::add);
        return queryParameters.stream()
                .collect(Collectors.joining("&", "?", ""));
    }

    private int calculateTotalPages(final Pageable pageable) {
        return (int) Math.ceil(TEST_VALUES.size() / (double) (pageable.isUnpaged() ? 20 : pageable.getPageSize()));
    }

    private static class ListOfGreetings extends ArrayList<Greeting> {}
}
