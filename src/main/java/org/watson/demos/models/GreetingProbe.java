package org.watson.demos.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;

import java.util.Locale;

@lombok.Value
@Builder
@JsonDeserialize(builder = GreetingProbe.GreetingProbeBuilder.class)
public class GreetingProbe {
    Locale locale;
}
