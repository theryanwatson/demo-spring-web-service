package org.watson.demos.services;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class TraceService {
    private final Optional<Tracer> tracer;

    public Optional<String> getCurrentTraceId() {
        return tracer.map(Tracer::currentSpan)
                .map(Span::context)
                .map(TraceContext::traceId)
                .filter(ObjectUtils::isNotEmpty);
    }
}
