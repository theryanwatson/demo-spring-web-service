package org.watson.demos.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.SpringDocUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class SpringDocConfiguration {
    static final String SPRING_DOC_PREFIX_INFO = "springdoc.info";
    static final String SPRING_DOC_PREFIX_EXTERNAL_DOCUMENTATION = SPRING_DOC_PREFIX_INFO + ".external-documentation";
    static final String SPRING_DOC_PREFIX_CONTACT = SPRING_DOC_PREFIX_INFO + ".contact";
    static final String SPRING_DOC_PREFIX_LICENSE = SPRING_DOC_PREFIX_INFO + ".license";

    /**
     * This constructor contains a 3PL-sadness workaround. If certain types (like {@link java.util.Locale}) are in
     * documented classes, this SpringDoc library error out with a {@code java.lang.StackOverflowError: null}. 3PL
     * documentation does not show a way to configure a way out of this error. Only statically accessing the lists
     * of ignored types can work around the failure.
     *
     * @param simpleTypes List of Full-Class-Path to the types to be added as "simple types"
     *                    springdoc.simple-types=java.util.Locale,java.time.ZoneId
     */
    public SpringDocConfiguration(@Value("${springdoc.simple-types:java.util.Locale}") Set<Class<?>> simpleTypes) {
        simpleTypes.forEach(c -> {
            SpringDocUtils.getConfig().addSimpleTypesForParameterObject(c);
            SpringDocUtils.getConfig().removeRequestWrapperToIgnore(c);
        });
    }

    @Bean
    public OpenAPI getOpenApi(@Autowired(required = false) Components components,
                              @Autowired(required = false) ExternalDocumentation externalDocumentation,
                              @Autowired(required = false) Info info) {
        return new OpenAPI()
                .components(components)
                .externalDocs(externalDocumentation)
                .info(info);
    }

    /**
     * Optionally constructed bean. Requires "url" property be set or bean will not get constructed.
     *
     * <li>springdoc.info.external-documentation.url=</li>
     * <li>springdoc.info.external-documentation.description=</li>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SPRING_DOC_PREFIX_EXTERNAL_DOCUMENTATION, name = "url")
    @ConfigurationProperties(prefix = SPRING_DOC_PREFIX_EXTERNAL_DOCUMENTATION)
    ExternalDocumentation externalDocumentation() {
        return new ExternalDocumentation();
    }

    /**
     * Creates {@link Info} based on {@link BuildProperties}, {@link License}, and {@link Contact} beans
     *
     * @param properties     [Optional] Populates {@link Info#title(String)}, {@link Info#description(String)}, {@link Info#version(String)}
     * @param license        [Optional] Populates {@link Info#license(License)}
     * @param contact        [Optional] Populates {@link Info#contact(Contact)}
     * @param termsOfService [Optional] Populates {@link Info#termsOfService(String)}
     * @return Populated {@link Info}
     */
    @Bean
    @ConditionalOnMissingBean
    Info apiInfo(@Autowired(required = false) Optional<BuildProperties> properties,
                 @Autowired(required = false) License license,
                 @Autowired(required = false) Contact contact,
                 @Value("${" + SPRING_DOC_PREFIX_INFO + ".terms-of-service:#{null}}") String termsOfService) {
        if (properties.isEmpty() && license == null && contact == null && termsOfService == null) {
            return null;
        }

        final String description = properties.map(p -> p.get("description")).orElse(null);
        final Instant created = properties.map(BuildProperties::getTime).orElse(null);

        return new Info()
                .title(properties.map(BuildProperties::getName).orElse(null))
                .description((description == null ? "" : "<div><b>" + description + "</b></div>") + (created == null ? "" : "\n<div>Created: " + created + "</div>"))
                .version(properties.map(BuildProperties::getVersion).orElse(null))
                .contact(contact)
                .license(license)
                .termsOfService(termsOfService);
    }

    /**
     * Optionally constructed bean. Requires "url" property be set or bean will not get constructed.
     *
     * <li>springdoc.info.license.url=</li>
     * <li>springdoc.info.license.name=</li>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SPRING_DOC_PREFIX_LICENSE, name = "url")
    @ConfigurationProperties(prefix = SPRING_DOC_PREFIX_LICENSE)
    License apiLicense() {
        return new License();
    }

    /**
     * Optionally constructed bean. Requires "url" property be set or bean will not get constructed.
     *
     * <li>springdoc.info.contact.url=</li>
     * <li>springdoc.info.contact.email=</li>
     * <li>springdoc.info.contact.name=</li>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SPRING_DOC_PREFIX_CONTACT, name = "url")
    @ConfigurationProperties(prefix = SPRING_DOC_PREFIX_CONTACT)
    Contact apiContact() {
        return new Contact();
    }
}
