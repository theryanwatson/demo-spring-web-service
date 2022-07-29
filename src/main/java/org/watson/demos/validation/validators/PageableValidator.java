package org.watson.demos.validation.validators;

import org.springframework.data.domain.Pageable;
import org.watson.demos.validation.constraints.ValidPageable;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Checks that a validated {@link Pageable} is {@code null} or
 * has both a {@link Pageable#getPageSize()} >= {@link #minSize} and a {@link Pageable#getPageNumber()} >= {@link #minNumber}.
 */
public class PageableValidator implements ConstraintValidator<ValidPageable, Pageable> {

    private int minSize;
    private int minNumber;

    @Override
    public void initialize(final ValidPageable constraintAnnotation) {
        this.minSize = constraintAnnotation.minSize();
        this.minNumber = constraintAnnotation.minPage();
    }

    @Override
    public boolean isValid(final Pageable pageable, final ConstraintValidatorContext ignored) {
        return pageable == null || (pageable.getPageSize() >= minSize && pageable.getPageNumber() >= minNumber);
    }
}
