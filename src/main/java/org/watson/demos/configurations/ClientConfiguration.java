package org.watson.demos.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class ClientConfiguration {
    @Bean
    public WebClient worldtimeClient(final WebClient.Builder builder,
                                     @Value("${worldtime.api.base.url:https://worldtimeapi.org/api/}") final String baseUrl) {
        return builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE, "text/yaml;charset=utf-8")
                .build();
    }
}
