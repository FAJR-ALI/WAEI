package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentUpdateRequest {

    @NotBlank
    @Pattern(regexp = "^(PENDING|PAID|FAILED|CANCELLED)$", message = "Invalid status")
    private String status;

    @Size(max = 255)
    private String moyasarPaymentId;

}
