package org.watson.demos.configurations;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.core.env.PropertyResolver;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class CachingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(CacheManager.class, NoOpCacheManager::new)
            .withBean(PropertyResolver.class, MockEnvironment::new)
            .withUserConfiguration(CachingConfiguration.class);

    @ValueSource(strings = "cachingConfiguration")
    @ParameterizedTest
    void enabledByDefault(final String beanName) {
        contextRunner
                .run(context -> assertThat(context).hasBean(beanName));
    }

    @ValueSource(strings = "placeholderNameCacheResolver")
    @ParameterizedTest
    void disabledByDefault(final String beanName) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .run(context -> assertThat(context).doesNotHaveBean(beanName));
    }

    @ValueSource(strings = "placeholderNameCacheResolver")
    @ParameterizedTest
    void enabledByProperty(final String beanName) {
        contextRunner
                .withPropertyValues("spring.cache.placeholder.name.cache.resolver.enabled=true")
                .run(context -> assertThat(context).hasBean(beanName));
    }

    @ValueSource(strings = "cachingConfiguration")
    @ParameterizedTest
    void disabledByProperty(final String beanName) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withPropertyValues("spring.cache.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(beanName));
    }
}
