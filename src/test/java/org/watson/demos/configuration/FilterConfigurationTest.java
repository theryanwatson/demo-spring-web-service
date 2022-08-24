package org.watson.demos.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.watson.demos.filters.RequestLoggingFilter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class FilterConfigurationTest {

    private final FilterConfiguration configuration = new FilterConfiguration();

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"/path", "/this/is/a/nested/path"})
    void urlPatternMatchesWildcardPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(path);
        assertThat(actual, notNullValue());
        assertThat(actual.getUrlPatterns(), hasSize(1));
        assertThat(actual.getUrlPatterns(), contains(path + "/*"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/path/", "/this/is/a/nested/path/"})
    void urlPatternMatchesTrailingSlashWildcardPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(path);
        assertThat(actual, notNullValue());
        assertThat(actual.getUrlPatterns(), hasSize(1));
        assertThat(actual.getUrlPatterns(), contains(path + "*"));
    }

    @Test
    void orderMatchesFilterOrder() {
        final FilterRegistrationBean<RequestLoggingFilter> actual = configuration.requestLoggingRegistrationBean("/any");
        assertThat(actual, notNullValue());
        assertThat(actual.getOrder(), is(new RequestLoggingFilter().getOrder()));
    }
}
