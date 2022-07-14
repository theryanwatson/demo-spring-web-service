package org.watson.demos.advice;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;

@RestControllerAdvice
public class ExceptionAdvice extends DefaultHandlerExceptionResolver {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> notFoundException(HttpServletRequest request, EntityNotFoundException exception) {
        return buildRequest(request, exception, HttpStatus.NOT_FOUND);
    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<Object> badRequest(HttpServletRequest request, ConstraintViolationException exception) {
//        return buildRequest(request, exception, HttpStatus.BAD_REQUEST);
//    }
//
    @ExceptionHandler({TransactionSystemException.class, javax.persistence.RollbackException.class, javax.transaction.RollbackException.class})
    public ResponseEntity<Object> unwrapTransactionException(HttpServletRequest request, Exception exception) {
        return buildRequest(request, unwrapTransactionException(exception), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> buildRequest(HttpServletRequest request, Throwable throwable, HttpStatus status) {
        return new ResponseEntity<>(formatBody(request, status, throwable), new HttpHeaders(), status);
    }

    private static String formatBody(HttpServletRequest request, HttpStatus status, Throwable throwable) {
        return "{" +
                "\"timestamp\":\"" + ZonedDateTime.now(UTC).truncatedTo(MILLIS) + "\"," +
                "\"status\":" + status.value() + "," +
                "\"error\":\"" + status.getReasonPhrase() + "\"," +
                (throwable != null ? "\"message\":\"" + throwable.getMessage() + "\"," : "") +
                (request != null ? "\"path\":\"" + request.getServletPath() + "\"" : "") +
                "}";
    }

    private Throwable unwrapTransactionException(Throwable throwable) {
        return Stream.<Function<Throwable, Throwable>>of(
                        e -> getThrowable(e, TransactionSystemException.class, TransactionSystemException::getOriginalException),
                        e -> getThrowable(e, javax.persistence.RollbackException.class, javax.persistence.RollbackException::getCause),
                        e -> getThrowable(e, javax.transaction.RollbackException.class, javax.transaction.RollbackException::getCause))
                .map(f -> f.apply(throwable))
                .findFirst()
                .orElse(throwable);
    }

    private <E extends Throwable> Throwable getThrowable(Throwable throwable, Class<E> clazz, Function<E, Throwable> innerExceptionFunction) {
        return Optional.ofNullable(throwable)
                .filter(e -> clazz.isAssignableFrom(e.getClass()))
                .map(clazz::cast)
                .map(innerExceptionFunction)
                .map(this::unwrapTransactionException)
                .orElse(null);
    }
}
