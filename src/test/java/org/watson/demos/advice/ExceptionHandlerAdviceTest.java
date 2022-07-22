package org.watson.demos.advice;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.context.request.ServletWebRequest;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ExceptionHandlerAdviceTest {
    public static final String TEST_PATH = "/this/is/the/way";
    public static final ServletWebRequest TEST_WEB_REQUEST = new ServletWebRequest(new MockHttpServletRequest() {{
        setServletPath(TEST_PATH);
    }});

    private final ExceptionHandlerAdvice advice = new ExceptionHandlerAdvice();

    @MethodSource("status_exception_handlerMethod_source")
    @ParameterizedTest
    <T extends Throwable> void handleMethodsReturnExpectedStatus(HttpStatus expectedStatus, T exception, HandlerMethodBiFunction<T> handlerMethod) {
        ResponseEntity<Object> actual = handlerMethod.apply(exception, TEST_WEB_REQUEST);

        assertThat(actual.getStatusCode(), is(expectedStatus));
        assertThat(actual.getBody(), notNullValue());
        assertThat(actual.getBody().toString(), containsString("status=" + expectedStatus.value()));
        assertThat(actual.getBody().toString(), containsString("message=" + exception.getMessage()));
        assertThat(actual.getBody().toString(), containsString("path=" + TEST_PATH));
    }

    @MethodSource("status_exception_handlerMethod_source")
    @ParameterizedTest
    void methodsHandleNulls(HttpStatus expectedStatus, Exception ignored, HandlerMethodBiFunction<Throwable> handlerMethod) {
        ResponseEntity<Object> actual = handlerMethod.apply(null, null);

        assertThat(actual.getStatusCode(), is(expectedStatus));
        assertThat(actual.getBody(), notNullValue());
        assertThat(actual.getBody().toString(), containsString("status=" + expectedStatus.value()));
    }

    @MethodSource("status_exception_handlerMethod_source")
    @ParameterizedTest
    void handleWrappedException_unwrapsAndReturnsExpectedStatus(HttpStatus expectedStatus, Throwable innerException) {
        Stream.of(
                        InvocationTargetException.class,
                        javax.persistence.RollbackException.class,
                        TransactionSystemException.class
                )
                .map(newExceptionInstance(innerException))
                .map(t -> advice.handleWrappedException(t, TEST_WEB_REQUEST))
                .forEach(actual -> {
                    assertThat(actual.getStatusCode(), is(expectedStatus));
                    assertThat(actual.getBody(), notNullValue());
                    assertThat(actual.getBody().toString(), containsString("status=" + expectedStatus.value()));
                    assertThat(actual.getBody().toString(), containsString("message=" + innerException.getMessage()));
                    assertThat(actual.getBody().toString(), containsString("path=" + TEST_PATH));
                });
    }

    private static Stream<Arguments> status_exception_handlerMethod_source() {
        ExceptionHandlerAdvice advice = new ExceptionHandlerAdvice();
        return Stream.of(
                Arguments.of(HttpStatus.CONFLICT, new DuplicateKeyException("Fake!"), (HandlerMethodBiFunction<Throwable>) advice::handleDuplicateKeyException),
                Arguments.of(HttpStatus.CONFLICT, new SQLIntegrityConstraintViolationException("Fake!"), (HandlerMethodBiFunction<Throwable>) advice::handleDuplicateKeyException),
                Arguments.of(HttpStatus.NOT_FOUND, new EntityNotFoundException("Fake!"), (HandlerMethodBiFunction<Throwable>) advice::handleNotFoundException),
                Arguments.of(HttpStatus.BAD_REQUEST, new ConstraintViolationException("Fake!", Set.of()), (HandlerMethodBiFunction<ConstraintViolationException>) advice::handleValidationException),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, new Exception("Fake!"), (HandlerMethodBiFunction<Exception>) advice::handleWrappedException)
        );
    }

    private Function<Class<? extends Throwable>, Exception> newExceptionInstance(Throwable cause) {
        return exceptionClass -> Stream.<Callable<? extends Throwable>>of(
                        () -> exceptionClass.getDeclaredConstructor(String.class, Throwable.class).newInstance("Wrapped!", cause),
                        () -> exceptionClass.getDeclaredConstructor(Throwable.class, String.class).newInstance(cause, "Wrapped!"),
                        () -> exceptionClass.getDeclaredConstructor(Throwable.class).newInstance(cause)
                )
                .map(callable -> {
                    try {
                        return callable.call();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Exception.class::isInstance)
                .map(Exception.class::cast)
                .findFirst()
                .orElse(null);
    }

    @FunctionalInterface
    interface HandlerMethodBiFunction<T extends Throwable> extends BiFunction<T, ServletWebRequest, ResponseEntity<Object>> {}
}
