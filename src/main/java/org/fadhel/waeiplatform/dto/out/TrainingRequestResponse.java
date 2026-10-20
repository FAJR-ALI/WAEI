package org.fadhel.waeiplatform.dto.out;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrainingRequestResponse {

    private Long id;

    private Integer participantCount;

    private String notes;

    private String status;

    private LocalDateTime requestedAt;

    private Long trainingProgramId;

    private String trainingProgramTitle;

    private Long beneficiaryOrganizationId;

    private String beneficiaryOrganizationName;

    private Long createdByAccountId;

    private String createdByFullName;

    private Set<PaymentResponse> payments;

}
