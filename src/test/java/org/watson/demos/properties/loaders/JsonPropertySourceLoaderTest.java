package org.watson.demos.properties.loaders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = JsonPropertySourceLoader.class, properties = {
        "spring.config.additional-location=" +
                "classpath:PropertySourceLoaderTest/more.json," +
                "classpath:PropertySourceLoaderTest/more.properties," +
                "classpath:PropertySourceLoaderTest/more.yml",
        "spring.config.location=classpath:PropertySourceLoaderTest/overrideable.properties",
})
class JsonPropertySourceLoaderTest {

    @Value("${this.json.value:#{null}}")
    private String thisJsonValue;
    @Value("${that.json.value:#{null}}")
    private String thatJsonValue;

    @Test
    void testJsonValues() {
        assertThat(thisJsonValue).isEqualTo("someJsonValue");
        assertThat(thatJsonValue).isEqualTo("anotherJsonValue");
    }
}
