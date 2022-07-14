package org.watson.demos.models;

import lombok.*;
import org.watson.demos.validators.AvailableLocale;

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
    private UUID id;

    @NotBlank
    @Basic(optional = false)
    private String content;

    @AvailableLocale
    @Builder.Default
    private Locale locale = Locale.getDefault();
}
