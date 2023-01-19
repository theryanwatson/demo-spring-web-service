package org.watson.demos.configurations;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.watson.demos.configurations.SpringDocConfiguration.SPRING_DOC_PREFIX_CONTACT;
import static org.watson.demos.configurations.SpringDocConfiguration.SPRING_DOC_PREFIX_EXTERNAL_DOCUMENTATION;
import static org.watson.demos.configurations.SpringDocConfiguration.SPRING_DOC_PREFIX_INFO;
import static org.watson.demos.configurations.SpringDocConfiguration.SPRING_DOC_PREFIX_LICENSE;
import static org.watson.demos.configurations.SpringDocConfigurationTest.BuildPropertiesTestConfiguration;
import static org.watson.demos.configurations.SpringDocConfigurationTest.CONTACT_EMAIL;
import static org.watson.demos.configurations.SpringDocConfigurationTest.CONTACT_NAME;
import static org.watson.demos.configurations.SpringDocConfigurationTest.CONTACT_URL;
import static org.watson.demos.configurations.SpringDocConfigurationTest.ERROR_SCHEMA_NAME;
import static org.watson.demos.configurations.SpringDocConfigurationTest.EXTERNAL_DOC_DESC;
import static org.watson.demos.configurations.SpringDocConfigurationTest.EXTERNAL_DOC_URL;
import static org.watson.demos.configurations.SpringDocConfigurationTest.LICENSE_NAME;
import static org.watson.demos.configurations.SpringDocConfigurationTest.LICENSE_URL;
import static org.watson.demos.configurations.SpringDocConfigurationTest.SHARED_ERRORS_STRING;
import static org.watson.demos.configurations.SpringDocConfigurationTest.TERM_OF_SERVICE;

@SpringBootTest(classes = SpringDocConfiguration.class, properties = {
        SPRING_DOC_PREFIX_CONTACT + ".email=" + CONTACT_EMAIL,
        SPRING_DOC_PREFIX_CONTACT + ".name=" + CONTACT_NAME,
        SPRING_DOC_PREFIX_CONTACT + ".url=" + CONTACT_URL,
        SPRING_DOC_PREFIX_EXTERNAL_DOCUMENTATION + ".description=" + EXTERNAL_DOC_DESC,
        SPRING_DOC_PREFIX_EXTERNAL_DOCUMENTATION + ".url=" + EXTERNAL_DOC_URL,
        SPRING_DOC_PREFIX_LICENSE + ".name=" + LICENSE_NAME,
        SPRING_DOC_PREFIX_LICENSE + ".url=" + LICENSE_URL,
        SPRING_DOC_PREFIX_INFO + ".terms-of-service=" + TERM_OF_SERVICE,
        "springdoc.shared-errors=" + SHARED_ERRORS_STRING,
        "springdoc.error.schema-name=" + ERROR_SCHEMA_NAME,
        "springdoc.use-array-schema=org.watson.demos.configuration.SpringDocConfigurationTest.TestArrayType",
})
@EnableConfigurationProperties
@ContextConfiguration(classes = {BuildPropertiesTestConfiguration.class, SpringDocConfiguration.class})
class SpringDocConfigurationTest {
    static final String CONTACT_EMAIL = "test@test.com";
    static final String CONTACT_NAME = "A Contact";
    static final String CONTACT_URL = "https://contact.info";
    static final String EXTERNAL_DOC_DESC = "An External Doc";
    static final String EXTERNAL_DOC_URL = "https://external-docs.info";
    static final String LICENSE_NAME = "A License";
    static final String LICENSE_URL = "https://license.info";
    static final String TERM_OF_SERVICE = "A Term of Service";
    static final String SHARED_ERRORS_STRING = "NOT_FOUND,BAD_REQUEST,INTERNAL_SERVER_ERROR";
    static final String ERROR_SCHEMA_NAME = "TestError";
    private static final Set<String> SHARED_ERROR_CODES = Set.of("404", "400", "500");
    private static final String ERROR_SCHEMA_REF = "#/components/schemas/" + ERROR_SCHEMA_NAME;
    private static final String DESCRIPTION = "A Description";
    private static final String NAME = "A Name";
    private static final Instant TIME = Instant.now().truncatedTo(MILLIS);
    private static final String VERSION = "A Version";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(SpringDocConfiguration.class);

    @Resource
    private OpenAPI openAPI;
    @Resource
    private Schema<?> errorSchema;
    @Resource
    private OpenApiCustomiser sharedErrorsCustomizer;
    @Resource
    private ModelConverter arraySchemaModelConverter;

    @Test
    void openApiInfoMatches() {
        assertThat(openAPI.getInfo().getTitle()).isEqualTo(NAME);
        assertThat(openAPI.getInfo().getVersion()).isEqualTo(VERSION);
        assertThat(openAPI.getInfo().getDescription()).contains(DESCRIPTION);
        assertThat(openAPI.getInfo().getDescription()).contains(TIME.toString());
    }

    @Test
    void openApiInfoContactMatches() {
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo(CONTACT_EMAIL);
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo(CONTACT_NAME);
        assertThat(openAPI.getInfo().getContact().getUrl()).isEqualTo(CONTACT_URL);
    }

    @Test
    void openApiExternalDocumentationMatches() {
        assertThat(openAPI.getExternalDocs().getDescription()).isEqualTo(EXTERNAL_DOC_DESC);
        assertThat(openAPI.getExternalDocs().getUrl()).isEqualTo(EXTERNAL_DOC_URL);
    }

    @Test
    void openApiInfoLicenseMatches() {
        assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo(LICENSE_NAME);
        assertThat(openAPI.getInfo().getLicense().getUrl()).isEqualTo(LICENSE_URL);
    }

    @Test
    void openApiInfoTermsOfServiceMatches() {
        assertThat(openAPI.getInfo().getTermsOfService()).isEqualTo(TERM_OF_SERVICE);
    }

    @Test
    void arraySchemaModelConverter_convertsSupportedType() {
        final Schema<?> schema = arraySchemaModelConverter.resolve(new AnnotatedType(TestArrayType.class), new ModelConverterContextImpl(List.of()), Collections.emptyIterator());

        assertThat(schema).isNotNull();
        assertThat(schema.getType()).isEqualTo("array");
    }

    @Test
    void arraySchemaModelConverter_doesNotConvertOtherTypes() {
        final Schema<?> schema = arraySchemaModelConverter.resolve(new AnnotatedType(TestUnmappedType.class), new ModelConverterContextImpl(List.of()), Collections.emptyIterator());

        assertThat(schema).isNull();
    }

    @Test
    void errorSchemaValues() {
        assertThat(errorSchema).isNotNull();
        assertThat(errorSchema.getName()).isEqualTo(ERROR_SCHEMA_NAME);
        assertThat(errorSchema.getProperties()).containsKeys("path", "error", "status", "timestamp");
    }

    @Test
    void sharedErrorsCustomizer_addsErrorSchema() {
        final OpenAPI api = new OpenAPI();

        sharedErrorsCustomizer.customise(api);

        assertThat(api.getComponents()).isNotNull();
        assertThat(api.getComponents().getSchemas())
                .containsKey(ERROR_SCHEMA_NAME)
                .containsValue(errorSchema);
    }

    @Test
    void sharedErrorsCustomizer_addsErrorResponses() {
        final OpenAPI api = new OpenAPI();
        IntStream.range(0, 10).forEach(i -> api.path("path-" + i, new PathItem().get(new Operation().responses(new ApiResponses()))));

        sharedErrorsCustomizer.customise(api);

        List<ApiResponses> apiResponses = api.getPaths().values().stream()
                .map(PathItem::readOperations)
                .flatMap(Collection::stream)
                .map(Operation::getResponses)
                .collect(Collectors.toUnmodifiableList());

        assertThat(apiResponses).allSatisfy(response -> assertThat(response).containsOnlyKeys(SHARED_ERROR_CODES));

        assertThat(apiResponses.stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .map(ApiResponse::getContent)
                .map(Map::values)
                .flatMap(Collection::stream)
                .map(MediaType::getSchema)
                .map(Schema::get$ref))
                .allMatch(ERROR_SCHEMA_REF::equals);

    }

    @ValueSource(classes = {OpenAPI.class, Info.class, ExternalDocumentation.class, License.class, Contact.class, ModelConverter.class})
    @ParameterizedTest
    void beansNotCreatedPropertiesNotSet(final Class<?> clazz) {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .withBean("errorSchema", Schema.class, Schema::new)
                .run(context -> assertThatThrownBy(() -> context.getBean(clazz)).isInstanceOf(NoSuchBeanDefinitionException.class));
    }

    @ValueSource(classes = {OpenAPI.class, Info.class})
    @ParameterizedTest
    void beansCreatedPropertiesSet(final Class<?> clazz) {
        contextRunner.withUserConfiguration(BuildPropertiesTestConfiguration.class)
                .withBean("errorSchema", Schema.class, Schema::new)
                .run(context -> assertThat(context).getBean(clazz).isNotNull());
    }

    @TestConfiguration
    static class BuildPropertiesTestConfiguration {
        @Bean
        BuildProperties buildProperties() {
            return new BuildProperties(new Properties() {{
                put("description", DESCRIPTION);
                put("name", NAME);
                put("time", TIME.toString());
                put("version", VERSION);
            }});
        }
    }

    static class TestArrayType {}

    static class TestUnmappedType {}
}
