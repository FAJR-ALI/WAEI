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

    private List<String> correctSteps;

    private List<String> mistakes;

    private List<String> recommendations;

    private String safetyNote;
}

