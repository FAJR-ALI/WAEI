package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuizResultResponse {

    private Long sessionId;

    private Integer correctCount;

    private Integer totalQuestions;

    private String message;

    private List<QuizAnswerResult> answers;
}
