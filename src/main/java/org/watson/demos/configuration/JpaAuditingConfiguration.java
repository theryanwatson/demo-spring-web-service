package org.watson.demos.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@ConditionalOnProperty(value = "spring.jpa.auditing.enabled", matchIfMissing = true)
@EnableJpaAuditing
@Configuration(proxyBeanMethods = false)
public class JpaAuditingConfiguration {}
