package org.watson.demos.configurations;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MeterRegistryConfiguration {

    @ConditionalOnBean(BuildProperties.class)
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags(final BuildProperties buildProperties) {
        return (registry) -> registry.config().commonTags("application.version", buildProperties.getVersion());
    }

    @ConditionalOnProperty(value = "spring.meter.aspect.enabled", matchIfMissing = true)
    @Bean
    public CountedAspect countedAspect(final MeterRegistry registry) {
        return new CountedAspect(registry);
    }

    @ConditionalOnProperty(value = "spring.meter.aspect.enabled", matchIfMissing = true)
    @Bean
    public TimedAspect timedAspect(final MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
