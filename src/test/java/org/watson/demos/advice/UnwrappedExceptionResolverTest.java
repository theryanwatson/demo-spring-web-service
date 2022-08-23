package org.watson.demos.advice;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.watson.demos.advice.UnwrappedExceptionResolver.ERROR_EXCEPTION_ATTRIBUTES;

class UnwrappedExceptionResolverTest {
    @Nested
    @SpringBootTest(classes = UnwrappedExceptionResolver.class, properties = {
            "server.error.unwrapped-exceptions=java.lang.reflect.InvocationTargetException,org.springframework.transaction.TransactionSystemException",
            "server.error.exception-codes={\"java.lang.RuntimeException\":400}",
    })
    class BeanEnabledTest {
        @Mock
        private HttpServletResponse response;
        @Mock
        private HttpServletRequest request;

        @Resource
        private UnwrappedExceptionResolver resolver;

        @Test
        void resolveException_unwrapsException() {
            final Exception expected = new Exception("expected");

            resolver.resolveException(request, response, null, new InvocationTargetException(expected));

            ERROR_EXCEPTION_ATTRIBUTES.forEach(a -> verify(request).setAttribute(a, expected));
        }

        @Test
        void resolveException_unwrapsLayeredExceptions() {
            final Exception expected = new Exception("expected");

            resolver.resolveException(request, response, null, new InvocationTargetException(new TransactionSystemException("fake", expected)));

            ERROR_EXCEPTION_ATTRIBUTES.forEach(a -> verify(request).setAttribute(a, expected));
        }

        @Test
        void resolveException_doesNotUnwrapException() {
            final Exception expected = new RuntimeException("expected", new Exception("fake"));

            resolver.resolveException(request, response, null, expected);

            ERROR_EXCEPTION_ATTRIBUTES.forEach(a -> verify(request).setAttribute(a, expected));
        }

        @SneakyThrows
        @Test
        void doResolveException_returnsMappedCode() {
            final Exception expected = new RuntimeException("expected", new Exception("fake"));

            final ModelAndView mv = resolver.doResolveException(request, response, null, expected);

            assertThat(mv, notNullValue());
            assertThat(mv.getViewName(), is("error"));
            verify(response).sendError(400);
        }

        @Test
        void doResolveException_returnsNullForUnmapped() {
            final Exception expected = new Exception("expected");

            final ModelAndView mv = resolver.doResolveException(request, response, null, expected);

            assertThat(mv, nullValue());
            verifyNoInteractions(response);
        }

        @SneakyThrows
        @Test
        void doResolveException_returnsNullOnSendErrorException() {
            doThrow(new IOException("fake"))
                    .when(response).sendError(anyInt());
            final Exception expected = new RuntimeException("expected");

            final ModelAndView mv = resolver.doResolveException(request, response, null, expected);

            assertThat(mv, nullValue());
            verify(response).sendError(400);
        }

        @SneakyThrows
        @Test
        void doResolveException_returnsSuperClassResult() {
            final Exception expected = new HttpMediaTypeNotAcceptableException("expected");

            final ModelAndView mv = resolver.doResolveException(request, response, null, expected);

            assertThat(mv, notNullValue());
            verify(response).sendError(SC_NOT_ACCEPTABLE);
        }
    }

    @Nested
    @SpringBootTest(classes = UnwrappedExceptionResolver.class, properties = {
            "spring.config.location=classpath:empty.properties"
    })
    class BeanDisabledTest {

        @Resource
        private ApplicationContext context;

        @Test
        void beanNotCreatedPropertiesNotSet() {
            assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(UnwrappedExceptionResolver.class));
        }
    }
}
