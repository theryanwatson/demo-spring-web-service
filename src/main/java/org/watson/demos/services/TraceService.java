package org.watson.demos.services;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Timed("service.trace")
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
