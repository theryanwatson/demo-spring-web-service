package org.watson.demos.repositories;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.watson.demos.configuration.JpaAuditingConfiguration;
import org.watson.demos.models.Greeting;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.watson.demos.utilities.GeneratorTestUtility.generateGreetings;

@DataJpaTest
@Import(JpaAuditingConfiguration.class)
class GreetingRepositoryTest {
    private static final List<Greeting> TEST_VALUES = generateGreetings("ohai");

    @Resource
    private GreetingRepository repository;

    @Test
    void saveAll_writesToRepository() {
        List<Greeting> saved = StreamSupport.stream(repository.saveAll(TEST_VALUES).spliterator(), false).toList();

        final Page<Greeting> actualPage = repository.findAll(Pageable.unpaged());
        assertThat(actualPage).isNotNull();
        assertThat(actualPage.getTotalElements()).isEqualTo(TEST_VALUES.size());

        final List<Greeting> actual = actualPage.getContent();
        for (AtomicInteger i = new AtomicInteger(0); i.get() < saved.size(); i.incrementAndGet()) {
            assertAuditFieldsSet(saved.get(i.get()));
            assertThat(actual.get(i.get())).satisfies(
                    a -> assertThat(a.getContent()).isEqualTo(TEST_VALUES.get(i.get()).getContent()),
                    a -> assertThat(a.getLocale()).isEqualTo(TEST_VALUES.get(i.get()).getLocale()),
                    a -> assertThat(a).isEqualTo(saved.get(i.get()))
            );
        }
    }

    @Test
    void saveGreeting_setsIdAndAuditDates() {
        final Greeting savedEntry = repository.save(Greeting.builder().content("some entry").locale(Locale.getDefault()).build());
        assertAuditFieldsSet(savedEntry);

        final Optional<Greeting> actual = repository.findById(savedEntry.getId());
        assertThat(actual).isPresent();
        assertAuditFieldsSet(actual.get());
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
        List<Greeting> savedEntries = saveAll(
                Greeting.builder().content("first entry").locale(Locale.getDefault()).build(),
                Greeting.builder().content("second entry").locale(Locale.getDefault()).build());

        repository.deleteById(savedEntries.get(0).getId());

        final Page<Greeting> actual = repository.findAllByLocale(Locale.getDefault(), Pageable.unpaged());
        assertThat(actual).anyMatch(n -> n.getId().equals(savedEntries.get(1).getId()));
        assertThat(actual).noneMatch(n -> n.getId().equals(savedEntries.get(0).getId()));
    }

    private List<Greeting> saveAll(final Greeting... entries) {
        return StreamSupport
                .stream(repository.saveAll(Arrays.asList(entries)).spliterator(), false)
                .toList();
    }

    private static void assertAuditFieldsSet(final Greeting entry) {
        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getCreated()).isBetween(ZonedDateTime.now(ZoneId.of("UTC")).minusSeconds(5), ZonedDateTime.now(ZoneId.of("UTC")));
        assertThat(entry.getModified()).isBetween(ZonedDateTime.now(ZoneId.of("UTC")).minusSeconds(5), ZonedDateTime.now(ZoneId.of("UTC")));
    }
}
