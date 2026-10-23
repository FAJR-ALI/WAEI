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
    @DecimalMin(value = "1.00", message = "Price must be at least 1 SAR")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @NotNull
    @DecimalMin(value = "0.5", message = "Program hours per day must be at least 0.5")
    @DecimalMax(value = "24.0", message = "Program hours per day must not exceed 24")
    @Digits(integer = 2, fraction = 1)
    private BigDecimal hoursPerDay;

    @NotNull
    @Min(value = 1, message = "Maximum days must be at least 1")
    @Max(value = 365, message = "Maximum days must not exceed 365")
    private Integer maxDays;

    @NotBlank
    @Pattern(regexp = "^(IN_PERSON|ONLINE)$", message = "Invalid delivery mode")
    private String deliveryMode;

    /**
     * Optional. Ignored for organization administrators and derived from their token.
     * Only the platform administrator may create a program for another organization.
     */
    private Long organizationId;

    private Boolean isActive;

}
