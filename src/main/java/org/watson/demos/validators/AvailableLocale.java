package org.watson.demos.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = AvailableLocaleValidator.class)
public @interface AvailableLocale {
    String message() default "{javax.validation.constraints.Locale.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
