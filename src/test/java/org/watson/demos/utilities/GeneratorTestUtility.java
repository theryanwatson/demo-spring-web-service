package org.watson.demos.utilities;

import lombok.Builder;
import lombok.experimental.UtilityClass;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.Identifiable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

@UtilityClass
public class GeneratorTestUtility {

    private static final List<Locale> AVAILABLE_LOCALES = Arrays.stream(Locale.getAvailableLocales())
            .filter(l -> !(l.toString().contains("#") || l.toString().isEmpty()))
            .toList();
    private static final int CONTENT_COUNT = 10;

    public static List<Greeting> generateGreetings(final String content) {
        return IntStream.range(0, CONTENT_COUNT).boxed()
                .map(i -> Greeting.builder()
                        .locale(AVAILABLE_LOCALES.get(i % AVAILABLE_LOCALES.size()))
                        .content(content + " " + i))
                .map(Greeting.GreetingBuilder::build)
                .toList();
    }

    public static List<Identifiable> generateIdentifiable(final String content, final int count) {
        return IntStream.range(0, count).boxed()
                .map(i -> ExampleIdentifiable.builder()
                        .id(i)
                        .content(content + " " + i))
                .map(ExampleIdentifiable.ExampleIdentifiableBuilder::build)
                .map(Identifiable.class::cast)
                .toList();
    }

    @Builder
    private record ExampleIdentifiable(Integer id, String content) implements Identifiable {
        public Integer getId() {
            return id();
        }
    }
}
