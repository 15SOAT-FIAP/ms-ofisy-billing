package br.com.ofisy.ms_ofisy_billing.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private List<String> publicPaths = List.of();
}
