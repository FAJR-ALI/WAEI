package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrainingProgramResponse {

    private Long id;

    private String title;

    private String category;

    private String description;

    private BigDecimal price;

    private String deliveryMode;

    private Boolean isActive;

    private Long organizationId;

    private String organizationName;

    private BigDecimal hoursPerDay;

    private Integer maxDays;

    private String imageUrl;

}
