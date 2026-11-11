package br.com.ofisy.ms_ofisy_billing.adapters.controllers.webhook;

import br.com.ofisy.ms_ofisy_billing.config.MercadoPagoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoSignatureValidatorTest {

    private static final String DATA_ID = "ORD01M28P44G5FG8RJPM579EH56FV";
    private static final String REQUEST_ID = "2066ca19-c6f1-498a-be75-1923005edd06";
    private static final String TS = "1742505638683";
    private static final String HASH_WITH_REQUEST_ID =
            "8f54105e95d7d7e0ae280242d47b9ed27dd4699aa4b6de41823e462e6859e9fe";
    private static final String HASH_WITHOUT_REQUEST_ID =
            "c5737994d268bf076def29939079fce63e89baabdbc8b28f399141747767f671";
    private static final String HASH_WITHOUT_DATA_ID =
            "85cceb9f10129f39986add79816c00c63234bb5c26f86ec13f48da3aecd437aa";

    private final MercadoPagoProperties properties = new MercadoPagoProperties();
    private MercadoPagoSignatureValidator validator;

    @BeforeEach
    void setUp() {
        properties.setWebhookSecret("test-secret");
        validator = new MercadoPagoSignatureValidator(properties);
    }

    @Test
    void shouldAcceptASignatureComputedWithTheDataIdInLowercase() {
        assertThat(validator.isValid(signature(HASH_WITH_REQUEST_ID), REQUEST_ID, DATA_ID)).isTrue();
    }

    @Test
    void shouldAcceptASignatureWithSpacesAroundTheParts() {
        assertThat(validator.isValid(" ts=" + TS + " , v1=" + HASH_WITH_REQUEST_ID + " ", REQUEST_ID, DATA_ID))
                .isTrue();
    }

    @Test
    void shouldLeaveTheRequestIdOutOfTheManifestWhenItIsMissing() {
        assertThat(validator.isValid(signature(HASH_WITHOUT_REQUEST_ID), null, DATA_ID)).isTrue();
    }

    @Test
    void shouldLeaveTheDataIdOutOfTheManifestWhenItIsMissing() {
        assertThat(validator.isValid(signature(HASH_WITHOUT_DATA_ID), REQUEST_ID, null)).isTrue();
    }

    @Test
    void shouldRejectWhenTheDataIdWasTampered() {
        assertThat(validator.isValid(signature(HASH_WITH_REQUEST_ID), REQUEST_ID, "ORD-OTHER")).isFalse();
    }

    @Test
    void shouldRejectWhenTheSecretIsDifferent() {
        properties.setWebhookSecret("other-secret");

        assertThat(validator.isValid(signature(HASH_WITH_REQUEST_ID), REQUEST_ID, DATA_ID)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void shouldRejectWhenTheSecretIsNotConfigured(String secret) {
        properties.setWebhookSecret(secret);

        assertThat(validator.isValid(signature(HASH_WITH_REQUEST_ID), REQUEST_ID, DATA_ID)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "garbage", "ts=" + TS, "v1=" + HASH_WITH_REQUEST_ID})
    void shouldRejectAMissingOrIncompleteSignature(String signature) {
        assertThat(validator.isValid(signature, REQUEST_ID, DATA_ID)).isFalse();
    }

    private String signature(String hash) {
        return "ts=" + TS + ",v1=" + hash;
    }
}