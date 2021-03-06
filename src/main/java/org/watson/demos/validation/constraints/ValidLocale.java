package org.watson.demos.validation.constraints;

import org.watson.demos.validation.validators.LocaleValidator;

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
@Constraint(validatedBy = LocaleValidator.class)
public @interface ValidLocale {
    String message() default "{validation.constraints.ValidLocale.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
