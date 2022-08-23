package org.watson.demos.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.geo.GeoPage;
import org.springframework.data.util.Streamable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class UnwrappedPageHttpMessageConverterTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Nested
    @SpringBootTest(classes = {UnwrappedPageHttpMessageConverter.class, ObjectMapper.class})
    class BeanEnabledTest {

        @Resource
        private HttpMessageConverter<Page<?>> converter;

        @Mock
        private HttpOutputMessage message;

        @Test
        void writeInternalCorrectlyUnwrapsAndMarshalsContents() throws IOException {
            final List<String> expected = List.of("a", "b", "c", "d", "e", "f");
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();

            when(message.getBody()).thenReturn(stream);
            when(message.getHeaders()).thenReturn(new HttpHeaders());

            converter.write(new PageImpl<>(expected), MediaType.APPLICATION_JSON, message);
            assertThat(OBJECT_MAPPER.readValue(stream.toString(), List.class),
                    is(expected));
        }

        @Test
        void supportsAcceptsClasses() {
            assertThat(Stream.of(Page.class, PageImpl.class, GeoPage.class)
                    .allMatch(c -> converter.canWrite(c, MediaType.APPLICATION_JSON)), is(true));
        }

        @Test
        void supportRejectsNonPageClasses() {
            assertThat(Stream.of(List.class, String.class, Map.class, Integer.class, Streamable.class, Iterable.class)
                    .noneMatch(c -> converter.canWrite(c, MediaType.APPLICATION_JSON)), is(true));
        }

        @Test
        void canReadAlwaysFalse() {
            assertThat(Stream.of(Page.class, PageImpl.class, GeoPage.class, List.class, String.class, Map.class, Integer.class, Streamable.class, Iterable.class)
                    .noneMatch(c -> converter.canRead(c, null)), is(true));
        }

        @SuppressWarnings("ConstantConditions")
        @Test
        void readAlwaysThrows() {
            assertThrows(HttpMessageNotReadableException.class, () -> converter.read(null, null));
        }
    }

    @Nested
    @SpringBootTest(classes = {UnwrappedPageHttpMessageConverter.class, ObjectMapper.class}, properties = "server.response.unwrap.page=false")
    class BeanDisabledFalseTest {
        @Resource
        private ApplicationContext context;

        @Test
        void beanNotCreatedIfPropertyFalse() {
            assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(UnwrappedPageHttpMessageConverter.class));
        }
    }

    @Nested
    @SpringBootTest(classes = {UnwrappedPageHttpMessageConverter.class, ObjectMapper.class}, properties = "spring.config.location=classpath:empty.properties")
    class BeanDisabledMissingTest {
        @Resource
        private ApplicationContext context;

        @Test
        void beanNotCreatedIfPropertyMissing() {
            assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(UnwrappedPageHttpMessageConverter.class));
        }
    }
}
