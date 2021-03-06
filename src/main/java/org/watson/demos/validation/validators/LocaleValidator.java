package org.watson.demos.validation.validators;

import org.watson.demos.validation.constraints.ValidLocale;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.Set;

/**
 * Checks that a validated {@link Locale} is {@code null} or exists in {@link Locale#getAvailableLocales()}.
 */
public class LocaleValidator implements ConstraintValidator<ValidLocale, Locale> {
    private static final Set<Locale> AVAILABLE_LOCALES = Set.of(Locale.getAvailableLocales());

    @Override
    public boolean isValid(final Locale locale, final ConstraintValidatorContext ignored) {
        return locale == null || AVAILABLE_LOCALES.contains(locale);
    }
}
