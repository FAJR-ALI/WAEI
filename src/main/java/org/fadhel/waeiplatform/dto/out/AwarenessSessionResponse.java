package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// No personal data here: only the evaluation result
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AwarenessSessionResponse {

    private Long id;

    private Long awarenessServiceId;

    private String awarenessServiceTitle;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private Double score;

    private String resultSummary;

    private String recommendations;

    private Integer quizScore;

    private Boolean consentGiven;
}
