package br.com.ofisy.ms_ofisy_billing.adapters.gateways.stock;

import br.com.ofisy.ms_ofisy_billing.adapters.gateways.stock.dto.StockResponse;
import br.com.ofisy.ms_ofisy_billing.application.ports.StockClient;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockUnavailableException;
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
public class StockClientImpl implements StockClient {

    private final RestClient stockRestClient;

    @Override
    public Optional<StockView> findById(UUID stockId) {
        try {
            StockResponse response = stockRestClient.get()
                    .uri("/api/v1/stocks/{id}", stockId)
                    .retrieve()
                    .body(StockResponse.class);

            if (response == null) {
                return Optional.empty();
            }
            return Optional.of(new StockView(response.id(), response.productName(), response.unitPrice()));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Falha ao consultar o item de estoque {} no ms-ofisy-stock", stockId, e);
            throw new StockUnavailableException(stockId, e);
        }
    }
}
