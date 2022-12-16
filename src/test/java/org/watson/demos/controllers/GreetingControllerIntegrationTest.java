package org.watson.demos.controllers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.unit.DataSize;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.GreetingProbe;
import org.watson.demos.services.GreetingService;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.watson.demos.utilities.ConverterTestUtility.toBiConsumer;
import static org.watson.demos.utilities.ConverterTestUtility.toQueryString;
import static org.watson.demos.utilities.GeneratorTestUtility.generateGreetings;

@Tag("Integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(GreetingController.class)
class GreetingControllerIntegrationTest {
    private static final String VERSION_1 = "v1";
    private static final Map<UUID, Greeting> INPUT_VALUES = new LinkedHashMap<>();
    private static final Map<UUID, Greeting> EXPECTED_VALUES = generateGreetings("integrate").stream()
            .map(g -> Map.entry(UUID.randomUUID(), g))
            .peek(toBiConsumer(INPUT_VALUES::put))
            .map(e -> e.getValue().toBuilder().id(e.getKey()))
            .map(g -> g.created(ZonedDateTime.now(ZoneId.of("UTC")).withNano(0)))
            .map(Greeting.GreetingBuilder::build)
            .collect(Collectors.toUnmodifiableMap(Greeting::getId, Function.identity()));

    @Value("${spring.data.web.pageable.default-page-size:20}")
    private int defaultPageSize;
    @Value("${spring.data.web.pageable.max-page-size:2000}")
    private int maxPageSize;
    @Value("${server.max-http-header-size:8KB}")
    private DataSize maxHeaderSize;

    @MockBean
    private GreetingService service;
    @Resource
    private MockMvc mockMvc;
    @Resource
    private ObjectMapper objectMapper;

    @BeforeEach
    void beforeEach() {
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        when(service.getOne(any()))
                .thenAnswer(a -> Optional.ofNullable(EXPECTED_VALUES.get(a.getArgument(0, UUID.class))));

        when(service.createAll(anyCollection()))
                .thenAnswer(a -> ((List<?>) a.getArgument(0, ArrayList.class)).stream()
                        .filter(Greeting.class::isInstance)
                        .map(Greeting.class::cast)
                        .map(Greeting::getContent)
                        .collect(Collectors.collectingAndThen(Collectors.toUnmodifiableSet(),
                                in -> EXPECTED_VALUES.values().stream()
                                        .filter(g -> in.contains(g.getContent()))
                                        .collect(Collectors.toList()))));

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
    void postGreetings(final List<Greeting> expected) {
        final Object request = expected.size() == 1 ? expected.get(0) : expected;
        final ResultActions result = mockMvc.perform(post("/{version}/greetings", VERSION_1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        assertCreatedHeaders(result, expected.size());
        assertActualMatchesExpected(result.andReturn(), expected);

        verify(service).createAll(expected);
    }

    Stream<Arguments> postGreetings() {
        return Stream.of(
                Arguments.of(Named.of("Greeting[1]", subList(INPUT_VALUES.values(), 0, 1))),
                Arguments.of(Named.of("Greeting[" + (INPUT_VALUES.size() - 1) + "]", subList(INPUT_VALUES.values(), 1, INPUT_VALUES.size())))
        );
    }

    @SneakyThrows
    @MethodSource
    @ParameterizedTest
    void getGreetings(final Pageable pageable) {
        final ResultActions result = mockMvc.perform(get("/{version}/greetings" + toQueryString(pageable), VERSION_1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.LINK));

        assertPageHeaders(result, pageable);

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
        for (final UUID id : EXPECTED_VALUES.keySet()) {
            final String actual = mockMvc.perform(get("/{version}/greetings/{id}", VERSION_1, id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertThat(actual).isNotEmpty();
            assertThat(objectMapper.readValue(actual, Greeting.class)).isEqualTo(EXPECTED_VALUES.get(id));

            verify(service).getOne(id);
        }
    }

    @SneakyThrows
    @MethodSource
    @ParameterizedTest
    void deleteGreeting(final List<UUID> ids) {
        mockMvc.perform(delete("/{version}/greetings" + toQueryString(ids, e -> e, "id"), VERSION_1))
                .andExpect(status().isNoContent());

        verify(service).deleteAll(Set.copyOf(ids));
    }

    Stream<Arguments> deleteGreeting() {
        final int maxUrlLength = Math.toIntExact((maxHeaderSize.toBytes() - 200) / 40); // (Max-Request size - room for headers and base URL) / Length of "&id={UUID}" (4+36)
        final int size = IntStream.of(INPUT_VALUES.size(), maxPageSize, maxUrlLength).min().orElse(1);
        return Stream.of(
                Arguments.of(Named.of("UUID[1]", subList(INPUT_VALUES.keySet(), 0, 1))),
                Arguments.of(Named.of("UUID[" + size + "]", subList(INPUT_VALUES.keySet(), 1, size + 1)))
        );
    }

    @SneakyThrows
    private void assertPageHeaders(final ResultActions result, final Pageable pageable) {
        result.andExpect(header().exists("Page-Index"))
                .andExpect(header().stringValues("Page-Index", String.valueOf(pageable.isUnpaged() ? 0 : pageable.getPageNumber())))
                .andExpect(header().exists("Page-Total-Pages"))
                .andExpect(header().stringValues("Page-Total-Pages", String.valueOf(calculateTotalPages(pageable))))
                .andExpect(header().exists("Page-Total-Elements"))
                .andExpect(header().stringValues("Page-Total-Elements", String.valueOf(EXPECTED_VALUES.size())))
                .andExpect(header().exists("Page-Size"))
                .andExpect(header().stringValues("Page-Size", String.valueOf(pageable.isUnpaged() ? 20 : pageable.getPageSize())))
                .andExpect(header().exists("Page-Sort"))
                .andExpect(header().stringValues("Page-Sort", String.valueOf(pageable.getSort())));
    }

    @SneakyThrows
    private void assertCreatedHeaders(final ResultActions result, final int expectedEntries) {
        result.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        if (expectedEntries == 0) {
            result.andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                    .andExpect(header().doesNotExist(HttpHeaders.CONTENT_LOCATION));
        } else if (expectedEntries == 1) {
            result.andExpect(header().doesNotExist(HttpHeaders.CONTENT_LOCATION))
                    .andExpect(header().exists(HttpHeaders.LOCATION));
        } else {
            result.andExpect(header().doesNotExist(HttpHeaders.LOCATION))
                    .andExpect(header().exists(HttpHeaders.CONTENT_LOCATION));
        }
    }

    @SneakyThrows
    private void assertActualMatchesExpected(final MvcResult result, final List<Greeting> expected) {
        final List<Greeting> actual = objectMapper.readValue(result.getResponse().getContentAsString(), ListOfGreetings.class);

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

    private int calculateTotalPages(final Pageable pageable) {
        return (int) Math.ceil(EXPECTED_VALUES.size() / (double) (pageable.isUnpaged() ? 20 : pageable.getPageSize()));
    }

    private <T> List<T> subList(final Collection<T> collection, final long offset, final long size) {
        return collection.stream()
                .skip(offset % collection.size())
                .limit(size % collection.size())
                .collect(Collectors.toUnmodifiableList());
    }

    private static class ListOfGreetings extends ArrayList<Greeting> {}
}
