package org.watson.demos.utilities;

import lombok.Builder;
import lombok.experimental.UtilityClass;
import org.watson.demos.models.Greeting;
import org.watson.demos.models.Identifiable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class GeneratorTestUtility {

    private static final List<Locale> AVAILABLE_LOCALES = Arrays.stream(Locale.getAvailableLocales())
            .filter(l -> !(l.toString().contains("#") || l.toString().isEmpty()))
            .collect(Collectors.toUnmodifiableList());
    private static final int CONTENT_COUNT = 10;

    public static List<Greeting> generateGreetings(final String content) {
        return IntStream.range(0, CONTENT_COUNT).boxed()
                .map(i -> Greeting.builder()
                        .locale(AVAILABLE_LOCALES.get(i % AVAILABLE_LOCALES.size()))
                        .content(content + " " + i))
                .map(Greeting.GreetingBuilder::build)
                .collect(Collectors.toUnmodifiableList());
    }

    public static List<Identifiable<?>> generateIdentifiable(final String content, final int count) {
        return IntStream.range(0, count).boxed()
                .map(i -> ExampleIdentifiable.builder()
                        .id(i)
                        .content(content + " " + i))
                .map(ExampleIdentifiable.ExampleIdentifiableBuilder::build)
                .collect(Collectors.toUnmodifiableList());
    }

    public static List<Identifiable<?>> generateIdentifiable(final String content) {
        return generateIdentifiable(content, CONTENT_COUNT);
    }

    @Builder(toBuilder = true)
    @lombok.Value
    private static class ExampleIdentifiable implements Identifiable<Integer> {
        Integer id;
        String content;
    }
}
