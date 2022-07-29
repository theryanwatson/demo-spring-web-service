package org.watson.demos.configuration;

import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = SpringDocConfiguration.class, properties = {
        "spring.config.location=classpath:empty.properties"
})
class SpringDocConfigurationConditionalTest {

    @Resource
    private ApplicationContext context;

    @ValueSource(classes = {OpenAPI.class, Info.class, ExternalDocumentation.class, License.class, Contact.class, ModelConverter.class})
    @ParameterizedTest
    void beansNotCreatedPropertiesNotSet(final Class<?> clazz) {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(clazz), clazz.getName());
    }
}
