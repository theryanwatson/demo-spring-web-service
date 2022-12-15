package org.watson.demos.utilities;

import lombok.Builder;
import lombok.experimental.UtilityClass;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.Identifiable;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class GeneratorTestUtility {

    private static final Locale[] AVAILABLE_LOCALES = Locale.getAvailableLocales();
    private static final int CONTENT_COUNT = 10;

    public static List<Greeting> generateGreetings(final String content) {
        return IntStream.range(0, CONTENT_COUNT).boxed()
                .map(i -> Greeting.builder()
                        .locale(AVAILABLE_LOCALES[i % AVAILABLE_LOCALES.length])
                        .content(content + " " + i))
                .map(Greeting.GreetingBuilder::build)
                .collect(Collectors.toUnmodifiableList());
    }

    public static List<Identifiable> generateIdentifiable(final String content, final int count) {
        return IntStream.range(0, count).boxed()
                .map(i -> ExampleIdentifiable.builder()
                        .id(i)
                        .content(content + " " + i))
                .map(ExampleIdentifiable.ExampleIdentifiableBuilder::build)
                .collect(Collectors.toUnmodifiableList());
    }

    public static List<Identifiable> generateIdentifiable(final String content) {
        return generateIdentifiable(content, CONTENT_COUNT);
    }

    @Builder(toBuilder = true)
    @lombok.Value
    private static class ExampleIdentifiable implements Identifiable {
        Integer id;
        String content;
    }

}
