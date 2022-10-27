package org.watson.demos.events;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationEvent;
import org.springframework.lang.NonNull;
import org.watson.demos.models.HealthStatus;

import java.time.Clock;
import java.util.Map;

public class HealthEvent extends ApplicationEvent {

    public static HealthChangeEventBuilder builder() {
        return new HealthChangeEventBuilder();
    }

    private HealthEvent(@NonNull final Health health, @NonNull final Clock clock) {
        super(health, clock);
    }

    @NonNull
    public Health getHealth() {
        return (Health) getSource();
    }

    @NonNull
    public Status getStatus() {
        return getHealth().getStatus();
    }

    @Accessors(fluent = true, chain = true)
    @Setter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class HealthChangeEventBuilder {
        @Tolerate
        public HealthChangeEventBuilder status(final HealthStatus status) {
            this.status = status.getStatus();
            return this;
        }

        @Tolerate
        public HealthChangeEventBuilder status(final String status) {
            this.status = new Status(status);
            return this;
        }

        private Status status;
        private Map<String, ?> details;
        private Clock clock;

        public HealthEvent build() {
            return new HealthEvent(
                    new Health.Builder()
                            .status(status != null ? status : Status.UNKNOWN)
                            .withDetails(details != null ? details : Map.of())
                            .build(),
                    this.clock != null ? this.clock : Clock.systemUTC());
        }
    }
}
