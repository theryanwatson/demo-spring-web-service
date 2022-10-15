package org.watson.demos.controllers;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.watson.demos.models.Greeting;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GreetingControllerIntegrationTests {
    private static final String VERSION_1 = "v1";
    private static final List<Greeting> TEST_VALUES = IntStream.range(0, 10).boxed()
            .map(i -> Greeting.builder()
                    .locale(Locale.getAvailableLocales()[i])
                    .content("integrate " + i))
            .map(Greeting.GreetingBuilder::build)
            .collect(Collectors.toUnmodifiableList());

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

    static Stream<Arguments> postGreetings() {
        return Stream.of(
                Arguments.of(TEST_VALUES.subList(0, 1)),
                Arguments.of(TEST_VALUES.subList(1, TEST_VALUES.size()))
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

    static Stream<Arguments> getGreetings() {
        return Stream.of(
                Arguments.of(Pageable.unpaged()),
                Arguments.of(PageRequest.ofSize(1)),
                Arguments.of(PageRequest.of(1, TEST_VALUES.size() / 2, Sort.by("locale", "content"))),
                Arguments.of(PageRequest.of(2, TEST_VALUES.size() / 3, Sort.Direction.DESC, "content")),
                Arguments.of(PageRequest.of(3, TEST_VALUES.size() / 4))
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

    static Stream<Arguments> deleteGreeting() {
        return Stream.of(
                Arguments.of(PageRequest.ofSize(1)),
                Arguments.of(PageRequest.ofSize(TEST_VALUES.size()))
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

    private static String buildQueryString(final Pageable pageable) {
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
