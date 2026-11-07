package br.com.ofisy.ms_ofisy_billing.adapters.gateways.servicecatalog;

import br.com.ofisy.ms_ofisy_billing.adapters.gateways.servicecatalog.dto.ServiceCatalogResponse;
import br.com.ofisy.ms_ofisy_billing.application.ports.ServiceCatalogClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceCatalogClientImpl implements ServiceCatalogClient {

    private final RestClient coreRestClient;

    @Override
    public Optional<ServiceCatalogView> findById(UUID serviceCatalogId) {
        try {
            ServiceCatalogResponse response = coreRestClient.get()
                    .uri("/api/v1/services-catalog/{id}", serviceCatalogId)
                    .retrieve()
                    .body(ServiceCatalogResponse.class);

            if (response == null) {
                return Optional.empty();
            }
            return Optional.of(new ServiceCatalogView(response.id(), response.name(), response.price()));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Falha ao consultar o catálogo de serviços {} no core", serviceCatalogId, e);
            return Optional.empty();
        }
    }
}
