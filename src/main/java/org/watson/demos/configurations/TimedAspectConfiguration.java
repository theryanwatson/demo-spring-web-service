package org.watson.demos.configurations;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(value = "spring.timed.aspect.enabled", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class TimedAspectConfiguration {
    @Bean
    public TimedAspect timedAspect(final MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
