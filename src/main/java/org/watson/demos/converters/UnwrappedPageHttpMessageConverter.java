package org.watson.demos.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.system.JavaVersion;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.watson.demos.advice.UnwrappedPageResponseBodyAdvice;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Write-only converter. When enabled, unwraps {@link Page} json response body objects from {@link RestController} classes,
 * returning only the {@link Page#get()} content, instead of the wrapped {@link Page} response. Used in conjunction with
 * {@link UnwrappedPageResponseBodyAdvice}, which writes all pertinent page data to the response headers.
 * <p></p><strong>Enable the feature with the Spring property:</strong><blockquote>server.response.unwrap.page=true</blockquote>
 *
 * @see UnwrappedPageResponseBodyAdvice
 */
@ConditionalOnWebApplication
@ConditionalOnJava(JavaVersion.SEVENTEEN)
@ConditionalOnProperty("server.response.unwrap.page")
@Component
public class UnwrappedPageHttpMessageConverter extends AbstractHttpMessageConverter<Page<?>> {
    private final ObjectMapper objectMapper;

    public UnwrappedPageHttpMessageConverter(final ObjectMapper objectMapper) {
        super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
        this.objectMapper = objectMapper
                .registerModule(new Jdk8Module());
    }

    @Override
    protected boolean supports(@NonNull final Class<?> clazz) {
        return Page.class.isAssignableFrom(clazz);
    }

    @Override
    protected void writeInternal(final Page<?> page, final HttpOutputMessage outputMessage) throws IOException {
        final JsonGenerator generator = objectMapper.getFactory()
                .createGenerator(outputMessage.getBody());
        objectMapper.writerFor(Stream.class)
                .writeValue(generator, page.get());
        generator.flush();
    }

    @Override
    public boolean canRead(@Nullable final Class<?> ignored1, final MediaType ignored2) {
        return false;
    }

    @NonNull
    @Override
    protected Page<?> readInternal(@Nullable final Class<? extends Page<?>> ignored, @NonNull final HttpInputMessage inputMessage) {
        throw new HttpMessageNotReadableException("Read not supported.", inputMessage);
    }
}
