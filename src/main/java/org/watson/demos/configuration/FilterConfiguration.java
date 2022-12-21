package org.watson.demos.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.watson.demos.filters.RequestLoggingFilter;
import org.watson.demos.services.TraceService;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Slf4j
@ConditionalOnWebApplication
@Configuration(proxyBeanMethods = false)
class FilterConfiguration {

    @Bean
    @ConditionalOnProperty(value = "server.request.logging.enabled", matchIfMissing = true)
    FilterRegistrationBean<RequestLoggingFilter> requestLoggingRegistrationBean(@Value("${server.request.logging.path.root:${spring.data.rest.base-path:#{null}}}") final Optional<String> rootPath,
                                                                                @Value("${server.request.logging.order:#{null}}") final Optional<Integer> order) {
        final RequestLoggingFilter filter = new RequestLoggingFilter();
        final String urlPattern = rootPath
                .map(String::trim)
                .map(p -> p.startsWith("/") ? p : "/" + p)
                .map(p -> p.endsWith("/") ? p : p + "/")
                .orElse("/");
        return new FilterRegistrationBean<>(filter) {{
            addUrlPatterns(urlPattern + "*");
            setOrder(order.orElse(Ordered.HIGHEST_PRECEDENCE + 2));
        }};
    }

    @Bean
    @ConditionalOnProperty(value = {"server.response.trace.header.enabled", "management.trace.http.enabled"}, matchIfMissing = true)
    Filter traceIdHeaderResponseFilter(final TraceService traceService, @Value("${server.response.trace.header.name:Trace-Id}") final String traceHeaderName) {
        return (request, response, chain) -> {
            traceService.getCurrentTraceId()
                    .ifPresent(traceId -> ((HttpServletResponse) response).addHeader(traceHeaderName, traceId));
            chain.doFilter(request, response);
        };
    }
}
