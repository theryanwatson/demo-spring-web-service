package org.watson.demos.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.watson.demos.converters.UnwrappedPageHttpMessageConverter;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.watson.demos.advice.UnwrappedPageResponseBodyAdviceTest.PAGE_HEADER_PREFIX;
import static org.watson.demos.advice.UnwrappedPageResponseBodyAdviceTest.PAGE_NUMBER_PARAMETER;

@SpringBootTest(classes = UnwrappedPageResponseBodyAdvice.class, properties = {
        "spring.data.web.pageable.header-prefix=" + PAGE_HEADER_PREFIX,
        "spring.data.web.pageable.page-parameter=" + PAGE_NUMBER_PARAMETER,
})
@ImportAutoConfiguration(SpringDataWebAutoConfiguration.class)
class UnwrappedPageResponseBodyAdviceTest {
    static final String PAGE_HEADER_PREFIX = "Page-Header-Prefix-";
    static final String PAGE_NUMBER_PARAMETER = "page-number-parameter";
    private static final String FAKE_URI = "https://www.fake.fake:5150/things/cool/" + PAGE_NUMBER_PARAMETER;
    private static final List<String> EXPECTED = List.of("a", "b", "c", "d", "e", "f");

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(UserConfigurations.of(UnwrappedExceptionResolver.class));

    private final HttpHeaders headers = new HttpHeaders();

    @Mock
    private ServerHttpRequest request;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private SpringDataWebProperties webProperties;
    @Mock
    private SpringDataWebProperties.Pageable pageable;
    @MockBean
    private UnwrappedPageHttpMessageConverter converter;

    @SpyBean
    private UnwrappedPageResponseBodyAdvice advice;

    @BeforeEach
    void setup() {
        when(response.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create(FAKE_URI));
    }

    @Test
    void adviceSupportIsLimitedToInternalConverter() {
        assertThat(advice.supports(null, StringHttpMessageConverter.class)).isFalse();
        assertThat(advice.supports(null, converter.getClass())).isTrue();
    }

    @Test
    void pageResultPassesObjectThrough() {
        final Page<String> page = new PageImpl<>(EXPECTED);

        assertThat(advice.beforeBodyWrite(page, null, null, null, request, response))
                .isSameAs(page);
    }

    @Test
    void pageHeadersSetCorrectlyNoResults() {
        forEachPage(advice, 10, Sort.unsorted(), Collections.emptyList(), (page, totalPages) ->
                assertPageHeaders("Index", page, 0, 10, 0, 0));
    }

    @Test
    void pageHeadersSetCorrectly() {
        forEachPage(advice, 200, Sort.unsorted(), EXPECTED, (page, totalPages) ->
                assertPageHeaders("Index", page, 0, 200, totalPages, EXPECTED.size()));
    }

    @Test
    void pageHeadersSetCorrectlyPerPage() {
        forEachPage(advice, 2, Sort.unsorted(), EXPECTED, (page, totalPages) ->
                assertPageHeaders("Index", page, 0, 2, totalPages, EXPECTED.size()));
    }

    @Test
    void pageHeadersSetCorrectlyPerPageWhenOneIndex() {
        when(webProperties.getPageable()).thenReturn(pageable);
        when(pageable.getPageParameter()).thenReturn(PAGE_NUMBER_PARAMETER);
        when(pageable.isOneIndexedParameters()).thenReturn(true);

        final UnwrappedPageResponseBodyAdvice advice = spy(new UnwrappedPageResponseBodyAdvice(PAGE_HEADER_PREFIX, webProperties));

        forEachPage(advice, 2, Sort.unsorted(), EXPECTED, (page, totalPages) ->
                assertPageHeaders("Number", page, 1, 2, totalPages, EXPECTED.size()));
    }

    @Test
    void linkSortSetCorrectlyPerUnsortedPage() {
        final Sort sort = Sort.unsorted();
        forEachPage(advice, 3, sort, EXPECTED, (page, totalPages) ->
                assertThat(headers.getOrEmpty(PAGE_HEADER_PREFIX + "Sort").get(0)).isEqualTo(String.valueOf(sort)));
    }

    @Test
    void linkSortSetCorrectlyPerPage() {
        final Sort sort = Sort.by(Sort.Direction.ASC, "property1").and(Sort.by("property2"));
        forEachPage(advice, 3, sort, EXPECTED, (page, totalPages) ->
                assertThat(headers.getOrEmpty(PAGE_HEADER_PREFIX + "Sort").get(0)).isEqualTo(String.valueOf(sort)));
    }

    @Test
    void linkHeadersSetCorrectlyPerPageNoArgs() {
        forEachPage(advice, 2, Sort.unsorted(), EXPECTED, (page, totalPages) ->
                assertLinkHeaders(FAKE_URI + "?", page, 0, totalPages));
    }

    @Test
    void linkHeadersSetCorrectlyPerPageWithArgsNoPage() {
        final String requestUri = FAKE_URI + "?things=cool&arg=" + PAGE_NUMBER_PARAMETER;
        when(request.getURI()).thenReturn(URI.create(requestUri));

        forEachPage(advice, 2, Sort.unsorted(), EXPECTED, (page, totalPages) ->
                assertLinkHeaders(requestUri + "&", page, 0, totalPages));
    }

    @Test
    void linkHeadersSetCorrectlyPerPageWithPageArgs() {
        final String requestUri = FAKE_URI + "?things=cool&arg=yup";
        when(response.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create(requestUri + "&" + PAGE_NUMBER_PARAMETER + "=13"));

        forEachPage(advice, 2, Sort.unsorted(), EXPECTED, (page, totalPages) ->
                assertLinkHeaders(requestUri + "&", page, 0, totalPages));
    }

    @Test
    void linkHeadersSetCorrectlyPerPageWhenOneIndex() {
        when(webProperties.getPageable()).thenReturn(pageable);
        when(pageable.getPageParameter()).thenReturn(PAGE_NUMBER_PARAMETER);
        when(pageable.isOneIndexedParameters()).thenReturn(true);

        final UnwrappedPageResponseBodyAdvice advice = spy(new UnwrappedPageResponseBodyAdvice(PAGE_HEADER_PREFIX, webProperties));

        forEachPage(advice, 2, Sort.unsorted(), EXPECTED, (page, totalPages) ->
                assertLinkHeaders(FAKE_URI + "?", page, 1, totalPages));
    }

    private void assertPageHeaders(final String indexFieldName, final int page, final int pageModifier, final int pageSize, final int totalPages, final int totalElements) {
        Map.of(
                        indexFieldName, page + pageModifier,
                        "Size", pageSize,
                        "Total-Pages", totalPages,
                        "Total-Elements", totalElements)
                .forEach((fieldName, value) -> {
                    List<String> actualHeader = headers.get(PAGE_HEADER_PREFIX + fieldName);
                    assertThat(actualHeader).isNotNull();
                    assertThat(actualHeader.get(0)).withFailMessage(fieldName).isEqualTo(String.valueOf(value));
                });
    }

    private void assertLinkHeaders(final String uriPrefix, final int page, final int pageModifier, final int totalPages) {
        assertThat(findLink("self").orElseThrow(AssertionError::new))
                .contains(uriPrefix + PAGE_NUMBER_PARAMETER + "=" + (page + pageModifier));

        if (page == 0) {
            assertThat(findLink("first")).isEmpty();
            assertThat(findLink("prev")).isEmpty();
        } else {
            assertThat(findLink("first").orElseThrow(AssertionError::new))
                    .contains(uriPrefix + PAGE_NUMBER_PARAMETER + "=" + pageModifier);
            assertThat(findLink("prev").orElseThrow(AssertionError::new))
                    .contains(uriPrefix + PAGE_NUMBER_PARAMETER + "=" + (page - 1 + pageModifier));
        }

        if (page == totalPages - 1) {
            assertThat(findLink("next")).isEmpty();
            assertThat(findLink("last")).isEmpty();
        } else {
            assertThat(findLink("next").orElseThrow(AssertionError::new))
                    .contains(uriPrefix + PAGE_NUMBER_PARAMETER + "=" + (page + 1 + pageModifier));

            assertThat(findLink("last").orElseThrow(AssertionError::new))
                    .contains(uriPrefix + PAGE_NUMBER_PARAMETER + "=" + (totalPages - 1 + pageModifier));
        }
    }

    private Optional<String> findLink(final String self) {
        final List<String> links = headers.get(HttpHeaders.LINK);
        return links == null ? Optional.empty() : links.stream().filter(f -> f.contains(self)).findFirst();
    }

    private void forEachPage(final UnwrappedPageResponseBodyAdvice advice, final int pageSize, final Sort sort, final List<String> expected, final BiConsumer<Integer, Integer> assertFunction) {
        int page = 0;
        final int totalPages = (int) Math.ceil((double) expected.size() / (double) pageSize);

        do {
            headers.clear();
            Page<String> pageBody = new PageImpl<>(expected.subList(page, Math.min(page + pageSize, expected.size())),
                    PageRequest.of(page, pageSize, sort), expected.size());

            assertThat(advice.beforeBodyWrite(pageBody, null, null, null, request, response))
                    .isEqualTo(pageBody);

            assertFunction.accept(page, totalPages);
        } while (++page < totalPages);

        verify(advice, times(totalPages == 0 ? 1 : totalPages)).beforeBodyWrite(any(), any(), any(), any(), any(), any());
    }

    @Test
    void beanNotCreatedIfHttpMessageConverterClassNotInScope() {
        contextRunner.withPropertyValues("spring.config.location=classpath:empty.properties")
                .run(context -> assertThat(context).doesNotHaveBean(UnwrappedPageResponseBodyAdvice.class));
    }
}
