package br.com.ofisy.ms_ofisy_billing.application.payment.sync;

public interface SyncPaymentUseCase {

    void execute(String externalId);
}