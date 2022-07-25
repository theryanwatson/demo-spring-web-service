package org.watson.demos.advice;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@Slf4j
@RequiredArgsConstructor
@ConditionalOnWebApplication
@RestControllerAdvice
public class ExceptionHandlerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            DuplicateKeyException.class,
            SQLIntegrityConstraintViolationException.class,
    })
    public ResponseEntity<Object> handleDuplicateKeyException(final Throwable exception, final ServletWebRequest request) {
        return handleExceptionInternal(exception, Throwable::getMessage, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler({
            EntityNotFoundException.class,
    })
    public ResponseEntity<Object> handleNotFoundException(final Throwable throwable, final ServletWebRequest request) {
        return handleExceptionInternal(throwable, Throwable::getMessage, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(javax.validation.ConstraintViolationException.class)
    public ResponseEntity<Object> handleValidationException(final javax.validation.ConstraintViolationException exception, final ServletWebRequest request) {
        return handleExceptionInternal(exception, this::buildValidationExceptionMessage, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({
            InvocationTargetException.class,
            javax.persistence.RollbackException.class,
            TransactionSystemException.class,
    })
    public ResponseEntity<Object> handleWrappedException(final Exception exception, final ServletWebRequest request) {
        Throwable unwrappedCause = unwrapCause(exception);

        if (unwrappedCause instanceof DuplicateKeyException || unwrappedCause instanceof SQLIntegrityConstraintViolationException) {
            return handleDuplicateKeyException(unwrappedCause, request);
        } else if (unwrappedCause instanceof EntityNotFoundException) {
            return handleNotFoundException(unwrappedCause, request);
        } else if (unwrappedCause instanceof javax.validation.ConstraintViolationException) {
            return handleValidationException((javax.validation.ConstraintViolationException) unwrappedCause, request);
        }

        return handleExceptionInternal(unwrappedCause, Throwable::getMessage, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private <T extends Throwable> ResponseEntity<Object> handleExceptionInternal(final T throwable, final Function<T, String> messageSupplier, final HttpStatus status, final ServletWebRequest request) {
        if (throwable == null || messageSupplier == null) {
            return new ResponseEntity<>(
                    formatBody(status, null, request),
                    buildHeaders(),
                    status);
        } else {
            return super.handleExceptionInternal(
                    throwable instanceof Exception ? (Exception) throwable : new RuntimeException(throwable),
                    formatBody(status, messageSupplier.apply(throwable), request),
                    buildHeaders(),
                    status,
                    request
            );
        }
    }

    private HttpHeaders buildHeaders() {
        return new HttpHeaders() {{
            setContentType(MediaType.APPLICATION_JSON);
        }};
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

    private Object formatBody(final HttpStatus status, final String message, final ServletWebRequest request) {
        return ErrorBody.builder()
                .timestamp(ZonedDateTime.now(UTC)
                        .truncatedTo(MILLIS))
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(Optional.ofNullable(request)
                        .map(ServletRequestAttributes::getRequest)
                        .map(HttpServletRequest::getServletPath)
                        .orElse(null))
                .build();
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
        String path;
        String message;
    }
}
