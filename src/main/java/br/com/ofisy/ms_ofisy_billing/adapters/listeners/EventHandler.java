package br.com.ofisy.ms_ofisy_billing.adapters.listeners;

import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;

public interface EventHandler {

    String type();

    void handle(EventEnvelope envelope);
}
