package org.watson.demos.advice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {UnwrappedPageResponseBodyAdvice.class, SpringDataWebProperties.class})
class UnwrappedPageResponseBodyAdviceConditionalBeanTest {

    @Resource
    private ApplicationContext context;

    @Test
    void beanNotCreatedIfHttpMessageConverterClassNotInScope() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(UnwrappedPageResponseBodyAdvice.class));
    }
}
