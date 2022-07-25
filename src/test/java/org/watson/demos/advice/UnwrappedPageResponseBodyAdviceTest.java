package org.watson.demos.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
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
        assertThat(advice.supports(null, StringHttpMessageConverter.class), is(false));
        assertThat(advice.supports(null, converter.getClass()), is(true));
    }

    @Test
    void pageResultPassesObjectThrough() {
        Page<String> page = new PageImpl<>(EXPECTED);

        assertThat(advice.beforeBodyWrite(page, null, null, null, request, response),
                sameInstance(page));
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

        UnwrappedPageResponseBodyAdvice advice = spy(new UnwrappedPageResponseBodyAdvice(PAGE_HEADER_PREFIX, webProperties));

        forEachPage(advice, 2, Sort.unsorted(), EXPECTED, (page, totalPages) ->
                assertPageHeaders("Number", page, 1, 2, totalPages, EXPECTED.size()));
    }

    @Test
    void linkSortSetCorrectlyPerUnsortedPage() {
        Sort sort = Sort.unsorted();
        forEachPage(advice, 3, sort, EXPECTED, (page, totalPages) ->
                assertThat(headers.getOrEmpty(PAGE_HEADER_PREFIX + "Sort").get(0), is(String.valueOf(sort))));
    }

    @Test
    void linkSortSetCorrectlyPerPage() {
        Sort sort = Sort.by(Sort.Direction.ASC, "property1").and(Sort.by("property2"));
        forEachPage(advice, 3, sort, EXPECTED, (page, totalPages) ->
                assertThat(headers.getOrEmpty(PAGE_HEADER_PREFIX + "Sort").get(0), is(String.valueOf(sort))));
    }

    @Test
    void linkHeadersSetCorrectlyPerPageNoArgs() {
        forEachPage(advice, 2, Sort.unsorted(), EXPECTED, (page, totalPages) ->
                assertLinkHeaders(FAKE_URI + "?", page, 0, totalPages));
    }

    @Test
    void linkHeadersSetCorrectlyPerPageWithArgsNoPage() {
        String requestUri = FAKE_URI + "?things=cool&arg=" + PAGE_NUMBER_PARAMETER;
        when(request.getURI()).thenReturn(URI.create(requestUri));

        forEachPage(advice, 2, Sort.unsorted(), EXPECTED, (page, totalPages) ->
                assertLinkHeaders(requestUri + "&", page, 0, totalPages));
    }

    @Test
    void linkHeadersSetCorrectlyPerPageWithPageArgs() {
        String requestUri = FAKE_URI + "?things=cool&arg=yup";
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

        UnwrappedPageResponseBodyAdvice advice = spy(new UnwrappedPageResponseBodyAdvice(PAGE_HEADER_PREFIX, webProperties));

        forEachPage(advice, 2, Sort.unsorted(), EXPECTED, (page, totalPages) ->
                assertLinkHeaders(FAKE_URI + "?", page, 1, totalPages));
    }

    private void assertPageHeaders(String indexFieldName, int page, int pageModifier, int pageSize, int totalPages, int totalElements) {
        Map.of(
                        indexFieldName, page + pageModifier,
                        "Size", pageSize,
                        "Total-Pages", totalPages,
                        "Total-Elements", totalElements)
                .forEach((fieldName, value) -> {
                    List<String> actualHeader = headers.get(PAGE_HEADER_PREFIX + fieldName);
                    assertThat(fieldName, actualHeader, is(notNullValue()));
                    assertThat(fieldName, actualHeader.get(0), is(String.valueOf(value)));
                });
    }

    private void assertLinkHeaders(String uriPrefix, int page, int pageModifier, int totalPages) {
        assertThat(findLink("self").orElseThrow(AssertionError::new),
                containsString(uriPrefix + PAGE_NUMBER_PARAMETER + "=" + (page + pageModifier)));

        if (page == 0) {
            assertThat(findLink("first"), is(Optional.empty()));
            assertThat(findLink("prev"), is(Optional.empty()));
        } else {
            assertThat(findLink("first").orElseThrow(AssertionError::new),
                    containsString(uriPrefix + PAGE_NUMBER_PARAMETER + "=" + pageModifier));
            assertThat(findLink("prev").orElseThrow(AssertionError::new),
                    containsString(uriPrefix + PAGE_NUMBER_PARAMETER + "=" + (page - 1 + pageModifier)));
        }

        if (page == totalPages - 1) {
            assertThat(findLink("next"), is(Optional.empty()));
            assertThat(findLink("last"), is(Optional.empty()));
        } else {
            assertThat(findLink("next").orElseThrow(AssertionError::new),
                    containsString(uriPrefix + PAGE_NUMBER_PARAMETER + "=" + (page + 1 + pageModifier)));

            assertThat(findLink("last").orElseThrow(AssertionError::new),
                    containsString(uriPrefix + PAGE_NUMBER_PARAMETER + "=" + (totalPages - 1 + pageModifier)));
        }
    }

    private Optional<String> findLink(String self) {
        List<String> links = headers.get(HttpHeaders.LINK);
        return links == null ? Optional.empty() : links.stream().filter(f -> f.contains(self)).findFirst();
    }

    private void forEachPage(UnwrappedPageResponseBodyAdvice advice, int pageSize, Sort sort, List<String> expected, BiConsumer<Integer, Integer> assertFunction) {
        int page = 0;
        int totalPages = (int) Math.ceil((double) expected.size() / (double) pageSize);

        do {
            headers.clear();
            Page<String> pageBody = new PageImpl<>(expected.subList(page, Math.min(page + pageSize, expected.size())),
                    PageRequest.of(page, pageSize, sort), expected.size());

            assertThat(advice.beforeBodyWrite(pageBody, null, null, null, request, response),
                    is(pageBody));

            assertFunction.accept(page, totalPages);
        } while (++page < totalPages);

        verify(advice, times(totalPages == 0 ? 1 : totalPages)).beforeBodyWrite(any(), any(), any(), any(), any(), any());
    }
}
