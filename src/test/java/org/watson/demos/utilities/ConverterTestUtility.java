package org.watson.demos.utilities;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@UtilityClass
public class ConverterTestUtility {
    public static String toQueryString(final Pageable pageable) {
        final List<String> queryParameters = new ArrayList<>();
        if (pageable.isPaged()) {
            queryParameters.add("page=" + pageable.getPageNumber());
            queryParameters.add("size=" + pageable.getPageSize());
        }
        pageable.getSort().get()
                .map(s -> "sort=" + s.getProperty() + (s.isDescending() ? ",desc" : ""))
                .forEach(queryParameters::add);
        return queryParameters.stream()
                .collect(joiningToQueryString());
    }

    public static <E, V> String toQueryString(final Collection<E> entries, final Function<E, V> entryMapper, final String key) {
        return entries.stream()
                .map(entryMapper)
                .map(value -> key + "=" + value)
                .collect(joiningToQueryString());
    }

    private static Collector<CharSequence, ?, String> joiningToQueryString() {
        return Collectors.joining("&", "?", "");
    }
}
