package org.watson.demos.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.Set;

/**
 * Checks that a validated {@link Locale} is {@code null} or exists in {@link Locale#getAvailableLocales()}.
 */
public class AvailableLocaleValidator implements ConstraintValidator<AvailableLocale, Locale> {
    private static final Set<Locale> AVAILABLE_LOCALES = Set.of(Locale.getAvailableLocales());

    @Override
    public void initialize(AvailableLocale availableLocale) {}

    @Override
    public boolean isValid(Locale locale, ConstraintValidatorContext context) {
        return locale == null || AVAILABLE_LOCALES.contains(locale);
    }
}
