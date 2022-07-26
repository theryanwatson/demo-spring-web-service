package org.watson.demos.advice;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.function.Predicate.not;
import static org.springframework.boot.autoconfigure.web.ErrorProperties.IncludeAttribute.ALWAYS;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@ConditionalOnWebApplication
@RestControllerAdvice
public class ExceptionHandlerAdvice extends ResponseEntityExceptionHandler {
    private final boolean includeException;
    private final boolean includeMessage;
    private final boolean includeStacktrace;

    @Autowired
    public ExceptionHandlerAdvice(Optional<ServerProperties> serverProperties) {
        this(
                serverProperties
                        .map(ServerProperties::getError)
                        .map(ErrorProperties::isIncludeException)
                        .orElse(false),
                serverProperties
                        .map(ServerProperties::getError)
                        .map(ErrorProperties::getIncludeMessage)
                        .map(ALWAYS::equals)
                        .orElse(false),
                serverProperties
                        .map(ServerProperties::getError)
                        .map(ErrorProperties::getIncludeStacktrace)
                        .map(ALWAYS::equals)
                        .orElse(false)
        );
    }

    /**
     * {@link HttpStatus#CONFLICT} Exception Handler. Add additional Exception "Conflict" classes
     */
    @ExceptionHandler({
            DuplicateKeyException.class,
            SQLIntegrityConstraintViolationException.class,
    })
    public ResponseEntity<Object> handleConflictException(final Throwable exception, final ServletWebRequest request) {
        return handleExceptionInternal(exception, Throwable::getMessage, HttpStatus.CONFLICT, request);
    }

    /**
     * {@link HttpStatus#NOT_FOUND} Exception Handler. Add additional Exception "Not Found" classes
     */
    @ExceptionHandler({
            EntityNotFoundException.class,
    })
    public ResponseEntity<Object> handleNotFoundException(final Throwable throwable, final ServletWebRequest request) {
        return handleExceptionInternal(throwable, Throwable::getMessage, HttpStatus.NOT_FOUND, request);
    }

    /**
     * {@link HttpStatus#BAD_REQUEST} Exception Handler. Add additional Exception "Bad Request" classes
     */
    @ExceptionHandler({
            PropertyReferenceException.class,
    })
    public ResponseEntity<Object> handleBadRequestException(final Throwable throwable, final ServletWebRequest request) {
        return handleExceptionInternal(throwable, Throwable::getMessage, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * {@link ConstraintViolationException} format-specific exception handler
     */
    @ExceptionHandler(javax.validation.ConstraintViolationException.class)
    public ResponseEntity<Object> handleValidationException(final javax.validation.ConstraintViolationException exception, final ServletWebRequest request) {
        return handleExceptionInternal(exception, this::buildValidationExceptionMessage, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Wrapped-Exception Handler. Unwraps Exception and routes to other handlers, base on class.
     */
    @ExceptionHandler({
            InvocationTargetException.class,
            javax.persistence.RollbackException.class,
            TransactionSystemException.class,
    })
    public ResponseEntity<Object> handleWrappedException(final Exception exception, final ServletWebRequest request) {
        Throwable unwrappedCause = unwrapCause(exception);

        if (unwrappedCause instanceof DuplicateKeyException || unwrappedCause instanceof SQLIntegrityConstraintViolationException) {
            return handleConflictException(unwrappedCause, request);
        } else if (unwrappedCause instanceof EntityNotFoundException) {
            return handleNotFoundException(unwrappedCause, request);
        } else if (unwrappedCause instanceof PropertyReferenceException) {
            return handleBadRequestException(unwrappedCause, request);
        } else if (unwrappedCause instanceof javax.validation.ConstraintViolationException) {
            return handleValidationException((javax.validation.ConstraintViolationException) unwrappedCause, request);
        }

        return handleExceptionInternal(unwrappedCause, Throwable::getMessage, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private <T extends Throwable> ResponseEntity<Object> handleExceptionInternal(final T throwable, final Function<T, String> messageSupplier, final HttpStatus status, final ServletWebRequest request) {
        final Object body = formatBody(status, throwable, messageSupplier, request);
        log.info("{}", body);

        if (request == null) {
            return new ResponseEntity<>(body, buildHeaders(), status);
        } else {
            return super.handleExceptionInternal(
                    throwable instanceof Exception ? (Exception) throwable : new RuntimeException(throwable),
                    body, buildHeaders(), status, request);
        }
    }

    private <T extends Throwable> Object formatBody(@NonNull final HttpStatus status,
                                                    @Nullable final T throwable,
                                                    @Nullable final Function<T, String> messageSupplier,
                                                    @Nullable final ServletWebRequest request) {
        return ErrorBody.builder()
                .timestamp(ZonedDateTime.now(UTC)
                        .truncatedTo(MILLIS))
                .status(status.value())
                .error(status.getReasonPhrase())
                .path(Optional.ofNullable(request)
                        .map(ServletRequestAttributes::getRequest)
                        .map(HttpServletRequest::getServletPath)
                        .orElse(null))
                .message(includeMessage && throwable != null && messageSupplier != null ? messageSupplier.apply(throwable) : null)
                .exception(includeException && throwable != null ? throwable.getClass().getName() : null)
                .trace(includeStacktrace && throwable != null ? ExceptionUtils.getStackTrace(throwable) : null)
                .build();
    }

    private String buildValidationExceptionMessage(ConstraintViolationException exception) {
        return Optional.ofNullable(exception.getConstraintViolations())
                .filter(not(Collection::isEmpty))
                .map(violations -> violations.stream()
                        .map(cv -> cv == null ? "null" : String.format("%s %s", cv.getPropertyPath(), cv.getMessage()))
                        .sorted()
                        .collect(Collectors.joining(", ")))
                .orElse(exception.getMessage());
    }

    private HttpHeaders buildHeaders() {
        return new HttpHeaders();
    }

    private Throwable unwrapCause(final Throwable exception) {
        Throwable unwrapped = null;
        if (exception instanceof TransactionSystemException) {
            unwrapped = unwrapCause(((TransactionSystemException) exception).getOriginalException());
        } else if (exception != null) {
            unwrapped = exception.getCause();
        }
        return unwrapped != null ? unwrapped : exception;
    }

    @Builder
    @lombok.Value
    private static class ErrorBody {
        ZonedDateTime timestamp;
        int status;
        String error;
        String exception;
        String path;
        String message;
        String trace;
    }
}
