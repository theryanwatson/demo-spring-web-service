package org.watson.demos.configuration;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.watson.demos.configuration.SpringDocConfiguration.*;
import static org.watson.demos.configuration.SpringDocConfigurationTest.*;

@SpringBootTest(classes = SpringDocConfiguration.class, properties = {
        SPRING_DOC_PREFIX_CONTACT + ".email=" + CONTACT_EMAIL,
        SPRING_DOC_PREFIX_CONTACT + ".name=" + CONTACT_NAME,
        SPRING_DOC_PREFIX_CONTACT + ".url=" + CONTACT_URL,
        SPRING_DOC_PREFIX_EXTERNAL_DOCUMENTATION + ".description=" + EXTERNAL_DOC_DESC,
        SPRING_DOC_PREFIX_EXTERNAL_DOCUMENTATION + ".url=" + EXTERNAL_DOC_URL,
        SPRING_DOC_PREFIX_LICENSE + ".name=" + LICENSE_NAME,
        SPRING_DOC_PREFIX_LICENSE + ".url=" + LICENSE_URL,
        SPRING_DOC_PREFIX_INFO + ".terms-of-service=" + TERM_OF_SERVICE,
        "springdoc.use-array-schema=org.watson.demos.configuration.SpringDocConfigurationTest.TestArrayType"
})
@EnableConfigurationProperties
@ContextConfiguration(classes = {BuildPropertiesTestConfiguration.class, SpringDocConfiguration.class})
class SpringDocConfigurationTest {
    protected static final String CONTACT_EMAIL = "test@test.com";
    protected static final String CONTACT_NAME = "A Contact";
    protected static final String CONTACT_URL = "https://contact.info";
    protected static final String EXTERNAL_DOC_DESC = "An External Doc";
    protected static final String EXTERNAL_DOC_URL = "https://external-docs.info";
    protected static final String LICENSE_NAME = "A License";
    protected static final String LICENSE_URL = "https://license.info";
    protected static final String TERM_OF_SERVICE = "A Term of Service";
    protected static final String DESCRIPTION = "A Description";
    protected static final String NAME = "A Name";
    protected static final Instant TIME = Instant.now().truncatedTo(MILLIS);
    protected static final String VERSION = "A Version";

    @Resource
    private OpenAPI openAPI;
    @Resource
    private ModelConverter arraySchemaModelConverter;

    @Test
    void openApiInfoMatches() {
        assertThat(openAPI.getInfo().getTitle(), is(NAME));
        assertThat(openAPI.getInfo().getVersion(), is(VERSION));
        assertThat(openAPI.getInfo().getDescription(), containsString(DESCRIPTION));
        assertThat(openAPI.getInfo().getDescription(), containsString(TIME.toString()));
    }

    @Test
    void openApiInfoContactMatches() {
        assertThat(openAPI.getInfo().getContact().getEmail(), is(CONTACT_EMAIL));
        assertThat(openAPI.getInfo().getContact().getName(), is(CONTACT_NAME));
        assertThat(openAPI.getInfo().getContact().getUrl(), is(CONTACT_URL));
    }

    @Test
    void openApiExternalDocumentationMatches() {
        assertThat(openAPI.getExternalDocs().getDescription(), is(EXTERNAL_DOC_DESC));
        assertThat(openAPI.getExternalDocs().getUrl(), is(EXTERNAL_DOC_URL));
    }

    @Test
    void openApiInfoLicenseMatches() {
        assertThat(openAPI.getInfo().getLicense().getName(), is(LICENSE_NAME));
        assertThat(openAPI.getInfo().getLicense().getUrl(), is(LICENSE_URL));
    }

    @Test
    void openApiInfoTermsOfServiceMatches() {
        assertThat(openAPI.getInfo().getTermsOfService(), is(TERM_OF_SERVICE));
    }

    @Test
    void arraySchemaModelConverter_convertsSupportedType() {
        final Schema<?> schema = arraySchemaModelConverter.resolve(new AnnotatedType(TestArrayType.class), new ModelConverterContextImpl(List.of()), Collections.emptyIterator());

        assertThat(schema, notNullValue());
        assertThat(schema.getType(), is("array"));
    }

    @Test
    void arraySchemaModelConverter_doesNotConvertOtherTypes() {
        final Schema<?> schema = arraySchemaModelConverter.resolve(new AnnotatedType(TestUnmappedType.class), new ModelConverterContextImpl(List.of()), Collections.emptyIterator());

        assertThat(schema, nullValue());
    }

    @Test
    void sharedErrors() {
        // TODO Verify!
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
