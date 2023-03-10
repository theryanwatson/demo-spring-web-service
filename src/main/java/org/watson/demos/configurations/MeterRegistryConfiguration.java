package org.watson.demos.configurations;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnBean(MeterRegistry.class)
@Configuration(proxyBeanMethods = false)
public class MeterRegistryConfiguration {

    @ConditionalOnBean(BuildProperties.class)
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags(final BuildProperties buildProperties) {
        return (registry) -> registry.config().commonTags("application.version", buildProperties.getVersion());
    }
}
