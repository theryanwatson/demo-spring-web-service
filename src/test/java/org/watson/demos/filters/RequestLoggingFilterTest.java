package org.watson.demos.filters;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestLoggingFilterTest {
    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Mock
    private HttpServletRequest mockRequest;
    @Mock
    private HttpServletResponse mockResponse;
    @Mock
    private FilterChain mockChain;

    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    @BeforeEach
    void before() {
        listAppender.start();
        logger.addAppender(listAppender);

        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getRequestURI()).thenReturn("/things/path");
        when(mockRequest.getQueryString()).thenReturn("x=y&a=b");
        when(mockRequest.getRemoteAddr()).thenReturn("10.10.10.10");
        when(mockResponse.getStatus()).thenReturn(200);
    }

    @AfterEach
    void after() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void doFilter_CreatesInfoLogWithExpectedValues() throws IOException, ServletException {
        filter.doFilter(mockRequest, mockResponse, mockChain);

        assertLogLineMatches("GET=/things/path?x=y&a=b;client=10.10.10.10;status=200;duration=", mockRequest, mockResponse);
    }

    @Test
    void doFilter_HandlesNullQueryString() throws IOException, ServletException {
        when(mockRequest.getQueryString()).thenReturn(null);

        filter.doFilter(mockRequest, mockResponse, mockChain);

        assertLogLineMatches("GET=/things/path;client=10.10.10.10;status=200;duration=", mockRequest, mockResponse);
    }

    @Test
    void doFilter_LogsEvenWhenChainThrows() throws IOException, ServletException {
        doThrow(new ServletException()).when(mockChain).doFilter(any(), any());

        try {
            filter.doFilter(mockRequest, mockResponse, mockChain);
            fail("Should have thrown exception");
        } catch (ServletException ignored) {
        }

        assertLogLineMatches("GET=/things/path?x=y&a=b;client=10.10.10.10;status=200;duration=", mockRequest, mockResponse);
    }

    @Test
    void doFilter_LogsWarnWhenUnknownRequestClass() throws IOException, ServletException {
        final ServletRequest wrongRequestType = mock(ServletRequest.class);
        filter.doFilter(wrongRequestType, mockResponse, mockChain);

        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
        verify(mockChain).doFilter(wrongRequestType, mockResponse);
    }

    @Test
    void doFilter_LogsWhenUnknownResponseClass() throws IOException, ServletException {
        final ServletResponse wrongResponseType = mock(ServletResponse.class);
        filter.doFilter(mockRequest, wrongResponseType, mockChain);

        assertLogLineMatches("GET=/things/path?x=y&a=b;client=10.10.10.10;status=null;duration=", mockRequest, wrongResponseType);
    }

    @Test
    void doFilter_CalculatesDuration() throws IOException, ServletException {
        final int expectedDuration = 14;

        doAnswer(invocation -> {
            Thread.sleep(expectedDuration);
            return null;
        }).when(mockChain).doFilter(mockRequest, mockResponse);

        filter.doFilter(mockRequest, mockResponse, mockChain);

        assertThat(listAppender.list).hasSize(1);
        final String message = listAppender.list.get(0).getFormattedMessage();
        final int duration = Integer.parseInt(message.substring(message.lastIndexOf('=') + 1));

        assertThat(duration).isBetween(expectedDuration, expectedDuration + 20);
        verify(mockChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void getOrder_isSet() {
        assertThat(filter.getOrder()).isNotZero();
    }

    private void assertLogLineMatches(final String logLinePrefix, final HttpServletRequest request, final ServletResponse response) throws IOException, ServletException {
        assertThat(listAppender.list).hasSize(1);
        assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(listAppender.list.get(0).getFormattedMessage()).startsWith(logLinePrefix);

        verify(mockChain).doFilter(request, response);
    }
}
