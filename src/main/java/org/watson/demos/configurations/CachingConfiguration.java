package org.watson.demos.configurations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@ConditionalOnProperty(value = "spring.cache.enabled", matchIfMissing = true)
@EnableCaching(proxyTargetClass = true)
@Configuration(proxyBeanMethods = false)
public class CachingConfiguration {

    /**
     * {@link CacheResolver} that allows {@link org.springframework.cache.annotation.Cacheable} and related caching
     * annotations to use property placeholders for cache names.
     * <pre>{@code @Cacheable(cacheNames = "${some.cache.name.property}", cacheResolver = "placeholderNameCacheResolver") }</pre>
     */
    @ConditionalOnProperty("spring.cache.placeholder.name.cache.resolver.enabled")
    @Bean
    CacheResolver placeholderNameCacheResolver(final CacheManager cacheManager, final PropertyResolver propertyResolver) {
        return new SimpleCacheResolver(cacheManager) {
            @Override
            protected Collection<String> getCacheNames(@NonNull CacheOperationInvocationContext<?> context) {
                return Optional.of(context)
                        .map(super::getCacheNames)
                        .map(names -> names.stream()
                                .map(propertyResolver::resolveRequiredPlaceholders)
                                .collect(Collectors.toUnmodifiableList()))
                        .orElseGet(List::of);
            }
        };
    }
}
