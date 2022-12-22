package org.watson.demos.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.watson.demos.validation.constraints.ValidLocale;

import java.util.Locale;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PROTECTED) // For @Entity
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@Getter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"content", "locale"}))
public class Greeting extends ZonedAuditable<UUID> implements Localizable {

    /** For GraphQL: Writable field constructor. */
    public Greeting(final String content, final Locale locale) {
        this.content = content;
        this.locale = locale;
    }

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
}
