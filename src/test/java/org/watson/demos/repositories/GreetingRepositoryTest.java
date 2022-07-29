package org.watson.demos.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.watson.demos.models.Greeting;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@DataJpaTest
class GreetingRepositoryTest {
    private static final List<Greeting> TEST_VALUES = IntStream.range(0, 10).boxed()
            .map(i -> Greeting.builder()
                    .locale(Locale.getAvailableLocales()[i])
                    .content("ohai"))
            .map(Greeting.GreetingBuilder::build)
            .collect(Collectors.toUnmodifiableList());

    @Resource
    private GreetingRepository repository;

    @Test
    void testStuff() {
        repository.saveAll(TEST_VALUES);

        final Page<Greeting> actual = repository.findAll(Pageable.unpaged());

        assertThat(actual, notNullValue());
        assertThat(actual.getTotalElements(), is((long) TEST_VALUES.size()));
        assertThat(actual.get().collect(Collectors.toList()), is(TEST_VALUES));
    }
}
