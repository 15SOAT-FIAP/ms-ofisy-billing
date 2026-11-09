package br.com.ofisy.ms_ofisy_billing;

import br.com.ofisy.ms_ofisy_billing.shared.jwt.JwtTestTokens;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    }

    @Bean
    @ServiceConnection
    LocalStackContainer localStackContainer() {
        return new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.13.1"))
                .withServices("sns", "sqs")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("localstack/init/ready.d/01-messaging.sh", 0755),
                        "/etc/localstack/init/ready.d/01-messaging.sh")
                .waitingFor(Wait.forHttp("/_localstack/init/ready")
                        .forPort(4566)
                        .forResponsePredicate(body -> body.contains("\"completed\": true")));
    }

    @Bean
    DynamicPropertyRegistrar applicationProperties() {
        return registry -> {
            registry.add("jwt.secret", () -> JwtTestTokens.SECRET);
            registry.add("core.base-url", () -> "http://localhost:8080");
            registry.add("core.service-token", () -> "test-token");
            registry.add("stock.base-url", () -> "http://localhost:8082");
            registry.add("stock.service-token", () -> "test-token");
        };
    }
}
