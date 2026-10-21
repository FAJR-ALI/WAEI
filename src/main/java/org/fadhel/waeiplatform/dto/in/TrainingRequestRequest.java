package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrainingRequestRequest {

    @NotNull
    @Positive
    private Long trainingProgramId;

    /**
     * Optional. Ignored for organization administrators and derived from their token.
     * Only the platform administrator may create a request on behalf of another organization.
     */
    private Long beneficiaryOrganizationId;

    /**
     * Optional. Ignored for organization administrators and derived from their token.
     * Only the platform administrator may create a request on behalf of another account.
     */
    private Long createdByAccountId;

    @NotNull
    @Positive
    private Integer participantCount;

    @NotNull
    @Future(message = "The training start date must be in the future")
    private LocalDateTime scheduledStart;

    @NotNull
    @Future(message = "The training end date must be in the future")
    private LocalDateTime scheduledEnd;

    @Size(max = 5000)
    private String notes;

}
