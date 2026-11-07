package br.com.ofisy.ms_ofisy_billing;

import br.com.ofisy.ms_ofisy_billing.config.CoreClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CoreClientProperties.class)
public class MsOfisyBillingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsOfisyBillingApplication.class, args);
    }
}
