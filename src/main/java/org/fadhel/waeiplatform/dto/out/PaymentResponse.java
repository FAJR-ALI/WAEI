package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;

    private BigDecimal amount;

    private String currency;

    private String status;

    private String moyasarPaymentId;

    private String moyasarInvoiceId;

    private String checkoutUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime paidAt;

    private BigDecimal refundedAmount;

    private LocalDateTime refundedAt;

    private Long trainingRequestId;

    private BigDecimal commissionRatePercent;

    private BigDecimal commissionAmount;

    private BigDecimal providerAmount;

}
