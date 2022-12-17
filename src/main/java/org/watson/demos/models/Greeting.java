package org.watson.demos.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.watson.demos.validation.constraints.ValidLocale;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"content", "locale"}))
@Setter(AccessLevel.PROTECTED) // For @Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For @Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE) // For @Builder
public class Greeting implements Identifiable, Localizable, Serializable {
    @Id
    @GeneratedValue
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Column(columnDefinition = "uuid", updatable = false, length = 36)
    private UUID id;

    @NotBlank
    @Basic(optional = false)
    private String content;

    @ValidLocale
    @Builder.Default
    @Schema(type = "string", format = "locale")
    private Locale locale = Locale.getDefault();

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC", shape = JsonFormat.Shape.STRING)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @Basic(optional = false)
    @CreatedDate
    private ZonedDateTime created;

    @PrePersist
    protected void onCreate() {
        created = ZonedDateTime.now().withNano(0);
    }
}
