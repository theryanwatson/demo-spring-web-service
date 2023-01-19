package org.watson.demos.configurations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConditionalOnProperty(value = "spring.task.scheduling.enabled", matchIfMissing = true)
@EnableScheduling
@Configuration(proxyBeanMethods = false)
public class SchedulingConfiguration {}
