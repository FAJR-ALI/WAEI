package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// What the user sees right after uploading the video
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoEvaluationResponse {

    private Long sessionId;

    private Long awarenessServiceId;

    private String awarenessServiceTitle;

    // CORRECT / PARTIALLY_CORRECT / INCORRECT
    private String verdict;

    private Double score;

    private String summary;

    // Right steps, each with the second it happened in the video
    private List<StepFeedback> correctSteps;

    // Wrong steps, each with the second it happened in the video
    private List<StepFeedback> mistakes;

    private List<String> recommendations;

    private String safetyNote;

    // Official video that shows the correct way
    private String officialVideoUrl;

    // 3 questions about the trainee's mistakes (answers are checked by /quiz/submit)
    private List<QuizQuestionResponse> quiz;
}
