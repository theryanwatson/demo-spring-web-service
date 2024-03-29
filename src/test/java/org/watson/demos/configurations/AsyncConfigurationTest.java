package org.watson.demos.configurations;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AsyncConfiguration.class);

    @ValueSource(strings = "asyncConfiguration")
    @ParameterizedTest
    void enabledByDefault(final String beanName) {
        contextRunner
                .run(context -> assertThat(context).hasBean(beanName));
    }

    @ValueSource(strings = "asyncConfiguration")
    @ParameterizedTest
    void disabledByProperty(final String beanName) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withPropertyValues("spring.task.async.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(beanName));
    }
}
