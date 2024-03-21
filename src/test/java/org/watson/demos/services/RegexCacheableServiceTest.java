package org.watson.demos.services;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.watson.demos.configurations.CachingConfiguration;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureCache(cacheProvider = CacheType.SIMPLE)
@SpringBootTest(classes = RegexCacheableService.class, properties = {
        "spring.cache.cache-names=" + RegexCacheableService.CACHE_NAME,
})
class RegexCacheableServiceTest {
    private final Collection<String> prefixes = List.of("prefix/", "another-prefix/", "/", "");
    private final Collection<String> suffixes = List.of("/suffix", "/another-suffix", "/", "");

    @ContextConfiguration(classes = CachingConfiguration.class)
    @TestPropertySource(properties = {
            "spring.cache.enabled=true",
    })
    @Nested
    class RegexCacheableServiceCachingEnabledTest {
        @Resource
        private RegexCacheableService service;

        @Test
        void getEntry_reduced_whenCacheEnabled() {
            final int entryCount = 8;

            assertThat(getEntries(entryCount, service::getEntry)).hasSize(entryCount);
        }
    }

    @ContextConfiguration(classes = CachingConfiguration.class)
    @TestPropertySource(properties = {
            "spring.cache.enabled=false",
    })
    @Nested
    class RegexCacheableServiceCachingDisabledTest {
        @Resource
        private RegexCacheableService service;

        @Test
        void getEntry_notReduced_whenCacheDisabled() {
            final int entryCount = 3;
            final int expectedSize = entryCount * prefixes.size() * suffixes.size();

            assertThat(getEntries(entryCount, service::getEntry)).hasSize(expectedSize);
        }
    }

    private <T> Collection<T> getEntries(final int entryCount, final Function<String, T> cacheLookup) {
        return IntStream.range(0, entryCount).boxed()
                .map(String::valueOf)
                .flatMap(id -> prefixes.stream()
                        .map(prefix -> prefix + id)
                        .flatMap(prefixed -> suffixes.stream()
                                .map(suffix -> prefixed + suffix)))
                .map(cacheLookup)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }
}
