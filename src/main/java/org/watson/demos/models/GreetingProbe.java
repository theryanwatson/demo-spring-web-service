package org.watson.demos.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import org.springdoc.api.annotations.ParameterObject;
import org.watson.demos.validation.constraints.ValidLocale;

import java.util.Locale;

@ParameterObject
@lombok.Value
@Builder
@JsonDeserialize(builder = GreetingProbe.GreetingProbeBuilder.class)
public class GreetingProbe {
    @ValidLocale
    Locale locale;
}
