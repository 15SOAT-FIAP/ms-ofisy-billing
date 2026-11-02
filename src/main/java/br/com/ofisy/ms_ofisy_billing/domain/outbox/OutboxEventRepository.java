package br.com.ofisy.ms_ofisy_billing.domain.outbox;

public interface OutboxEventRepository {

    void save(OutboxEvent event);
}
