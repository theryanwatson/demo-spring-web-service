package org.watson.demos.validation.constraints;

import org.watson.demos.validation.validators.LocaleValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({METHOD, FIELD, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = LocaleValidator.class)
public @interface ValidLocale {
    String message() default "{validation.constraints.ValidLocale.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
