package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|SUSPENDED|PENDING", message = "Status must be ACTIVE, SUSPENDED or PENDING")
    private String status;
}
