package org.watson.demos.configurations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class CachingConfiguration {
    private static final String ENABLED_PLACEHOLDER = "spring.cache.enabled";

    @ConditionalOnProperty(value = ENABLED_PLACEHOLDER, havingValue = "true", matchIfMissing = true)
    @EnableCaching(proxyTargetClass = true)
    @Configuration(proxyBeanMethods = false)
    public static class CachingEnabledConfiguration {
        public CachingEnabledConfiguration() {
            log.info("Caching enabled.");
        }
    }

    @ConditionalOnProperty(value = ENABLED_PLACEHOLDER, havingValue = "false")
    @Configuration(proxyBeanMethods = false)
    public static class CachingDisabledConfiguration {
        public CachingDisabledConfiguration() {
            log.info("Caching disabled.");
        }

        @ConditionalOnMissingBean
        @Bean
        protected CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }

    @Bean
    public Cache serviceTimeResponses(final CacheManager cacheManager) {
        return cacheManager.getCache("service.time.direct.responses");
    }

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
