package org.watson.demos.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.brave.bridge.BraveTracer;
import org.watson.demos.filters.RequestLoggingFilter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FilterConfigurationTest {

    private final FilterConfiguration configuration = new FilterConfiguration();
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(FilterConfiguration.class);

    @ValueSource(strings = {"traceIdHeaderResponseFilter", "requestLoggingRegistrationBean"})
    @ParameterizedTest
    void filterBeansEnabledByDefault(final String beanName) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withBean(Tracer.class, () -> new BraveTracer(null, null, null))
                .run(context -> assertThat(context).hasBean(beanName));
    }

    @ValueSource(strings = {"traceIdHeaderResponseFilter", "requestLoggingRegistrationBean"})
    @ParameterizedTest
    void filterBeansDisabledByProperty(final String beanName) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withPropertyValues("server.response.trace.header.enabled=false", "server.request.logging.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(beanName));
    }

    @NullAndEmptySource
    @ValueSource(strings = {"/some-path", "/this/is/a/nested/path"})
    @ParameterizedTest
    void urlPatternMatchesWildcardPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.ofNullable(path));
        assertUrlPatternsContains(actual, (path == null ? "" : path) + "/*");
    }

    @ValueSource(strings = {"/", "/some-path/", "/this/is/a/nested/path/"})
    @ParameterizedTest
    void urlPatternMatchesTrailingSlashWildcardPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.of(path));
        assertUrlPatternsContains(actual, path + "*");
    }

    @ValueSource(strings = {"some-path", "this/is/a/nested/path"})
    @ParameterizedTest
    void urlPatternMatchesMissingLeadingSlashWildcardPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.of(path));
        assertUrlPatternsContains(actual, "/" + path + "/*");
    }

    @ValueSource(strings = {" ", " /some-path ", " /this/is/a/nested/path "})
    @ParameterizedTest
    void urlPatternMatchesTrimmedPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.of(path));
        assertUrlPatternsContains(actual, path.trim() + "/*");
    }

    @Test
    void orderMatchesFilterOrder() {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.of("/any-path"));
        assertThat(actual).isNotNull();
        assertThat(actual.getOrder()).isEqualTo(new RequestLoggingFilter().getOrder());
    }

    private static void assertUrlPatternsContains(final FilterRegistrationBean<?> actual, final String path) {
        assertThat(actual).isNotNull();
        assertThat(actual.getUrlPatterns()).containsExactly(path);
    }
}
