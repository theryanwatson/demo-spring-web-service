package org.watson.demos.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Import;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = UnwrappedPageHttpMessageConverter.class)
@Import(ObjectMapper.class)
class UnwrappedPageHttpMessageConverterTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(UserConfigurations.of(UnwrappedPageHttpMessageConverter.class));

    @Resource
    private ObjectMapper objectMapper;
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
        assertThat(objectMapper.readValue(stream.toString(), new TypeReference<List<String>>() {})).isEqualTo(expected);
    }

    @Test
    void supportsAcceptsClasses() {
        assertThat(Stream.of(Page.class, PageImpl.class, GeoPage.class))
                .allMatch(c -> converter.canWrite(c, MediaType.APPLICATION_JSON));
    }

    @Test
    void supportRejectsNonPageClasses() {
        assertThat(Stream.of(List.class, String.class, Map.class, Integer.class, Streamable.class, Iterable.class))
                .noneMatch(c -> converter.canWrite(c, MediaType.APPLICATION_JSON));
    }

    @Test
    void canReadAlwaysFalse() {
        assertThat(Stream.of(Page.class, PageImpl.class, GeoPage.class, List.class, String.class, Map.class, Integer.class, Streamable.class, Iterable.class))
                .noneMatch(c -> converter.canRead(c, null));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void readAlwaysThrows() {
        assertThatThrownBy(() -> converter.read(null, null))
                .isInstanceOf(HttpMessageNotReadableException.class)
                .hasMessageContaining("Read not supported");
    }

    @Test
    void beanNotCreatedIfPropertyFalse() {
        contextRunner.withPropertyValues("server.response.unwrap.page=false")
                .run(c -> assertThat(c).doesNotHaveBean(UnwrappedPageHttpMessageConverter.class));
    }

    @Test
    void beanNotCreatedIfPropertyMissing() {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .run(c -> assertThat(c).doesNotHaveBean(UnwrappedPageHttpMessageConverter.class));
    }
}
