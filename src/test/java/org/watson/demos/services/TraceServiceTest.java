package org.watson.demos.services;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TraceService.class)
class TraceServiceTest {
    // TODO How do test a real span? Which AutoConfigurations to import?

    @Mock
    private Span span;
    @Mock
    private TraceContext traceContext;
    @MockBean
    private Tracer tracer;
    @Resource
    private TraceService service;

    @Test
    void getCurrentTraceId_returnsCurrent() {
        final String expected = "some-trace-id";

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(expected);

        assertThat(service.getCurrentTraceId()).contains(expected);
    }

    @Test
    void getCurrentTraceId_handlesNulls() {
        when(tracer.currentSpan()).thenReturn(null);
        assertThat(service.getCurrentTraceId()).isNotPresent();

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(null);
        assertThat(service.getCurrentTraceId()).isNotPresent();

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn(null);
        assertThat(service.getCurrentTraceId()).isNotPresent();

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("");
        assertThat(service.getCurrentTraceId()).isNotPresent();
    }
}
