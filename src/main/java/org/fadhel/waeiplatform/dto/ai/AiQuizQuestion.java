package org.fadhel.waeiplatform.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiQuizQuestion {

    private String question;

    private String optionA;

    private String optionB;

    private String optionC;

    // A, B or C
    private String correctAnswer;

    private String explanation;
}
