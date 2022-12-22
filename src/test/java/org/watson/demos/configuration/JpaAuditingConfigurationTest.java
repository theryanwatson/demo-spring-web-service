package org.watson.demos.configuration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JpaAuditingConfiguration.class);

    @ValueSource(strings = {"auditingConfiguration", "jpaAuditingHandler"})
    @ParameterizedTest
    void disabledByProperty(final String beanName) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withPropertyValues("spring.jpa.auditing.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(beanName));
    }
}
