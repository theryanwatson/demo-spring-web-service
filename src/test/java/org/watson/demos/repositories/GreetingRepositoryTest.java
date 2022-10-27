package org.watson.demos.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.watson.demos.models.Greeting;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.watson.demos.utilities.GeneratorTestUtility.generateGreetings;

@DataJpaTest
class GreetingRepositoryTest {
    private static final List<Greeting> TEST_VALUES = generateGreetings("ohai");

    @Resource
    private GreetingRepository repository;

    @Test
    void saveAll_writesToRepository() {
        repository.saveAll(TEST_VALUES);

        final Page<Greeting> actual = repository.findAll(Pageable.unpaged());

        assertThat(actual).isNotNull();
        assertThat(actual.getTotalElements()).isEqualTo(TEST_VALUES.size());
        assertThat(actual.get().collect(Collectors.toUnmodifiableList())).containsExactlyInAnyOrderElementsOf(TEST_VALUES);
    }

    @Test
    void saveGreeting_setsIdAndCreatedDate() {
        final Greeting savedEntry = repository.save(Greeting.builder().content("some entry").locale(Locale.getDefault()).build());
        assertThat(savedEntry.getId()).isNotNull();
        assertThat(savedEntry.getCreated()).isBetween(ZonedDateTime.now(ZoneId.of("UTC")).minusSeconds(5), ZonedDateTime.now(ZoneId.of("UTC")));

        final Optional<Greeting> actual = repository.findById(savedEntry.getId());
        assertThat(actual).isPresent();
        assertThat(actual.get().getCreated()).isBetween(ZonedDateTime.now(ZoneId.of("UTC")).minusSeconds(5), ZonedDateTime.now(ZoneId.of("UTC")));
    }

    @Test
    void findAllByLocal_returnsEntries() {
        final List<Greeting> expected = saveAll(
                Greeting.builder().content("oh").locale(Locale.getDefault()).build(),
                Greeting.builder().content("hello").locale(Locale.getDefault()).build(),
                Greeting.builder().content("there").locale(Locale.getDefault()).build());

        repository.save(Greeting.builder().content("stuff").locale(Locale.CANADA_FRENCH).build());

        final Page<Greeting> actual = repository.findAllByLocale(Locale.getDefault(), Pageable.unpaged());

        assertThat(actual.getTotalElements()).isEqualTo(expected.size());
        assertThat(actual.getContent()).isEqualTo(expected);
    }

    @Test
    void findAllByLocale_pagesAsExpected() {
        final List<Greeting> expected = saveAll(
                Greeting.builder().content("hello").locale(Locale.getDefault()).build(),
                Greeting.builder().content("there").locale(Locale.getDefault()).build(),
                Greeting.builder().content("friend").locale(Locale.getDefault()).build());

        for (int i = 0; i < expected.size(); i++) {
            final Page<Greeting> actual = repository.findAllByLocale(Locale.getDefault(), PageRequest.of(i, 1));

            assertThat(actual.getNumberOfElements()).isEqualTo(1);
            assertThat(actual.getTotalElements()).isEqualTo(expected.size());
            assertThat(actual.getContent()).isEqualTo(List.of(expected.get(i)));
        }
    }

    @Test
    void deleteById_deletesEntries() {
        final Greeting savedEntry = Greeting.builder().content("first entry").locale(Locale.getDefault()).build();
        final Greeting secondEntry = Greeting.builder().content("second entry").locale(Locale.getDefault()).build();
        saveAll(savedEntry, secondEntry);
        repository.deleteById(savedEntry.getId());

        final Page<Greeting> actual = repository.findAllByLocale(Locale.getDefault(), Pageable.unpaged());
        assertThat(actual).anyMatch(n -> n.getId().equals(secondEntry.getId()));
        assertThat(actual).noneMatch(n -> n.getId().equals(savedEntry.getId()));
    }

    private List<Greeting> saveAll(final Greeting... entries) {
        return StreamSupport
                .stream(repository.saveAll(Arrays.asList(entries)).spliterator(), false)
                .collect(Collectors.toUnmodifiableList());
    }
}
