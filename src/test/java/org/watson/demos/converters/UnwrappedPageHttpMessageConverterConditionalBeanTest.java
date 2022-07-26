package org.watson.demos.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {UnwrappedPageHttpMessageConverter.class, ObjectMapper.class}, properties = "server.response.unwrap.page=false")
class UnwrappedPageHttpMessageConverterConditionalBeanTest {
    @Resource
    private ApplicationContext context;

    @Test
    void beanNotCreatedIfPropertyFalse() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(UnwrappedPageHttpMessageConverter.class));
    }
}
