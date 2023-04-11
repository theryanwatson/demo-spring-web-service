package org.watson.demos.services;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.watson.demos.models.CacheMode;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

@Timed(TimeServiceAnnotation.SERVICE_NAME)
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeServiceAnnotation {
    static final String SERVICE_NAME = "service.time.annotation";
    private static final String CACHE_NAME = SERVICE_NAME + ".responses";

    private static final Map<CacheMode, BiFunction<TimeServiceAnnotation, Optional<String>, Flux<Object>>> routes = Map.of(
            CacheMode.cache, TimeServiceAnnotation::getCacheable,
            CacheMode.evict, TimeServiceAnnotation::getCacheEvict,
            CacheMode.none, TimeServiceAnnotation::get,
            CacheMode.put, TimeServiceAnnotation::getCachePut
    );

    private final WebClient worldtimeClient;

    /**
     * Returns the route from the {@link CacheMode} to a specific function. Returns null if unknown.
     *
     * @param mode The {@link CacheMode} linked to a function
     * @return Null or selected function
     */
    public BiFunction<TimeServiceAnnotation, Optional<String>, Flux<Object>> route(final CacheMode mode) {
        return routes.get(mode);
    }

    /**
     * The {@link Cacheable} annotation will cache the {@link Flux} object, not the {@link Flux} value or remote response.
     * The method {@link Flux#cache()} will cache the results of the call forever, which means as long as this {@link Flux}
     * is in the cache the response will remain the same. But, the whole {@link Flux} object is cached, which might be a
     * waste of memory, probably.
     */
    public Flux<Object> get(final Optional<String> path) {
        log.debug("path={}", path);
        return worldtimeClient.get()
                .uri(path.map(p -> '/' == p.charAt(0) ? p : '/' + p).orElse(""))
                .retrieve()
                .bodyToFlux(Object.class)
                .cache(); // <- See Doc Above
    }

    @Cacheable(CACHE_NAME)
    public Flux<Object> getCacheable(final Optional<String> path) {
        return get(path);
    }

    @CacheEvict(CACHE_NAME)
    public Flux<Object> getCacheEvict(final Optional<String> path) {
        return get(path);
    }

    @CachePut(CACHE_NAME)
    public Flux<Object> getCachePut(final Optional<String> path) {
        return get(path);
    }

    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    @ConditionalOnProperty("spring.cache.clear.enabled")
    @Scheduled(fixedRateString = "${spring.cache.clear.fixed.rate:PT10M}")
    public void clearAllCacheEntries() {
        log.debug("Clear cache. name={}", CACHE_NAME);
    }
}
