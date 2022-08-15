package org.watson.demos.configuration;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.SpringDocUtils;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ErrorProperties.IncludeAttribute;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Customizes SpringDoc Open API documentation through standard Spring methods, like application.properties,
 * environment variables, etc. All properties are optional.
 *
 * <li>The optional configuration properties change/fix the behavior of the library:</li><ul>
 * <li>{@code springdoc.shared-errors=} {@link #sharedErrors(Schema, Set)}</li>
 * <li>{@code springdoc.simple-types=} {@link SpringDocConfiguration#SpringDocConfiguration(Set)}</li>
 * <li>{@code springdoc.use-array-schema=} {@link #arraySchemaModelConverter(Set)}</li>
 * </ul>
 *
 * <li>The optional info properties are displayed on the documentation page:</li><ul>
 * <li>{@code springdoc.info.contact.email=}</li>
 * <li>{@code springdoc.info.contact.name=}</li>
 * <li>{@code springdoc.info.contact.url=}</li>
 * <li>{@code springdoc.info.external-documentation.description=}</li>
 * <li>{@code springdoc.info.external-documentation.url=}</li>
 * <li>{@code springdoc.info.license.name=}</li>
 * <li>{@code springdoc.info.license.url=}</li>
 * <li>{@code springdoc.info.terms-of-service=}</li>
 * </ul>
 */
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
     * <li>[Optional] {@code springdoc.simple-types=java.util.Locale,java.time.ZoneId}</li>
     *
     * @param simpleTypes List of full-class-path to the classes to be added as "simple types"
     */
    public SpringDocConfiguration(@Value("${springdoc.simple-types:}") final Set<Class<?>> simpleTypes) {
        simpleTypes.forEach(c -> SpringDocUtils.getConfig()
                .addSimpleTypesForParameterObject(c)
                .removeRequestWrapperToIgnore(c));
    }

    /**
     * Creates {@link OpenAPI} based on {@link Components}, {@link ExternalDocumentation}, and {@link Info} beans
     *
     * @param components   [Optional] Populates {@link OpenAPI#components(Components)}
     * @param externalDocs [Optional] Populates {@link OpenAPI#externalDocs(ExternalDocumentation)}
     * @param info         [Optional] Populates {@link OpenAPI#info(Info)}
     * @return Populated {@link OpenAPI}
     */
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
     * <li>[Optional] {@code springdoc.info.external-documentation.description=}</li>
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
     * <li>[Optional] {@code springdoc.info.license.name=}</li>
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
     * <li>[Optional] {@code springdoc.info.contact.email=}</li>
     * <li>[Optional] {@code springdoc.info.contact.name=}</li>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = SPRING_DOC_PREFIX_CONTACT, name = "url")
    @ConfigurationProperties(prefix = SPRING_DOC_PREFIX_CONTACT)
    Contact apiContact() {
        return new Contact();
    }

    /**
     * Sets the Schema for these class types as "array" schemas
     * <li>{@code springdoc.use-array-schema=org.springframework.data.domain.Page}</li>
     *
     * @param arrayTypes List of full-class-path to the types to be added as "array schemas"
     */
    @ConditionalOnProperty("springdoc.use-array-schema")
    @Bean
    public ModelConverter arraySchemaModelConverter(@Value("${springdoc.use-array-schema}") final Set<Class<?>> arrayTypes) {
        return isEmpty(arrayTypes) ? null : new ModelConverter() {
            @Override
            public Schema<?> resolve(final AnnotatedType originalType, final ModelConverterContext context, final Iterator<ModelConverter> chain) {
                final Optional<AnnotatedType> iterableType = Optional.ofNullable(originalType)
                        .map(AnnotatedType::getType)
                        .map(Json.mapper()::constructType)
                        .filter(jt -> arrayTypes.contains(jt.getRawClass()))
                        .map(jt -> new AnnotatedType()
                                .type(jt.containedType(0))
                                .ctxAnnotations(originalType.getCtxAnnotations())
                                .parent(originalType.getParent())
                                .schemaProperty(originalType.isSchemaProperty())
                                .name(originalType.getName())
                                .resolveAsRef(originalType.isResolveAsRef())
                                .jsonViewAnnotation(originalType.getJsonViewAnnotation())
                                .propertyName(originalType.getPropertyName())
                                .skipOverride(true));

                if (iterableType.isEmpty()) {
                    return (chain.hasNext()) ? chain.next().resolve(originalType, context, chain) : null;
                } else {
                    return new ArraySchema().items(this.resolve(iterableType.get(), context, chain));
                }
            }
        };
    }

    /**
     * Adds "Error" schema. Sets shared {@link HttpStatus} codes on every API Operation Response.
     * <li>[Optional] {@code springdoc.shared-errors=INTERNAL_SERVER_ERROR}</li>
     */
    @Bean
    public OpenApiCustomiser sharedErrors(final Schema<?> errorSchema,
                                          @Value("${springdoc.shared-errors:}") final Set<HttpStatus> sharedErrors) {
        return openApi -> {
            openApi.schema(errorSchema.getName(), errorSchema);

            if (!isEmpty(sharedErrors)) {
                final Content content = new Content().addMediaType(APPLICATION_JSON_VALUE, new MediaType().schema(new Schema<>().$ref(errorSchema.getName())));

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
            }
        };
    }

    /**
     * Based on DefaultErrorAttributes
     */
    @ConditionalOnMissingBean
    @Bean
    Schema<?> errorSchema(@Value("${server.error.include-exception:false}") final boolean includeException,
                          @Value("${server.error.include-message:never}") final IncludeAttribute includeMessage,
                          @Value("${server.error.include-stacktrace:never}") final IncludeAttribute includeTrace,
                          @Value("${server.error.include-binding-errors:never}") final IncludeAttribute includeErrors) {

        final Schema<?> schema = new Schema<>().name("Error")
                .addProperty("path", new StringSchema())
                .addProperty("error", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("timestamp", new StringSchema().format("date-time"));

        if (includeException) {
            schema.addProperty("exception", new StringSchema());
        }
        setOptionalProperty(schema, "message", includeMessage);
        setOptionalProperty(schema, "errors", includeErrors);
        setOptionalProperty(schema, "trace", includeTrace);

        return schema;
    }

    private void setOptionalProperty(final Schema<?> schema, final String key, final IncludeAttribute includeAttribute) {
        if (!IncludeAttribute.NEVER.equals(includeAttribute)) {
            schema.addProperty(key, new StringSchema()
                    .description(IncludeAttribute.ON_PARAM.equals(includeAttribute) ? "Optional on parameter" : null));
        }
    }

    private String buildDescription(final BuildProperties properties) {
        final String description = Stream.of(
                        Optional.ofNullable(properties.get("description"))
                                .map(StringUtils::trimToNull)
                                .map(d -> "<div>" + d + "</div>"),
                        Optional.ofNullable(properties.getTime())
                                .map(t -> "<div>Created: " + t + "</div>"))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining("\n"));

        return StringUtils.trimToNull(description);
    }
}
