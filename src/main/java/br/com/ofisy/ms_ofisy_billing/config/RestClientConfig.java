package br.com.ofisy.ms_ofisy_billing.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final CoreClientProperties coreClientProperties;

    @Bean
    public RestClient coreRestClient() {
        return RestClient.builder()
                .baseUrl(coreClientProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + coreClientProperties.getServiceToken())
                .build();
    }
}
