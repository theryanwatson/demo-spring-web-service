package org.watson.demos.advice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.watson.demos.converters.UnwrappedPageHttpMessageConverter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Adds {@link Page} data to the response header to allow the {@link Page} body to be unwrapped without losing any
 * information. Used in conjunction with {@link UnwrappedPageHttpMessageConverter}, which is enabled
 * through a Spring property.<p/>
 * Adds the following headers to the response, with the configured prefix (default: {@value #DEFAULT_PAGE_PREFIX}):<ul>
 * <li>"Page-Size"</li>
 * <li>"Page-Sort"</li>
 * <li>"Page-Index" || "Page-Number" (If {@link SpringDataWebProperties.Pageable#isOneIndexedParameters()} is true)</li>
 * <li>"Page-Total-Pages"</li>
 * <li>"Page-Total-Elements"</li>
 * <li>{@value HttpHeaders#LINK} (In accordance with <a href="https://tools.ietf.org/html/rfc5988#page-6">RFC 5988</a>, with relation types (rel) of self, next, prev, and last)</li>
 * </ul>
 * <strong>Configure the header prefix from default {@value #DEFAULT_PAGE_PREFIX} (or set to empty-string) by setting Spring property:</strong><blockquote>spring.data.web.pageable.header-prefix=New-Prefix-</blockquote>
 *
 * @see UnwrappedPageHttpMessageConverter
 * @see <a href="https://tools.ietf.org/html/rfc5988#page-6">RFC 5988</a>
 */
@ConditionalOnWebApplication
@ConditionalOnBean(UnwrappedPageHttpMessageConverter.class)
@ControllerAdvice(annotations = RestController.class)
public class UnwrappedPageResponseBodyAdvice implements ResponseBodyAdvice<Page<?>> {
    private static final String DEFAULT_PAGE_PREFIX = "Page-";
    private static final String PAGE_NUMBER_REPLACE_TOKEN = "##PAGE_NUMBER##";
    private static final Pattern PAGE_NUMBER_REPLACE_PATTERN = Pattern.compile(PAGE_NUMBER_REPLACE_TOKEN, Pattern.LITERAL);

    private final String pageParameter;
    private final String pageQueryReplaceToken;
    private final int indexOffset;
    private final String pageSizeHeader;
    private final String pageSortHeader;
    private final String pageIndexHeader;
    private final String pageTotalHeader;
    private final String pageElementsHeader;

    public UnwrappedPageResponseBodyAdvice(@Value("${spring.data.web.pageable.header-prefix:" + DEFAULT_PAGE_PREFIX + "}") String pageHeaderPrefix,
                                           SpringDataWebProperties webProperties) {
        final boolean isOneIndexed = webProperties.getPageable().isOneIndexedParameters();

        this.pageParameter = webProperties.getPageable().getPageParameter() + "=";
        this.pageQueryReplaceToken = pageParameter + PAGE_NUMBER_REPLACE_TOKEN;
        this.indexOffset = isOneIndexed ? 1 : 0;

        this.pageSizeHeader = pageHeaderPrefix + "Size";
        this.pageSortHeader = pageHeaderPrefix + "Sort";
        this.pageIndexHeader = pageHeaderPrefix + (isOneIndexed ? "Number" : "Index");
        this.pageTotalHeader = pageHeaderPrefix + "Total-Pages";
        this.pageElementsHeader = pageHeaderPrefix + "Total-Elements";
    }

    @Override
    public boolean supports(final @Nullable MethodParameter ignored, final @NonNull Class<? extends HttpMessageConverter<?>> aClass) {
        return UnwrappedPageHttpMessageConverter.class.isAssignableFrom(aClass);
    }

    @Override
    public Page<?> beforeBodyWrite(final Page<?> page, final @Nullable MethodParameter ignored1, final @Nullable MediaType ignored2, final @Nullable Class<? extends HttpMessageConverter<?>> ignored3,
                                   final @NonNull ServerHttpRequest request, final @NonNull ServerHttpResponse response) {
        if (page != null) { // find-bugs null-check
            response.getHeaders().setAll(buildPageHeaders(page));
            response.getHeaders().addAll(HttpHeaders.LINK, buildLinkHeaders(request.getURI(), page));
        }
        return page;
    }

    private Map<String, String> buildPageHeaders(final Page<?> page) {
        return Map.of(
                pageSizeHeader, String.valueOf(page.getSize()),
                pageSortHeader, String.valueOf(page.getSort()),
                pageIndexHeader, String.valueOf(page.getNumber() + indexOffset),
                pageTotalHeader, String.valueOf(page.getTotalPages()),
                pageElementsHeader, String.valueOf(page.getTotalElements())
        );
    }

    private List<String> buildLinkHeaders(final URI uri, final Page<?> page) {
        final String uriString = generateTokenizedPageUri(uri);
        final List<String> links = new ArrayList<>();

        links.add(buildLinkHeader(uriString, page.getNumber() + indexOffset, "self"));
        if (!page.isFirst()) {
            links.add(buildLinkHeader(uriString, indexOffset, "first"));
        }
        if (page.hasNext()) {
            links.add(buildLinkHeader(uriString, page.getNumber() + 1 + indexOffset, "next"));
        }
        if (page.hasPrevious()) {
            links.add(buildLinkHeader(uriString, page.getNumber() - 1 + indexOffset, "prev"));
        }
        if (!page.isLast()) {
            links.add(buildLinkHeader(uriString, page.getTotalPages() - 1 + indexOffset, "last"));
        }
        return links;
    }

    private String generateTokenizedPageUri(final URI uri) {
        final String uriString = uri.toString();
        if (!uriString.contains("?")) { // No Query Parameters, add page token first
            return uriString + "?" + pageQueryReplaceToken;
        } else if (uri.getQuery().contains(pageParameter)) { // Replace 'page=\d+' parameter with page token
            return uriString.replaceFirst("" + pageParameter + "\\d+", "" + pageQueryReplaceToken);
        } else { // No Page Parameter, add page token last
            return uriString + "&" + pageQueryReplaceToken;
        }
    }

    private String buildLinkHeader(final String uri, final int pageNumber, final String rel) {
        return "<" + PAGE_NUMBER_REPLACE_PATTERN.matcher(uri).replaceFirst(String.valueOf(pageNumber)) + ">; rel=\"" + rel + "\"";
    }
}
