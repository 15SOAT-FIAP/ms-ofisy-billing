package br.com.ofisy.ms_ofisy_billing;

import br.com.ofisy.ms_ofisy_billing.config.CoreClientProperties;
import br.com.ofisy.ms_ofisy_billing.config.MercadoPagoProperties;
import br.com.ofisy.ms_ofisy_billing.config.jwt.JwtProperties;
import br.com.ofisy.ms_ofisy_billing.config.StockClientProperties;
import br.com.ofisy.ms_ofisy_billing.config.security.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({SecurityProperties.class, JwtProperties.class, CoreClientProperties.class,
        StockClientProperties.class, MercadoPagoProperties.class})
public class MsOfisyBillingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsOfisyBillingApplication.class, args);
    }
}
