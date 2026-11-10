package br.com.ofisy.ms_ofisy_billing.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    public static final String BEARER = "Bearer ";
    private static final Duration MERCADO_PAGO_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration MERCADO_PAGO_READ_TIMEOUT = Duration.ofSeconds(10);
    private final CoreClientProperties coreClientProperties;
    private final StockClientProperties stockClientProperties;
    private final MercadoPagoProperties mercadoPagoProperties;

    @Bean
    public RestClient coreRestClient() {
        return RestClient.builder()
                .baseUrl(coreClientProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, BEARER + coreClientProperties.getServiceToken())
                .build();
    }

    @Bean
    public RestClient stockRestClient() {
        return RestClient.builder()
                .baseUrl(stockClientProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, BEARER + stockClientProperties.getServiceToken())
                .build();
    }

    @Bean
    public RestClient mercadoPagoRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(MERCADO_PAGO_CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(MERCADO_PAGO_READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(mercadoPagoProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, BEARER + mercadoPagoProperties.getAccessToken())
                .build();
    }
}
