package org.watson.demos.configurations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@ConditionalOnProperty(value = "spring.task.async.enabled", matchIfMissing = true)
@EnableAsync(proxyTargetClass = true)
@Configuration(proxyBeanMethods = false)
public class AsyncConfiguration {}
