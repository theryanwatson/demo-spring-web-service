package org.watson.demos.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Auditable;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;

@Data
@Setter(AccessLevel.PROTECTED)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
abstract class ZonedAuditable<ID> implements Auditable<Void, ID, Instant>, Identifiable<ID>, Persistable<ID>, Serializable {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC", shape = JsonFormat.Shape.STRING)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private ZonedDateTime created, modified;

    @NonNull
    abstract public ID getId();

    @Override
    @Deprecated
    @NonNull
    @Transient
    @JsonIgnore
    public Optional<Instant> getCreatedDate() {
        return toTemporalAccessor(getCreated());
    }

    @Override
    @Deprecated
    @CreatedDate
    public void setCreatedDate(@NonNull final Instant creationDate) {
        setCreated(toZonedDateTime(creationDate));
    }

    @Override
    @Deprecated
    @NonNull
    @Transient
    @JsonIgnore
    public Optional<Instant> getLastModifiedDate() {
        return toTemporalAccessor(getModified());
    }

    @Override
    @Deprecated
    @LastModifiedDate
    public void setLastModifiedDate(@NonNull final Instant lastModifiedDate) {
        setModified(toZonedDateTime(lastModifiedDate));
    }

    @Override
    @Deprecated
    @NonNull
    @Transient
    @JsonIgnore
    public Optional<Void> getCreatedBy() {
        return Optional.empty();
    }

    @Override
    @Deprecated
    @CreatedBy
    public void setCreatedBy(final @Nullable Void createdBy) {}

    @Override
    @Deprecated
    @NonNull
    @Transient
    @JsonIgnore
    public Optional<Void> getLastModifiedBy() {
        return Optional.empty();
    }

    @Override
    @Deprecated
    @LastModifiedBy
    public void setLastModifiedBy(@Nullable final Void lastModifiedBy) {}

    @Override
    @Deprecated
    @Transient
    @JsonIgnore
    public boolean isNew() {
        return false;
    }

    private static ZonedDateTime toZonedDateTime(@NonNull final Instant instant) {
        return instant.truncatedTo(ChronoUnit.MILLIS).atZone(UTC);
    }

    private static Optional<Instant> toTemporalAccessor(@Nullable final ZonedDateTime date) {
        return Optional.ofNullable(date).map(ZonedDateTime::toInstant);
    }
}
