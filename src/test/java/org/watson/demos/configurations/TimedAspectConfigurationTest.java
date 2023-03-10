package org.watson.demos.configurations;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TimedAspectConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TimedAspectConfiguration.class);

    @ValueSource(strings = "timedAspectConfiguration")
    @ParameterizedTest
    void enabledByDefault(final String beanName) {
        contextRunner
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> assertThat(context).hasBean(beanName));
    }

    @ValueSource(strings = "timedAspectConfiguration")
    @ParameterizedTest
    void disabledByProperty(final String beanName) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("spring.timed.aspect.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(beanName));
    }

    @ValueSource(strings = "timedAspectConfiguration")
    @ParameterizedTest
    void disabledByMissingDependencyBean(final String beanName) {
        contextRunner
                .run(context -> assertThat(context).doesNotHaveBean(beanName));
    }
}
