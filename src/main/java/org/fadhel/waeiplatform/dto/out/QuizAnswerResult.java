package org.fadhel.waeiplatform.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuizAnswerResult {

    private Long questionId;

    private String question;

    private String yourAnswer;

    private String correctAnswer;

    private Boolean isCorrect;

    private String explanation;
}
