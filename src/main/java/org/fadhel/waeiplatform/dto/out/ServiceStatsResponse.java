package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceStatsResponse {

    private Long awarenessServiceId;

    private String awarenessServiceTitle;

    private Integer attemptsCount;

    private Double averageScore;

    // Average of correct quiz answers out of 3
    private Double averageQuizScore;

    private String message;
}
