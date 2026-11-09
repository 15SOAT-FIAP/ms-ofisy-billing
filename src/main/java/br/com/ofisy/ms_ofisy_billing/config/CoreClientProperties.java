package br.com.ofisy.ms_ofisy_billing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "core")
public class CoreClientProperties {

    private String baseUrl;
    private String serviceToken;
}
