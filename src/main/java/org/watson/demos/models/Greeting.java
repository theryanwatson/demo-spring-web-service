package org.watson.demos.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.watson.demos.validation.constraints.ValidLocale;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Locale;
import java.util.UUID;

@Data
@Builder
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"content", "locale"}))
@Setter(AccessLevel.PROTECTED) // For @Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED) // For @Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE) // For @Builder
public class Greeting implements Serializable {
    @Id
    @GeneratedValue
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @NotBlank
    @Basic(optional = false)
    private String content;

    @ValidLocale
    @Builder.Default
    @Schema(type = "string", format = "locale")
    private Locale locale = Locale.getDefault();
}
