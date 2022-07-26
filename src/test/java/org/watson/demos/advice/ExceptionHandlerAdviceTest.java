package org.watson.demos.advice;

import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.context.request.ServletWebRequest;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.boot.autoconfigure.web.ErrorProperties.IncludeAttribute.ALWAYS;
import static org.springframework.boot.autoconfigure.web.ErrorProperties.IncludeAttribute.NEVER;

class ExceptionHandlerAdviceTest {
    public static final String TEST_PATH = "/this/is/the/way";
    public static final ServletWebRequest TEST_WEB_REQUEST = new ServletWebRequest(new MockHttpServletRequest() {{
        setServletPath(TEST_PATH);
    }});

    private final ExceptionHandlerAdvice advice = new ExceptionHandlerAdvice(true, true, false);

    @MethodSource("status_exception_handlerMethod_source")
    @ParameterizedTest
    <T extends Throwable> void handleMethodsReturnExpectedFields(HttpStatus expectedStatus, T exception, HandlerMethodBiFunction<T> handlerMethod) {
        ResponseEntity<Object> actual = handlerMethod.apply(exception, TEST_WEB_REQUEST);

        assertThat(actual.getStatusCode(), is(expectedStatus));
        assertThat(actual.getBody(), notNullValue());
        assertThat(actual.getBody().toString(), containsString("status=" + expectedStatus.value()));
        assertThat(actual.getBody().toString(), containsString("message=" + exception.getMessage()));
        assertThat(actual.getBody().toString(), containsString("path=" + TEST_PATH));
        assertThat(actual.getBody().toString(), containsString("exception=" + exception.getClass().getName()));
    }

    @MethodSource("status_exception_handlerMethod_source")
    @ParameterizedTest
    void methodsHandleNulls(HttpStatus expectedStatus, Throwable ignored, HandlerMethodBiFunction<Throwable> handlerMethod) {
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
                    assertThat(actual.getBody().toString(), containsString("exception=" + innerException.getClass().getName()));
                });
    }

    @Test
    void messageFormatting_handleValidationException() {
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(
                createViolation("aField", "should be a"),
                createViolation("bField", "should be b"),
                createViolation("cField", "should be c")
        ));
        ResponseEntity<Object> actual = advice.handleValidationException(exception, TEST_WEB_REQUEST);

        assertThat(actual.getBody(), notNullValue());
        assertThat(actual.getBody().toString(), containsString("message=aField should be a, bField should be b, cField should be c"));
    }

    @Test
    void messageFormatting_fieldsCanBeEnabledThroughServerProperties() {
        ExceptionHandlerAdvice advice = new ExceptionHandlerAdvice(mockServerProperties(true, true, true));
        RuntimeException expected = new RuntimeException("fake");
        ResponseEntity<Object> actual = advice.handleBadRequestException(expected, TEST_WEB_REQUEST);

        assertThat(actual.getBody(), notNullValue());
        assertThat(actual.getBody().toString(), containsString("message=" + expected.getMessage()));
        assertThat(actual.getBody().toString(), containsString("exception=" + expected.getClass().getName()));
        assertThat(actual.getBody().toString(), not(containsString("trace=null")));
    }

    @Test
    void messageFormatting_fieldsCanBeDisabledThroughServerProperties() {
        ExceptionHandlerAdvice advice = new ExceptionHandlerAdvice(mockServerProperties(false, false, false));
        ResponseEntity<Object> actual = advice.handleBadRequestException(new RuntimeException("fake"), TEST_WEB_REQUEST);

        assertThat(actual.getBody(), notNullValue());
        assertThat(actual.getBody().toString(), containsString("message=null"));
        assertThat(actual.getBody().toString(), containsString("exception=null"));
        assertThat(actual.getBody().toString(), containsString("trace=null"));
    }

    private static Stream<Arguments> status_exception_handlerMethod_source() {
        ExceptionHandlerAdvice advice = new ExceptionHandlerAdvice(true, true, false);

        return Stream.of(
                Arguments.of(HttpStatus.CONFLICT, new DuplicateKeyException("Fake!"), (HandlerMethodBiFunction<Throwable>) advice::handleConflictException),
                Arguments.of(HttpStatus.CONFLICT, new SQLIntegrityConstraintViolationException("Fake!"), (HandlerMethodBiFunction<Throwable>) advice::handleConflictException),
                Arguments.of(HttpStatus.NOT_FOUND, new EntityNotFoundException("Fake!"), (HandlerMethodBiFunction<Throwable>) advice::handleNotFoundException),
                Arguments.of(HttpStatus.BAD_REQUEST, new PropertyReferenceException("Fake!", ClassTypeInformation.from(String.class), List.of()), (HandlerMethodBiFunction<PropertyReferenceException>) advice::handleBadRequestException),
                Arguments.of(HttpStatus.BAD_REQUEST, new ConstraintViolationException("Fake!", Set.of()), (HandlerMethodBiFunction<ConstraintViolationException>) advice::handleValidationException),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, new Exception("Fake!"), (HandlerMethodBiFunction<Exception>) advice::handleWrappedException)
        );
    }

    private static Optional<ServerProperties> mockServerProperties(boolean includeException, boolean includeMessage, boolean includeStacktrace) {
        return Optional.of(new ServerProperties() {{
            getError().setIncludeMessage(includeMessage ? ALWAYS : NEVER);
            getError().setIncludeException(includeException);
            getError().setIncludeStacktrace(includeStacktrace ? ALWAYS : NEVER);
        }});
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

    private ConstraintViolation<TestValidatedClass> createViolation(String property, String message) {
        return ConstraintViolationImpl.forReturnValueValidation("", Map.of(), Map.of(), message, TestValidatedClass.class, new TestValidatedClass(), null, null, PathImpl.createPathFromString(property), null, null, null);
    }

    private static class TestValidatedClass {}

    @FunctionalInterface
    interface HandlerMethodBiFunction<T extends Throwable> extends BiFunction<T, ServletWebRequest, ResponseEntity<Object>> {}
}
