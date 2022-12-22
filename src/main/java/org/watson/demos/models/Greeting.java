package org.watson.demos.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.watson.demos.validation.constraints.ValidLocale;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
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
