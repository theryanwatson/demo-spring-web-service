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

    @ValueSource(strings = {"meterRegistryConfiguration", "countedAspect", "timedAspect"})
    @ParameterizedTest
    void enabledByDefault(final String beanName) {
        contextRunner
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> assertThat(context).hasBean(beanName));
    }

    @ValueSource(strings = {"countedAspect", "timedAspect"})
    @ParameterizedTest
    void aspect_disabledByProperty(final String beanName) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withPropertyValues("spring.meter.aspect.enabled=false")
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
