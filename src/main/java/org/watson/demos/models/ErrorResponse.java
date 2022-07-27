package org.watson.demos.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
@lombok.Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    ZonedDateTime timestamp;
    int status;
    String error;
    String exception;
    String path;
    String message;
    String trace;
}
