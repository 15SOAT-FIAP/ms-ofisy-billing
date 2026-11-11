package br.com.ofisy.ms_ofisy_billing.adapters.controllers.webhook;

import br.com.ofisy.ms_ofisy_billing.config.MercadoPagoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class MercadoPagoSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final MercadoPagoProperties properties;

    public boolean isValid(String signature, String requestId, String dataId) {
        if (signature == null || properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            return false;
        }

        String ts = null;
        String hash = null;
        for (String part : signature.split(",")) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length != 2) {
                continue;
            }
            String key = keyValue[0].trim();
            if ("ts".equals(key)) {
                ts = keyValue[1].trim();
            } else if ("v1".equals(key)) {
                hash = keyValue[1].trim();
            }
        }
        if (ts == null || hash == null) {
            return false;
        }

        String expected = hmac(manifest(dataId, requestId, ts));
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8));
    }

    private String manifest(String dataId, String requestId, String ts) {
        StringBuilder manifest = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifest.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
        }
        if (requestId != null && !requestId.isBlank()) {
            manifest.append("request-id:").append(requestId).append(';');
        }
        return manifest.append("ts:").append(ts).append(';').toString();
    }

    private String hmac(String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível calcular a assinatura do webhook", e);
        }
    }
}