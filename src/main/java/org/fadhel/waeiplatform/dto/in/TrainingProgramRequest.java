package org.fadhel.waeiplatform.dto.in;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrainingProgramRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 100)
    private String category;

    @NotBlank
    @Size(max = 5000)
    private String description;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @NotBlank
    @Pattern(regexp = "^(IN_PERSON|ONLINE|HYBRID)$", message = "Invalid delivery mode")
    private String deliveryMode;

    private Boolean isActive;

}
