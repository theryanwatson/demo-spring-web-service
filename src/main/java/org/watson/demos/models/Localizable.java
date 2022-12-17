package org.watson.demos.models;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;

public interface Localizable {
    @Schema(type = "string", format = "locale")
    Locale getLocale();
}
