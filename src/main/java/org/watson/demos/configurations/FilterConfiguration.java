package org.watson.demos.configurations;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.watson.demos.filters.RequestLoggingFilter;
import org.watson.demos.services.TraceService;

import java.util.Collection;
import java.util.Optional;

@Slf4j
@ConditionalOnWebApplication
@Configuration(proxyBeanMethods = false)
class FilterConfiguration {

    @Bean
    @ConditionalOnProperty(value = "server.request.logging.enabled", matchIfMissing = true)
    FilterRegistrationBean<RequestLoggingFilter> requestLoggingRegistrationBean(@Value("${server.request.logging.order:#{null}}") final Optional<Integer> order,
                                                                                @Value("${server.request.logging.path.root:#{'${spring.graphql.path:/graphql},${spring.data.rest.base-path:/}'.split(',')}}") final Collection<String> rootPaths
    ) {
        final RequestLoggingFilter filter = new RequestLoggingFilter();
        final String[] urlPatterns = rootPaths.stream()
                .filter(StringUtils::isNotEmpty)
                .map(String::trim)
                .map(p -> p.startsWith("/") ? p : "/" + p)
                .map(p -> p.endsWith("/") ? p : p + "/")
                .map(p -> p + "*")
                .toArray(String[]::new);
        return new FilterRegistrationBean<>(filter) {{
            addUrlPatterns(urlPatterns);
            setOrder(order.orElse(Ordered.HIGHEST_PRECEDENCE + 2));
        }};
    }

    @Bean
    @ConditionalOnEnabledTracing
    @ConditionalOnProperty(value = "server.response.trace.header.enabled", matchIfMissing = true)
    Filter traceIdHeaderResponseFilter(final TraceService traceService, @Value("${server.response.trace.header.name:Trace-Id}") final String traceHeaderName) {
        return (request, response, chain) -> {
            traceService.getCurrentTraceId()
                    .ifPresent(traceId -> ((HttpServletResponse) response).addHeader(traceHeaderName, traceId));
            chain.doFilter(request, response);
        };
    }
}
