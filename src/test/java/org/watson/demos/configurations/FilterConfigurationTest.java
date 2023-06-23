package org.watson.demos.configurations;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.watson.demos.services.TraceService;

import java.util.Collection;
import java.util.List;
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
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.empty(), path == null ? List.of() : List.of(path));
        assertUrlPatternsContains(actual, StringUtils.isEmpty(path) ? List.of() : List.of(path + "/*"));
    }

    @ValueSource(strings = {"spring.data.rest.base-path", "spring.graphql.path", "server.request.logging.path.root"})
    @ParameterizedTest
    void propertiesSetWildcardPaths(final String property) {
        final String path = "/some-path";
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties", "server.response.trace.header.enabled=false")
                .withPropertyValues(property + "=" + path)
                .run(context -> {
                    final FilterRegistrationBean<?> actual = context.getBean(FilterRegistrationBean.class);
                    assertUrlPatternsContains(actual, List.of(path + "/*"));
                });
    }

    @Test
    void propertiesPreferFilterPathParameter() {
        final String path = "/some-path";
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties", "server.response.trace.header.enabled=false",
                        "spring.data.rest.base-path=/ignored", "spring.graphql.path=/ignored", "server.request.logging.path.root=" + path)
                .run(context -> {
                    final FilterRegistrationBean<?> actual = context.getBean(FilterRegistrationBean.class);
                    assertUrlPatternsContains(actual, List.of(path + "/*"));
                });
    }

    @Test
    void propertiesFallbackToSpringProperties() {
        final String restPath = "/some-path-1";
        final String graphPath = "/some-path-2";
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties", "server.response.trace.header.enabled=false",
                        "spring.data.rest.base-path=" + restPath, "spring.graphql.path=" + graphPath)
                .run(context -> {
                    final FilterRegistrationBean<?> actual = context.getBean(FilterRegistrationBean.class);
                    assertUrlPatternsContains(actual, List.of(restPath + "/*", graphPath + "/*"));
                });
    }

    @ValueSource(strings = {"/", "/some-path/", "/this/is/a/nested/path/"})
    @ParameterizedTest
    void urlPatternMatchesTrailingSlashWildcardPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.empty(), List.of(path));
        assertUrlPatternsContains(actual, List.of(path + "*"));
    }

    @ValueSource(strings = {"some-path", "this/is/a/nested/path"})
    @ParameterizedTest
    void urlPatternMatchesMissingLeadingSlashWildcardPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.empty(), List.of(path));
        assertUrlPatternsContains(actual, List.of("/" + path + "/*"));
    }

    @ValueSource(strings = {" ", " /some-path ", " /this/is/a/nested/path "})
    @ParameterizedTest
    void urlPatternMatchesTrimmedPaths(final String path) {
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.empty(), List.of(path));
        assertUrlPatternsContains(actual, List.of(path.trim() + "/*"));
    }

    @Test
    void orderMatchesFilterOrder() {
        final int expected = 8675309;
        final FilterRegistrationBean<?> actual = configuration.requestLoggingRegistrationBean(Optional.of(expected), List.of("/any-path"));
        assertThat(actual).isNotNull();
        assertThat(actual.getOrder()).isEqualTo(expected);
    }

    private static void assertUrlPatternsContains(final FilterRegistrationBean<?> actual, final Collection<String> paths) {
        assertThat(actual).isNotNull();
        assertThat(actual.getUrlPatterns()).containsAll(paths);
    }
}
