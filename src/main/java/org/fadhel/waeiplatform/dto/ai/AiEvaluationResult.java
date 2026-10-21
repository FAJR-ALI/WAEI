package org.fadhel.waeiplatform.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fadhel.waeiplatform.dto.out.StepFeedback;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiEvaluationResult {

    // false when the video does not show this training at all
    private Boolean relevant;

    // Why the video was rejected (only when relevant = false)
    private String rejectionReason;

    private Double score;

    private String summary;

    private List<StepFeedback> correctSteps;

    private List<StepFeedback> mistakes;

    private List<String> recommendations;

    private String safetyNote;

    // 3 questions about the trainee's mistakes
    private List<AiQuizQuestion> quiz;
}
