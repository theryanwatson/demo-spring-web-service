package org.watson.demos.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.watson.demos.models.Identifiable;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adds id location data to the response header.<p/>
 * Assumes that if content created by a POST to {@code /some-content}, then the GET call will be {@code /some-content/{id}}.
 * Configurable with optional {@code identifiable.location.path.prefix=}, defaults to "/".<p/>
 * Reads optional properties to calculate the max number of ids that can be added to the header;
 * {@code server.max-http-header-size=} defaults to "8KB" and optional {@code identifiable.location.id.character.length=} defaults to 36 (UUID length).<p/>
 * Supports {@link RestController} methods that return a {@link Collection} of supported types with a {@link ResponseStatus} of {@link HttpStatus#CREATED}.
 * <ul>
 * <li>{@value HttpHeaders#CONTENT_LOCATION} header is set with a <strong>relative URI</strong> when multiple items are returned. (See <a href=https://www.rfc-editor.org/rfc/rfc2616#section-14.14>RFC 2616, Section 14.14</a>)</li>
 * <li>{@value HttpHeaders#LOCATION} header is set with an <strong>absolute URI</strong> when a single item is returned. (See <a href=https://www.rfc-editor.org/rfc/rfc2616#section-14.30>RFC 2616, Section 14.30</a>)</li>
 * </ul>
 *
 * @see <a href=https://www.rfc-editor.org/rfc/rfc2616#section-14.14>RFC 2616, Section 14.14</a>
 * @see <a href=https://www.rfc-editor.org/rfc/rfc2616#section-14.30>RFC 2616, Section 14.30</a>
 */
@Slf4j
@ConditionalOnWebApplication
@ControllerAdvice(annotations = RestController.class)
public class LocationResponseBodyAdvice implements ResponseBodyAdvice<Collection<? extends Identifiable<?>>> {

    private final String prefix;
    private final long maxHeaderCount;

    public LocationResponseBodyAdvice(@Value("${server.max-http-header-size:8KB}") final DataSize maxHeaderSize,
                                      @Value("${identifiable.location.header-reserve-size:3KB}") final DataSize headerReserveSize,
                                      @Value("${identifiable.location.path.prefix:/}") final String prefix,
                                      @Value("${identifiable.location.id.character.length:36}") final int idCharacterLength) {
        this.prefix = prefix;
        this.maxHeaderCount = calculateMaxHeaderCount(maxHeaderSize.toBytes() - headerReserveSize.toBytes(), prefix.length() + idCharacterLength);
    }

    @Override
    public boolean supports(@NonNull final MethodParameter returnType, @Nullable final Class<? extends HttpMessageConverter<?>> ignored) {
        return supportsResponseStatus(returnType) && supportsClass(returnType);
    }

    @Override
    public Collection<? extends Identifiable<?>> beforeBodyWrite(@Nullable final Collection<? extends Identifiable<?>> body, @Nullable final MethodParameter ignored1, @Nullable final MediaType ignored2, @Nullable final Class<? extends HttpMessageConverter<?>> ignored3,
                                                                 @NonNull final ServerHttpRequest request, @NonNull final ServerHttpResponse response) {
        if (body != null) {
            final List<String> relativePaths = body.stream()
                    .map(Identifiable::getId)
                    .map(this::toRelativeLocationPath)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableList());

            if (relativePaths.size() == 1) {
                log.debug("Setting {} header.", HttpHeaders.LOCATION);
                relativePaths.stream()
                        .findFirst()
                        .map(relative -> toAbsoluteLocationUri(relative, request.getURI()))
                        .ifPresent(response.getHeaders()::setLocation);
            } else {
                log.debug("Setting {} header.", HttpHeaders.CONTENT_LOCATION);
                relativePaths.stream()
                        .limit(maxHeaderCount)
                        .forEach(relative -> response.getHeaders().add(HttpHeaders.CONTENT_LOCATION, relative));
            }
        }
        return body;
    }

    private URI toAbsoluteLocationUri(final String relativePath, @NonNull final URI requestUri) {
        return Optional.ofNullable(relativePath)
                .map(relative -> requestUri.getPath() + relative)
                .map(requestUri::resolve)
                .orElse(null);
    }

    private String toRelativeLocationPath(@Nullable final Object id) {
        return id == null ? null : prefix + id;
    }

    private boolean supportsResponseStatus(@NonNull final MethodParameter returnType) {
        return Optional.of(returnType)
                .map(t -> t.getMethodAnnotation(ResponseStatus.class))
                .map(ResponseStatus::value)
                .filter(HttpStatus.CREATED::equals)
                .isPresent();
    }

    private boolean supportsClass(@NonNull final MethodParameter returnType) {
        return Collection.class.isAssignableFrom(returnType.getParameterType()) &&
                Optional.of(returnType)
                        .map(ResolvableType::forMethodParameter)
                        .map(ResolvableType::getGeneric)
                        .map(ResolvableType::toClass)
                        .filter(Identifiable.class::isAssignableFrom)
                        .isPresent();
    }

    private long calculateMaxHeaderCount(final long availableBytes, final long bytesPerIdentifier) {
        return availableBytes < bytesPerIdentifier || bytesPerIdentifier < 1 ? 0 : Math.min(availableBytes / bytesPerIdentifier, Integer.MAX_VALUE);
    }
}
