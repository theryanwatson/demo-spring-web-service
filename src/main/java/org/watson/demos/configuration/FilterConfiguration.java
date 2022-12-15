package org.watson.demos.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.watson.demos.filters.RequestLoggingFilter;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Slf4j
@ConditionalOnWebApplication
@Configuration(proxyBeanMethods = false)
class FilterConfiguration {

    @Bean
    @ConditionalOnProperty(value = "server.request.logging.enabled", matchIfMissing = true)
    FilterRegistrationBean<RequestLoggingFilter> requestLoggingRegistrationBean(@Value("${server.rest.path.root}") final Optional<String> rootPath) {
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

    @Bean
    @ConditionalOnBean(Tracer.class)
    @ConditionalOnProperty(value = "server.response.trace.header.enabled", matchIfMissing = true)
    Filter traceIdHeaderResponseFilter(final Optional<Tracer> tracer, @Value("${server.response.trace.header.name:Trace-Id}") final String traceHeaderName) {
        return (request, response, chain) -> {
            tracer.map(Tracer::currentSpan)
                    .map(Span::context)
                    .map(TraceContext::traceId)
                    .ifPresent(traceId -> ((HttpServletResponse) response).addHeader(traceHeaderName, traceId));
            chain.doFilter(request, response);
        };
    }
}
