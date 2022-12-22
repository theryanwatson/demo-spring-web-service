package org.watson.demos.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration(proxyBeanMethods = false)
public class EventRepositoryConfiguration {
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "management.auditevents", name = {"enabled", "repository.enabled"}, matchIfMissing = true)
    @Bean
    public AuditEventRepository auditEventRepository(@Value("${management.auditevents.repository.event.capacity:#{null}}") final Optional<Integer> capacity) {
        return capacity.map(InMemoryAuditEventRepository::new)
                .orElseGet(InMemoryAuditEventRepository::new);
    }

    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "management.httpexchanges.recording", name = {"enabled", "repository.enabled"}, matchIfMissing = true)
    @ConditionalOnWebApplication
    @Bean
    public HttpExchangeRepository httpExchangeRepository(@Value("${management.httpexchanges.recording.repository.event.capacity:#{null}}") final Optional<Integer> capacity) {
        return new InMemoryHttpExchangeRepository() {{
            capacity.ifPresent(this::setCapacity);
        }};
    }
}
