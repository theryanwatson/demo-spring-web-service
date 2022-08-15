package org.watson.demos.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Extends DefaultHandlerExceptionResolver to unwrap Exceptions and associate Exceptions with Error Codes.
 *
 * <li>[Optional] {@code server.error.unwrapped-exceptions=full.path.to.Exception,full.path.to.OtherException}</li>
 * <li>[Optional] {@code springdoc.info.external-documentation.description=server.error.exception-codes={"full.path.to.Exception": 400, "full.path.to.OtherException": 404}}</li>
 */
@ConditionalOnExpression("#{'${server.error.unwrapped-exceptions:}' != '' || '${server.error.exception-codes:}' != ''}")
@Slf4j
@Component
public class UnwrappedExceptionResolver extends DefaultHandlerExceptionResolver {
    static final Set<String> ERROR_EXCEPTION_ATTRIBUTES = Set.of(
            WebUtils.ERROR_EXCEPTION_ATTRIBUTE,
            org.springframework.boot.web.reactive.error.DefaultErrorAttributes.class.getName() + ".ERROR",
            org.springframework.boot.web.servlet.error.DefaultErrorAttributes.class.getName() + ".ERROR"
    );

    private final Set<Class<? extends Throwable>> unwrappedExceptions;
    private final Map<Class<? extends Throwable>, Integer> exceptionErrorCodes;

    public UnwrappedExceptionResolver(@Value("${server.error.unwrapped-exceptions:}") final Set<Class<? extends Throwable>> unwrappedExceptions,
                                      @Value("#{${server.error.exception-codes:{:}}}") final Map<Class<? extends Throwable>, Integer> exceptionErrorCodes) {
        this.unwrappedExceptions = unwrappedExceptions;
        this.exceptionErrorCodes = exceptionErrorCodes;
        setOrder(Ordered.LOWEST_PRECEDENCE);
        setWarnLogCategory(getClass().getName());
    }

    @Override
    public ModelAndView resolveException(@NonNull final HttpServletRequest request, @NonNull final HttpServletResponse response, final Object handler, @NonNull final Exception ex) {
        final Exception unwrapped = unwrap(ex);
        ERROR_EXCEPTION_ATTRIBUTES.forEach(attribute -> request.setAttribute(attribute, unwrapped));
        return super.resolveException(request, response, handler, unwrapped);
    }

    @Override
    protected ModelAndView doResolveException(@NonNull final HttpServletRequest request, @NonNull final HttpServletResponse response, final Object handler, @NonNull final Exception exception) {
        final ModelAndView modelAndView = super.doResolveException(request, response, handler, exception);
        if (modelAndView != null) {
            return modelAndView;
        }

        final Optional<Integer> code = findHttpStatusCode(exception);
        if (code.isPresent()) {
            try {
                response.sendError(code.get());
                return new ModelAndView("error");
            } catch (IOException e) {
                log.debug("Unable to send error. {}", e.getMessage());
            }
        }
        return null;
    }

    private Optional<Integer> findHttpStatusCode(final Throwable throwable) {
        return exceptionErrorCodes.entrySet().stream()
                .filter(e -> e.getKey().isAssignableFrom(throwable.getClass()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    @NonNull
    private Exception unwrap(@Nullable final Throwable original) {
        Throwable unwrapped = null;
        if (original != null && unwrappedExceptions.contains(original.getClass())) {
            if (original instanceof TransactionSystemException) {
                unwrapped = unwrap(((TransactionSystemException) original).getOriginalException());
            } else {
                unwrapped = unwrap(original.getCause());
            }
            log.debug("Unwrapped exception. original={}, unwrapped={}", original.getClass().getName(), unwrapped.getClass().getName());
        }
        unwrapped = unwrapped != null ? unwrapped : original;
        return unwrapped instanceof Exception ? (Exception) unwrapped : new RuntimeException(unwrapped);
    }
}
