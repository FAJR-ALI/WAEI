package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrainingRequestRequest {

    @NotNull
    @Positive
    private Long trainingProgramId;

    @NotNull
    @Positive
    private Long beneficiaryOrganizationId;

    @NotNull
    @Positive
    private Long createdByAccountId;

    @NotNull
    @Positive
    private Integer participantCount;

    @Size(max = 5000)
    private String notes;
}
