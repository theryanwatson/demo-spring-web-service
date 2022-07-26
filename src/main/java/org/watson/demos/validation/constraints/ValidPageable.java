package org.watson.demos.validation.constraints;

import org.watson.demos.validation.validators.PageableValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraintvalidation.SupportedValidationTarget;
import javax.validation.constraintvalidation.ValidationTarget;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = PageableValidator.class)
@SupportedValidationTarget(ValidationTarget.ANNOTATED_ELEMENT)
@Target({METHOD, FIELD, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface ValidPageable {
    int minPage() default 0;

    int minSize() default 1;

    String message() default "{validation.constraints.ValidPageable.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
