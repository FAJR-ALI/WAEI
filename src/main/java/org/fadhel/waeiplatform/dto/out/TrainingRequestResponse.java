package org.fadhel.waeiplatform.dto.out;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrainingRequestResponse {

    private Long id;

    private Integer participantCount;

    private BigDecimal programPrice;

    private String notes;

    private String status;

    private LocalDateTime requestedAt;

    private LocalDateTime scheduledStart;

    private LocalDateTime scheduledEnd;

    private LocalDateTime acceptedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private String cancellationReason;

    private Long trainingProgramId;

    private String trainingProgramTitle;

    private Long providerOrganizationId;

    private String providerOrganizationName;

    private Long beneficiaryOrganizationId;

    private String beneficiaryOrganizationName;

    private Long createdByAccountId;

    private String createdByFullName;

    private Set<PaymentResponse> payments;

}
