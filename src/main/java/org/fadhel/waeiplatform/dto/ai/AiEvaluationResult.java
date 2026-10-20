package org.fadhel.waeiplatform.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiEvaluationResult {

    // false when the video does not show this scenario at all
    private Boolean relevant;

    private Double score;

    private String summary;

    private List<String> correctSteps;

    private List<String> mistakes;

    private List<String> recommendations;

    private String safetyNote;
}
