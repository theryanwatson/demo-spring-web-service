package org.watson.demos.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync(proxyTargetClass = true)
@Configuration(proxyBeanMethods = false)
public class AsyncConfiguration {}
