package org.watson.demos.configuration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EventRepositoryConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(EventRepositoryConfiguration.class);

    @ValueSource(strings = {"auditEventRepository", "httpExchangeRepository"})
    @ParameterizedTest
    void enabledByDefault(final String beanName) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .run(context -> assertThat(context).hasBean(beanName));
    }

    @ValueSource(strings = {"management.auditevents.enabled=false", "management.auditevents.repository.enabled=false"})
    @ParameterizedTest
    void auditEventRepository_disabledByProperty(final String property) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withPropertyValues(property)
                .run(context -> assertThat(context).doesNotHaveBean("auditEventRepository"));
    }

    @ValueSource(strings = {"management.httpexchanges.recording.enabled=false", "management.httpexchanges.recording.repository.enabled=false"})
    @ParameterizedTest
    void httpExchangeRepository_disabledByProperty(final String property) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withPropertyValues(property)
                .run(context -> assertThat(context).doesNotHaveBean("httpExchangeRepository"));
    }

    @MethodSource
    @ParameterizedTest
    <T> void auditEventRepository_disabledByExistingBean(Class<T> type, Supplier<T> supplier) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withBean(type, supplier)
                .run(context -> assertThat(context).hasSingleBean(type));
    }

    static Stream<Arguments> auditEventRepository_disabledByExistingBean() {
        return Stream.of(
                Arguments.of(AuditEventRepository.class, (Supplier<?>) InMemoryAuditEventRepository::new),
                Arguments.of(HttpExchangeRepository.class, (Supplier<?>) InMemoryHttpExchangeRepository::new)
        );
    }
}
