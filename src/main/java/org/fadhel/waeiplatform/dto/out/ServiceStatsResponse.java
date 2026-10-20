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

    private String message;
}

