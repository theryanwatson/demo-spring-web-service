package org.watson.demos.services;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.watson.demos.models.CacheMode;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

@Timed(TimeServiceDirect.SERVICE_NAME)
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeServiceDirect {
    static final String SERVICE_NAME = "service.time.direct";

    private static final Map<CacheMode, BiFunction<TimeServiceDirect, Optional<String>, Flux<Object>>> routes = Map.of(
            CacheMode.cache, TimeServiceDirect::getCacheable,
            CacheMode.evict, TimeServiceDirect::getCacheEvict,
            CacheMode.none, TimeServiceDirect::get,
            CacheMode.put, TimeServiceDirect::getCachePut
    );

    private final WebClient worldtimeClient;
    private final Cache serviceTimeResponses;

    /**
     * Returns the route from the {@link CacheMode} to a specific function. Returns null if unknown.
     *
     * @param mode The {@link CacheMode} linked to a function
     * @return Null or selected function
     */
    public BiFunction<TimeServiceDirect, Optional<String>, Flux<Object>> route(final CacheMode mode) {
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

    public Flux<Object> getCacheable(final Optional<String> path) {
        return serviceTimeResponses.get(path, () -> get(path));
    }

    public Flux<Object> getCacheEvict(final Optional<String> path) {
        serviceTimeResponses.evict(path);
        return get(path);
    }

    public Flux<Object> getCachePut(final Optional<String> path) {
        final Flux<Object> flux = get(path);
        serviceTimeResponses.put(path, flux);
        return flux;
    }

    @ConditionalOnProperty("spring.cache.clear.enabled")
    @Scheduled(fixedRateString = "${spring.cache.clear.fixed.rate:PT10M}")
    public void clearAllCacheEntries() {
        log.debug("Clear cache. name={}", serviceTimeResponses.getName());
        serviceTimeResponses.invalidate();
    }
}
