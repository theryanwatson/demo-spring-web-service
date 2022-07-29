package org.watson.demos.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.core.Ordered;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * {@link OrderedFilter} that logs request and response details plus timing.
 * <p>
 * Designed to be set to {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE} and start timing at the very beginning of the request chain, then log at the very end of the request chain.
 * Use a {@link org.springframework.boot.web.servlet.FilterRegistrationBean} to configure which endpoints will use the {@link RequestLoggingFilter}:
 * <pre>{@code    FilterRegistrationBean<T> registrationBean = new FilterRegistrationBean<>(filter);
 *    registrationBean.addUrlPatterns("/path/*");
 *    registrationBean.setOrder(filter.getOrder());
 * }</pre>
 * Logger uses SLF4J. Log info example: <blockquote>GET=/path/sub-path?queryParam=0&size=999;client=192.168.0.1;status=200;duration=30</blockquote>
 */
@Slf4j
public class RequestLoggingFilter implements OrderedFilter {

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        final long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            if (servletRequest instanceof HttpServletRequest) {
                final HttpServletRequest request = (HttpServletRequest) servletRequest;
                log.info("{}={}{};client={};status={};duration={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getQueryString() != null ? "?" + request.getQueryString() : "",
                        request.getRemoteAddr(),
                        servletResponse instanceof HttpServletResponse ? ((HttpServletResponse) servletResponse).getStatus() : null,
                        System.currentTimeMillis() - start);
            } else {
                log.warn("Unknown request: {}", servletRequest != null ? servletRequest.getClass() : null);
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
