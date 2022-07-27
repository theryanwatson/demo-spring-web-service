package org.watson.demos.configuration;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.SpringDocUtils;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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
    public SpringDocConfiguration(@Value("${springdoc.simple-types:java.util.Locale}") final Set<Class<?>> simpleTypes) {
        simpleTypes.forEach(c -> {
            SpringDocUtils.getConfig().addSimpleTypesForParameterObject(c);
            SpringDocUtils.getConfig().removeRequestWrapperToIgnore(c);
        });
    }

    @Bean
    public OpenAPI getOpenApi(@Autowired(required = false) final Optional<Components> components,
                              @Autowired(required = false) final Optional<ExternalDocumentation> externalDocs,
                              @Autowired(required = false) final Optional<Info> info) {
        if (components.isEmpty() && externalDocs.isEmpty() && info.isEmpty()) {
            return null;
        }

        final OpenAPI openApi = new OpenAPI();
        components.ifPresent(openApi::components);
        externalDocs.ifPresent(openApi::externalDocs);
        info.ifPresent(openApi::info);
        return openApi;
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
    Info apiInfo(@Autowired(required = false) final Optional<BuildProperties> properties,
                 @Autowired(required = false) final Optional<License> license,
                 @Autowired(required = false) final Optional<Contact> contact,
                 @Value("${" + SPRING_DOC_PREFIX_INFO + ".terms-of-service:#{null}}") final Optional<String> termsOfService) {
        if (properties.isEmpty() && license.isEmpty() && contact.isEmpty() && termsOfService.isEmpty()) {
            return null;
        }

        final Info info = new Info();

        properties.map(this::buildDescription).ifPresent(info::description);
        properties.map(BuildProperties::getName).ifPresent(info::title);
        properties.map(BuildProperties::getVersion).ifPresent(info::version);

        contact.ifPresent(info::contact);
        license.ifPresent(info::license);
        termsOfService.ifPresent(info::termsOfService);

        return info;
    }

    /**
     * Optionally constructed bean. Requires "url" property be set or bean will not get constructed.
     *
     * <li>{@code springdoc.info.external-documentation.url=}</li>
     * <li>{@code springdoc.info.external-documentation.description=}</li>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SPRING_DOC_PREFIX_EXTERNAL_DOCUMENTATION, name = "url")
    @ConfigurationProperties(prefix = SPRING_DOC_PREFIX_EXTERNAL_DOCUMENTATION)
    ExternalDocumentation externalDocumentation() {
        return new ExternalDocumentation();
    }

    /**
     * Optionally constructed bean. Requires "url" property be set or bean will not get constructed.
     *
     * <li>{@code springdoc.info.license.url=}</li>
     * <li>{@code springdoc.info.license.name=}</li>
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
     * <li>{@code springdoc.info.contact.url=}</li>
     * <li>{@code springdoc.info.contact.email=}</li>
     * <li>{@code springdoc.info.contact.name=}</li>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SPRING_DOC_PREFIX_CONTACT, name = "url")
    @ConfigurationProperties(prefix = SPRING_DOC_PREFIX_CONTACT)
    Contact apiContact() {
        return new Contact();
    }

    /**
     * Sets Iterables types as "array" schemas, which seems like something this library should do already.
     */
    @Bean
    public ModelConverter iterableModelConverter() {
        return new ModelConverter() {
            @Override
            public Schema<?> resolve(final AnnotatedType originalType, final ModelConverterContext context, final Iterator<ModelConverter> chain) {
                final JavaType javaType = Json.mapper().constructType(originalType.getType());

                if (javaType != null && !Collection.class.isAssignableFrom(javaType.getRawClass()) && Iterable.class.isAssignableFrom(javaType.getRawClass())) {
                    final AnnotatedType iterableType = new AnnotatedType()
                            .type(javaType.containedType(0))
                            .ctxAnnotations(originalType.getCtxAnnotations())
                            .parent(originalType.getParent())
                            .schemaProperty(originalType.isSchemaProperty())
                            .name(originalType.getName())
                            .resolveAsRef(originalType.isResolveAsRef())
                            .jsonViewAnnotation(originalType.getJsonViewAnnotation())
                            .propertyName(originalType.getPropertyName())
                            .skipOverride(true);
                    return new ArraySchema().items(this.resolve(iterableType, context, chain));
                }
                return (chain.hasNext()) ? chain.next().resolve(originalType, context, chain) : null;
            }
        };
    }

    /**
     * Adds "Error" schema. Sets shared {@link HttpStatus} codes on every API Operation Response.
     * <li>{@code springdoc.error-response-class=my.class.path.Error}</li>
     * <li>{@code springdoc.shared-errors=INTERNAL_SERVER_ERROR}</li>
     */
    @Bean
    public OpenApiCustomiser sharedErrors(@Value("${springdoc.error-response-class:org.watson.demos.models.ErrorResponse}") final Class<?> errorResponseClass,
                                          @Value("${springdoc.shared-errors:BAD_REQUEST,INTERNAL_SERVER_ERROR}") final Set<HttpStatus> sharedErrors) {
        return openApi -> {
            final Schema<?> errorResponseSchema = ModelConverters.getInstance()
                    .read(errorResponseClass)
                    .getOrDefault(errorResponseClass.getSimpleName(), new Schema<>());

            openApi.schema(errorResponseSchema.getName(), errorResponseSchema);

            final Content content = new Content().addMediaType(APPLICATION_JSON_VALUE, new MediaType().schema(new Schema<>().$ref(errorResponseSchema.getName())));

            final Map<String, ApiResponse> errorApiResponses = sharedErrors.stream()
                    .filter(HttpStatus::isError)
                    .collect(Collectors.toMap(
                            status -> String.valueOf(status.value()),
                            status -> new ApiResponse()
                                    .description(status.getReasonPhrase())
                                    .content(content),
                            (s1, s2) -> s2));

            Stream.of(openApi)
                    .map(OpenAPI::getPaths)
                    .map(Map::values)
                    .flatMap(Collection::stream)
                    .map(PathItem::readOperations)
                    .flatMap(Collection::stream)
                    .map(Operation::getResponses)
                    .forEach(r -> errorApiResponses.forEach(r::addApiResponse));
        };
    }

    private String buildDescription(BuildProperties properties) {
        final String description = Stream.of(
                        Optional.ofNullable(properties.get("description"))
                                .map(StringUtils::trimToNull)
                                .map(d -> "<div><b>" + d + "</b></div>"),
                        Optional.ofNullable(properties.getTime())
                                .map(t -> "<div>Created: " + t + "</div>"))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining("\n"));

        return StringUtils.trimToNull(description);
    }
}
