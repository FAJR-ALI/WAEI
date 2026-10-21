package org.fadhel.waeiplatform.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MoyasarClient {

    private final RestClient restClient;
    private final String secretKey;
    private final String successUrl;
    private final String backUrl;

    public MoyasarClient(@Value("${moyasar.api.base-url}") String baseUrl,
                         @Value("${moyasar.api.secret-key:}") String secretKey,
                         @Value("${moyasar.checkout.success-url}") String successUrl,
                         @Value("${moyasar.checkout.back-url}") String backUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.secretKey = secretKey;
        this.successUrl = successUrl;
        this.backUrl = backUrl;
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    public MoyasarInvoice createInvoice(BigDecimal amount, String description, Long trainingRequestId) {
        requireConfigured();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("training_request_id", String.valueOf(trainingRequestId));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", toHalalas(amount));
        body.put("currency", "SAR");
        body.put("description", description);
        body.put("success_url", successUrl);
        body.put("back_url", backUrl);
        body.put("metadata", metadata);

        try {
            return restClient.post()
                    .uri("/invoices")
                    .headers(headers -> headers.setBasicAuth(secretKey, ""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MoyasarInvoice.class);
        } catch (RestClientResponseException e) {
            throw new ApiException("Moyasar rejected the payment request: " + e.getResponseBodyAsString());
        }
    }

    public MoyasarPayment fetchPayment(String paymentId) {
        requireConfigured();

        try {
            return restClient.get()
                    .uri("/payments/{id}", paymentId)
                    .headers(headers -> headers.setBasicAuth(secretKey, ""))
                    .retrieve()
                    .body(MoyasarPayment.class);
        } catch (RestClientResponseException e) {
            throw new ApiException("Could not fetch the payment from Moyasar: " + e.getResponseBodyAsString());
        }
    }

    public MoyasarInvoice fetchInvoice(String invoiceId) {
        requireConfigured();

        try {
            return restClient.get()
                    .uri("/invoices/{id}", invoiceId)
                    .headers(headers -> headers.setBasicAuth(secretKey, ""))
                    .retrieve()
                    .body(MoyasarInvoice.class);
        } catch (RestClientResponseException e) {
            throw new ApiException("Could not fetch the invoice from Moyasar: " + e.getResponseBodyAsString());
        }
    }

    public MoyasarPayment refundPayment(String paymentId) {
        requireConfigured();

        try {
            return restClient.post()
                    .uri("/payments/{id}/refund", paymentId)
                    .headers(headers -> headers.setBasicAuth(secretKey, ""))
                    .retrieve()
                    .body(MoyasarPayment.class);
        } catch (RestClientResponseException e) {
            throw new ApiException("Moyasar rejected the refund: " + e.getResponseBodyAsString());
        }
    }

    public MoyasarInvoice cancelInvoice(String invoiceId) {
        requireConfigured();

        try {
            return restClient.post()
                    .uri("/invoices/{id}/cancel", invoiceId)
                    .headers(headers -> headers.setBasicAuth(secretKey, ""))
                    .retrieve()
                    .body(MoyasarInvoice.class);
        } catch (RestClientResponseException e) {
            throw new ApiException("Moyasar rejected the invoice cancellation: " + e.getResponseBodyAsString());
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new ApiException("Moyasar is not configured. Set MOYASAR_SECRET_KEY to a test or live secret key.");
        }
    }

    public static int toHalalas(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MoyasarInvoice(
            String id,
            String status,
            Integer amount,
            String currency,
            String url,
            @JsonProperty("created_at") String createdAt,
            List<MoyasarPayment> payments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MoyasarPayment(
            String id,
            String status,
            Integer amount,
            String currency,
            @JsonProperty("invoice_id") String invoiceId,
            Integer refunded,
            @JsonProperty("refunded_at") String refundedAt,
            @JsonProperty("created_at") String createdAt,
            Map<String, Object> metadata) {

        public BigDecimal refundedAmount() {
            return refunded != null ? BigDecimal.valueOf(refunded).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }

        public LocalDateTime refundedAtAsDateTime() {
            if (refundedAt == null) {
                return null;
            }
            try {
                return LocalDateTime.parse(refundedAt, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception ignored) {
                return LocalDateTime.now();
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MoyasarWebhook(
            String id,
            String type,
            @JsonProperty("secret_token") String secretToken,
            Boolean live,
            MoyasarPayment data) {
    }
}
