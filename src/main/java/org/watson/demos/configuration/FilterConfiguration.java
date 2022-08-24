package org.watson.demos.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.watson.demos.filters.RequestLoggingFilter;

@ConditionalOnWebApplication
@Configuration(proxyBeanMethods = false)
public class FilterConfiguration {

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingRegistrationBean(@Value("${server.rest.path.root:}") final String rootPath) {
        final RequestLoggingFilter filter = new RequestLoggingFilter();
        return new FilterRegistrationBean<>(filter) {{
            addUrlPatterns((rootPath + "/*").replace("//", "/"));
            setOrder(filter.getOrder());
        }};
    }
}
