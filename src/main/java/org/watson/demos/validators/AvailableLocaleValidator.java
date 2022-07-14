package org.watson.demos.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Set;

public class AvailableLocaleValidator implements ConstraintValidator<AvailableLocale, java.util.Locale> {
    private static final Set<java.util.Locale> AVAILABLE_LOCALES = Set.of(java.util.Locale.getAvailableLocales());

    @Override
    public void initialize(AvailableLocale availableLocale) {}

    @Override
    public boolean isValid(java.util.Locale locale, ConstraintValidatorContext context) {
        return AVAILABLE_LOCALES.contains(locale);
    }
}
