package org.watson.demos.advice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = UnwrappedExceptionResolver.class, properties = {
        "spring.config.location=classpath:empty.properties"
})
class UnwrappedExceptionResolverConditionalTest {

    @Resource
    private ApplicationContext context;

    @Test
    void beanNotCreatedPropertiesNotSet() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(UnwrappedExceptionResolver.class));
    }
}
