package org.watson.demos.controllers;

import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.GreetingProbe;
import org.watson.demos.services.GreetingService;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.watson.demos.utilities.ConverterTestUtility.subList;
import static org.watson.demos.utilities.ConverterTestUtility.toBiConsumer;
import static org.watson.demos.utilities.GeneratorTestUtility.generateGreetings;

@Tag("Integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@GraphQlTest(GreetingGraphController.class)
class GreetingGraphControllerIntegrationTest {
    private static final String GREETING_FIELDS = """
                id
                content
                locale
                created
                modified
            """;
    private static final Map<UUID, Greeting> INPUT_VALUES = new LinkedHashMap<>();
    private static final Map<UUID, Greeting> EXPECTED_VALUES = generateGreetings("integrate").stream()
            .map(g -> Map.entry(UUID.randomUUID(), g))
            .peek(toBiConsumer(INPUT_VALUES::put))
            .map(e -> e.getValue().toBuilder().id(e.getKey()))
            .map(g -> g.created(ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)))
            .map(g -> g.modified(ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)))
            .map(Greeting.GreetingBuilder::build)
            .collect(Collectors.toUnmodifiableMap(Greeting::getId, Function.identity()));

    @Value("${spring.data.web.pageable.default-page-size:20}")
    private int defaultPageSize;
    @Value("${spring.data.web.pageable.max-page-size:2000}")
    private int maxPageSize;

    @MockBean
    private GreetingService service;
    @Resource
    private GraphQlTester tester;

    @BeforeEach
    void beforeEach() {
        when(service.getOne(any()))
                .thenAnswer(a -> Optional.ofNullable(EXPECTED_VALUES.get(a.getArgument(0, UUID.class))));

        when(service.createAll(anyCollection()))
                .thenAnswer(a -> ((Collection<?>) a.getArgument(0, Collection.class)).stream()
                        .filter(Greeting.class::isInstance)
                        .map(Greeting.class::cast)
                        .map(Greeting::getContent)
                        .collect(Collectors.collectingAndThen(Collectors.toUnmodifiableSet(),
                                in -> EXPECTED_VALUES.values().stream()
                                        .filter(g -> in.contains(g.getContent()))
                                        .toList())));

        when(service.getAll(any(), any()))
                .thenAnswer(a -> {
                    final Pageable pageable = a.getArgument(1, Pageable.class);
                    final List<Greeting> sublist = subList(EXPECTED_VALUES.values(), pageable.getOffset(), pageable.getPageSize());
                    return new PageImpl<>(sublist, pageable, EXPECTED_VALUES.size());
                });
    }

    @SneakyThrows
    @EmptySource
    @MethodSource
    @ParameterizedTest
    void createGreetings(final List<Greeting> expected) {
        final GraphQlTester.Request<?> document = tester.document("""
                mutation CreateGreetings($greetings: [GreetingInput]!) {
                    createGreetings(greetings: $greetings) {
                        %s
                    }
                }""".formatted(GREETING_FIELDS));

        final GraphQlTester.Response response = expected.stream()
                .map(e -> Map.of("content", e.getContent(), "locale", e.getLocale()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), l -> document.variable("greetings", l)))
                .execute();

        final ListOfGreetings actual = response
                .path("createGreetings")
                .entity(ListOfGreetings.class)
                .get();

        assertActualMatchesExpected(
                actual.stream().sorted(Comparator.comparing(Greeting::getContent)).toList(),
                expected.stream().sorted(Comparator.comparing(Greeting::getContent)).toList());

        verify(service).createAll(Set.copyOf(expected));
    }

    @SneakyThrows
    @MethodSource("createGreetings")
    @ParameterizedTest
    void createGreeting(final List<Greeting> expectedList) {
        for (Greeting expected : expectedList) {
            GraphQlTester.Request<?> document = tester.document("""
                mutation CreateGreeting($greeting: GreetingInput!) {
                    createGreeting(greeting: $greeting) {
                        %s
                    }
                }""".formatted(GREETING_FIELDS));

            final GraphQlTester.Response response = document
                    .variable("greeting", Map.of("content", expected.getContent(), "locale", expected.getLocale()))
                    .execute();

            final Greeting actual = response
                    .path("createGreeting")
                    .entity(Greeting.class)
                    .get();

            assertActualMatchesExpected(actual, expected);

            verify(service).createAll(List.of(expected));
        }
    }

    Stream<Arguments> createGreetings() {
        return Stream.of(
                Arguments.of(Named.of("Greeting[1]", subList(INPUT_VALUES.values(), 0, 1))),
                Arguments.of(Named.of("Greeting[" + (INPUT_VALUES.size() - 1) + "]", subList(INPUT_VALUES.values(), 1, INPUT_VALUES.size())))
        );
    }

    @SneakyThrows
    @MethodSource
    @ParameterizedTest
    void getGreetings(final Pageable pageable) {
        final GraphQlTester.Request<?> document = tester.document("""
                query GetGreetings($locale: String = null, $page: Int = 0, $size: Int = 20, $sort: [String!] = null) {
                    greetings(locale: $locale, page: $page, size: $size, sort: $sort) {
                        %s
                    }
                }""".formatted(GREETING_FIELDS));

        if (pageable.isPaged()) {
            document.variable("page", pageable.getPageNumber())
                    .variable("size", pageable.getPageSize());
        }
        if (pageable.getSort().isSorted()) {
            document.variable("sort", pageable.getSort().get()
                    .map(s -> s.getProperty() + (s.isDescending() ? ",desc" : ""))
                    .toList());
        }

        final GraphQlTester.Response response = document.execute();
        response.errors().verify();

        verify(service).getAll(GreetingProbe.builder().build(), pageable.equals(Pageable.unpaged()) ? PageRequest.ofSize(defaultPageSize) : pageable);
    }

    Stream<Arguments> getGreetings() {
        return Stream.of(
                Arguments.of(Pageable.unpaged()),
                Arguments.of(PageRequest.ofSize(1)),
                Arguments.of(PageRequest.of(1, Math.min(EXPECTED_VALUES.size() / 2, maxPageSize), Sort.by("locale", "content"))),
                Arguments.of(PageRequest.of(2, Math.min(EXPECTED_VALUES.size() / 3, maxPageSize), Sort.Direction.DESC, "content")),
                Arguments.of(PageRequest.of(3, Math.min(EXPECTED_VALUES.size() / 4, maxPageSize)))
        );
    }

    @SneakyThrows
    @Test
    void getGreeting() {
        final GraphQlTester.Request<?> document = tester.document("""
                query GetGreeting($id: ID!) {
                    greeting(id: $id) { %s }
                }""".formatted(GREETING_FIELDS));

        EXPECTED_VALUES.forEach((id, expectedEntry) -> {
            final GraphQlTester.Response response = document.variable("id", id).execute();
            response.errors().verify();

            assertThat(response.path("greeting").entity(Greeting.class).get()).satisfies(
                    a -> assertThat(a.getId()).isEqualTo(expectedEntry.getId()),
                    a -> assertThat(a.getLocale()).isEqualTo(expectedEntry.getLocale()),
                    a -> assertThat(a.getContent()).isEqualTo(expectedEntry.getContent()),
                    a -> assertThat(a.getCreated()).isEqualTo(expectedEntry.getCreated()),
                    a -> assertThat(a.getModified()).isEqualTo(expectedEntry.getModified())
            );

            verify(service).getOne(id);
        });
    }

    @SneakyThrows
    @MethodSource
    @ParameterizedTest
    void deleteGreeting(final Collection<UUID> ids) {
        final GraphQlTester.Request<?> document = tester.document("""
                mutation DeleteGreetings($ids: [ID]!) {
                    deleteGreetings(ids: $ids)
                }""");
        document.variable("ids", ids).executeAndVerify();

        verify(service).deleteAll(Set.copyOf(ids));
    }

    Stream<Arguments> deleteGreeting() {
        return Stream.of(
                Arguments.of(Named.of("UUID[1]", subList(INPUT_VALUES.keySet(), 0, 1))),
                Arguments.of(Named.of("UUID[" + INPUT_VALUES.size() + "]", INPUT_VALUES.keySet()))
        );
    }

    private void assertActualMatchesExpected(final List<Greeting> actual, final List<Greeting> expected) {
        assertThat(actual).hasSize(expected.size());

        for (int i = 0; i < expected.size(); i++) {
            assertActualMatchesExpected(actual.get(i), expected.get(i));
        }
    }

    private void assertActualMatchesExpected(final Greeting actual, final Greeting expected) {
        assertThat(actual)
                .isNotNull()
                .satisfies(
                        a -> assertThat(a.getId()).isNotNull(),
                        a -> assertThat(a.getCreated()).isBetween(ZonedDateTime.now().minusMinutes(5), ZonedDateTime.now()),
                        a -> assertThat(a.getModified()).isBetween(ZonedDateTime.now().minusMinutes(5), ZonedDateTime.now()),
                        a -> assertThat(a.getLocale()).isEqualTo(expected.getLocale()),
                        a -> assertThat(a.getContent()).isEqualTo(expected.getContent())
                );
    }

    private static class ListOfGreetings extends ArrayList<Greeting> {}
}
