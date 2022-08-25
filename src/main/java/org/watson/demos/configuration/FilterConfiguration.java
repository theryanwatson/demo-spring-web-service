package org.watson.demos.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.watson.demos.filters.RequestLoggingFilter;

import java.util.Optional;

@ConditionalOnWebApplication
@Configuration(proxyBeanMethods = false)
public class FilterConfiguration {

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingRegistrationBean(@Value("${server.rest.path.root}") final Optional<String> rootPath) {
        final RequestLoggingFilter filter = new RequestLoggingFilter();
        final String urlPattern = rootPath
                .map(String::trim)
                .map(p -> p.startsWith("/") ? p : "/" + p)
                .map(p -> p.endsWith("/") ? p : p + "/")
                .orElse("/");
        return new FilterRegistrationBean<>(filter) {{
            addUrlPatterns(urlPattern + "*");
            setOrder(filter.getOrder());
        }};
    }
}
