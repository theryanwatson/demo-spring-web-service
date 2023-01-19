package org.watson.demos.configurations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.watson.demos.services.TraceService;

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
                .withBean(TraceService.class)
                .run(context -> assertThat(context).hasBean(beanName));
    }

    @ValueSource(strings = {"traceIdHeaderResponseFilter", "requestLoggingRegistrationBean"})
    @ParameterizedTest
    void filterBeansDisabledByProperty(final String beanName) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withPropertyValues("server.response.trace.header.enabled=false", "server.request.logging.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(beanName));
    }

    @ValueSource(strings = "traceIdHeaderResponseFilter")
    @ParameterizedTest
    void filterBeansDisabledByTracingDisabled(final String beanName) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withPropertyValues("management.trace.http.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(beanName));
    }

    @NullAndEmptySource
    @ValueSource(strings = {"/some-path", "/this/is/a/nested/path"})
    @ParameterizedTest
    void urlPatternMatchesWildcardPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.ofNullable(path), Optional.empty());
        assertUrlPatternsContains(actual, (path == null ? "" : path) + "/*");
    }

    @ValueSource(strings = {"spring.data.rest.base-path", "server.request.logging.path.root"})
    @ParameterizedTest
    void propertiesSetWildcardPaths(final String property) {
        final String path = "/some-path";
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties", "server.response.trace.header.enabled=false")
                .withPropertyValues(property + "=" + path)
                .run(context -> {
                    final FilterRegistrationBean<?> actual = context.getBean(FilterRegistrationBean.class);
                    assertUrlPatternsContains(actual, path + "/*");
                });
    }

    @ValueSource(strings = {"/", "/some-path/", "/this/is/a/nested/path/"})
    @ParameterizedTest
    void urlPatternMatchesTrailingSlashWildcardPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.of(path), Optional.empty());
        assertUrlPatternsContains(actual, path + "*");
    }

    @ValueSource(strings = {"some-path", "this/is/a/nested/path"})
    @ParameterizedTest
    void urlPatternMatchesMissingLeadingSlashWildcardPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.of(path), Optional.empty());
        assertUrlPatternsContains(actual, "/" + path + "/*");
    }

    @ValueSource(strings = {" ", " /some-path ", " /this/is/a/nested/path "})
    @ParameterizedTest
    void urlPatternMatchesTrimmedPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.of(path), Optional.empty());
        assertUrlPatternsContains(actual, path.trim() + "/*");
    }

    @Test
    void orderMatchesFilterOrder() {
        final int expected = 8675309;
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.of("/any-path"), Optional.of(expected));
        assertThat(actual).isNotNull();
        assertThat(actual.getOrder()).isEqualTo(expected);
    }

    private static void assertUrlPatternsContains(final FilterRegistrationBean<?> actual, final String path) {
        assertThat(actual).isNotNull();
        assertThat(actual.getUrlPatterns()).containsExactly(path);
    }
}
