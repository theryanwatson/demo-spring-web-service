package org.watson.demos.models;

import lombok.Getter;
import org.springframework.boot.actuate.health.Status;

@Getter
public enum HealthStatus {
    UP(Status.UP),
    DOWN(Status.DOWN),
    OUT_OF_SERVICE(Status.OUT_OF_SERVICE),
    UNKNOWN(Status.UNKNOWN);

    private final Status status;
    private final String name;

    HealthStatus(final Status status) {
        this.status = status;
        this.name = status.getCode();
    }
}
