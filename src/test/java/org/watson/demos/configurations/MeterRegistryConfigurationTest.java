package org.watson.demos.configurations;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class MeterRegistryConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MeterRegistryConfiguration.class);

    @ValueSource(strings = "meterRegistryConfiguration")
    @ParameterizedTest
    void enabledByDefault(final String beanName) {
        contextRunner
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> assertThat(context).hasBean(beanName));
    }

    @ValueSource(strings = "meterRegistryConfiguration")
    @ParameterizedTest
    void disabledByMissingDependencyBean(final String beanName) {
        contextRunner
                .run(context -> assertThat(context).doesNotHaveBean(beanName));
    }

    @ValueSource(strings = "commonTags")
    @ParameterizedTest
    void commonTags_enabledByDefault(final String beanName) {
        contextRunner
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(BuildProperties.class, () -> new BuildProperties(new Properties()))
                .run(context -> assertThat(context).hasBean(beanName));
    }

    @ValueSource(strings = "commonTags")
    @ParameterizedTest
    void commonTags_disabledByMissingDependencyBean(final String beanName) {
        contextRunner
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> assertThat(context).doesNotHaveBean(beanName));
    }
}
