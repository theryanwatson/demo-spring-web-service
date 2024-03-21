package org.watson.demos.services;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Timed("service.regex.cacheable")
@Slf4j
@Service(RegexCacheableService.BEAN_NAME)
@RequiredArgsConstructor
public class RegexCacheableService {
    static final String CACHE_NAME = "regex-cacheable-service-entries";
    static final String BEAN_NAME = "regexCacheableService";
    private final Pattern idGenerator = Pattern.compile("^[^/]*/?([0-9]+)/?.*");

    @Cacheable(value = CACHE_NAME, key = "@" + BEAN_NAME + ".generateKey(#id)")
    public Entry getEntry(final String id) {
        return lookupEntry(generateKey(id));
    }

    public String generateKey(final String id) {
        final Matcher matcher = idGenerator.matcher(id);
        return matcher.matches() ? matcher.group(1) : id;
    }

    private Entry lookupEntry(String id) {
        log.info("Entry lookup. id={}", id);
        return new Entry(Integer.parseInt(id), UUID.randomUUID(), "entries/" + id);
    }

    @lombok.Value
    public static class Entry implements Serializable {
        int id;
        UUID uuid;
        String path;
    }
}
