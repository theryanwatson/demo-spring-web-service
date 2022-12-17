package org.watson.demos.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.springdoc.api.annotations.ParameterObject;
import org.watson.demos.validation.constraints.ValidLocale;

import java.io.Serializable;
import java.util.Locale;

@ParameterObject
@lombok.Value
@Builder
@JsonDeserialize(builder = GreetingProbe.GreetingProbeBuilder.class)
public class GreetingProbe implements Localizable, Serializable {
    @ValidLocale
    @Schema(type = "string", format = "locale")
    Locale locale;
}
