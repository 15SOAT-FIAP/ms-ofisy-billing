package br.com.ofisy.ms_ofisy_billing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mercadopago")
public class MercadoPagoProperties {

    private String baseUrl;
    private String accessToken;
    private String expirationTime;
    private boolean retriesAllowed;
    private String webhookSecret;
}
